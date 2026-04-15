package com.raksha.app.feature_login_register.data.remote.api

import com.raksha.app.feature_login_register.data.remote.dto.AuthTokenResponseDto
import com.raksha.app.feature_login_register.data.remote.dto.LoginRequestDto
import com.raksha.app.feature_login_register.data.remote.dto.RegisterCompleteRequestDto
import com.raksha.app.feature_login_register.data.remote.dto.RegisterInitRequestDto
import com.raksha.app.feature_login_register.data.remote.dto.RegisterInitResponseDto
import com.raksha.app.feature_login_register.data.remote.dto.VerifyMobileOtpRequestDto
import com.raksha.app.feature_login_register.data.remote.dto.VerifyMobileOtpResponseDto
import com.raksha.app.feature_login_register.data.remote.dto.VerifyOtpRequestDto
import com.raksha.app.feature_login_register.data.remote.dto.VerifyOtpResponseDto
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

    @POST("auth/register/verify-mobile")
    suspend fun verifyMobileOtp(
        @Body request: VerifyMobileOtpRequestDto
    ): VerifyMobileOtpResponseDto

    @POST("auth/register/complete")
    suspend fun completeRegistration(
        @Body request: RegisterCompleteRequestDto
    ): AuthTokenResponseDto

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): AuthTokenResponseDto
}
