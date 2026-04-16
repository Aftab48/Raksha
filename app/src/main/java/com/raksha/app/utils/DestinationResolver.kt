package com.raksha.app.utils

import com.google.android.gms.maps.model.LatLng

interface DestinationResolver {
    suspend fun resolve(query: String, near: LatLng? = null): DestinationResolutionResult
    suspend fun suggest(query: String, near: LatLng? = null): List<DestinationSuggestion>
}

data class DestinationSuggestion(
    val label: String,
    val latLng: LatLng
)

sealed interface DestinationResolutionResult {
    data class Resolved(
        val latLng: LatLng,
        val label: String?
    ) : DestinationResolutionResult

    object NoMatch : DestinationResolutionResult

    object Unavailable : DestinationResolutionResult

    data class Failure(val reason: String?) : DestinationResolutionResult
}
