package com.lagradost.cloudstream3.tv.presentation.screens.player.state

import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerCatalogUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.catalog.PlayerCatalogStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow

internal class PlayerCatalogStateModule(
    internal val uiState: MutableStateFlow<PlayerCatalogUiState>,
) {
    internal val store = PlayerCatalogStore()
    internal var loadingJob: Job? = null
    internal var hasFinalized = false
    internal var pendingReadyRefreshChanges: Int = 0
    internal var pendingReadyRefreshJob: Job? = null
}
