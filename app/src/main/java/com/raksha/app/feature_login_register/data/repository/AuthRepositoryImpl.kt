package com.raksha.app.feature_login_register.data.repository

import com.google.gson.Gson
import com.raksha.app.BuildConfig
import com.raksha.app.feature_login_register.data.local.SessionManager
import com.raksha.app.feature_login_register.data.remote.api.AuthApiService
import com.raksha.app.feature_login_register.data.remote.dto.ApiErrorResponseDto
import com.raksha.app.feature_login_register.data.remote.dto.LoginRequestDto
import com.raksha.app.feature_login_register.data.remote.dto.RegisterCompleteRequestDto
import com.raksha.app.feature_login_register.data.remote.dto.RegisterInitRequestDto
import com.raksha.app.feature_login_register.data.remote.dto.VerifyMobileOtpRequestDto
import com.raksha.app.feature_login_register.data.remote.dto.VerifyOtpRequestDto
import com.raksha.app.feature_login_register.data.remote.mapper.toAuthResult
import com.raksha.app.feature_login_register.data.remote.mapper.toDomain
import com.raksha.app.feature_login_register.domain.model.AuthResult
import com.raksha.app.feature_login_register.domain.model.PendingRegistration
import com.raksha.app.feature_login_register.domain.repository.AuthRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val sessionManager: SessionManager,
    private val gson: Gson
) : AuthRepository {

    override suspend fun registerInit(
        userName: String,
        email: String,
        password: String,
        phoneNumber: String
    ): Result<PendingRegistration> = safeApiCall {
        sessionManager.saveLastAuthPhone(phoneNumber)
        authApiService.registerInit(
            RegisterInitRequestDto(
                userName = userName,
                email = email,
                password = password,
                phoneNumber = phoneNumber
            )
        ).toDomain()
    }

    override suspend fun verifyEmailOtp(
        registrationId: String,
        otp: String
    ): Result<String> = safeApiCall {
        authApiService.verifyEmailOtp(
            VerifyOtpRequestDto(
                registrationId = registrationId,
                otp = otp
            )
        ).msg
    }

    override suspend fun verifyMobileOtp(
        registrationId: String,
        otp: String
    ): Result<String> =  safeApiCall{
        authApiService.verifyMobileOtp(
            VerifyMobileOtpRequestDto(
                registrationId = registrationId,
                otp = otp
            )
        ).msg
    }

    override suspend fun completeRegistration(registrationId: String): Result<AuthResult> = safeApiCall {
        val response = authApiService.completeRegistration(
            RegisterCompleteRequestDto(registrationId = registrationId)
        )
        sessionManager.saveAuthToken(response.token)
        response.token.toAuthResult(response.msg)
    }

    override suspend fun login(phoneNumber: String, password: String): Result<AuthResult> = safeApiCall {
        sessionManager.saveLastAuthPhone(phoneNumber)
        val response = authApiService.login(
            LoginRequestDto(
                phoneNumber = phoneNumber,
                password = password
            )
        )
        sessionManager.saveAuthToken(response.token)
        response.token.toAuthResult(response.msg)
    }

    private suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(block())
            } catch (exception: HttpException) {
                val errorBody = exception.response()?.errorBody()?.string()

                val apiMessage = errorBody?.let {
                    runCatching {
                        gson.fromJson(it, ApiErrorResponseDto::class.java)
                    }.getOrNull()
                }?.msg

                Result.failure(Exception(apiMessage ?: "Server request failed"))
            } catch (exception: IOException) {
                Result.failure(Exception("Could not connect to the server at ${BuildConfig.BASE_URL}"))
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }
    }
}
