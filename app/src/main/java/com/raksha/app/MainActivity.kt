package com.raksha.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.raksha.app.repository.UserRepository
import com.raksha.app.ui.navigation.RakshaNavGraph
import com.raksha.app.ui.navigation.Screen
import com.raksha.app.ui.theme.RakshaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        var keepSplash = true
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplash }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Determine start destination synchronously before showing content
        val isOnboardingComplete = runBlocking {
            userRepository.isOnboardingComplete.first()
        }
        keepSplash = false

        val startDestination = if (isOnboardingComplete) Screen.Home.route else Screen.Onboarding.route

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
