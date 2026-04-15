package com.raksha.app.feature_login_register.presentation.auth.verifyotp

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raksha.app.feature_login_register.presentation.auth.common.AuthFooterText
import com.raksha.app.feature_login_register.presentation.auth.common.AuthPrimaryButton
import com.raksha.app.feature_login_register.presentation.auth.common.AuthScreenContainer
import com.raksha.app.feature_login_register.presentation.auth.common.AuthStatusBanner
import com.raksha.app.feature_login_register.presentation.auth.common.AuthTextField
import com.raksha.app.feature_login_register.presentation.auth.signup.SignUpUiState
import com.raksha.app.feature_login_register.presentation.auth.signup.SignUpViewModel

@Composable
fun VerifyOtpRoute(
    viewModel: SignUpViewModel,
    onRegistrationComplete: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.registrationComplete) {
        if (uiState.registrationComplete) {
            viewModel.consumeRegistrationComplete()
            onRegistrationComplete()
        }
    }

    VerifyOtpScreen(
        uiState = uiState,
        onEmailOtpChanged = viewModel::onEmailOtpChanged,
        onMobileOtpChanged = viewModel::onMobileOtpChanged,
        onVerifyClick = viewModel::verifyOtpAndCompleteRegistration,
        onBack = onBack
    )
}

@Composable
private fun VerifyOtpScreen(
    uiState: SignUpUiState,
    onEmailOtpChanged: (String) -> Unit,
    onMobileOtpChanged: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onBack: () -> Unit
) {
    val pendingRegistration = uiState.pendingRegistration
    val focusManager = LocalFocusManager.current

    AuthScreenContainer(
        title = "Verify OTP",
        subtitle = if (pendingRegistration != null) {
            "Enter the email and mobile OTP sent to ${pendingRegistration.maskedEmail} and ${pendingRegistration.maskedPhoneNumber} to finish your sign up."
        } else {
            "Your sign up session is missing. Go back and submit the form again."
        }
    ) {
        AuthTextField(
            value = uiState.emailOtp,
            onValueChange = onEmailOtpChanged,
            label = "Email OTP",
            keyboardType = KeyboardType.Number,
            enabled = pendingRegistration != null,
            imeAction = ImeAction.Next
        )
        Spacer(modifier = Modifier.height(10.dp))
        AuthTextField(
            value = uiState.mobileOtp,
            onValueChange = onMobileOtpChanged,
            label = "Mobile OTP",
            keyboardType = KeyboardType.Number,
            enabled = pendingRegistration != null,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onVerifyClick()
                }
            )
        )
        uiState.errorMessage?.let {
            AuthStatusBanner(text = it, isError = true)
        }
        uiState.infoMessage?.let {
            AuthStatusBanner(text = it, isError = false)
        }
        Spacer(modifier = Modifier.height(8.dp))
        AuthPrimaryButton(
            text = "Verify OTP",
            isLoading = uiState.isVerifyingOtp,
            onClick = {
                focusManager.clearFocus()
                onVerifyClick()
            }
        )
        AuthFooterText(
            prompt = "Need to edit your details?",
            actionLabel = "Go back",
            onActionClick = onBack
        )
    }
}
