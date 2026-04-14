package com.example.raksha.feature_login_register.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.raksha.feature_login_register.presentation.auth.signin.SignInRoute
import com.example.raksha.feature_login_register.presentation.auth.signup.SignUpRoute
import com.example.raksha.feature_login_register.presentation.auth.signup.SignUpViewModel
import com.example.raksha.feature_login_register.presentation.auth.verifyotp.VerifyOtpRoute

object AuthRoutes {
    const val SignIn = "sign_in"
    const val SignUpFlow = "sign_up_flow"
    const val SignUp = "sign_up"
    const val VerifyOtp = "verify_otp"
}

@Composable
fun RakshaNavHost(
    innerPadding: PaddingValues
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AuthRoutes.SignIn,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        composable(AuthRoutes.SignIn) {
            SignInRoute(
                onNavigateToSignUp = {
                    navController.navigate(AuthRoutes.SignUpFlow)
                }
            )
        }

        navigation(
            startDestination = AuthRoutes.SignUp,
            route = AuthRoutes.SignUpFlow
        ) {
            composable(AuthRoutes.SignUp) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AuthRoutes.SignUpFlow)
                }
                val viewModel: SignUpViewModel = hiltViewModel(parentEntry)
                SignUpRoute(
                    viewModel = viewModel,
                    onBackToSignIn = { navController.popBackStack() },
                    onNavigateToOtp = {
                        navController.navigate(AuthRoutes.VerifyOtp)
                    }
                )
            }

            composable(AuthRoutes.VerifyOtp) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AuthRoutes.SignUpFlow)
                }
                val viewModel: SignUpViewModel = hiltViewModel(parentEntry)
                VerifyOtpRoute(
                    viewModel = viewModel,
                    onRegistrationComplete = {
                        navController.navigate(AuthRoutes.SignIn) {
                            popUpTo(AuthRoutes.SignIn) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
