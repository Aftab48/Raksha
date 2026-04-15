package com.example.raksha.feature_trusted_contacts.data.repository

import com.example.raksha.feature_login_register.data.remote.dto.ApiErrorResponseDto
import com.example.raksha.feature_trusted_contacts.data.local.cache.TrustedContactsLocalCache
import com.example.raksha.feature_trusted_contacts.data.local.dao.TrustedContactsDao
import com.example.raksha.feature_trusted_contacts.data.local.entity.TrustedContactEntity
import com.example.raksha.feature_trusted_contacts.data.remote.api.UserTrustedContactsAPI
import com.example.raksha.feature_trusted_contacts.data.remote.dto.SaveTrustedContactRequestDto
import com.example.raksha.feature_trusted_contacts.data.remote.mapper.toDomain
import com.example.raksha.feature_trusted_contacts.data.remote.mapper.toEntity
import com.example.raksha.feature_trusted_contacts.domain.model.TrustedContact
import com.example.raksha.feature_trusted_contacts.domain.repository.TrustedContactsRepository
import com.google.gson.Gson
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class TrustedContactsRepositoryImpl @Inject constructor(
    private val api: UserTrustedContactsAPI,
    private val dao: TrustedContactsDao,
    private val cache: TrustedContactsLocalCache,
    private val gson: Gson
) : TrustedContactsRepository {

    override fun observeTrustedContacts(): Flow<List<TrustedContact>> {
        return dao.observeTrustedContacts().map { contacts ->
            contacts.map { it.toDomain() }
        }
    }

    override suspend fun refreshTrustedContacts(): Result<Unit> = withContext(Dispatchers.IO) {
        val localContacts = dao.getTrustedContacts()
        if (localContacts.isEmpty()) {
            val cachedContacts = cache.getContacts()
            if (cachedContacts.isNotEmpty()) {
                persistContacts(cachedContacts)
            }
        } else {
            cache.saveContacts(localContacts)
        }

        try {
            val remoteContacts = api.getTrustedContacts().trustedContacts.map { it.toEntity() }
            persistContacts(remoteContacts)
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(mapException(exception))
        }
    }

    override suspend fun addTrustedContact(
        name: String,
        phoneNumber: String
    ): Result<List<TrustedContact>> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        val sanitizedPhoneNumber = sanitizePhoneNumber(phoneNumber)
        val currentContacts = dao.getTrustedContacts()

        when {
            trimmedName.isBlank() || sanitizedPhoneNumber.isBlank() -> {
                return@withContext Result.failure(Exception("Enter a name and phone number"))
            }

            currentContacts.size >= MAX_CONTACTS -> {
                return@withContext Result.failure(Exception("You can only add up to $MAX_CONTACTS trusted contacts"))
            }

            currentContacts.any { sanitizePhoneNumber(it.phoneNumber) == sanitizedPhoneNumber } -> {
                return@withContext Result.failure(Exception("Trusted contact with this phone number already exists"))
            }
        }

        val updatedContacts = reindexContacts(
            currentContacts + TrustedContactEntity(
                name = trimmedName,
                phoneNumber = sanitizedPhoneNumber,
                priority = currentContacts.size + 1
            )
        )
        persistContacts(updatedContacts)

        try {
            val remoteContacts = api.saveTrustedContact(
                SaveTrustedContactRequestDto(
                    name = trimmedName,
                    phoneNumber = sanitizedPhoneNumber
                )
            ).trustedContacts.map { it.toEntity() }
            persistContacts(remoteContacts)
            Result.success(remoteContacts.map { it.toDomain() })
        } catch (exception: Exception) {
            Result.failure(
                Exception(
                    "Saved locally, but couldn't sync to the server. ${mapException(exception).message.orEmpty()}".trim()
                )
            )
        }
    }

    override suspend fun deleteTrustedContact(phoneNumber: String): Result<List<TrustedContact>> =
        withContext(Dispatchers.IO) {
            val sanitizedPhoneNumber = sanitizePhoneNumber(phoneNumber)
            val currentContacts = dao.getTrustedContacts()
            val existingContact = currentContacts.firstOrNull {
                sanitizePhoneNumber(it.phoneNumber) == sanitizedPhoneNumber
            } ?: return@withContext Result.failure(Exception("Trusted contact not found"))

            val updatedContacts = reindexContacts(
                currentContacts.filterNot { it.phoneNumber == existingContact.phoneNumber }
            )
            persistContacts(updatedContacts)

            try {
                val remoteContacts = api.deleteTrustedContact(existingContact.phoneNumber)
                    .trustedContacts
                    .map { it.toEntity() }
                persistContacts(remoteContacts)
                Result.success(remoteContacts.map { it.toDomain() })
            } catch (exception: Exception) {
                Result.failure(
                    Exception(
                        "Removed locally, but couldn't sync to the server. ${mapException(exception).message.orEmpty()}".trim()
                    )
                )
            }
        }

    private suspend fun persistContacts(contacts: List<TrustedContactEntity>) {
        val normalizedContacts = reindexContacts(contacts)
        dao.replaceTrustedContacts(normalizedContacts)
        cache.saveContacts(normalizedContacts)
    }

    private fun reindexContacts(contacts: List<TrustedContactEntity>): List<TrustedContactEntity> {
        return contacts.mapIndexed { index, contact ->
            contact.copy(priority = index + 1)
        }
    }

    private fun sanitizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.trim().replace(" ", "").replace("-", "")
    }

    private fun mapException(exception: Exception): Exception {
        return when (exception) {
            is HttpException -> {
                val errorBody = exception.response()?.errorBody()?.string()
                val apiMessage = errorBody?.let {
                    runCatching {
                        gson.fromJson(it, ApiErrorResponseDto::class.java)
                    }.getOrNull()
                }?.msg
                Exception(apiMessage ?: "Server request failed")
            }

            is IOException -> Exception("You're offline right now. Raksha kept your local contact data safely on device.")
            else -> exception
        }
    }

    companion object {
        private const val MAX_CONTACTS = 5
    }
}
