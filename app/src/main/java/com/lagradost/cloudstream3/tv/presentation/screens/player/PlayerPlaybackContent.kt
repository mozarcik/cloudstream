package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.media3.exoplayer.ExoPlayer
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.screens.player.core.seekPlayerBy
import com.lagradost.cloudstream3.tv.presentation.screens.player.core.subtitleSyncDebugLog
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.BufferingOverlay
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlaybackControlsLayer
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerOverlayStateHolder
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.TvPlayerControlsEvent
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelItemAction
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.player.runtime.PlayerRuntimeTracksStateHolder
import com.lagradost.cloudstream3.tv.presentation.screens.player.video.TvPlayerVideoSurface
import com.lagradost.cloudstream3.ui.player.SubtitleData
import kotlinx.coroutines.flow.MutableSharedFlow

@Composable
internal fun PlayerPlaybackContent(
    state: TvPlayerUiState.Ready,
    catalogState: PlayerCatalogUiState,
    panelsState: TvPlayerPanelsUiState,
    overlayState: PlayerOverlayStateHolder,
    runtimeTracksState: PlayerRuntimeTracksStateHolder,
    exoPlayer: ExoPlayer,
    subtitleSyncController: TvPlayerSubtitleSyncController,
    selectedSubtitle: SubtitleData?,
    selectedSubtitleId: String?,
    playerResizeMode: PlayerResizeMode,
    seekPreferencesState: TvPlayerSeekPreferencesState,
    rootFocusRequester: FocusRequester,
    playPauseFocusRequester: FocusRequester,
    timelineFocusRequester: FocusRequester,
    controlsInteractionEvents: MutableSharedFlow<Unit>,
    actions: PlayerScreenActions,
    onPendingPlaybackRestoreCaptured: (Long, Boolean) -> Unit,
    onRuntimeSubtitleDelayChanged: (Long) -> Unit,
    onPlayerResizeModeChanged: (PlayerResizeMode) -> Unit,
) {
    val context = LocalContext.current
    val hasSidePanel = panelsState.activePanel != TvPlayerSidePanel.None ||
        runtimeTracksState.audioPanelVisible ||
        runtimeTracksState.videoPanelVisible ||
        runtimeTracksState.subtitleSyncPanelVisible ||
        overlayState.sourceErrorDialogEffect != null

    fun registerControlsInteraction() {
        controlsInteractionEvents.tryEmit(Unit)
    }

    fun seekBy(deltaMs: Long) {
        seekPlayerBy(exoPlayer, deltaMs)
    }

    fun toggleResizeMode() {
        val nextMode = playerResizeMode.next()
        onPlayerResizeModeChanged(nextMode)
        Toast.makeText(
            context,
            context.getString(nextMode.labelResId),
            Toast.LENGTH_SHORT,
        ).show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                handlePlayerPreviewKeyEvent(
                    event = event,
                    hasSidePanel = hasSidePanel,
                    controlsVisible = overlayState.controlsVisible,
                    overlayState = overlayState,
                    registerControlsInteraction = ::registerControlsInteraction,
                    onSeekBackward = {
                        seekBy(-seekPreferencesState.seekWhenControlsHiddenMs)
                    },
                    onSeekForward = {
                        seekBy(seekPreferencesState.seekWhenControlsHiddenMs)
                    },
                    onPauseForControls = {
                        exoPlayer.pause()
                    },
                )
            }
    ) {
        TvPlayerVideoSurface(
            player = exoPlayer,
            resizeMode = playerResizeMode.resizeMode,
            subtitleSyncController = subtitleSyncController,
            modifier = Modifier.fillMaxSize(),
        )

        if (overlayState.showBufferingOverlay) {
            BufferingOverlay()
        }

        PlaybackControlsLayer(
            visible = overlayState.controlsVisible,
            controlsEnabled = !hasSidePanel,
            metadata = state.metadata,
            link = state.link,
            isPlaying = overlayState.isPlaying,
            showAudioTracksButton = runtimeTracksState.showRuntimeAudioTracksButton,
            showVideoTracksButton = runtimeTracksState.showRuntimeVideoTracksButton,
            showSyncButton = catalogState.selectedSubtitleIndex >= 0,
            showNextEpisodeButton = state.metadata.isEpisodeBased,
            playPauseFocusRequester = playPauseFocusRequester,
            timelineFocusRequester = timelineFocusRequester,
            exoPlayer = exoPlayer,
            onPlaybackProgress = actions.onPlaybackProgress,
            onControlsEvent = { event ->
                registerControlsInteraction()
                handlePlayerControlsEvent(
                    event = event,
                    exoPlayer = exoPlayer,
                    state = state,
                    catalogState = catalogState,
                    selectedSubtitle = selectedSubtitle,
                    selectedSubtitleId = selectedSubtitleId,
                    subtitleSyncController = subtitleSyncController,
                    runtimeTracksState = runtimeTracksState,
                    seekPreferencesState = seekPreferencesState,
                    onOpenPanel = actions.onOpenPanel,
                    onToggleResizeMode = ::toggleResizeMode,
                    onSeekBy = ::seekBy,
                )
            },
        )

        PlayerPlaybackPanelsLayer(
            panelsState = panelsState,
            runtimeTracksState = runtimeTracksState,
            overlayState = overlayState,
            exoPlayer = exoPlayer,
            subtitleSyncController = subtitleSyncController,
            selectedSubtitle = selectedSubtitle,
            selectedSubtitleId = selectedSubtitleId,
            onClosePanel = actions.onClosePanel,
            onSubtitlesSidePanelBackPressed = actions.onSubtitlesSidePanelBackPressed,
            onPanelItemAction = actions.onPanelItemAction,
            onPendingPlaybackRestoreCaptured = onPendingPlaybackRestoreCaptured,
            onRuntimeSubtitleDelayChanged = onRuntimeSubtitleDelayChanged,
            onRetrySource = actions.onRetrySource,
            registerControlsInteraction = ::registerControlsInteraction,
        )
    }
}

