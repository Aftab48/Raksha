package com.example.raksha.feature_trusted_contacts.data.local.cache

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.raksha.feature_trusted_contacts.data.local.entity.TrustedContactEntity
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.trustedContactsCacheDataStore by preferencesDataStore(name = "trusted_contacts_cache")

@Singleton
class TrustedContactsLocalCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val trustedContactsKey = stringPreferencesKey("trusted_contacts_json")

    suspend fun saveContacts(contacts: List<TrustedContactEntity>) {
        context.trustedContactsCacheDataStore.edit { preferences ->
            preferences[trustedContactsKey] = gson.toJson(contacts)
        }
    }

    suspend fun getContacts(): List<TrustedContactEntity> {
        val cachedJson = context.trustedContactsCacheDataStore.data
            .map { preferences -> preferences[trustedContactsKey].orEmpty() }
            .first()

        if (cachedJson.isBlank()) {
            return emptyList()
        }

        return gson.fromJson(cachedJson, Array<TrustedContactEntity>::class.java)
            ?.toList()
            .orEmpty()
    }
}
