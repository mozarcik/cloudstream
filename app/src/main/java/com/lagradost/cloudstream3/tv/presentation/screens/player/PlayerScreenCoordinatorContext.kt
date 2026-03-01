package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.lifecycle.SavedStateHandle
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.state.PlayerCatalogStateModule
import com.lagradost.cloudstream3.tv.presentation.screens.player.state.PlayerCoreStateModule
import com.lagradost.cloudstream3.tv.presentation.screens.player.state.PlayerPanelsStateModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow

internal class PlayerScreenCoordinatorContext(
    internal val savedStateHandle: SavedStateHandle,
    internal val coroutineScope: CoroutineScope,
    internal val uiState: MutableStateFlow<TvPlayerUiState>,
    internal val catalogUiState: MutableStateFlow<PlayerCatalogUiState>,
    internal val panelsUiState: MutableStateFlow<TvPlayerPanelsUiState>,
) {
    internal val core = PlayerCoreStateModule(uiState = uiState)
    internal val catalog = PlayerCatalogStateModule(uiState = catalogUiState)
    internal val panels = PlayerPanelsStateModule(
        coroutineScope = coroutineScope,
        uiState = panelsUiState,
        stringResolver = { resId, fallback ->
            stringFromAppContext(
                context = this,
                resId = resId,
                fallback = fallback,
            )
        },
        defaultQueryProvider = { defaultOnlineSubtitlesQuery(this) },
        createSearchRequest = { query, languageTag ->
            createSubtitleSearchRequest(
                context = this,
                query = query,
                languageTag = languageTag,
            )
        },
        onVisibleUiRefreshRequested = { refreshReadyStateIfSubtitlesPanelVisible(this) },
        onSubtitlesDownloaded = { downloadedSubtitles ->
            applyOnlineSubtitlesSelection(
                context = this,
                downloadedSubtitles = downloadedSubtitles,
            )
        },
    )

    internal val panelEffects: SharedFlow<TvPlayerPanelEffect>
        get() = panels.panelEffects

    internal fun emitPanelEffect(effect: TvPlayerPanelEffect) {
        panels.emitEffect(effect)
    }
}
