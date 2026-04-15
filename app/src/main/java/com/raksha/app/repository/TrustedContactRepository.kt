package com.raksha.app.repository

import com.google.gson.Gson
import com.raksha.app.data.local.dao.TrustedContactDao
import com.raksha.app.data.local.entity.TrustedContactEntity
import com.raksha.app.feature_login_register.data.local.SessionManager
import com.raksha.app.feature_login_register.data.remote.dto.ApiErrorResponseDto
import com.raksha.app.feature_trusted_contacts.data.remote.api.UserTrustedContactsAPI
import com.raksha.app.feature_trusted_contacts.data.remote.dto.SaveTrustedContactRequestDto
import com.raksha.app.feature_trusted_contacts.data.remote.dto.TrustedContactDto
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@Singleton
class TrustedContactRepository @Inject constructor(
    private val contactDao: TrustedContactDao,
    private val trustedContactsApi: UserTrustedContactsAPI,
    private val sessionManager: SessionManager,
    private val gson: Gson
) {
    val contacts: Flow<List<TrustedContactEntity>> = contactDao.getContacts()

    suspend fun getContactsOnce(): List<TrustedContactEntity> = contactDao.getContactsOnce()

    suspend fun getContactCount(): Int = contactDao.getContactCount()

    suspend fun refreshContactsFromRemote(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isAuthenticated()) return@withContext Result.success(Unit)

        try {
            val remoteContacts = trustedContactsApi.getTrustedContacts().trustedContacts
            contactDao.replaceContacts(remoteContacts.toLocalEntities())
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(mapException(exception))
        }
    }

    suspend fun addContact(name: String, phone: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        val sanitizedPhone = sanitizePhone(phone)

        if (trimmedName.isBlank() || sanitizedPhone.isBlank()) {
            return@withContext Result.failure(Exception("Enter a name and phone number"))
        }

        val localContacts = contactDao.getContactsOnce()
        if (localContacts.size >= MAX_CONTACTS) {
            return@withContext Result.success(false)
        }
        if (localContacts.any { sanitizePhone(it.phone) == sanitizedPhone }) {
            return@withContext Result.failure(Exception("Trusted contact with this phone number already exists"))
        }

        contactDao.insertContact(TrustedContactEntity(name = trimmedName, phone = sanitizedPhone))

        if (!isAuthenticated()) {
            return@withContext Result.success(true)
        }

        try {
            val remoteContacts = trustedContactsApi.saveTrustedContact(
                SaveTrustedContactRequestDto(
                    name = trimmedName,
                    phoneNumber = sanitizedPhone
                )
            ).trustedContacts
            contactDao.replaceContacts(remoteContacts.toLocalEntities())
            Result.success(true)
        } catch (exception: Exception) {
            Result.failure(
                Exception(
                    "Saved locally, but couldn't sync to the server. ${mapException(exception).message.orEmpty()}".trim()
                )
            )
        }
    }

    suspend fun deleteContact(contact: TrustedContactEntity): Result<Unit> = withContext(Dispatchers.IO) {
        contactDao.deleteContact(contact)

        if (!isAuthenticated()) {
            return@withContext Result.success(Unit)
        }

        try {
            val remoteContacts = trustedContactsApi.deleteTrustedContact(contact.phone).trustedContacts
            contactDao.replaceContacts(remoteContacts.toLocalEntities())
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(
                Exception(
                    "Removed locally, but couldn't sync to the server. ${mapException(exception).message.orEmpty()}".trim()
                )
            )
        }
    }

    suspend fun deleteContactById(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val localContacts = contactDao.getContactsOnce()
        val target = localContacts.firstOrNull { it.id == id }
            ?: return@withContext Result.failure(Exception("Trusted contact not found"))
        deleteContact(target)
    }

    private suspend fun isAuthenticated(): Boolean {
        return !sessionManager.authToken.first().isNullOrBlank()
    }

    private fun List<TrustedContactDto>.toLocalEntities(): List<TrustedContactEntity> {
        return sortedBy { it.priority }.map {
            TrustedContactEntity(
                name = it.name.trim(),
                phone = sanitizePhone(it.phoneNumber)
            )
        }
    }

    private fun sanitizePhone(phone: String): String {
        return phone.trim().replace(" ", "").replace("-", "")
    }

    private fun mapException(exception: Exception): Exception {
        return when (exception) {
            is HttpException -> {
                val errorBody = exception.response()?.errorBody()?.string()
                val apiMessage = errorBody?.let {
                    runCatching { gson.fromJson(it, ApiErrorResponseDto::class.java) }.getOrNull()
                }?.msg
                Exception(apiMessage ?: "Server request failed")
            }

            is IOException -> Exception("You're offline. Raksha kept trusted contacts saved locally on device.")
            else -> exception
        }
    }

    companion object {
        private const val MAX_CONTACTS = 5
    }
}
