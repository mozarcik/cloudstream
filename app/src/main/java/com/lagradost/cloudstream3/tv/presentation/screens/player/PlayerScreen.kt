package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerNoticeLayer
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerNoticeStack
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.SharedFlow
import com.lagradost.cloudstream3.tv.presentation.screens.player.core.PlayerSessionController
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelItemAction
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPlaybackErrorDetails
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSidePanel

@Composable
internal fun PlayerScreen(
    uiState: TvPlayerUiState,
    catalogState: PlayerCatalogUiState,
    panelsState: TvPlayerPanelsUiState,
    seekPreferencesState: TvPlayerSeekPreferencesState,
    actions: PlayerScreenActions,
    panelEffects: SharedFlow<TvPlayerPanelEffect>,
    notices: PlayerNoticeStack,
    playerSessionController: PlayerSessionController,
) {
    Box(modifier = Modifier.fillMaxSize()) {
    when (uiState) {
        is TvPlayerUiState.LoadingSources -> {
            LoadingSourcesState(
                state = uiState,
                onSkipLoading = actions.onSkipLoading,
                onBackPressed = actions.onBackPressed,
            )
        }

        is TvPlayerUiState.Ready -> {
            PlayerPlaybackScreen(
                state = uiState,
                catalogState = catalogState,
                panelsState = panelsState,
                seekPreferencesState = seekPreferencesState,
                actions = actions,
                panelEffects = panelEffects,
                notices = notices,
                playerSessionController = playerSessionController,
            )
        }

        is TvPlayerUiState.Error -> {
            ErrorState(
                state = uiState,
                onRetry = actions.onRetry,
                onBackPressed = actions.onBackPressed,
            )
        }
    }
    PlayerNoticeLayer(
        stack = notices,
        modifier = Modifier.align(Alignment.TopEnd),
    )
    }
}
