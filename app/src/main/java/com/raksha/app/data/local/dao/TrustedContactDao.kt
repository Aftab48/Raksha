package com.raksha.app.data.local.dao

import androidx.room.*
import com.raksha.app.data.local.entity.TrustedContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedContactDao {
    @Query("SELECT * FROM trusted_contacts ORDER BY id ASC")
    fun getContacts(): Flow<List<TrustedContactEntity>>

    @Query("SELECT * FROM trusted_contacts ORDER BY id ASC")
    suspend fun getContactsOnce(): List<TrustedContactEntity>

    @Query("SELECT COUNT(*) FROM trusted_contacts")
    suspend fun getContactCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: TrustedContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<TrustedContactEntity>)

    @Delete
    suspend fun deleteContact(contact: TrustedContactEntity)

    @Query("DELETE FROM trusted_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Int)

    @Query("DELETE FROM trusted_contacts")
    suspend fun clearContacts()

    @Transaction
    suspend fun replaceContacts(contacts: List<TrustedContactEntity>) {
        clearContacts()
        if (contacts.isNotEmpty()) {
            insertContacts(contacts)
        }
    }
}
