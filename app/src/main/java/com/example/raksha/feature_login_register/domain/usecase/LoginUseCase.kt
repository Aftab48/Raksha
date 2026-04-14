package com.example.raksha.feature_login_register.domain.usecase

import com.example.raksha.feature_login_register.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phoneNumber: String, password: String) =
        authRepository.login(phoneNumber, password)
}
