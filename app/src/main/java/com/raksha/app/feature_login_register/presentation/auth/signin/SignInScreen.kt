package com.raksha.app.feature_login_register.presentation.auth.signin

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raksha.app.feature_login_register.presentation.auth.common.AuthFooterText
import com.raksha.app.feature_login_register.presentation.auth.common.AuthPrimaryButton
import com.raksha.app.feature_login_register.presentation.auth.common.AuthScreenContainer
import com.raksha.app.feature_login_register.presentation.auth.common.AuthStatusBanner
import com.raksha.app.feature_login_register.presentation.auth.common.AuthTextField

@Composable
fun SignInRoute(
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: (PostLoginDestination) -> Unit,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.nextDestination) {
        uiState.nextDestination?.let { destination ->
            onLoginSuccess(destination)
            viewModel.onNavigationHandled()
        }
    }

    AuthScreenContainer(
        title = "Welcome back",
        subtitle = "Sign in with your phone number and password to continue."
    ) {
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
                    viewModel.signIn()
                }
            )
        )
        uiState.errorMessage?.let {
            AuthStatusBanner(text = it, isError = true)
        }
        uiState.successMessage?.let {
            AuthStatusBanner(text = it, isError = false)
        }
        Spacer(modifier = Modifier.height(8.dp))
        AuthPrimaryButton(
            text = "Sign In",
            isLoading = uiState.isLoading,
            onClick = {
                focusManager.clearFocus()
                viewModel.signIn()
            }
        )
        AuthFooterText(
            prompt = "Don't have an account?",
            actionLabel = "Sign Up",
            onActionClick = onNavigateToSignUp
        )
    }
}
