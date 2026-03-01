package com.lagradost.cloudstream3.tv.presentation.screens.player.state

import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.PlayerOnlineSubtitlesController
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsStateHolder
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsUiState
import com.lagradost.cloudstream3.ui.player.SubtitleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class PlayerPanelsStateModule(
    coroutineScope: CoroutineScope,
    internal val uiState: MutableStateFlow<TvPlayerPanelsUiState>,
    stringResolver: (Int, String) -> String,
    defaultQueryProvider: () -> String,
    createSearchRequest: (String, String?) -> com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities.SubtitleSearch,
    onVisibleUiRefreshRequested: () -> Unit,
    onSubtitlesDownloaded: (List<SubtitleData>) -> Unit,
) {
    private val mutablePanelEffects = MutableSharedFlow<TvPlayerPanelEffect>(
        replay = 0,
        extraBufferCapacity = 8,
    )
    internal val panelEffects: SharedFlow<TvPlayerPanelEffect> = mutablePanelEffects.asSharedFlow()
    internal val stateHolder = TvPlayerPanelsStateHolder()
    internal val onlineSubtitlesController = PlayerOnlineSubtitlesController(
        coroutineScope = coroutineScope,
        stringResolver = stringResolver,
        defaultQueryProvider = defaultQueryProvider,
        createSearchRequest = createSearchRequest,
        onVisibleUiRefreshRequested = onVisibleUiRefreshRequested,
        onSubtitlesDownloaded = onSubtitlesDownloaded,
    )

    internal fun emitEffect(effect: TvPlayerPanelEffect) {
        mutablePanelEffects.tryEmit(effect)
    }
}
