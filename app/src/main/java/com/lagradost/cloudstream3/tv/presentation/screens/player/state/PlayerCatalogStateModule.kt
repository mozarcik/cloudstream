package com.lagradost.cloudstream3.tv.presentation.screens.player.state

import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerCatalogUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.catalog.PlayerCatalogStore
import com.lagradost.cloudstream3.ui.player.IGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow

internal class PlayerCatalogStateModule(
    internal val uiState: MutableStateFlow<PlayerCatalogUiState>,
) {
    internal val store = PlayerCatalogStore()
    internal var generator: IGenerator? = null
    internal var loadingJob: Job? = null
    internal var pendingInitialFinalizeJob: Job? = null
    internal var hasFinalized = false
    internal var pendingReadyRefreshChanges: Int = 0
    internal var pendingReadyRefreshJob: Job? = null
    internal var prefetchJob: Job? = null
    internal var prefetchedFromEpisodeId: Int? = null
    internal var prefetchingFromEpisodeId: Int? = null
}
