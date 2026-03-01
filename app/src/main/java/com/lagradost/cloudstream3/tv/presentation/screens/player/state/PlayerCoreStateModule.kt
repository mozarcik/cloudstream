package com.lagradost.cloudstream3.tv.presentation.screens.player.state

import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerMetadata
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerPlaybackProgressState
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerUiState
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import kotlinx.coroutines.flow.MutableStateFlow

internal class PlayerCoreStateModule(
    internal val uiState: MutableStateFlow<TvPlayerUiState>,
) {
    internal var metadata: TvPlayerMetadata = TvPlayerMetadata.Empty
    internal var currentLoadResponse: LoadResponse? = null
    internal var currentEpisode: ResultEpisode? = null
    internal val playbackProgressState = TvPlayerPlaybackProgressState()
}
