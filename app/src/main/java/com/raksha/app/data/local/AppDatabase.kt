package com.raksha.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.raksha.app.data.local.dao.LocationUpdateDao
import com.raksha.app.data.local.dao.SosEventDao
import com.raksha.app.data.local.dao.TrustedContactDao
import com.raksha.app.data.local.dao.UserDao
import com.raksha.app.data.local.entity.LocationUpdateEntity
import com.raksha.app.data.local.entity.SosEventEntity
import com.raksha.app.data.local.entity.TrustedContactEntity
import com.raksha.app.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        TrustedContactEntity::class,
        SosEventEntity::class,
        LocationUpdateEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun sosEventDao(): SosEventDao
    abstract fun locationUpdateDao(): LocationUpdateDao
}
