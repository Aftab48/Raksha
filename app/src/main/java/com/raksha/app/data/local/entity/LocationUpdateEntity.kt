package com.raksha.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_updates",
    foreignKeys = [
        ForeignKey(
            entity = SosEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["sosEventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sosEventId")]
)
data class LocationUpdateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sosEventId: Int,
    val timestamp: String,  // ISO 8601
    val lat: Double,
    val lng: Double
)
