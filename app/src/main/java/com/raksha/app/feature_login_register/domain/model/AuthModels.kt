package com.raksha.app.feature_login_register.domain.model

data class PendingRegistration(
    val msg: String,
    val registrationId: String,
    val expiresAt: String,
    val maskedEmail: String,
    val maskedPhoneNumber: String
)

data class AuthResult(
    val token: String,
    val msg: String? = null
)
