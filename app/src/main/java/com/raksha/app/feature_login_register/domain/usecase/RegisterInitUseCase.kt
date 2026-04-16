package com.raksha.app.feature_login_register.domain.usecase

import com.raksha.app.feature_login_register.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterInitUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        userName: String,
        email: String,
        password: String,
        phoneNumber: String
    ) = authRepository.registerInit(
        userName = userName,
        email = email,
        password = password,
        phoneNumber = phoneNumber
    )
}
