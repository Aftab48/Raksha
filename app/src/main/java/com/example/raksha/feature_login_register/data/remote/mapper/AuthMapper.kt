package com.example.raksha.feature_login_register.data.remote.mapper

import com.example.raksha.feature_login_register.data.remote.dto.RegisterInitResponseDto
import com.example.raksha.feature_login_register.domain.model.AuthResult
import com.example.raksha.feature_login_register.domain.model.PendingRegistration

fun RegisterInitResponseDto.toDomain(): PendingRegistration {
    return PendingRegistration(
        msg = message,
        registrationId = registrationId,
        expiresAt = expiresAt,
        maskedEmail = email,
        maskedPhoneNumber = phoneNumber
    )
}

fun String.toAuthResult(message: String? = null): AuthResult {
    return AuthResult(
        token = this,
        msg = message
    )
}
