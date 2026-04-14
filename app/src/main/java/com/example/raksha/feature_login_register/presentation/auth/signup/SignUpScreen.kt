package com.example.raksha.feature_login_register.presentation.auth.signup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.raksha.feature_login_register.presentation.auth.common.AuthFooterText
import com.example.raksha.feature_login_register.presentation.auth.common.AuthPrimaryButton
import com.example.raksha.feature_login_register.presentation.auth.common.AuthScreenContainer
import com.example.raksha.feature_login_register.presentation.auth.common.AuthTextField
import com.example.raksha.ui.theme.ErrorRose

@Composable
fun SignUpRoute(
    onBackToSignIn: () -> Unit,
    onNavigateToOtp: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AuthScreenContainer(
        title = "Create your account",
        subtitle = "Fill in your details, then verify the OTP before registration is completed."
    ) {
        AuthTextField(
            value = uiState.userName,
            onValueChange = viewModel::onUserNameChanged,
            label = "User name"
        )
        AuthTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChanged,
            label = "Email",
            keyboardType = KeyboardType.Email
        )
        AuthTextField(
            value = uiState.phoneNumber,
            onValueChange = viewModel::onPhoneNumberChanged,
            label = "Phone number",
            keyboardType = KeyboardType.Phone
        )
        AuthTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChanged,
            label = "Password",
            isPassword = true
        )
        uiState.errorMessage?.let {
            Text(text = it, color = ErrorRose, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        AuthPrimaryButton(
            text = "Sign Up",
            isLoading = uiState.isSubmitting,
            onClick = { viewModel.submitRegistration(onNavigateToOtp) }
        )
        AuthFooterText(
            prompt = "Already have an account?",
            actionLabel = "Sign In",
            onActionClick = onBackToSignIn
        )
    }
}
