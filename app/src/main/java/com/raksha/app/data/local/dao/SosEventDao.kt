package com.raksha.app.data.local.dao

import androidx.room.*
import com.raksha.app.data.local.entity.SosEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SosEventDao {
    @Query("SELECT * FROM sos_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<SosEventEntity>>

    @Query("SELECT * FROM sos_events WHERE status = 'active' LIMIT 1")
    suspend fun getActiveEvent(): SosEventEntity?

    @Query("SELECT * FROM sos_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Int): SosEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SosEventEntity): Long

    @Query("UPDATE sos_events SET status = 'resolved' WHERE id = :id")
    suspend fun resolveEvent(id: Int)

    @Query("DELETE FROM sos_events")
    suspend fun clearAll()
}
