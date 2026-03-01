package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelItemAction
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPlaybackErrorDetails
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSidePanel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TvPlayerScreenViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TvPlayerUiState>(
        TvPlayerUiState.LoadingSources(
            metadata = TvPlayerMetadata.Empty,
            loadedSources = 0,
            canSkip = false,
        )
    )
    val uiState: StateFlow<TvPlayerUiState> = _uiState.asStateFlow()

    private val _catalogUiState = MutableStateFlow(PlayerCatalogUiState())
    val catalogUiState: StateFlow<PlayerCatalogUiState> = _catalogUiState.asStateFlow()

    private val _panelsUiState = MutableStateFlow(TvPlayerPanelsUiState())
    val panelsUiState: StateFlow<TvPlayerPanelsUiState> = _panelsUiState.asStateFlow()

    private val coordinator = PlayerScreenCoordinator(
        savedStateHandle = savedStateHandle,
        coroutineScope = viewModelScope,
        uiState = _uiState,
        catalogUiState = _catalogUiState,
        panelsUiState = _panelsUiState,
    )

    val panelEffects: SharedFlow<TvPlayerPanelEffect>
        get() = coordinator.panelEffects

    fun retry() = coordinator.retry()

    fun skipLoading() = coordinator.skipLoading()

    fun onPlaybackProgress(positionMs: Long, durationMs: Long) {
        coordinator.onPlaybackProgress(positionMs, durationMs)
    }

    fun onPlaybackStopped(positionMs: Long, durationMs: Long) {
        coordinator.onPlaybackStopped(positionMs, durationMs)
    }

    fun selectSource(
        index: Int,
        forceReloadCurrent: Boolean = false,
    ) {
        coordinator.selectSource(
            index = index,
            forceReloadCurrent = forceReloadCurrent,
        )
    }

    fun retrySource(index: Int) = coordinator.retrySource(index)

    fun openPanel(panel: TvPlayerSidePanel) = coordinator.openPanel(panel)

    fun closePanel() = coordinator.closePanel()

    fun disableSubtitlesFromPlaybackError() = coordinator.disableSubtitlesFromPlaybackError()

    fun onPanelItemAction(action: TvPlayerPanelItemAction) = coordinator.onPanelItemAction(action)

    fun onSubtitleFileSelected(uri: Uri?) = coordinator.onSubtitleFileSelected(uri)

    fun onSubtitlesSidePanelBackPressed(): Boolean = coordinator.onSubtitlesSidePanelBackPressed()

    fun onPlaybackReady() = coordinator.onPlaybackReady()

    fun onPlaybackError(error: TvPlayerPlaybackErrorDetails?) = coordinator.onPlaybackError(error)
}