private fun handlePlayerPreviewKeyEvent(
    event: KeyEvent,
    hasSidePanel: Boolean,
    controlsVisible: Boolean,
    overlayState: PlayerOverlayStateHolder,
    registerControlsInteraction: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onPauseForControls: () -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) {
        return false
    }
    registerControlsInteraction()

    if (hasSidePanel) {
        return false
    }

    return when (event.key) {
        Key.DirectionCenter,
        Key.Enter,
        Key.NumPadEnter -> {
            if (!controlsVisible) {
                onPauseForControls()
                overlayState.controlsVisible = true
                true
            } else {
                false
            }
        }

        Key.DirectionUp,
        Key.DirectionDown -> {
            if (!controlsVisible) {
                overlayState.controlsVisible = true
                true
            } else {
                false
            }
        }

        Key.DirectionLeft -> {
            if (!controlsVisible) {
                onSeekBackward()
                true
            } else {
                false
            }
        }

        Key.DirectionRight -> {
            if (!controlsVisible) {
                onSeekForward()
                true
            } else {
                false
            }
        }

        else -> false
    }
}

private fun handlePlayerControlsEvent(
    event: TvPlayerControlsEvent,
    exoPlayer: ExoPlayer,
    state: TvPlayerUiState.Ready,
    catalogState: PlayerCatalogUiState,
    selectedSubtitle: SubtitleData?,
    selectedSubtitleId: String?,
    subtitleSyncController: TvPlayerSubtitleSyncController,
    runtimeTracksState: PlayerRuntimeTracksStateHolder,
    seekPreferencesState: TvPlayerSeekPreferencesState,
    onOpenPanel: (TvPlayerSidePanel) -> Unit,
    onToggleResizeMode: () -> Unit,
    onSeekBy: (Long) -> Unit,
) {
    when (event) {
        TvPlayerControlsEvent.PlayPause -> {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.playWhenReady = true
                exoPlayer.play()
            }
        }

        TvPlayerControlsEvent.OpenSources -> onOpenPanel(TvPlayerSidePanel.Sources)
        TvPlayerControlsEvent.OpenSubtitles -> onOpenPanel(TvPlayerSidePanel.Subtitles)
        TvPlayerControlsEvent.OpenVideoTracks -> {
            runtimeTracksState.videoPanelVisible = runtimeTracksState.videoTracks.size > 1
        }
        TvPlayerControlsEvent.OpenTracks -> {
            runtimeTracksState.audioPanelVisible = runtimeTracksState.audioTracks.size > 1
        }
        TvPlayerControlsEvent.SyncSubtitles -> {
            subtitleSyncDebugLog(
                "open sync panel: selectedSubtitleIndex=${catalogState.selectedSubtitleIndex}" +
                    " subtitleId=${selectedSubtitleId ?: "null"}" +
                    " subtitleName=${selectedSubtitle?.name ?: "null"}" +
                    " mime=${selectedSubtitle?.mimeType ?: "null"}" +
                    " language=${selectedSubtitle?.languageCode ?: "null"}" +
                    " playerPosMs=${exoPlayer.currentPosition.coerceAtLeast(0L)}",
            )
            subtitleSyncController.logDebugSnapshot(
                player = exoPlayer,
                reason = "open_panel",
                subtitleId = selectedSubtitleId,
                subtitleLabel = selectedSubtitle?.name,
                subtitleMimeType = selectedSubtitle?.mimeType,
            )
            runtimeTracksState.subtitleSyncPanelVisible = true
        }
        TvPlayerControlsEvent.ToggleResizeMode -> onToggleResizeMode()
        TvPlayerControlsEvent.Restart -> {
            exoPlayer.seekTo(0L)
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        }
        TvPlayerControlsEvent.NextEpisode -> Unit
        TvPlayerControlsEvent.SeekBackward -> onSeekBy(-seekPreferencesState.seekWhenControlsVisibleMs)
        TvPlayerControlsEvent.SeekForward -> onSeekBy(seekPreferencesState.seekWhenControlsVisibleMs)
    }
}
