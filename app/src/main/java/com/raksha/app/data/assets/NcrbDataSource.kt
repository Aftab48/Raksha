package com.raksha.app.data.assets

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class NcrbDistrict(
    val id: String,
    val name: String,
    val state: String,
    val lat: Double,
    val lng: Double,
    val incidentCount: Int,
    val incidentTypes: Map<String, Int>,
    val riskScore: Double
)

data class NcrbDataset(
    val version: String,
    val source: String,
    val districts: List<NcrbDistrict>
)

@Singleton
class NcrbDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    private val dataset: NcrbDataset by lazy {
        val json = context.assets.open("ncrb_crime_data.json")
            .bufferedReader()
            .use { it.readText() }
        gson.fromJson(json, NcrbDataset::class.java)
    }

    fun getAllDistricts(): List<NcrbDistrict> = dataset.districts

    /**
     * Returns districts within a rough bounding box of the given corridor.
     * Uses a simple lat/lng proximity check for district-level granularity.
     */
    fun getDistrictsNearCorridor(
        points: List<Pair<Double, Double>>,
        radiusDegrees: Double = 0.15
    ): List<NcrbDistrict> {
        if (points.isEmpty()) return emptyList()
        val minLat = points.minOf { it.first } - radiusDegrees
        val maxLat = points.maxOf { it.first } + radiusDegrees
        val minLng = points.minOf { it.second } - radiusDegrees
        val maxLng = points.maxOf { it.second } + radiusDegrees
        return dataset.districts.filter { d ->
            d.lat in minLat..maxLat && d.lng in minLng..maxLng
        }
    }

    fun getMaxIncidentCount(): Int =
        dataset.districts.maxOfOrNull { it.incidentCount } ?: 1
}
