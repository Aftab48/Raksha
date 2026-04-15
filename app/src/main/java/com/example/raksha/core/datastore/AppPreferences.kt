package com.example.raksha.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.datastore by preferencesDataStore(name = "app_prefs")

data class StoredPermissionState(
    val microphoneGranted: Boolean = false,
    val locationGranted: Boolean = false,
    val smsGranted: Boolean = false,
    val phoneGranted: Boolean = false
) {
    val allGranted: Boolean
        get() = microphoneGranted && locationGranted && smsGranted && phoneGranted
}

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_HAS_SEEN_INTRO = booleanPreferencesKey("has_seen_intro")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_HAS_SETUP_CONTACTS = booleanPreferencesKey("has_setup_contacts")
        val KEY_MICROPHONE_PERMISSION_GRANTED = booleanPreferencesKey("microphone_permission_granted")
        val KEY_LOCATION_PERMISSION_GRANTED = booleanPreferencesKey("location_permission_granted")
        val KEY_SMS_PERMISSION_GRANTED = booleanPreferencesKey("sms_permission_granted")
        val KEY_PHONE_PERMISSION_GRANTED = booleanPreferencesKey("phone_permission_granted")
    }

    //read
    val hasSeenIntro = context.datastore.data.map { preference ->
        preference[KEY_HAS_SEEN_INTRO] ?: false
    }
    val isLoggedIn = context.datastore.data.map { preference ->
        preference[KEY_IS_LOGGED_IN] ?: false
    }
    val hasSetupContacts = context.datastore.data.map { preference ->
        preference[KEY_HAS_SETUP_CONTACTS] ?: false
    }
    val storedPermissionState = context.datastore.data.map { preference ->
        StoredPermissionState(
            microphoneGranted = preference[KEY_MICROPHONE_PERMISSION_GRANTED] ?: false,
            locationGranted = preference[KEY_LOCATION_PERMISSION_GRANTED] ?: false,
            smsGranted = preference[KEY_SMS_PERMISSION_GRANTED] ?: false,
            phoneGranted = preference[KEY_PHONE_PERMISSION_GRANTED] ?: false
        )
    }

    //write
    suspend fun setIntroSeen() {
        context.datastore.edit {
            it[KEY_HAS_SEEN_INTRO] = true
        }
    }
    suspend fun setLoggedIn(isLoggedIn: Boolean) {
        context.datastore.edit {
            it[KEY_IS_LOGGED_IN] = isLoggedIn
        }
    }
    suspend fun setContactsSetup() {
        context.datastore.edit {
            it[KEY_HAS_SETUP_CONTACTS] = true
        }
    }

    suspend fun updatePermissionState(permissionState: StoredPermissionState) {
        context.datastore.edit {
            it[KEY_MICROPHONE_PERMISSION_GRANTED] = permissionState.microphoneGranted
            it[KEY_LOCATION_PERMISSION_GRANTED] = permissionState.locationGranted
            it[KEY_SMS_PERMISSION_GRANTED] = permissionState.smsGranted
            it[KEY_PHONE_PERMISSION_GRANTED] = permissionState.phoneGranted
        }
    }
}
