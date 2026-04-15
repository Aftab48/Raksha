package com.example.raksha.core.di

import android.content.Context
import androidx.room.Room
import com.example.raksha.core.database.RakshaDatabase
import com.example.raksha.core.datastore.AppPreferences
import com.example.raksha.feature_trusted_contacts.data.local.dao.TrustedContactsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppPreferences(
        @ApplicationContext context: Context
    ): AppPreferences {
        return AppPreferences(context)
    }

    @Provides
    @Singleton
    fun provideRakshaDatabase(
        @ApplicationContext context: Context
    ): RakshaDatabase {
        return Room.databaseBuilder(
            context,
            RakshaDatabase::class.java,
            RakshaDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideTrustedContactsDao(
        database: RakshaDatabase
    ): TrustedContactsDao {
        return database.trustedContactsDao()
    }
}
