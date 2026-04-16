package com.raksha.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sos_events")
data class SosEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: String, // ISO 8601
    val lat: Double,
    val lng: Double,
    val confidenceScore: Double, // TFLite output score 0.0-1.0, 0.0 if manual
    val triggerType: String, // "auto" or "manual"
    val status: String, // "active" or "resolved"
    val remoteAlertId: String? = null,
    val incidentType: String = "sos" // "sos" or "panic"
)
