package com.lagradost.cloudstream3.tv.presentation.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import com.lagradost.cloudstream3.tv.compat.home.FeedRepository
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.compat.home.SourcePreferencesRepository
import com.lagradost.cloudstream3.tv.compat.home.SourcePreferencesState
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository
import com.lagradost.cloudstream3.tv.compat.home.sourceId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private const val FEED_PRELOAD_SIZE = 20
private const val CONTINUE_WATCHING_FALLBACK_NAME = "Continue Watching"
private const val QUICK_SOURCES_LIMIT = 10

sealed interface HomeFeedLoadState {
    data object Loading : HomeFeedLoadState
    data object Error : HomeFeedLoadState
    data class Success(val items: List<MediaItemCompat>) : HomeFeedLoadState
}

enum class SourceSortMode {
    MOST_USED,
    AZ,
}

data class HomeFeedSectionUiState(
    val feed: FeedCategory,
    val state: HomeFeedLoadState = HomeFeedLoadState.Loading,
)

data class HomeScreenV2UiState(
    val selectedSource: MainAPI? = null,
    val allSources: List<MainAPI> = emptyList(),
    val quickSources: List<MainAPI> = emptyList(),
    val morePanelSources: List<MainAPI> = emptyList(),
    val pinnedSourceIds: Set<String> = emptySet(),
    val usageCountBySourceId: Map<String, Int> = emptyMap(),
    val lastSelectedSourceId: String? = null,
    val sortMode: SourceSortMode = SourceSortMode.MOST_USED,
    val searchQuery: String = "",
    val isMorePanelOpen: Boolean = false,
    val continueWatchingState: HomeFeedLoadState = HomeFeedLoadState.Loading,
    val feedSections: List<HomeFeedSectionUiState> = emptyList(),
    val isFeedListLoading: Boolean = true,
)

