package com.example.raksha.feature_login_register.presentation.auth.verifyotp

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.raksha.feature_login_register.presentation.auth.common.AuthFooterText
import com.example.raksha.feature_login_register.presentation.auth.common.AuthPrimaryButton
import com.example.raksha.feature_login_register.presentation.auth.common.AuthScreenContainer
import com.example.raksha.feature_login_register.presentation.auth.common.AuthTextField
import com.example.raksha.feature_login_register.presentation.auth.signup.SignUpUiState
import com.example.raksha.feature_login_register.presentation.auth.signup.SignUpViewModel
import com.raksha.app.ui.theme.ErrorRose
import com.raksha.app.ui.theme.SuccessGreen

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
        onOtpChanged = viewModel::onOtpChanged,
        onVerifyClick = viewModel::verifyOtpAndCompleteRegistration,
        onBack = onBack
    )
}

@Composable
private fun VerifyOtpScreen(
    uiState: SignUpUiState,
    onOtpChanged: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onBack: () -> Unit
) {
    val pendingRegistration = uiState.pendingRegistration

    AuthScreenContainer(
        title = "Verify OTP",
        subtitle = if (pendingRegistration != null) {
            "Enter the email OTP sent to ${pendingRegistration.maskedEmail} to finish your sign up."
        } else {
            "Your sign up session is missing. Go back and submit the form again."
        }
    ) {
        AuthTextField(
            value = uiState.otp,
            onValueChange = onOtpChanged,
            label = "Email OTP",
            keyboardType = KeyboardType.Number,
            enabled = pendingRegistration != null
        )
        uiState.errorMessage?.let {
            Text(text = it, color = ErrorRose, style = MaterialTheme.typography.bodyLarge)
        }
        uiState.infoMessage?.let {
            Text(text = it, color = SuccessGreen, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        AuthPrimaryButton(
            text = "Verify OTP",
            isLoading = uiState.isVerifyingOtp,
            onClick = onVerifyClick
        )
        AuthFooterText(
            prompt = "Need to edit your details?",
            actionLabel = "Go back",
            onActionClick = onBack
        )
    }
}
