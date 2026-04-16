package com.raksha.app.repository

import android.util.Log
import com.raksha.app.BuildConfig
import com.raksha.app.data.local.dao.LocationUpdateDao
import com.raksha.app.data.local.dao.SosEventDao
import com.raksha.app.data.local.entity.LocationUpdateEntity
import com.raksha.app.data.local.entity.SosEventEntity
import com.raksha.app.feature_sos_sync.data.remote.api.SosDashboardApi
import com.raksha.app.feature_sos_sync.data.remote.dto.CreateDashboardSosRequest
import com.raksha.app.feature_sos_sync.data.remote.dto.DashboardLocationUpdateRequest
import com.raksha.app.feature_sos_sync.data.remote.dto.DashboardResolveRequest
import com.raksha.app.feature_sos_sync.data.remote.dto.UserNote
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Singleton
class SosRepository @Inject constructor(
    private val sosEventDao: SosEventDao,
    private val locationUpdateDao: LocationUpdateDao,
    private val sosDashboardApi: SosDashboardApi
) {
    companion object {
        private const val TAG = "SosRepository"
    }

    val allEvents: Flow<List<SosEventEntity>> = sosEventDao.getAllEvents()

    suspend fun getActiveEvent(): SosEventEntity? = sosEventDao.getActiveEvent()

    suspend fun createSosEvent(
        lat: Double,
        lng: Double,
        confidenceScore: Double,
        triggerType: String,
        userName: String = "User",
        phone: String = "",
        incidentType: String = "sos",
        callRequested: Boolean = false
    ): Long {
        val now = Instant.now().toString()
        val event = SosEventEntity(
            timestamp = now,
            lat = lat,
            lng = lng,
            confidenceScore = confidenceScore,
            triggerType = triggerType,
            status = "active",
            incidentType = incidentType
        )

        val localEventId = sosEventDao.insertEvent(event)
        val localEvent = sosEventDao.getEventById(localEventId.toInt()) ?: return localEventId

        runCatching {
            val remote = sosDashboardApi.createSosAlert(
                request = CreateDashboardSosRequest(
                    user_name = userName.ifBlank { "User" },
                    phone = phone,
                    lat = lat,
                    lng = lng,
                    timestamp = now,
                    confidence_score = if (triggerType == "auto") confidenceScore else null,
                    trigger_type = triggerType,
                    device_id = "raksha-android",
                    incident_type = incidentType,
                    call_requested = callRequested
                ),
                apiKey = BuildConfig.SOS_INGEST_API_KEY.takeIf { it.isNotBlank() }
            )
            sosEventDao.setRemoteAlertId(localEvent.id, remote.sos_id)
        }.onFailure {
            Log.w(TAG, "Failed to sync SOS create to dashboard backend", it)
        }

        return localEventId
    }

    suspend fun getUserNotes(remoteAlertId: String, since: String? = null): List<UserNote> {
        return runCatching {
            sosDashboardApi.getUserNotes(alertId = remoteAlertId, since = since).notes
        }.getOrElse {
            Log.w(TAG, "Failed to fetch user notes for alert $remoteAlertId", it)
            emptyList()
        }
    }

    suspend fun logLocationUpdate(sosEventId: Int, lat: Double, lng: Double) {
        val timestamp = Instant.now().toString()
        locationUpdateDao.insertUpdate(
            LocationUpdateEntity(
                sosEventId = sosEventId,
                timestamp = timestamp,
                lat = lat,
                lng = lng
            )
        )

        val event = sosEventDao.getEventById(sosEventId) ?: return
        val remoteAlertId = event.remoteAlertId ?: return

        runCatching {
            sosDashboardApi.sendLocationUpdate(
                alertId = remoteAlertId,
                request = DashboardLocationUpdateRequest(
                    lat = lat,
                    lng = lng,
                    timestamp = timestamp
                ),
                apiKey = BuildConfig.SOS_INGEST_API_KEY.takeIf { it.isNotBlank() }
            )
        }.onFailure {
            Log.w(TAG, "Failed to sync location update for alert $remoteAlertId", it)
        }
    }

    suspend fun resolveEvent(
        eventId: Int,
        resolvedBy: String = "Raksha User",
        notes: String? = null,
        falseAlert: Boolean = false
    ) {
        val event = sosEventDao.getEventById(eventId)
        sosEventDao.resolveEvent(eventId)

        val remoteAlertId = event?.remoteAlertId ?: return
        runCatching {
            sosDashboardApi.resolveAlert(
                alertId = remoteAlertId,
                request = DashboardResolveRequest(
                    resolved_by = resolvedBy,
                    notes = notes,
                    false_alert = falseAlert
                )
            )
        }.onFailure {
            Log.w(TAG, "Failed to sync resolve for alert $remoteAlertId", it)
        }
    }

    suspend fun clearHistory() {
        sosEventDao.clearAll()
    }

    fun getLocationUpdates(sosEventId: Int): Flow<List<LocationUpdateEntity>> =
        locationUpdateDao.getUpdatesForEvent(sosEventId)
}