class HomeScreenV2ViewModel(
    private val feedRepository: FeedRepository,
    private val sourcePreferencesRepository: SourcePreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeScreenV2UiState())
    val uiState = _uiState.asStateFlow()

    private var homeLoadJob: Job? = null

    init {
        observeSourcePreferences()
        observeSelectedSource()
        observeAvailableSourcesPool()

        viewModelScope.launch {
            val initialPreferences = sourcePreferencesRepository.state.first()
            SourceRepository.waitForApiOrNull()

            val availableSources = SourceRepository.getAvailableApis()
            val restoredSource = initialPreferences.lastSelectedSourceId
                ?.let { savedId ->
                    availableSources.firstOrNull { source ->
                        source.sourceId() == savedId
                    }
                }

            if (restoredSource != null) {
                SourceRepository.selectApi(restoredSource)
            }

            refreshSources(rerankQuickSources = true)

            val selectedApi = SourceRepository.selectedApi.first()
            if (selectedApi == null) {
                _uiState.value.selectedSource?.let { selected ->
                    loadHomeForApi(selected)
                }
            }
        }
    }

    private fun observeAvailableSourcesPool() {
        viewModelScope.launch {
            var previousSourceIds: Set<String> = emptySet()

            repeat(60) {
                val availableSourceIds = SourceRepository.getAvailableApis()
                    .map { source -> source.sourceId() }
                    .toSet()

                if (availableSourceIds != previousSourceIds) {
                    previousSourceIds = availableSourceIds
                    refreshSources(rerankQuickSources = false)
                }

                delay(500)
            }
        }
    }

    fun refreshSources(rerankQuickSources: Boolean = false) {
        val availableSources = SourceRepository.getAvailableApis()
        var fallbackToSelect: MainAPI? = null

        _uiState.update { state ->
            val resolvedSelection = resolveSelection(
                availableSources = availableSources,
                currentSelection = state.selectedSource,
                lastSelectedSourceId = state.lastSelectedSourceId
            )

            if (state.selectedSource?.sourceId() != resolvedSelection?.sourceId()) {
                fallbackToSelect = resolvedSelection
            }

            val quickSources = if (rerankQuickSources || state.quickSources.isEmpty()) {
                rankQuickSources(
                    sources = availableSources,
                    pinnedSourceIds = state.pinnedSourceIds,
                    usageCountBySourceId = state.usageCountBySourceId,
                    selectedSource = resolvedSelection
                )
            } else {
                maintainQuickSources(
                    currentQuick = state.quickSources,
                    allSources = availableSources,
                    pinnedSourceIds = state.pinnedSourceIds,
                    usageCountBySourceId = state.usageCountBySourceId,
                    selectedSource = resolvedSelection
                )
            }

            state.copy(
                allSources = availableSources,
                selectedSource = resolvedSelection,
                quickSources = quickSources,
                morePanelSources = buildMorePanelSources(
                    sources = availableSources,
                    pinnedSourceIds = state.pinnedSourceIds,
                    usageCountBySourceId = state.usageCountBySourceId,
                    sortMode = state.sortMode,
                    searchQuery = state.searchQuery
                )
            )
        }

        fallbackToSelect?.let { source ->
            SourceRepository.selectApi(source)
            viewModelScope.launch {
                sourcePreferencesRepository.setLastSelected(source.sourceId())
            }
        }
    }

    fun selectSource(source: MainAPI) {
        val selectedSourceId = source.sourceId()
        val currentSelectedId = _uiState.value.selectedSource?.sourceId()
        if (selectedSourceId == currentSelectedId) return

        SourceRepository.selectApi(source)

        viewModelScope.launch {
            sourcePreferencesRepository.setLastSelected(selectedSourceId)
            sourcePreferencesRepository.incrementUsage(selectedSourceId)
        }

        _uiState.update { state ->
            state.copy(
                selectedSource = source,
                quickSources = ensureSelectedInQuickSources(
                    currentQuick = state.quickSources,
                    allSources = state.allSources,
                    selectedSource = source
                )
            )
        }
    }

    fun setMorePanelOpen(isOpen: Boolean) {
        _uiState.update { state ->
            state.copy(isMorePanelOpen = isOpen)
        }

        if (isOpen) {
            refreshSources(rerankQuickSources = false)
        } else {
            refreshSources(rerankQuickSources = true)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                morePanelSources = buildMorePanelSources(
                    sources = state.allSources,
                    pinnedSourceIds = state.pinnedSourceIds,
                    usageCountBySourceId = state.usageCountBySourceId,
                    sortMode = state.sortMode,
                    searchQuery = query
                )
            )
        }
    }

    fun setSortMode(sortMode: SourceSortMode) {
        _uiState.update { state ->
            state.copy(
                sortMode = sortMode,
                morePanelSources = buildMorePanelSources(
                    sources = state.allSources,
                    pinnedSourceIds = state.pinnedSourceIds,
                    usageCountBySourceId = state.usageCountBySourceId,
                    sortMode = sortMode,
                    searchQuery = state.searchQuery
                )
            )
        }
    }

    fun togglePinned(source: MainAPI) {
        val sourceId = source.sourceId()
        val currentlyPinned = _uiState.value.pinnedSourceIds.contains(sourceId)

        viewModelScope.launch {
            sourcePreferencesRepository.setPinned(sourceId, !currentlyPinned)
        }
    }

    private fun observeSourcePreferences() {
        viewModelScope.launch {
            sourcePreferencesRepository.state.collect { preferences ->
                applySourcePreferences(preferences)
            }
        }
    }

    private fun applySourcePreferences(preferences: SourcePreferencesState) {
        var fallbackToSelect: MainAPI? = null

        _uiState.update { state ->
            val resolvedSelection = resolveSelection(
                availableSources = state.allSources,
                currentSelection = state.selectedSource,
                lastSelectedSourceId = preferences.lastSelectedSourceId
            )

            if (state.selectedSource?.sourceId() != resolvedSelection?.sourceId()) {
                fallbackToSelect = resolvedSelection
            }

            val quickSources = maintainQuickSources(
                currentQuick = state.quickSources,
                allSources = state.allSources,
                pinnedSourceIds = preferences.pinnedSourceIds,
                usageCountBySourceId = preferences.usageCountBySourceId,
                selectedSource = resolvedSelection
            )

            state.copy(
                selectedSource = resolvedSelection,
                pinnedSourceIds = preferences.pinnedSourceIds,
                usageCountBySourceId = preferences.usageCountBySourceId,
                lastSelectedSourceId = preferences.lastSelectedSourceId,
                quickSources = quickSources,
                morePanelSources = buildMorePanelSources(
                    sources = state.allSources,
                    pinnedSourceIds = preferences.pinnedSourceIds,
                    usageCountBySourceId = preferences.usageCountBySourceId,
                    sortMode = state.sortMode,
                    searchQuery = state.searchQuery
                )
            )
        }

        fallbackToSelect?.let { source ->
            SourceRepository.selectApi(source)
        }
    }

    private fun observeSelectedSource() {
        viewModelScope.launch {
            SourceRepository.selectedApi.collect { selectedApi ->
                _uiState.update { state ->
                    state.copy(
                        selectedSource = selectedApi,
                        quickSources = ensureSelectedInQuickSources(
                            currentQuick = state.quickSources,
                            allSources = state.allSources,
                            selectedSource = selectedApi
                        )
                    )
                }

                selectedApi?.let { api ->
                    loadHomeForApi(api)
                }
            }
        }
    }

    private fun loadHomeForApi(api: MainAPI) {
        homeLoadJob?.cancel()
        homeLoadJob = viewModelScope.launch {
            refreshSources(rerankQuickSources = false)

            _uiState.update { state ->
                state.copy(
                    continueWatchingState = HomeFeedLoadState.Loading,
                    isFeedListLoading = true,
                    feedSections = state.feedSections.map {
                        it.copy(state = HomeFeedLoadState.Loading)
                    }
                )
            }

            launch {
                loadContinueWatching(api)
            }

            val categories = runCatching {
                feedRepository.getFeedCategories(api).first()
            }.onFailure { throwable ->
                Log.e(TAG, "Failed to load feed categories for ${api.name}", throwable)
            }.getOrDefault(emptyList())

            val feedSections = categories
                .filterNot { category -> category.isContinueWatching }
                .map { category ->
                    HomeFeedSectionUiState(
                        feed = category,
                        state = HomeFeedLoadState.Loading
                    )
                }

            _uiState.update { state ->
                state.copy(
                    feedSections = feedSections,
                    isFeedListLoading = false
                )
            }

            feedSections.forEach { section ->
                launch {
                    loadFeedSection(api, section.feed)
                }
            }

        }
    }

    private suspend fun loadContinueWatching(api: MainAPI) {
        val state = feedRepository
            .getMediaForFeed(api, FeedCategory.continueWatching(CONTINUE_WATCHING_FALLBACK_NAME), page = 1)
            .fold(
                onSuccess = { items ->
                    HomeFeedLoadState.Success(items.take(FEED_PRELOAD_SIZE))
                },
                onFailure = { throwable ->
                    Log.e(TAG, "Failed to load continue watching", throwable)
                    HomeFeedLoadState.Error
                }
            )

        _uiState.update { currentState ->
            currentState.copy(continueWatchingState = state)
        }
    }

    private suspend fun loadFeedSection(
        api: MainAPI,
        feed: FeedCategory,
    ) {
        val sectionState = feedRepository
            .getMediaForFeed(api, feed, page = 1)
            .fold(
                onSuccess = { items ->
                    HomeFeedLoadState.Success(items.take(FEED_PRELOAD_SIZE))
                },
                onFailure = { throwable ->
                    Log.e(TAG, "Failed to load feed section '${feed.name}'", throwable)
                    HomeFeedLoadState.Error
                }
            )

        _uiState.update { state ->
            state.copy(
                feedSections = state.feedSections.map { section ->
                    if (section.feed.id == feed.id) {
                        section.copy(state = sectionState)
                    } else {
                        section
                    }
                }
            )
        }
    }

    private fun resolveSelection(
        availableSources: List<MainAPI>,
        currentSelection: MainAPI?,
        lastSelectedSourceId: String?,
    ): MainAPI? {
        if (availableSources.isEmpty()) return null

        currentSelection?.sourceId()?.let { currentId ->
            availableSources.firstOrNull { source -> source.sourceId() == currentId }?.let { source ->
                return source
            }
        }

        if (!lastSelectedSourceId.isNullOrBlank()) {
            availableSources.firstOrNull { source -> source.sourceId() == lastSelectedSourceId }?.let { source ->
                return source
            }
        }

        return availableSources.firstOrNull()
    }

    private fun rankQuickSources(
        sources: List<MainAPI>,
        pinnedSourceIds: Set<String>,
        usageCountBySourceId: Map<String, Int>,
        selectedSource: MainAPI?,
    ): List<MainAPI> {
        if (sources.isEmpty()) return emptyList()

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
    ): List<MainAPI> {
        if (allSources.isEmpty()) return emptyList()

        val sourceById = allSources.associateBy { source -> source.sourceId() }
        val keptQuick = currentQuick
            .mapNotNull { quick -> sourceById[quick.sourceId()] }
            .distinctBy { source -> source.sourceId() }
            .toMutableList()

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
                    keptQuick += candidate
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
    ): List<MainAPI> {
        if (allSources.isEmpty()) return emptyList()

        val sourceById = allSources.associateBy { source -> source.sourceId() }
        val quickSources = currentQuick
            .mapNotNull { quick -> sourceById[quick.sourceId()] }
            .distinctBy { source -> source.sourceId() }
            .take(QUICK_SOURCES_LIMIT)
            .toMutableList()

        val selected = selectedSource?.let { sourceById[it.sourceId()] ?: it }
        if (selected != null && quickSources.none { source -> source.sourceId() == selected.sourceId() }) {
            if (quickSources.size < QUICK_SOURCES_LIMIT) {
                quickSources += selected
            } else if (quickSources.isNotEmpty()) {
                quickSources[quickSources.lastIndex] = selected
            } else {
                quickSources += selected
            }
        }

        return quickSources
            .distinctBy { source -> source.sourceId() }
            .take(QUICK_SOURCES_LIMIT)
    }

    private fun buildMorePanelSources(
        sources: List<MainAPI>,
        pinnedSourceIds: Set<String>,
        usageCountBySourceId: Map<String, Int>,
        sortMode: SourceSortMode,
        searchQuery: String,
    ): List<MainAPI> {
        val normalizedQuery = searchQuery.trim()

        val filtered = if (normalizedQuery.isBlank()) {
            sources
        } else {
            sources.filter { source ->
                source.name.contains(normalizedQuery, ignoreCase = true)
            }
        }

        val comparator = when (sortMode) {
            SourceSortMode.MOST_USED -> {
                compareByDescending<MainAPI> { source -> pinnedSourceIds.contains(source.sourceId()) }
                    .thenByDescending { source -> usageCountBySourceId[source.sourceId()] ?: 0 }
                    .thenBy { source -> source.name.lowercase(Locale.ROOT) }
            }

            SourceSortMode.AZ -> {
                compareByDescending<MainAPI> { source -> pinnedSourceIds.contains(source.sourceId()) }
                    .thenBy { source -> source.name.lowercase(Locale.ROOT) }
                    .thenByDescending { source -> usageCountBySourceId[source.sourceId()] ?: 0 }
            }
        }

        return filtered.sortedWith(comparator)
    }

    private companion object {
        private const val TAG = "HomeScreenV2ViewModel"
    }
}
