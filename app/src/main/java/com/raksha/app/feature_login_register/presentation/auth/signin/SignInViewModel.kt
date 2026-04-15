package com.raksha.app.feature_login_register.presentation.auth.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raksha.app.feature_login_register.data.local.SessionManager
import com.raksha.app.feature_login_register.domain.usecase.LoginUseCase
import com.raksha.app.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PostLoginDestination {
    ONBOARDING,
    HOME
}

data class SignInUiState(
    val phoneNumber: String = "+91",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val nextDestination: PostLoginDestination? = null
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.lastAuthPhone.first()?.takeIf { it.isNotBlank() }?.let { lastPhone ->
                _uiState.update { state ->
                    if (state.phoneNumber == "+91") {
                        state.copy(phoneNumber = lastPhone)
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun onPhoneNumberChanged(value: String) {
        _uiState.update { it.copy(phoneNumber = value, errorMessage = null, successMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null, successMessage = null) }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(nextDestination = null) }
    }

    fun signIn() {
        val currentState = _uiState.value
        if (currentState.phoneNumber.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter phone number and password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            loginUseCase(
                phoneNumber = currentState.phoneNumber.trim(),
                password = currentState.password
            ).fold(
                onSuccess = {
                    val isOnboardingComplete = userRepository.isOnboardingComplete.first()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            successMessage = "Signed in successfully",
                            nextDestination = if (isOnboardingComplete) {
                                PostLoginDestination.HOME
                            } else {
                                PostLoginDestination.ONBOARDING
                            }
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Sign in failed"
                        )
                    }
                }
            )
        }
    }
}
