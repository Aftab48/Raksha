package com.example.raksha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.raksha.feature_login_register.presentation.navigation.RakshaNavHost
import com.example.raksha.ui.theme.RakshaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RakshaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RakshaNavHost(innerPadding = innerPadding)
                }
            }
        }
    }
}
