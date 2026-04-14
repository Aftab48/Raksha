package com.example.raksha.feature_login_register.presentation.auth.signin

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
import com.example.raksha.ui.theme.SuccessGreen

@Composable
fun SignInRoute(
    onNavigateToSignUp: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AuthScreenContainer(
        title = "Welcome back",
        subtitle = "Sign in with your phone number and password to continue."
    ) {
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
        uiState.successMessage?.let {
            Text(text = it, color = SuccessGreen, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        AuthPrimaryButton(
            text = "Sign In",
            isLoading = uiState.isLoading,
            onClick = viewModel::signIn
        )
        AuthFooterText(
            prompt = "Don't have an account?",
            actionLabel = "Sign Up",
            onActionClick = onNavigateToSignUp
        )
    }
}
