package com.example.raksha.feature_login_register.data.remote.dto

data class RegisterInitRequestDto(
    val userName: String,
    val email: String,
    val password: String,
    val phoneNumber: String
)

data class RegisterInitResponseDto(
    val message: String,
    val registrationId: String,
    val expiresAt: String,
    val email: String,
    val phoneNumber: String
)

data class VerifyOtpRequestDto(
    val registrationId: String,
    val otp: String
)

data class VerifyMobileOtpRequestDto(
    val registrationId: String,
    val otp: String
)

data class VerifyOtpResponseDto(
    val msg: String,
    val emailVerified: Boolean
)

data class VerifyMobileOtpResponseDto(
    val msg: String,
    val emailVerified: Boolean
)

data class RegisterCompleteRequestDto(
    val registrationId: String
)

data class LoginRequestDto(
    val phoneNumber: String,
    val password: String
)

data class AuthTokenResponseDto(
    val msg: String? = null,
    val token: String
)

data class ApiErrorResponseDto(
    val msg: String? = null
)
