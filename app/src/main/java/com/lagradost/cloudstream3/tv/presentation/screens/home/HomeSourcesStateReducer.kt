package com.lagradost.cloudstream3.tv.presentation.screens.home

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.tv.compat.home.SourcePreferencesState
import com.lagradost.cloudstream3.tv.compat.home.sourceId
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import java.util.Locale

private const val QUICK_SOURCES_LIMIT = 10

internal data class HomeSourcesRefreshResult(
    val state: HomeSourcesUiState,
    val fallbackSource: MainAPI? = null,
)

internal object HomeSourcesStateReducer {

    fun refresh(
        state: HomeSourcesUiState,
        availableSources: List<MainAPI>,
        rerankQuickSources: Boolean,
        allowSelectionFallback: Boolean,
    ): HomeSourcesRefreshResult {
        val resolvedSelection = resolveCurrentSelection(
            availableSources = availableSources,
            currentSelection = state.selectedSource,
        )
        val effectiveSelection = resolvedSelection ?: if (allowSelectionFallback) {
            availableSources.firstOrNull()
        } else {
            null
        }

        val quickSources = if (rerankQuickSources || state.quickSources.isEmpty()) {
            rankQuickSources(
                sources = availableSources,
                pinnedSourceIds = state.pinnedSourceIds,
                usageCountBySourceId = state.usageCountBySourceId,
                selectedSource = effectiveSelection
            )
        } else {
            maintainQuickSources(
                currentQuick = state.quickSources,
                allSources = availableSources,
                pinnedSourceIds = state.pinnedSourceIds,
                usageCountBySourceId = state.usageCountBySourceId,
                selectedSource = effectiveSelection
            )
        }

        val fallbackSource = if (resolvedSelection == null && allowSelectionFallback) {
            effectiveSelection
        } else {
            null
        }

        return HomeSourcesRefreshResult(
            state = state.copy(
                allSources = availableSources.toPersistentList(),
                selectedSource = effectiveSelection,
                quickSources = quickSources,
                morePanelSources = buildMorePanelSources(
                    sources = availableSources,
                    usageCountBySourceId = state.usageCountBySourceId
                )
            ),
            fallbackSource = fallbackSource
        )
    }

    fun applyPreferences(
        state: HomeSourcesUiState,
        preferences: SourcePreferencesState,
    ): HomeSourcesUiState {
        val resolvedSelection = resolveCurrentSelection(
            availableSources = state.allSources,
            currentSelection = state.selectedSource,
        )

        val quickSources = maintainQuickSources(
            currentQuick = state.quickSources,
            allSources = state.allSources,
            pinnedSourceIds = preferences.pinnedSourceIds,
            usageCountBySourceId = preferences.usageCountBySourceId,
            selectedSource = resolvedSelection
        )

        return state.copy(
            selectedSource = resolvedSelection,
            pinnedSourceIds = preferences.pinnedSourceIds.toPersistentSet(),
            usageCountBySourceId = preferences.usageCountBySourceId.toPersistentMap(),
            quickSources = quickSources,
            morePanelSources = buildMorePanelSources(
                sources = state.allSources,
                usageCountBySourceId = preferences.usageCountBySourceId
            )
        )
    }

    fun syncSelectedSource(
        state: HomeSourcesUiState,
        selectedSource: MainAPI?,
    ): HomeSourcesUiState {
        return state.copy(
            selectedSource = selectedSource,
            quickSources = ensureSelectedInQuickSources(
                currentQuick = state.quickSources,
                allSources = state.allSources,
                selectedSource = selectedSource
            )
        )
    }

    private fun resolveCurrentSelection(
        availableSources: List<MainAPI>,
        currentSelection: MainAPI?,
    ): MainAPI? {
        if (availableSources.isEmpty()) return null

        currentSelection?.sourceId()?.let { currentId ->
            availableSources.firstOrNull { source -> source.sourceId() == currentId }?.let { source ->
                return source
            }
        }

        return null
    }

