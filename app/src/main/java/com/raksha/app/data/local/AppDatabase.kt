package com.raksha.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.raksha.app.data.local.dao.*
import com.raksha.app.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        TrustedContactEntity::class,
        SosEventEntity::class,
        LocationUpdateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun sosEventDao(): SosEventDao
    abstract fun locationUpdateDao(): LocationUpdateDao
}
