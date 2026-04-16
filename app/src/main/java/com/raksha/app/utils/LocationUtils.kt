package com.raksha.app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class LocationUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location -> cont.resume(location) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? = suspendCancellableCoroutine { cont ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location -> cont.resume(location) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }
}
