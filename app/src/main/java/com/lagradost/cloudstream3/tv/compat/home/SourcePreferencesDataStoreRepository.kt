package com.lagradost.cloudstream3.tv.compat.home

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fasterxml.jackson.core.type.TypeReference
import com.lagradost.cloudstream3.utils.DataStore.mapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val TV_HOME_SOURCES_DATASTORE = "tv_home_sources_preferences"
private val Context.tvHomeSourcesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = TV_HOME_SOURCES_DATASTORE
)

private val PINNED_SOURCES_KEY = stringSetPreferencesKey("pinned_sources")
private val USAGE_COUNTS_JSON_KEY = stringPreferencesKey("usage_counts_json")
private val LAST_SELECTED_SOURCE_ID_KEY = stringPreferencesKey("last_selected_source_id")

private val USAGE_COUNTS_TYPE = object : TypeReference<Map<String, Int>>() {}

class SourcePreferencesDataStoreRepository(
    private val context: Context,
) : SourcePreferencesRepository {

    override val state: Flow<SourcePreferencesState> = context.tvHomeSourcesDataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            SourcePreferencesState(
                pinnedSourceIds = preferences[PINNED_SOURCES_KEY] ?: emptySet(),
                usageCountBySourceId = decodeUsageCounts(preferences[USAGE_COUNTS_JSON_KEY]),
                lastSelectedSourceId = preferences[LAST_SELECTED_SOURCE_ID_KEY]
            )
        }

    override suspend fun setPinned(sourceId: String, pinned: Boolean) {
        context.tvHomeSourcesDataStore.edit { preferences ->
            val updatedPinned = (preferences[PINNED_SOURCES_KEY] ?: emptySet()).toMutableSet()
            if (pinned) {
                updatedPinned.add(sourceId)
            } else {
                updatedPinned.remove(sourceId)
            }
            preferences[PINNED_SOURCES_KEY] = updatedPinned
        }
    }

    override suspend fun incrementUsage(sourceId: String) {
        context.tvHomeSourcesDataStore.edit { preferences ->
            val usageCounts = decodeUsageCounts(preferences[USAGE_COUNTS_JSON_KEY]).toMutableMap()
            usageCounts[sourceId] = (usageCounts[sourceId] ?: 0) + 1
            preferences[USAGE_COUNTS_JSON_KEY] = encodeUsageCounts(usageCounts)
        }
    }

    override suspend fun setLastSelected(sourceId: String?) {
        context.tvHomeSourcesDataStore.edit { preferences ->
            if (sourceId.isNullOrBlank()) {
                preferences.remove(LAST_SELECTED_SOURCE_ID_KEY)
            } else {
                preferences[LAST_SELECTED_SOURCE_ID_KEY] = sourceId
            }
        }
    }

    private fun decodeUsageCounts(rawJson: String?): Map<String, Int> {
        if (rawJson.isNullOrBlank()) return emptyMap()

        return runCatching {
            mapper.readValue(rawJson, USAGE_COUNTS_TYPE)
        }.getOrDefault(emptyMap())
            .filterValues { it > 0 }
    }

    private fun encodeUsageCounts(usageCounts: Map<String, Int>): String {
        return runCatching {
            mapper.writeValueAsString(usageCounts.filterValues { it > 0 })
        }.getOrDefault("{}")
    }
}
