package com.example.raksha.feature_login_register.data.repository

import com.google.gson.Gson
import com.example.raksha.feature_login_register.data.local.SessionManager
import com.example.raksha.feature_login_register.data.remote.api.AuthApiService
import com.example.raksha.feature_login_register.data.remote.dto.ApiErrorResponseDto
import com.example.raksha.feature_login_register.data.remote.dto.LoginRequestDto
import com.example.raksha.feature_login_register.data.remote.dto.RegisterCompleteRequestDto
import com.example.raksha.feature_login_register.data.remote.dto.RegisterInitRequestDto
import com.example.raksha.feature_login_register.data.remote.dto.VerifyOtpRequestDto
import com.example.raksha.feature_login_register.data.remote.mapper.toAuthResult
import com.example.raksha.feature_login_register.data.remote.mapper.toDomain
import com.example.raksha.feature_login_register.domain.model.AuthResult
import com.example.raksha.feature_login_register.domain.model.PendingRegistration
import com.example.raksha.feature_login_register.domain.repository.AuthRepository
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

    override suspend fun completeRegistration(registrationId: String): Result<AuthResult> = safeApiCall {
        val response = authApiService.completeRegistration(
            RegisterCompleteRequestDto(registrationId = registrationId)
        )
        sessionManager.saveAuthToken(response.token)
        response.token.toAuthResult(response.msg)
    }

    override suspend fun login(phoneNumber: String, password: String): Result<AuthResult> = safeApiCall {
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

                Result.failure(Exception(apiMessage ?: errorBody!!))
            } catch (exception: IOException) {
                Result.failure(Exception("Could not connect to the server. Make sure the backend is running on localhost:3000"))
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }
    }
}
