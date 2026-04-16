package com.raksha.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.raksha.app.feature_login_register.data.local.SessionManager
import com.raksha.app.repository.UserRepository
import com.raksha.app.ui.navigation.RakshaNavGraph
import com.raksha.app.ui.navigation.Screen
import com.raksha.app.ui.theme.RakshaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        var keepSplash = true
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplash }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val (authToken, isOnboardingComplete) = runBlocking {
            val token = sessionManager.authToken.first()
            val onboardingComplete = userRepository.isOnboardingComplete.first()
            token to onboardingComplete
        }
        keepSplash = false

        val startDestination = when {
            authToken.isNullOrBlank() -> Screen.AuthGraph.route
            isOnboardingComplete -> Screen.Home.route
            else -> Screen.Onboarding.route
        }

        setContent {
            RakshaTheme {
                val navController = rememberNavController()
                RakshaNavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}
