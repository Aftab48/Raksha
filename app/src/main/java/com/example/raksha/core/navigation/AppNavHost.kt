package com.example.raksha.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.raksha.feature_login_register.presentation.navigation.authGraph
import com.example.raksha.feature_onboarding.presentation.navigation.introGraph
import com.example.raksha.feature_onboarding.presentation.navigation.onboardingGraph
import com.example.raksha.ui.MainScreen

@Composable
fun AppNavHost(startDestination: String) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        introGraph(navController)
        authGraph(navController)
        onboardingGraph(navController)

        composable("main_graph") {
            MainScreen() // create simple screen for now
        }
    }
}