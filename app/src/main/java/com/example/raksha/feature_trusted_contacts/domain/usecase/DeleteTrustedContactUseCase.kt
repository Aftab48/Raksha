package com.example.raksha.feature_trusted_contacts.domain.usecase

import com.example.raksha.feature_trusted_contacts.domain.repository.TrustedContactsRepository
import javax.inject.Inject

class DeleteTrustedContactUseCase @Inject constructor(
    private val repository: TrustedContactsRepository
) {
    suspend operator fun invoke(phoneNumber: String) =
        repository.deleteTrustedContact(phoneNumber)
}
