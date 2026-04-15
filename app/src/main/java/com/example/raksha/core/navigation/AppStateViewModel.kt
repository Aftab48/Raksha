package com.example.raksha.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raksha.core.datastore.AppPreferences
import com.example.raksha.feature_login_register.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppStateViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val sessionManager: SessionManager
): ViewModel() {
    val appState: StateFlow<String?> = combine(
        prefs.hasSeenIntro,
        sessionManager.authToken,
        prefs.hasSetupContacts
    ) {intro, token, contacts ->
        when {
            !intro -> "intro_graph"
            token == null -> "auth_graph"
            !contacts -> "onboarding_graph"
            else -> "main_graph"
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null
    )
}
