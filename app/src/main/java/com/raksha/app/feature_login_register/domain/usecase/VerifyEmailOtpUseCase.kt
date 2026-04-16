package com.raksha.app.feature_login_register.domain.usecase

import com.raksha.app.feature_login_register.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyEmailOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(registrationId: String, otp: String) =
        authRepository.verifyEmailOtp(registrationId, otp)
}
