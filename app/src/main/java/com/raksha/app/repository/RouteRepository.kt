package com.raksha.app.repository

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.raksha.app.data.assets.NcrbDataSource
import com.raksha.app.utils.RouteScorer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class ScoredRoute(
    val name: String,
    val safetyScore: Double,        // 0.0–1.0, lower = safer
    val distanceMeters: Int,
    val durationSeconds: Int,
    val polylinePoints: List<LatLng>,
    val overview: String
)

@Singleton
class RouteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
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
            val status = json.getString("status")
            if (status != "OK") {
                return@withContext Result.failure(Exception("Directions API error: $status"))
            }

            val routes = json.getJSONArray("routes")
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

            val top2 = scoredRoutes.sortedBy { it.safetyScore }.take(2)
            Result.success(top2)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildDirectionsUrl(origin: LatLng, dest: LatLng, apiKey: String): String =
        "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=${origin.latitude},${origin.longitude}" +
            "&destination=${dest.latitude},${dest.longitude}" +
            "&alternatives=true" +
            "&key=$apiKey"
}
