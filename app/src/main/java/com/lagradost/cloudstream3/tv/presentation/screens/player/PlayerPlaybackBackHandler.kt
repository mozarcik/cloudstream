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
            closeRuntimePanelOnBack(runtimeTracksState) -> Unit
            activePanel == TvPlayerSidePanel.Subtitles && actions.onSubtitlesSidePanelBackPressed() -> Unit
            activePanel != TvPlayerSidePanel.None -> actions.onClosePanel()
            // Back hides visible controls in every state (playing, paused, buffering).
            overlayState.controlsVisible -> {
                overlayState.startupAutoHideArmed = false
                overlayState.controlsVisible = false
            }
            overlayState.showBufferingOverlay -> actions.onBackPressed()
            else -> actions.onBackPressed()
        }
    }
}

internal fun hasRuntimePanelOpen(runtimeTracksState: PlayerRuntimeTracksStateHolder): Boolean =
    runtimeTracksState.subtitleSyncPanelVisible ||
        runtimeTracksState.audioPanelVisible ||
        runtimeTracksState.videoPanelVisible

/** Closes the open sync / audio / video panel; false when none is open. */
internal fun closeRuntimePanelOnBack(runtimeTracksState: PlayerRuntimeTracksStateHolder): Boolean = when {
    runtimeTracksState.subtitleSyncPanelVisible -> {
        runtimeTracksState.subtitleSyncPanelVisible = false
        true
    }
    runtimeTracksState.audioPanelVisible -> {
        runtimeTracksState.audioPanelVisible = false
        true
    }
    runtimeTracksState.videoPanelVisible -> {
        runtimeTracksState.videoPanelVisible = false
        true
    }
    else -> false
}
