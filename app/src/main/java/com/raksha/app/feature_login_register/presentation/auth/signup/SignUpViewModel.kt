package com.raksha.app.feature_login_register.presentation.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raksha.app.feature_login_register.domain.model.PendingRegistration
import com.raksha.app.feature_login_register.domain.usecase.CompleteRegistrationUseCase
import com.raksha.app.feature_login_register.domain.usecase.RegisterInitUseCase
import com.raksha.app.feature_login_register.domain.usecase.VerifyEmailOtpUseCase
import com.raksha.app.feature_login_register.domain.usecase.VerifyMobileOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpUiState(
    val userName: String = "",
    val email: String = "",
    val phoneNumber: String = "+91",
    val password: String = "",
    val emailOtp: String = "",
    val mobileOtp: String = "",
    val isSubmitting: Boolean = false,
    val isVerifyingOtp: Boolean = false,
    val pendingRegistration: PendingRegistration? = null,
    val registrationComplete: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val registerInitUseCase: RegisterInitUseCase,
    private val verifyEmailOtpUseCase: VerifyEmailOtpUseCase,
    private val verifyMobileOtpUseCase: VerifyMobileOtpUseCase,
    private val completeRegistrationUseCase: CompleteRegistrationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onUserNameChanged(value: String) {
        _uiState.update { it.copy(userName = value, errorMessage = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPhoneNumberChanged(value: String) {
        _uiState.update { it.copy(phoneNumber = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onEmailOtpChanged(value: String) {
        _uiState.update { it.copy(emailOtp = value, errorMessage = null) }
    }

    fun onMobileOtpChanged(value: String) {
        _uiState.update{ it.copy(mobileOtp = value, errorMessage = null)}
    }

    fun submitRegistration(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        if (
            currentState.userName.isBlank() ||
            currentState.email.isBlank() ||
            currentState.phoneNumber.isBlank() ||
            currentState.password.isBlank()
        ) {
            _uiState.update { it.copy(errorMessage = "Fill in all registration details") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }

            registerInitUseCase(
                userName = currentState.userName.trim(),
                email = currentState.email.trim(),
                password = currentState.password,
                phoneNumber = currentState.phoneNumber.trim()
            ).fold(
                onSuccess = { pendingRegistration ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            pendingRegistration = pendingRegistration,
                            infoMessage = pendingRegistration.msg
                        )
                    }
                    onSuccess()
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = throwable.message ?: "Registration failed"
                        )
                    }
                }
            )
        }
    }

    fun verifyOtpAndCompleteRegistration() {
        val currentState = _uiState.value
        val pendingRegistration = currentState.pendingRegistration
        if (pendingRegistration == null) {
            _uiState.update { it.copy(errorMessage = "Registration session missing. Please sign up again.") }
            return
        }
        if (currentState.emailOtp.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter the OTP sent to your email") }
            return
        }
        if (currentState.mobileOtp.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter the OTP sent to your phone") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isVerifyingOtp = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }

            verifyEmailOtpUseCase(
                registrationId = pendingRegistration.registrationId,
                otp = currentState.emailOtp.trim()
            ).fold(
                onSuccess = {
                    verifyMobileOtpUseCase(
                        registrationId = pendingRegistration.registrationId,
                        otp = currentState.mobileOtp.trim()
                    ).fold(
                        onSuccess = {
                            completeRegistrationUseCase(pendingRegistration.registrationId).fold(
                                onSuccess = { authResult ->
                                    _uiState.update {
                                        it.copy(
                                            isVerifyingOtp = false,
                                            registrationComplete = true,
                                            infoMessage = authResult.msg ?: "Registration completed successfully"
                                        )
                                    }
                                },
                                onFailure = { throwable ->
                                    _uiState.update {
                                        it.copy(
                                            isVerifyingOtp = false,
                                            errorMessage = throwable.message ?: "Could not complete registration"
                                        )
                                    }
                                }
                            )
                        },
                        onFailure = {throwable ->
                            _uiState.update {
                                it.copy(
                                    isVerifyingOtp = false,
                                    errorMessage = throwable.message ?: "Mobile OTP verification failed"
                                )
                            }
                        }
                    )
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isVerifyingOtp = false,
                            errorMessage = throwable.message ?: "Email OTP verification failed"
                        )
                    }
                }
            )
        }
    }

    fun consumeRegistrationComplete() {
        _uiState.update { it.copy(registrationComplete = false) }
    }
}
