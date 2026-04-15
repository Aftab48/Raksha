package com.raksha.app.feature_login_register.data.remote.dto

import com.google.gson.annotations.SerializedName

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
    @SerializedName(value = "msg", alternate = ["message"])
    val msg: String,
    val emailVerified: Boolean
)

data class VerifyMobileOtpResponseDto(
    @SerializedName(value = "msg", alternate = ["message"])
    val msg: String,
    val mobileVerified: Boolean
)

data class RegisterCompleteRequestDto(
    val registrationId: String
)

data class LoginRequestDto(
    val phoneNumber: String,
    val password: String
)

data class AuthTokenResponseDto(
    @SerializedName(value = "msg", alternate = ["message"])
    val msg: String? = null,
    val token: String
)

data class ApiErrorResponseDto(
    val msg: String? = null
)
