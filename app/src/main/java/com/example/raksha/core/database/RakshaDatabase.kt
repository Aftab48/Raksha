package com.example.raksha.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.raksha.feature_trusted_contacts.data.local.dao.TrustedContactsDao
import com.example.raksha.feature_trusted_contacts.data.local.entity.TrustedContactEntity

@Database(
    entities = [TrustedContactEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RakshaDatabase : RoomDatabase() {
    abstract fun trustedContactsDao(): TrustedContactsDao

    companion object {
        const val DATABASE_NAME = "raksha.db"
    }
}
