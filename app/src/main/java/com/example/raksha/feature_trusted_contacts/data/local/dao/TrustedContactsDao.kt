package com.example.raksha.feature_trusted_contacts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.raksha.feature_trusted_contacts.data.local.entity.TrustedContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedContactsDao {
    @Query("SELECT * FROM trusted_contacts ORDER BY priority ASC")
    fun observeTrustedContacts(): Flow<List<TrustedContactEntity>>

    @Query("SELECT * FROM trusted_contacts ORDER BY priority ASC")
    suspend fun getTrustedContacts(): List<TrustedContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrustedContacts(contacts: List<TrustedContactEntity>)

    @Query("DELETE FROM trusted_contacts")
    suspend fun clearTrustedContacts()

    @Transaction
    suspend fun replaceTrustedContacts(contacts: List<TrustedContactEntity>) {
        clearTrustedContacts()
        if (contacts.isNotEmpty()) {
            upsertTrustedContacts(contacts)
        }
    }
}
