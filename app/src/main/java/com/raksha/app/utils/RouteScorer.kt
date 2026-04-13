package com.raksha.app.utils

import com.raksha.app.data.assets.NcrbDataSource
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteScorer @Inject constructor() {

    /**
     * safety_score = (incident_density_weight * 0.5)
     *              + (time_of_day_weight * 0.3)
     *              + (route_length_weight * 0.2)
     *
     * Lower score = safer.
     */
    fun score(
        routePoints: List<Pair<Double, Double>>,
        distanceMeters: Int,
        ncrbDataSource: NcrbDataSource
    ): Double {
        val nearbyDistricts = ncrbDataSource.getDistrictsNearCorridor(routePoints)
        val maxCount = ncrbDataSource.getMaxIncidentCount().toDouble().coerceAtLeast(1.0)

        val avgIncidentDensity = if (nearbyDistricts.isEmpty()) 0.5
        else nearbyDistricts.map { it.incidentCount / maxCount }.average()

        val timeWeight = getTimeOfDayWeight()

        // Normalize distance: cap at 20km, shorter preferred
        val maxDistance = 20_000.0
        val routeLengthWeight = (distanceMeters / maxDistance).coerceIn(0.0, 1.0)

        return (avgIncidentDensity * 0.5) + (timeWeight * 0.3) + (routeLengthWeight * 0.2)
    }

    fun getTimeOfDayWeight(): Double {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 6..19 -> 0.2   // 6am–8pm: day
            hour in 20..22 -> 0.7  // 8pm–11pm: evening
            else -> 1.0            // 11pm–6am: night
        }
    }
}