    private fun rankQuickSources(
        sources: List<MainAPI>,
        pinnedSourceIds: Set<String>,
        usageCountBySourceId: Map<String, Int>,
        selectedSource: MainAPI?,
    ): PersistentList<MainAPI> {
        if (sources.isEmpty()) return persistentListOf()

        val pinned = sources
            .filter { source -> pinnedSourceIds.contains(source.sourceId()) }
            .sortedBy { source -> source.name.lowercase(Locale.ROOT) }

        val byUsage = sources
            .filterNot { source -> pinnedSourceIds.contains(source.sourceId()) }
            .sortedWith(
                compareByDescending<MainAPI> { source -> usageCountBySourceId[source.sourceId()] ?: 0 }
                    .thenBy { source -> source.name.lowercase(Locale.ROOT) }
            )

        val combined = (pinned + byUsage)
            .distinctBy { source -> source.sourceId() }
            .take(QUICK_SOURCES_LIMIT)
            .toPersistentList()

        return ensureSelectedInQuickSources(
            currentQuick = combined,
            allSources = sources,
            selectedSource = selectedSource
        )
    }

    private fun maintainQuickSources(
        currentQuick: List<MainAPI>,
        allSources: List<MainAPI>,
        pinnedSourceIds: Set<String>,
        usageCountBySourceId: Map<String, Int>,
        selectedSource: MainAPI?,
    ): PersistentList<MainAPI> {
        if (allSources.isEmpty()) return persistentListOf()

        val sourceById = allSources.associateBy { source -> source.sourceId() }

        // WHY: nie przetasowujemy istniejącego quick row przy każdym odświeżeniu listy providerów,
        // żeby focus i pamięć mięśniowa na TV pozostały stabilne podczas startu i zmian źródeł.
        var keptQuick = currentQuick
            .mapNotNull { quick -> sourceById[quick.sourceId()] }
            .distinctBy { source -> source.sourceId() }
            .toPersistentList()

        if (keptQuick.size < QUICK_SOURCES_LIMIT) {
            val ranked = rankQuickSources(
                sources = allSources,
                pinnedSourceIds = pinnedSourceIds,
                usageCountBySourceId = usageCountBySourceId,
                selectedSource = null
            )

            ranked.forEach { candidate ->
                if (keptQuick.size >= QUICK_SOURCES_LIMIT) return@forEach
                if (keptQuick.none { quick -> quick.sourceId() == candidate.sourceId() }) {
                    keptQuick = keptQuick.add(candidate)
                }
            }
        }

        return ensureSelectedInQuickSources(
            currentQuick = keptQuick,
            allSources = allSources,
            selectedSource = selectedSource
        )
    }

    private fun ensureSelectedInQuickSources(
        currentQuick: List<MainAPI>,
        allSources: List<MainAPI>,
        selectedSource: MainAPI?,
    ): PersistentList<MainAPI> {
        if (allSources.isEmpty()) return persistentListOf()

        val sourceById = allSources.associateBy { source -> source.sourceId() }
        var quickSources = currentQuick
            .mapNotNull { quick -> sourceById[quick.sourceId()] }
            .distinctBy { source -> source.sourceId() }
            .take(QUICK_SOURCES_LIMIT)
            .toPersistentList()

        val selected = selectedSource?.let { sourceById[it.sourceId()] ?: it }
        if (selected != null && quickSources.none { source -> source.sourceId() == selected.sourceId() }) {
            quickSources = when {
                quickSources.size < QUICK_SOURCES_LIMIT -> quickSources.add(selected)
                quickSources.isNotEmpty() -> quickSources.set(quickSources.lastIndex, selected)
                else -> quickSources.add(selected)
            }
        }

        return quickSources
            .distinctBy { source -> source.sourceId() }
            .take(QUICK_SOURCES_LIMIT)
            .toPersistentList()
    }

    private fun buildMorePanelSources(
        sources: List<MainAPI>,
        usageCountBySourceId: Map<String, Int>,
    ): PersistentList<MainAPI> {
        return sources
            .sortedWith(
                compareByDescending<MainAPI> { source ->
                    usageCountBySourceId[source.sourceId()] ?: 0
                }.thenBy { source ->
                    source.name.lowercase(Locale.ROOT)
                }
            )
            .toPersistentList()
    }
}
