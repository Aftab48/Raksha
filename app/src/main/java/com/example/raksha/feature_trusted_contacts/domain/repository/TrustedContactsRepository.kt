package com.example.raksha.feature_trusted_contacts.domain.repository

import com.example.raksha.feature_trusted_contacts.domain.model.TrustedContact
import kotlinx.coroutines.flow.Flow

interface TrustedContactsRepository {
    fun observeTrustedContacts(): Flow<List<TrustedContact>>
    suspend fun refreshTrustedContacts(): Result<Unit>
    suspend fun addTrustedContact(name: String, phoneNumber: String): Result<List<TrustedContact>>
    suspend fun deleteTrustedContact(phoneNumber: String): Result<List<TrustedContact>>
}
