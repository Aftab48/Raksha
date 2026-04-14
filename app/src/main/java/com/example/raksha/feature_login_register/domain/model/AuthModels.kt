package com.example.raksha.feature_login_register.domain.model

data class PendingRegistration(
    val msg: String,
    val registrationId: String,
    val expiresAt: String,
    val maskedEmail: String
)

data class AuthResult(
    val token: String,
    val msg: String? = null
)
