package com.raksha.app.repository

import com.google.android.gms.maps.model.LatLng
import com.raksha.app.data.assets.NcrbDataSource
import com.raksha.app.utils.RouteScorer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class ScoredRoute(
    val name: String,
    val safetyScore: Double, // 0.0-1.0, lower = safer
    val distanceMeters: Int,
    val durationSeconds: Int,
    val polylinePoints: List<LatLng>,
    val overview: String
)

class DirectionsApiException(
    val userMessage: String,
    val debugDetails: String
) : Exception(userMessage)

@Singleton
class RouteRepository @Inject constructor(
    private val ncrbDataSource: NcrbDataSource,
    private val routeScorer: RouteScorer
) {
    /**
     * Fetches routes from Directions API and scores each one for safety.
     * Returns top 2 routes sorted by safety score (safest first).
     */
    suspend fun getScoredRoutes(
        origin: LatLng,
        destination: LatLng,
        apiKey: String
    ): Result<List<ScoredRoute>> = withContext(Dispatchers.IO) {
        try {
            val url = buildDirectionsUrl(origin, destination, apiKey)
            val response = URL(url).readText()
            val json = JSONObject(response)
            val status = json.optString("status")

            if (status != "OK") {
                val errorMessage = json.optString("error_message").takeIf { it.isNotBlank() }
                return@withContext Result.failure(
                    buildDirectionsApiException(
                        status = status,
                        errorMessage = errorMessage,
                        origin = origin,
                        destination = destination
                    )
                )
            }

            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) {
                return@withContext Result.failure(
                    DirectionsApiException(
                        userMessage = "No routes found for this destination.",
                        debugDetails = "Directions status=OK; routes=[]"
                    )
                )
            }

            val scoredRoutes = mutableListOf<ScoredRoute>()
            for (i in 0 until routes.length()) {
                val route = routes.getJSONObject(i)
                val leg = route.getJSONArray("legs").getJSONObject(0)
                val distance = leg.getJSONObject("distance").getInt("value")
                val duration = leg.getJSONObject("duration").getInt("value")
                val summary = route.getString("summary")

                val steps = leg.getJSONArray("steps")
                val points = mutableListOf<Pair<Double, Double>>()
                for (j in 0 until steps.length()) {
                    val loc = steps.getJSONObject(j).getJSONObject("start_location")
                    points.add(loc.getDouble("lat") to loc.getDouble("lng"))
                }

                val score = routeScorer.score(points, distance, ncrbDataSource)
                val latLngPoints = points.map { LatLng(it.first, it.second) }
                scoredRoutes.add(
                    ScoredRoute(
                        name = summary.ifBlank { "Route ${i + 1}" },
                        safetyScore = score,
                        distanceMeters = distance,
                        durationSeconds = duration,
                        polylinePoints = latLngPoints,
                        overview = summary
                    )
                )
            }

            Result.success(scoredRoutes.sortedBy { it.safetyScore }.take(2))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildDirectionsApiException(
        status: String,
        errorMessage: String?,
        origin: LatLng,
        destination: LatLng
    ): DirectionsApiException {
        val normalizedStatus = status.uppercase().ifBlank { "UNKNOWN_STATUS" }
        val userMessage = when (normalizedStatus) {
            "REQUEST_DENIED" ->
                "Google Maps request was denied. Check Directions API enablement and API key restrictions."
            "OVER_QUERY_LIMIT" ->
                "Google Maps quota was exceeded. Try again shortly or review quota limits."
            "ZERO_RESULTS" ->
                "No driving route was found for this destination."
            "INVALID_REQUEST", "NOT_FOUND" ->
                "Google Maps could not understand this route request. Try a more specific destination."
            "UNKNOWN_ERROR" ->
                "Google Maps returned a temporary error. Please try again."
            else ->
                "Could not fetch routes from Google Maps right now."
        }

        val debugDetails = buildString {
            append("Directions status=").append(normalizedStatus)
            if (!errorMessage.isNullOrBlank()) {
                append("; error_message=").append(errorMessage)
            }
            append("; origin=").append(origin.latitude).append(",").append(origin.longitude)
            append("; destination=").append(destination.latitude).append(",").append(destination.longitude)
        }

        return DirectionsApiException(userMessage = userMessage, debugDetails = debugDetails)
    }

    private fun buildDirectionsUrl(origin: LatLng, dest: LatLng, apiKey: String): String =
        "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=${origin.latitude},${origin.longitude}" +
            "&destination=${dest.latitude},${dest.longitude}" +
            "&alternatives=true" +
            "&key=$apiKey"
}
