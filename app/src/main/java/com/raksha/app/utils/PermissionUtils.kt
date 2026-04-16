package com.raksha.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionUtils {
    val SHIELD_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )

    val SOS_REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val SOS_OPTIONAL_PERMISSIONS = arrayOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    val EVIDENCE_STREAM_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    val ONBOARDING_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    val ONBOARDING_REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.CALL_PHONE
    )

    fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasAllPermissions(context: Context, permissions: Array<String>): Boolean =
        permissions.all { hasPermission(context, it) }

    fun hasShieldPermissions(context: Context): Boolean =
        hasAllPermissions(context, SHIELD_PERMISSIONS)

    fun hasSosRequiredPermissions(context: Context): Boolean =
        hasAllPermissions(context, SOS_REQUIRED_PERMISSIONS)

    fun hasSmsPermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.SEND_SMS)

    fun hasCallPermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.CALL_PHONE)

    fun hasEvidenceStreamingPermissions(context: Context): Boolean =
        hasAllPermissions(context, EVIDENCE_STREAM_PERMISSIONS)

    fun hasNotificationPermission(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
            hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
}
