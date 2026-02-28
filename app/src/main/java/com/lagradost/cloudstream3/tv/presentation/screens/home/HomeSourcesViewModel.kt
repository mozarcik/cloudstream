package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.tv.compat.home.HomeSourceSelectionRepository
import com.lagradost.cloudstream3.tv.compat.home.SourcePreferencesRepository
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository
import com.lagradost.cloudstream3.tv.compat.home.sourceId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeSourcesViewModel(
    private val sourcePreferencesRepository: SourcePreferencesRepository,
    private val homeSourceSelectionRepository: HomeSourceSelectionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeSourcesUiState())
    val uiState = _uiState.asStateFlow()

    private val sourceBootstrapper = HomeSourceBootstrapper(homeSourceSelectionRepository)
    private var hasCompletedInitialSourceBootstrap = false

    init {
        observeSourcePreferences()
        observeSelectedSource()
        observeAvailableSourcesPool()
        bootstrapSelectedSource()
    }

    fun refreshSources(
        rerankQuickSources: Boolean = false,
        allowSelectionFallback: Boolean = hasCompletedInitialSourceBootstrap,
    ) {
        val availableSources = SourceRepository.getAvailableApis()
        var refreshResult: HomeSourcesRefreshResult? = null

        _uiState.update { state ->
            HomeSourcesStateReducer.refresh(
                state = state,
                availableSources = availableSources,
                rerankQuickSources = rerankQuickSources,
                allowSelectionFallback = allowSelectionFallback
            ).also { result ->
                refreshResult = result
            }.state
        }

        refreshResult?.fallbackSource?.let { source ->
            applySourceSelection(
                source = source,
                incrementUsage = false,
                persistLegacySelection = true
            )
        }
    }

    fun selectSource(source: MainAPI) {
        val selectedSourceId = source.sourceId()
        val currentSelectedId = _uiState.value.selectedSource?.sourceId()
        if (selectedSourceId == currentSelectedId) return

        applySourceSelection(
            source = source,
            incrementUsage = true,
            persistLegacySelection = true
        )
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
                _uiState.update { state ->
                    HomeSourcesStateReducer.applyPreferences(
                        state = state,
                        preferences = preferences
                    )
                }
            }
        }
    }

    private fun observeSelectedSource() {
        viewModelScope.launch {
            SourceRepository.selectedApi.collect { selectedApi ->
                _uiState.update { state ->
                    HomeSourcesStateReducer.syncSelectedSource(
                        state = state,
                        selectedSource = selectedApi
                    )
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
                    refreshSources(
                        rerankQuickSources = false,
                        allowSelectionFallback = hasCompletedInitialSourceBootstrap
                    )
                }

                delay(500)
            }
        }
    }

    private fun bootstrapSelectedSource() {
        viewModelScope.launch {
            val initialSelection = sourceBootstrapper.awaitInitialSelection()

            hasCompletedInitialSourceBootstrap = true
            refreshSources(
                rerankQuickSources = true,
                allowSelectionFallback = false
            )

            initialSelection.source?.let { source ->
                applySourceSelection(
                    source = source,
                    incrementUsage = false,
                    persistLegacySelection = false
                )
            }
        }
    }

    private fun applySourceSelection(
        source: MainAPI,
        incrementUsage: Boolean,
        persistLegacySelection: Boolean,
    ) {
        SourceRepository.selectApi(source)

        viewModelScope.launch {
            if (persistLegacySelection) {
                homeSourceSelectionRepository.setSelectedSourceName(source.name)
            }

            if (incrementUsage) {
                sourcePreferencesRepository.incrementUsage(source.sourceId())
            }
        }
    }
}
