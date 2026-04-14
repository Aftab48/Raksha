package com.example.raksha.feature_login_register.data.remote.api

import com.example.raksha.feature_login_register.data.remote.dto.AuthTokenResponseDto
import com.example.raksha.feature_login_register.data.remote.dto.LoginRequestDto
import com.example.raksha.feature_login_register.data.remote.dto.RegisterCompleteRequestDto
import com.example.raksha.feature_login_register.data.remote.dto.RegisterInitRequestDto
import com.example.raksha.feature_login_register.data.remote.dto.RegisterInitResponseDto
import com.example.raksha.feature_login_register.data.remote.dto.VerifyOtpRequestDto
import com.example.raksha.feature_login_register.data.remote.dto.VerifyOtpResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/register/init")
    suspend fun registerInit(
        @Body request: RegisterInitRequestDto
    ): RegisterInitResponseDto

    @POST("auth/register/verify-email")
    suspend fun verifyEmailOtp(
        @Body request: VerifyOtpRequestDto
    ): VerifyOtpResponseDto

    @POST("auth/register/complete")
    suspend fun completeRegistration(
        @Body request: RegisterCompleteRequestDto
    ): AuthTokenResponseDto

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): AuthTokenResponseDto

    companion object {
        const val BASE_URL = "http://10.0.2.2:3000/api/v1/"
    }
}
