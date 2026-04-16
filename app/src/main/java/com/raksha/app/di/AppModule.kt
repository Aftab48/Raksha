package com.raksha.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.raksha.app.data.local.AppDatabase
import com.raksha.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE sos_events ADD COLUMN incidentType TEXT NOT NULL DEFAULT 'sos'"
        )
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "raksha_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "raksha_db")
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideTrustedContactDao(db: AppDatabase): TrustedContactDao = db.trustedContactDao()

    @Provides
    fun provideSosEventDao(db: AppDatabase): SosEventDao = db.sosEventDao()

    @Provides
    fun provideLocationUpdateDao(db: AppDatabase): LocationUpdateDao = db.locationUpdateDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}
