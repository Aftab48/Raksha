package com.example.raksha.feature_login_register.domain.usecase

import com.example.raksha.feature_login_register.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyMobileOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(registrationId: String, otp: String) =
        authRepository.verifyMobileOtp(registrationId, otp)
}