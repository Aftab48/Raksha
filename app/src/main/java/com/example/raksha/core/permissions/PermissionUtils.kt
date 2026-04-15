package com.example.raksha.core.permissions

import android.Manifest
import com.example.raksha.core.datastore.StoredPermissionState

object PermissionUtils {
    val ONBOARDING_PERMISSIONS = listOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CALL_PHONE
    )

    fun toStoredPermissionState(grants: Map<String, Boolean>): StoredPermissionState {
        return StoredPermissionState(
            microphoneGranted = grants[Manifest.permission.RECORD_AUDIO] == true,
            locationGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true,
            smsGranted = grants[Manifest.permission.SEND_SMS] == true,
            phoneGranted = grants[Manifest.permission.CALL_PHONE] == true
        )
    }
}
