package com.raksha.app.utils

import com.google.android.gms.maps.model.LatLng

interface DestinationResolver {
    suspend fun resolve(query: String, near: LatLng? = null): DestinationResolutionResult
}

sealed interface DestinationResolutionResult {
    data class Resolved(
        val latLng: LatLng,
        val label: String?
    ) : DestinationResolutionResult

    object NoMatch : DestinationResolutionResult

    object Unavailable : DestinationResolutionResult

    data class Failure(val reason: String?) : DestinationResolutionResult
}
