package com.raksha.app.feature_login_register.presentation.auth.signup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raksha.app.feature_login_register.presentation.auth.common.AuthFooterText
import com.raksha.app.feature_login_register.presentation.auth.common.AuthPrimaryButton
import com.raksha.app.feature_login_register.presentation.auth.common.AuthScreenContainer
import com.raksha.app.feature_login_register.presentation.auth.common.AuthStatusBanner
import com.raksha.app.feature_login_register.presentation.auth.common.AuthTextField

@Composable
fun SignUpRoute(
    onBackToSignIn: () -> Unit,
    onNavigateToOtp: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    AuthScreenContainer(
        title = "Create your account",
        subtitle = "Fill in your details, then verify the OTP before registration is completed."
    ) {
        AuthTextField(
            value = uiState.userName,
            onValueChange = viewModel::onUserNameChanged,
            label = "User name",
            imeAction = ImeAction.Next
        )
        AuthTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChanged,
            label = "Email",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )
        AuthTextField(
            value = uiState.phoneNumber,
            onValueChange = viewModel::onPhoneNumberChanged,
            label = "Phone number",
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        )
        AuthTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChanged,
            label = "Password",
            isPassword = true,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.submitRegistration(onNavigateToOtp)
                }
            )
        )
        uiState.errorMessage?.let {
            AuthStatusBanner(text = it, isError = true)
        }
        Spacer(modifier = Modifier.height(8.dp))
        AuthPrimaryButton(
            text = "Sign Up",
            isLoading = uiState.isSubmitting,
            onClick = {
                focusManager.clearFocus()
                viewModel.submitRegistration(onNavigateToOtp)
            }
        )
        AuthFooterText(
            prompt = "Already have an account?",
            actionLabel = "Sign In",
            onActionClick = onBackToSignIn
        )
    }
}
