package com.lagradost.cloudstream3.tv.presentation.screens.player

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
    playerSessionController: PlayerSessionController,
) {
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
}
