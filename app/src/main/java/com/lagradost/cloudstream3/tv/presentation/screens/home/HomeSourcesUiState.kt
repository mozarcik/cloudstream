package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.MainAPI
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Immutable
data class HomeSourcesUiState(
    val selectedSource: MainAPI? = null,
    val allSources: PersistentList<MainAPI> = persistentListOf(),
    val quickSources: PersistentList<MainAPI> = persistentListOf(),
    val morePanelSources: PersistentList<MainAPI> = persistentListOf(),
    val pinnedSourceIds: PersistentSet<String> = persistentSetOf(),
    val usageCountBySourceId: PersistentMap<String, Int> = persistentHashMapOf(),
    val isMorePanelOpen: Boolean = false,
)
