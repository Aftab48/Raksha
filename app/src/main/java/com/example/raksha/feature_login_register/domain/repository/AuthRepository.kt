package com.example.raksha.feature_login_register.domain.repository

import com.example.raksha.feature_login_register.domain.model.AuthResult
import com.example.raksha.feature_login_register.domain.model.PendingRegistration

interface AuthRepository {
    suspend fun registerInit(
        userName: String,
        email: String,
        password: String,
        phoneNumber: String
    ): Result<PendingRegistration>

    suspend fun verifyEmailOtp(
        registrationId: String,
        otp: String
    ): Result<String>

    suspend fun verifyMobileOtp(
        registrationId: String,
        otp: String
    ): Result<String>

    suspend fun completeRegistration(
        registrationId: String
    ): Result<AuthResult>

    suspend fun login(
        phoneNumber: String,
        password: String
    ): Result<AuthResult>
}
