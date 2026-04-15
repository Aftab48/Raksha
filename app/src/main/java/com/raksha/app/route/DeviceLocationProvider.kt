package com.raksha.app.route

import com.google.android.gms.maps.model.LatLng
import com.raksha.app.utils.LocationUtils
import javax.inject.Inject
import javax.inject.Singleton

interface DeviceLocationProvider {
    suspend fun getCurrentOrLastLocation(): LatLng?
}

@Singleton
class FusedDeviceLocationProvider @Inject constructor(
    private val locationUtils: LocationUtils
) : DeviceLocationProvider {
    override suspend fun getCurrentOrLastLocation(): LatLng? {
        val location = runCatching {
            locationUtils.getCurrentLocation() ?: locationUtils.getLastKnownLocation()
        }.getOrNull()
        return location?.let { LatLng(it.latitude, it.longitude) }
    }
}
