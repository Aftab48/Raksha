package com.raksha.app.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class HelpKeywordRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val KEY_HELP_KEYWORDS = stringSetPreferencesKey("help_keywords")
        private const val MAX_KEYWORDS = 15
    }

    val keywords: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[KEY_HELP_KEYWORDS]
            ?.map { normalizeKeyword(it) }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?.sorted()
            ?: emptyList()
    }

    suspend fun getKeywordsOnce(): List<String> = keywords.first()

    suspend fun addKeyword(rawKeyword: String): Result<Unit> {
        val keyword = normalizeKeyword(rawKeyword)
        if (keyword.isBlank()) {
            return Result.failure(Exception("Keyword cannot be blank"))
        }
        return runCatching {
            dataStore.edit { prefs ->
                val current = prefs[KEY_HELP_KEYWORDS].orEmpty()
                    .map { normalizeKeyword(it) }
                    .filter { it.isNotBlank() }
                    .toMutableSet()
                if (current.size >= MAX_KEYWORDS) {
                    throw IllegalStateException("You can add up to $MAX_KEYWORDS keywords")
                }
                current.add(keyword)
                prefs[KEY_HELP_KEYWORDS] = current
            }
        }.map { Unit }
    }

    suspend fun removeKeyword(rawKeyword: String) {
        val keyword = normalizeKeyword(rawKeyword)
        dataStore.edit { prefs ->
            val current = prefs[KEY_HELP_KEYWORDS].orEmpty()
                .map { normalizeKeyword(it) }
                .filter { it.isNotBlank() }
                .toMutableSet()
            current.remove(keyword)
            prefs[KEY_HELP_KEYWORDS] = current
        }
    }

    private fun normalizeKeyword(value: String): String {
        return value.trim().lowercase().replace(Regex("\\s+"), " ")
    }
}
