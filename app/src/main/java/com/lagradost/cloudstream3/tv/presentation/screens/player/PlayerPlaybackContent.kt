package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.view.KeyEvent as AndroidKeyEvent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
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
    val previewKeyState = remember { PlayerPreviewKeyState() }
    val hasSidePanel = panelsState.activePanel != TvPlayerSidePanel.None ||
        runtimeTracksState.audioPanelVisible ||
        runtimeTracksState.videoPanelVisible ||
        runtimeTracksState.subtitleSyncPanelVisible ||
        overlayState.sourceErrorDialogEffect != null

    fun registerControlsInteraction() {
        overlayState.startupAutoHideArmed = false
        overlayState.hideExtendedMetadata()
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
                    previewKeyState = previewKeyState,
                    hasSidePanel = hasSidePanel,
                    controlsVisible = overlayState.controlsVisible,
                    overlayState = overlayState,
                    rootFocusRequester = rootFocusRequester,
                    playerWantsToPlay = overlayState.playerWantsToPlay,
                    showBufferingOverlay = overlayState.showBufferingOverlay,
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
            controlsVisible = overlayState.controlsVisible,
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
            showExtendedMetadata = overlayState.showExtendedMetadata,
            showAudioTracksButton = runtimeTracksState.showRuntimeAudioTracksButton,
            showVideoTracksButton = runtimeTracksState.showRuntimeVideoTracksButton,
            showSyncButton = catalogState.selectedSubtitleIndex >= 0,
            showNextEpisodeButton = state.hasNextEpisode,
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
                    onPlayNextEpisode = actions.onPlayNextEpisode,
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

internal class PlayerPreviewKeyState {
    private var suppressedConfirmKeyUpDownTime: Long? = null

    fun suppressConfirmKeyUp(downTime: Long) {
        suppressedConfirmKeyUpDownTime = downTime
    }

    fun consumeSuppressedConfirmKeyUp(
        eventType: KeyEventType,
        keyCode: Int,
        downTime: Long,
    ): Boolean {
        val expectedDownTime = suppressedConfirmKeyUpDownTime ?: return false
        if (!isPlayerConfirmKeyCode(keyCode) || eventType != KeyEventType.KeyUp) {
            return false
        }
        if (downTime != expectedDownTime) {
            return false
        }
        suppressedConfirmKeyUpDownTime = null
        return true
    }
}

internal fun handlePlayerPreviewKeyEvent(
    event: KeyEvent,
    previewKeyState: PlayerPreviewKeyState,
    hasSidePanel: Boolean,
    controlsVisible: Boolean,
    overlayState: PlayerOverlayStateHolder,
    rootFocusRequester: FocusRequester,
    playerWantsToPlay: Boolean,
    showBufferingOverlay: Boolean,
    registerControlsInteraction: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onPauseForControls: () -> Unit,
): Boolean {
    return handlePlayerPreviewKeyEvent(
        eventType = event.type,
        keyCode = event.nativeKeyEvent.keyCode,
        downTime = event.nativeKeyEvent.downTime,
        previewKeyState = previewKeyState,
        hasSidePanel = hasSidePanel,
        controlsVisible = controlsVisible,
        overlayState = overlayState,
        rootFocusRequester = rootFocusRequester,
        playerWantsToPlay = playerWantsToPlay,
        showBufferingOverlay = showBufferingOverlay,
        registerControlsInteraction = registerControlsInteraction,
        onSeekBackward = onSeekBackward,
        onSeekForward = onSeekForward,
        onPauseForControls = onPauseForControls,
    )
}

internal fun handlePlayerPreviewKeyEvent(
    eventType: KeyEventType,
    keyCode: Int,
    downTime: Long,
    previewKeyState: PlayerPreviewKeyState,
    hasSidePanel: Boolean,
    controlsVisible: Boolean,
    overlayState: PlayerOverlayStateHolder,
    rootFocusRequester: FocusRequester,
    playerWantsToPlay: Boolean,
    showBufferingOverlay: Boolean,
    registerControlsInteraction: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onPauseForControls: () -> Unit,
): Boolean {
    if (previewKeyState.consumeSuppressedConfirmKeyUp(eventType, keyCode, downTime)) {
        return true
    }
    if (eventType != KeyEventType.KeyDown) {
        return false
    }
    val isBackKey = keyCode == AndroidKeyEvent.KEYCODE_BACK

    if (!isBackKey) {
        registerControlsInteraction()
    }

    if (hasSidePanel) {
        return false
    }

    if (isBackKey) {
        return if (controlsVisible && playerWantsToPlay && !showBufferingOverlay) {
            overlayState.startupAutoHideArmed = false
            overlayState.controlsVisible = false
            rootFocusRequester.requestFocus()
            true
        } else {
            false
        }
    }

    return when {
        isPlayerConfirmKeyCode(keyCode) -> {
            if (!controlsVisible) {
                if (playerWantsToPlay) {
                    onPauseForControls()
                }
                overlayState.controlsVisible = true
                // Swallow the matching key-up so restored focus does not instantly click Play/Pause.
                previewKeyState.suppressConfirmKeyUp(downTime)
                true
            } else {
                false
            }
        }

        isPlayerVerticalKeyCode(keyCode) -> {
            if (!controlsVisible) {
                overlayState.controlsVisible = true
                true
            } else {
                false
            }
        }

        isPlayerLeftKeyCode(keyCode) -> {
            if (!controlsVisible) {
                onSeekBackward()
                true
            } else {
                false
            }
        }

        isPlayerRightKeyCode(keyCode) -> {
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

private fun isPlayerConfirmKeyCode(keyCode: Int): Boolean {
    return keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
        keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
        keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
}

private fun isPlayerVerticalKeyCode(keyCode: Int): Boolean {
    return keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP ||
        keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN ||
        keyCode == AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP ||
        keyCode == AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN
}

private fun isPlayerLeftKeyCode(keyCode: Int): Boolean {
    return keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT ||
        keyCode == AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT
}

private fun isPlayerRightKeyCode(keyCode: Int): Boolean {
    return keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT ||
        keyCode == AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT
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
    onPlayNextEpisode: (Long, Long) -> Unit,
    onToggleResizeMode: () -> Unit,
    onSeekBy: (Long) -> Unit,
) {
    fun resolvedDurationMs(): Long {
        val durationMs = exoPlayer.duration
        return if (durationMs > 0L) durationMs else 0L
    }

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
        TvPlayerControlsEvent.NextEpisode -> {
            exoPlayer.pause()
            onPlayNextEpisode(
                exoPlayer.currentPosition.coerceAtLeast(0L),
                resolvedDurationMs(),
            )
        }
        TvPlayerControlsEvent.SeekBackward -> onSeekBy(-seekPreferencesState.seekWhenControlsVisibleMs)
        TvPlayerControlsEvent.SeekForward -> onSeekBy(seekPreferencesState.seekWhenControlsVisibleMs)
    }
}
