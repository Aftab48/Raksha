package com.raksha.app.data.local.dao

import androidx.room.*
import com.raksha.app.data.local.entity.LocationUpdateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationUpdateDao {
    @Query("SELECT * FROM location_updates WHERE sosEventId = :sosEventId ORDER BY timestamp DESC")
    fun getUpdatesForEvent(sosEventId: Int): Flow<List<LocationUpdateEntity>>

    @Query("SELECT * FROM location_updates WHERE sosEventId = :sosEventId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestUpdate(sosEventId: Int): LocationUpdateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpdate(update: LocationUpdateEntity)
}
