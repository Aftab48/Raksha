package com.example.raksha.feature_trusted_contacts.data.remote.mapper

import com.example.raksha.feature_trusted_contacts.data.local.entity.TrustedContactEntity
import com.example.raksha.feature_trusted_contacts.data.remote.dto.TrustedContactDto
import com.example.raksha.feature_trusted_contacts.domain.model.TrustedContact

fun TrustedContactDto.toEntity(): TrustedContactEntity {
    return TrustedContactEntity(
        name = name,
        phoneNumber = phoneNumber,
        priority = priority
    )
}

fun TrustedContactEntity.toDomain(): TrustedContact {
    return TrustedContact(
        name = name,
        phoneNumber = phoneNumber,
        priority = priority
    )
}
