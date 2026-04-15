package com.raksha.app.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.raksha.app.data.local.dao.UserDao
import com.raksha.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    val user: Flow<UserEntity?> = userDao.getUser()

    val isOnboardingComplete: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETE] ?: false
    }

    suspend fun saveUser(name: String, phone: String) {
        userDao.clearUser()
        userDao.insertUser(UserEntity(name = name, phone = phone))
    }

    suspend fun getUserOnce(): UserEntity? = userDao.getUserOnce()

    suspend fun markOnboardingComplete() {
        dataStore.edit { prefs -> prefs[KEY_ONBOARDING_COMPLETE] = true }
    }
}
