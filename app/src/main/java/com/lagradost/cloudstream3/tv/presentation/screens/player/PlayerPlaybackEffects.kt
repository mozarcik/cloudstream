package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.tv.presentation.screens.player.core.PlayerSessionController
import com.lagradost.cloudstream3.tv.presentation.screens.player.core.toTvPlayerPlaybackErrorDetails
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerOverlayStateHolder
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.player.runtime.PlayerRuntimeTracksStateHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun PlayerPlaybackListenerEffect(
    exoPlayer: ExoPlayer,
    subtitleSyncController: TvPlayerSubtitleSyncController,
    overlayState: PlayerOverlayStateHolder,
    runtimeTracksState: PlayerRuntimeTracksStateHolder,
    selectedSubtitleIndex: Int,
    actions: PlayerScreenActions,
    onPendingPlaybackRestoreCaptured: (Long, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val currentOverlayState by rememberUpdatedState(overlayState)
    val currentRuntimeTracksState by rememberUpdatedState(runtimeTracksState)
    val currentSelectedSubtitleIndex by rememberUpdatedState(selectedSubtitleIndex)
    val currentActions by rememberUpdatedState(actions)
    val currentPendingPlaybackRestoreCapture by rememberUpdatedState(onPendingPlaybackRestoreCaptured)
    DisposableEffect(exoPlayer, subtitleSyncController) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                currentOverlayState.isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                currentOverlayState.playerPlaybackState = playbackState
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
                    currentRuntimeTracksState.refresh()
                }
                if (playbackState == Player.STATE_READY) {
                    currentActions.onPlaybackReady()
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                currentOverlayState.playerWantsToPlay = playWhenReady
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                val extractedAudioTracks = currentRuntimeTracksState.refresh(tracks)
                currentRuntimeTracksState.applyInitialAudioSelectionIfNeeded(extractedAudioTracks)
            }

            override fun onPlayerError(error: PlaybackException) {
                if (currentOverlayState.errorHandled) return
                currentOverlayState.controlsVisible = true
                val currentPosition = exoPlayer.currentPosition
                val currentPlayWhenReady = exoPlayer.playWhenReady

                if (currentSelectedSubtitleIndex >= 0) {
                    currentPendingPlaybackRestoreCapture(currentPosition, currentPlayWhenReady)
                    currentActions.onDisableSubtitlesFromPlaybackError()
                    android.widget.Toast
                        .makeText(context, context.getString(com.lagradost.cloudstream3.R.string.no_subtitles), android.widget.Toast.LENGTH_SHORT)
                        .show()
                    return
                }

                currentOverlayState.errorHandled = true
                currentPendingPlaybackRestoreCapture(currentPosition, currentPlayWhenReady)
                currentActions.onPlaybackError(error.toTvPlayerPlaybackErrorDetails())
            }
        }

        exoPlayer.addListener(listener)
        onDispose {
            val rawDuration = exoPlayer.duration
            val finalDurationMs = if (rawDuration == C.TIME_UNSET || rawDuration < 0L) 0L else rawDuration
            currentActions.onPlaybackStopped(
                exoPlayer.currentPosition.coerceAtLeast(0L),
                finalDurationMs,
            )
            exoPlayer.removeListener(listener)
            subtitleSyncController.clearTextRenderer()
        }
    }
}

