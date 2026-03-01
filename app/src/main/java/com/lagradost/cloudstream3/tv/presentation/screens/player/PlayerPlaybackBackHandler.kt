package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerOverlayStateHolder
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.player.runtime.PlayerRuntimeTracksStateHolder

@Composable
internal fun PlayerPlaybackBackHandler(
    overlayState: PlayerOverlayStateHolder,
    runtimeTracksState: PlayerRuntimeTracksStateHolder,
    activePanel: TvPlayerSidePanel,
    actions: PlayerScreenActions,
) {
    BackHandler {
        when {
            overlayState.sourceErrorDialogEffect != null -> overlayState.sourceErrorDialogEffect = null
            runtimeTracksState.subtitleSyncPanelVisible -> runtimeTracksState.subtitleSyncPanelVisible = false
            runtimeTracksState.audioPanelVisible -> runtimeTracksState.audioPanelVisible = false
            runtimeTracksState.videoPanelVisible -> runtimeTracksState.videoPanelVisible = false
            activePanel == TvPlayerSidePanel.Subtitles && actions.onSubtitlesSidePanelBackPressed() -> Unit
            activePanel != TvPlayerSidePanel.None -> actions.onClosePanel()
            overlayState.showBufferingOverlay -> actions.onBackPressed()
            overlayState.controlsVisible && overlayState.playerWantsToPlay -> {
                overlayState.controlsVisible = false
            }
            else -> actions.onBackPressed()
        }
    }
}
