package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.runtime.Composable
import androidx.media3.exoplayer.ExoPlayer
import com.lagradost.cloudstream3.tv.presentation.screens.player.core.subtitleSyncDebugLog
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.PlayerSidePanels
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.SourceErrorDialog
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelItemAction
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.requiresPlaybackRestore
import com.lagradost.cloudstream3.tv.presentation.screens.player.runtime.PlayerRuntimeTracksStateHolder
import com.lagradost.cloudstream3.tv.presentation.screens.player.runtime.RuntimeAudioTracksSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.player.runtime.RuntimeVideoTracksSidePanel
import com.lagradost.cloudstream3.ui.player.SubtitleData

@Composable
internal fun PlayerPlaybackPanelsLayer(
    panelsState: TvPlayerPanelsUiState,
    runtimeTracksState: PlayerRuntimeTracksStateHolder,
    overlayState: com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerOverlayStateHolder,
    exoPlayer: ExoPlayer,
    subtitleSyncController: TvPlayerSubtitleSyncController,
    selectedSubtitle: SubtitleData?,
    selectedSubtitleId: String?,
    onClosePanel: () -> Unit,
    onSubtitlesSidePanelBackPressed: () -> Boolean,
    onPanelItemAction: (TvPlayerPanelItemAction) -> Unit,
    onPendingPlaybackRestoreCaptured: (Long, Boolean) -> Unit,
    onRuntimeSubtitleDelayChanged: (Long) -> Unit,
    onRetrySource: (Int) -> Unit,
    registerControlsInteraction: () -> Unit,
) {
    val activePanel = panelsState.activePanel

    if (activePanel != TvPlayerSidePanel.None) {
        PlayerSidePanels(
            panels = panelsState,
            onCloseRequested = onClosePanel,
            onSubtitlesBackRequested = onSubtitlesSidePanelBackPressed,
            onItemAction = { action ->
                registerControlsInteraction()
                if (action.requiresPlaybackRestore()) {
                    onPendingPlaybackRestoreCaptured(
                        exoPlayer.currentPosition,
                        exoPlayer.playWhenReady,
                    )
                }
                onPanelItemAction(action)
            },
        )
    }

    if (runtimeTracksState.audioPanelVisible) {
        RuntimeAudioTracksSidePanel(
            tracks = runtimeTracksState.audioTracks,
            selectedTrackId = runtimeTracksState.selectedAudioTrackId,
            onCloseRequested = {
                runtimeTracksState.audioPanelVisible = false
            },
            onSelectDefault = {
                registerControlsInteraction()
                runtimeTracksState.selectAudioTrack(null)
                runtimeTracksState.audioPanelVisible = false
            },
            onSelectTrack = { track ->
                registerControlsInteraction()
                runtimeTracksState.selectAudioTrack(track)
                runtimeTracksState.audioPanelVisible = false
            },
        )
    }

    if (runtimeTracksState.videoPanelVisible) {
        RuntimeVideoTracksSidePanel(
            tracks = runtimeTracksState.videoTracks,
            selectedTrackId = runtimeTracksState.selectedVideoTrackId,
            onCloseRequested = {
                runtimeTracksState.videoPanelVisible = false
            },
            onSelectDefault = {
                registerControlsInteraction()
                runtimeTracksState.selectVideoTrack(null)
                runtimeTracksState.videoPanelVisible = false
            },
            onSelectTrack = { track ->
                registerControlsInteraction()
                runtimeTracksState.selectVideoTrack(track)
                runtimeTracksState.videoPanelVisible = false
            },
        )
    }

    if (runtimeTracksState.subtitleSyncPanelVisible) {
        com.lagradost.cloudstream3.tv.presentation.screens.player.subtitlesync.PlayerSubtitleSyncPanel(
            visible = true,
            player = exoPlayer,
            subtitleSyncController = subtitleSyncController,
            hasActiveSubtitleTrack = selectedSubtitle != null,
            onCloseRequested = {
                runtimeTracksState.subtitleSyncPanelVisible = false
            },
            onSubtitleDelayChanged = onRuntimeSubtitleDelayChanged,
        )
    }

    overlayState.sourceErrorDialogEffect?.let { effect ->
        SourceErrorDialog(
            state = effect.dialog,
            onDismiss = {
                overlayState.sourceErrorDialogEffect = null
            },
            onRetry = {
                overlayState.sourceErrorDialogEffect = null
                onPendingPlaybackRestoreCaptured(
                    exoPlayer.currentPosition,
                    exoPlayer.playWhenReady,
                )
                onRetrySource(effect.dialog.sourceIndex)
            },
        )
    }

    if (runtimeTracksState.subtitleSyncPanelVisible && selectedSubtitle != null) {
        subtitleSyncDebugLog(
            "sync panel visible: subtitleId=${selectedSubtitleId ?: "null"}" +
                " subtitleName=${selectedSubtitle.name}" +
                " playerPosMs=${exoPlayer.currentPosition.coerceAtLeast(0L)}",
        )
    }
}
