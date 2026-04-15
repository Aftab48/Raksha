package com.raksha.app.feature_login_register.presentation.navigation

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.raksha.app.feature_login_register.presentation.auth.signin.PostLoginDestination
import com.raksha.app.feature_login_register.presentation.auth.signin.SignInRoute
import com.raksha.app.feature_login_register.presentation.auth.signup.SignUpRoute
import com.raksha.app.feature_login_register.presentation.auth.signup.SignUpViewModel
import com.raksha.app.feature_login_register.presentation.auth.verifyotp.VerifyOtpRoute
import com.raksha.app.ui.navigation.Screen

object AuthRoutes {
    const val SignIn = "sign_in"
    const val SignUpFlow = "sign_up_flow"
    const val SignUp = "sign_up"
    const val VerifyOtp = "verify_otp"
}

fun NavGraphBuilder.authGraph(
    navController: NavController
) {

    navigation(
        startDestination = AuthRoutes.SignIn,
        route = Screen.AuthGraph.route
    ) {

        composable(AuthRoutes.SignIn) {
            SignInRoute(
                onNavigateToSignUp = {
                    navController.navigate(AuthRoutes.SignUpFlow)
                },
                onLoginSuccess = { destination ->
                    val route = if (destination == PostLoginDestination.HOME) {
                        Screen.Home.route
                    } else {
                        Screen.Onboarding.route
                    }
                    navController.navigate(route) {
                        popUpTo(Screen.AuthGraph.route) { inclusive = true }
                    }
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
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.AuthGraph.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
