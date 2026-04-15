package com.example.raksha.feature_onboarding.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.raksha.feature_trusted_contacts.presentation.TrustedContactsOnboardingRoute

fun NavGraphBuilder.onboardingGraph(navController: NavController) {

    navigation(
        startDestination = "contact_setup",
        route = "onboarding_graph"
    ) {

        composable("contact_setup") {
            TrustedContactsOnboardingRoute(
                onBack = { navController.popBackStack() },
                onFinish = {
                    navController.navigate("main_graph") {
                        popUpTo("onboarding_graph") { inclusive = true }
                    }
                }
            )
        }
    }
}
