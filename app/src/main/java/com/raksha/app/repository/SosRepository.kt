package com.raksha.app.repository

import com.raksha.app.data.local.dao.LocationUpdateDao
import com.raksha.app.data.local.dao.SosEventDao
import com.raksha.app.data.local.entity.LocationUpdateEntity
import com.raksha.app.data.local.entity.SosEventEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SosRepository @Inject constructor(
    private val sosEventDao: SosEventDao,
    private val locationUpdateDao: LocationUpdateDao
) {
    val allEvents: Flow<List<SosEventEntity>> = sosEventDao.getAllEvents()

    suspend fun getActiveEvent(): SosEventEntity? = sosEventDao.getActiveEvent()

    suspend fun createSosEvent(
        lat: Double,
        lng: Double,
        confidenceScore: Double,
        triggerType: String
    ): Long {
        val event = SosEventEntity(
            timestamp = Instant.now().toString(),
            lat = lat,
            lng = lng,
            confidenceScore = confidenceScore,
            triggerType = triggerType,
            status = "active"
        )
        return sosEventDao.insertEvent(event)
    }

    suspend fun logLocationUpdate(sosEventId: Int, lat: Double, lng: Double) {
        locationUpdateDao.insertUpdate(
            LocationUpdateEntity(
                sosEventId = sosEventId,
                timestamp = Instant.now().toString(),
                lat = lat,
                lng = lng
            )
        )
    }

    suspend fun resolveEvent(eventId: Int) {
        sosEventDao.resolveEvent(eventId)
    }

    suspend fun clearHistory() {
        sosEventDao.clearAll()
    }

    fun getLocationUpdates(sosEventId: Int): Flow<List<LocationUpdateEntity>> =
        locationUpdateDao.getUpdatesForEvent(sosEventId)
}
