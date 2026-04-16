package com.raksha.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionUtils {
    val SHIELD_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )

    val SOS_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CALL_PHONE
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
        Manifest.permission.CALL_PHONE
    )

    fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasAllPermissions(context: Context, permissions: Array<String>): Boolean =
        permissions.all { hasPermission(context, it) }

    fun hasShieldPermissions(context: Context): Boolean =
        hasAllPermissions(context, SHIELD_PERMISSIONS)

    fun hasSosPermissions(context: Context): Boolean =
        hasAllPermissions(context, SOS_PERMISSIONS)

    fun hasEvidenceStreamingPermissions(context: Context): Boolean =
        hasAllPermissions(context, EVIDENCE_STREAM_PERMISSIONS)
}
