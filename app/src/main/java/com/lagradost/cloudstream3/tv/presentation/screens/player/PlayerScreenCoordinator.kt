package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelItemAction
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPlaybackErrorDetails
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSidePanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow

internal class PlayerScreenCoordinator(
    savedStateHandle: SavedStateHandle,
    coroutineScope: CoroutineScope,
    uiState: MutableStateFlow<TvPlayerUiState>,
    catalogUiState: MutableStateFlow<PlayerCatalogUiState>,
    panelsUiState: MutableStateFlow<TvPlayerPanelsUiState>,
) {
    private val context = PlayerScreenCoordinatorContext(
        savedStateHandle = savedStateHandle,
        coroutineScope = coroutineScope,
        uiState = uiState,
        catalogUiState = catalogUiState,
        panelsUiState = panelsUiState,
    )

    val panelEffects: SharedFlow<TvPlayerPanelEffect>
        get() = context.panelEffects

    init {
        loadSources(context)
    }

    fun retry() = retry(context)

    fun skipLoading() = skipLoading(context)

    fun onPlaybackProgress(positionMs: Long, durationMs: Long) {
        onPlaybackProgress(
            context = context,
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    fun onPlaybackStopped(positionMs: Long, durationMs: Long) {
        onPlaybackStopped(
            context = context,
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    fun selectSource(
        index: Int,
        forceReloadCurrent: Boolean = false,
    ) {
        selectSource(
            context = context,
            index = index,
            forceReloadCurrent = forceReloadCurrent,
        )
    }

    fun retrySource(index: Int) = retrySource(context, index)

    fun openPanel(panel: TvPlayerSidePanel) = openPanel(context, panel)

    fun closePanel() = closePanel(context)

    fun disableSubtitlesFromPlaybackError() = disableSubtitlesFromPlaybackError(context)

    fun onPanelItemAction(action: TvPlayerPanelItemAction) = onPanelItemAction(context, action)

    fun onSubtitleFileSelected(uri: Uri?) = onSubtitleFileSelected(context, uri)

    fun onSubtitlesSidePanelBackPressed(): Boolean = onSubtitlesSidePanelBackPressed(context)

    fun onPlaybackReady() = onPlaybackReady(context)

    fun onPlaybackError(error: TvPlayerPlaybackErrorDetails?) = onPlaybackError(context, error)
}
