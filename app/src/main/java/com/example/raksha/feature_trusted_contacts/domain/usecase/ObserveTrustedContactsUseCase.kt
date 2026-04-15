package com.example.raksha.feature_trusted_contacts.domain.usecase

import com.example.raksha.feature_trusted_contacts.domain.repository.TrustedContactsRepository
import javax.inject.Inject

class ObserveTrustedContactsUseCase @Inject constructor(
    private val repository: TrustedContactsRepository
) {
    operator fun invoke() = repository.observeTrustedContacts()
}
