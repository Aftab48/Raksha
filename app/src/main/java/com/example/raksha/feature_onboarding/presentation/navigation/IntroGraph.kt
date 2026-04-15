package com.example.raksha.feature_onboarding.presentation.navigation

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.raksha.core.datastore.AppPreferences
import com.example.raksha.feature_onboarding.presentation.WelcomeStep
import kotlinx.coroutines.launch

fun NavGraphBuilder.introGraph(navController: NavController) {

    navigation(
        startDestination = "welcome",
        route = "intro_graph"
    ) {

        composable("welcome") {

            val context = LocalContext.current
            val prefs = AppPreferences(context)
            val scope = rememberCoroutineScope()

            WelcomeStep(
                onNext = {
                    scope.launch {
                        prefs.setIntroSeen()
                    }

                    navController.navigate("auth_graph") {
                        popUpTo("intro_graph") { inclusive = true }
                    }
                }
            )
        }
    }
}