@Composable
internal fun PlayerPlaybackLoadEffect(
    state: TvPlayerUiState.Ready,
    selectedSubtitle: com.lagradost.cloudstream3.ui.player.SubtitleData?,
    selectedSubtitleId: String?,
    initialSubtitleDelayMs: Long,
    initialPlayerPositionMs: Long,
    initialPlayerPlayWhenReady: Boolean,
    playerSessionController: PlayerSessionController,
    overlayState: PlayerOverlayStateHolder,
    runtimeTracksState: PlayerRuntimeTracksStateHolder,
    onPlaybackRestoreConsumed: () -> Unit,
) {
    LaunchedEffect(
        state.link.url,
        selectedSubtitleId,
        initialSubtitleDelayMs,
        initialPlayerPositionMs,
        initialPlayerPlayWhenReady,
    ) {
        overlayState.resetForSourceChange()
        playerSessionController.load(
            link = state.link,
            subtitle = selectedSubtitle,
            audioTracks = state.link.audioTracks,
            subtitleDelayMs = initialSubtitleDelayMs,
            startPositionMs = initialPlayerPositionMs,
            startPlayWhenReady = initialPlayerPlayWhenReady,
        )
        overlayState.syncFromPlayer(playerSessionController.player)
        runtimeTracksState.refresh()
        onPlaybackRestoreConsumed()
    }
}

@Composable
internal fun PlayerExtractorVerificationEffect(
    state: TvPlayerUiState.Ready,
) {
    LaunchedEffect(state.link.url, state.link.extractorData, state.link.source) {
        runCatching {
            APIHolder.getApiFromNameNull(state.link.source)?.extractorVerifierJob(state.link.extractorData)
        }
    }
}

@Composable
internal fun PlayerPlaybackFocusEffects(
    overlayState: PlayerOverlayStateHolder,
    hasSidePanel: Boolean,
    activePanel: TvPlayerSidePanel,
    runtimeTracksState: PlayerRuntimeTracksStateHolder,
    controlsInteractionEvents: MutableSharedFlow<Unit>,
    playPauseFocusRequester: androidx.compose.ui.focus.FocusRequester,
    rootFocusRequester: androidx.compose.ui.focus.FocusRequester,
) {
    LaunchedEffect(overlayState.controlsVisible, hasSidePanel) {
        if (overlayState.controlsVisible && !hasSidePanel) {
            val focused = requestFocusWithRetry(playPauseFocusRequester)
            if (!focused) {
                requestFocusWithRetry(rootFocusRequester)
            }
        } else if (!overlayState.controlsVisible) {
            requestFocusWithRetry(rootFocusRequester)
        }
    }

    LaunchedEffect(
        overlayState.controlsVisible,
        overlayState.playerWantsToPlay,
        activePanel,
        runtimeTracksState.audioPanelVisible,
        runtimeTracksState.videoPanelVisible,
        controlsInteractionEvents,
    ) {
        if (!overlayState.controlsVisible ||
            !overlayState.playerWantsToPlay ||
            activePanel != TvPlayerSidePanel.None ||
            runtimeTracksState.audioPanelVisible ||
            runtimeTracksState.videoPanelVisible
        ) {
            return@LaunchedEffect
        }

        controlsInteractionEvents.tryEmit(Unit)
        controlsInteractionEvents.collectLatest {
            delay(4_500L)
            overlayState.controlsVisible = false
        }
    }

    LaunchedEffect(overlayState.playerWantsToPlay) {
        if (!overlayState.playerWantsToPlay) {
            overlayState.controlsVisible = true
        }
    }
}

@Composable
internal fun PlayerPanelEffectsCollector(
    panelEffects: SharedFlow<TvPlayerPanelEffect>,
    overlayState: PlayerOverlayStateHolder,
    onOpenSubtitleFilePicker: () -> Unit,
) {
    val currentOverlayState by rememberUpdatedState(overlayState)
    val currentOpenSubtitleFilePicker by rememberUpdatedState(onOpenSubtitleFilePicker)
    val context = LocalContext.current
    LaunchedEffect(panelEffects) {
        panelEffects.collectLatest { effect ->
            when (effect) {
                is TvPlayerPanelEffect.OpenSourceErrorDialog -> {
                    currentOverlayState.sourceErrorDialogEffect = effect
                }
                TvPlayerPanelEffect.OpenSubtitleFilePicker -> {
                    currentOpenSubtitleFilePicker()
                }
                is TvPlayerPanelEffect.ShowMessage -> {
                    android.widget.Toast
                        .makeText(context, effect.message, android.widget.Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}
