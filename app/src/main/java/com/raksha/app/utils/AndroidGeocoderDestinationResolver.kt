package com.raksha.app.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AndroidGeocoderDestinationResolver @Inject constructor(
    @ApplicationContext private val context: Context
) : DestinationResolver {

    override suspend fun resolve(query: String, near: LatLng?): DestinationResolutionResult =
        withContext(Dispatchers.IO) {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isBlank()) {
                return@withContext DestinationResolutionResult.NoMatch
            }
            if (!Geocoder.isPresent()) {
                return@withContext DestinationResolutionResult.Unavailable
            }

            runCatching {
                val geocoder = Geocoder(context, Locale.getDefault())
                getAddresses(geocoder, normalizedQuery, near).firstOrNull()
            }.fold(
                onSuccess = { address ->
                    if (address == null) {
                        DestinationResolutionResult.NoMatch
                    } else {
                        DestinationResolutionResult.Resolved(
                            latLng = LatLng(address.latitude, address.longitude),
                            label = address.getAddressLine(0)
                        )
                    }
                },
                onFailure = { error ->
                    DestinationResolutionResult.Failure(error.message)
                }
            )
        }

    @Suppress("DEPRECATION")
    private fun getAddresses(
        geocoder: Geocoder,
        query: String,
        near: LatLng?
    ): List<Address> {
        if (near != null) {
            val minLat = (near.latitude - SEARCH_RADIUS_DEGREES).coerceIn(-90.0, 90.0)
            val maxLat = (near.latitude + SEARCH_RADIUS_DEGREES).coerceIn(-90.0, 90.0)
            val minLng = (near.longitude - SEARCH_RADIUS_DEGREES).coerceIn(-180.0, 180.0)
            val maxLng = (near.longitude + SEARCH_RADIUS_DEGREES).coerceIn(-180.0, 180.0)
            val nearbyMatches = geocoder
                .getFromLocationName(query, MAX_RESULTS, minLat, minLng, maxLat, maxLng)
                .orEmpty()
            if (nearbyMatches.isNotEmpty()) {
                return nearbyMatches
            }
        }

        return geocoder.getFromLocationName(query, MAX_RESULTS).orEmpty()
    }

    private companion object {
        const val MAX_RESULTS = 5
        const val SEARCH_RADIUS_DEGREES = 0.6
    }
}
