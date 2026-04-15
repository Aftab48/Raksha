package com.example.raksha.feature_trusted_contacts.data.remote.dto

data class SaveTrustedContactRequestDto(
    val name: String,
    val phoneNumber: String
)

data class TrustedContactDto(
    val name: String,
    val phoneNumber: String,
    val priority: Int
)

data class TrustedContactsResponseDto(
    val message: String? = null,
    val trustedContacts: List<TrustedContactDto> = emptyList()
)
