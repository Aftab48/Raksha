package com.raksha.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.raksha.app.feature_login_register.presentation.navigation.authGraph
import com.raksha.app.ui.screen.home.HomeScreen
import com.raksha.app.ui.screen.onboarding.OnboardingScreen
import com.raksha.app.ui.screen.route.RouteScreen
import com.raksha.app.ui.screen.settings.SettingsScreen
import com.raksha.app.ui.screen.sos.ActiveSosScreen

sealed class Screen(val route: String) {
    object AuthGraph : Screen("auth_graph")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Route : Screen("route")
    object ActiveSos : Screen("active_sos/{sosEventId}") {
        fun createRoute(sosEventId: Int) = "active_sos/$sosEventId"
    }
    object Settings : Screen("settings")
}

@Composable
fun RakshaNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        authGraph(navController)

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateSafely = { navController.navigate(Screen.Route.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onSosTriggered = { eventId ->
                    navController.navigate(Screen.ActiveSos.createRoute(eventId))
                }
            )
        }

        composable(Screen.Route.route) {
            RouteScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.ActiveSos.route,
            arguments = listOf(navArgument("sosEventId") { type = NavType.IntType })
        ) { backStackEntry ->
            val sosEventId = backStackEntry.arguments?.getInt("sosEventId") ?: -1
            ActiveSosScreen(
                sosEventId = sosEventId,
                onAlertCancelled = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
