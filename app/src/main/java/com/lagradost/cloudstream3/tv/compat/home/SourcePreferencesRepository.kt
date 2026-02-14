package com.lagradost.cloudstream3.tv.compat.home

import kotlinx.coroutines.flow.Flow

data class SourcePreferencesState(
    val pinnedSourceIds: Set<String> = emptySet(),
    val usageCountBySourceId: Map<String, Int> = emptyMap(),
    val lastSelectedSourceId: String? = null,
)

interface SourcePreferencesRepository {
    val state: Flow<SourcePreferencesState>

    suspend fun setPinned(sourceId: String, pinned: Boolean)

    suspend fun incrementUsage(sourceId: String)

    suspend fun setLastSelected(sourceId: String?)
}

