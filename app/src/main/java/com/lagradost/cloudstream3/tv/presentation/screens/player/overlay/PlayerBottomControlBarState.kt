package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.delay

@Stable
internal class PlayerBottomControlBarState(
    internal val playPauseFocusRequester: FocusRequester,
) {
    internal val restartFocusRequester = FocusRequester()
    internal val videoFocusRequester = FocusRequester()
    internal val sourcesFocusRequester = FocusRequester()
    internal val nextEpisodeFocusRequester = FocusRequester()
    internal val subtitlesFocusRequester = FocusRequester()
    internal val syncFocusRequester = FocusRequester()
    internal val audioFocusRequester = FocusRequester()
    internal val aspectRatioFocusRequester = FocusRequester()

    internal var controlsBoundsInRoot by mutableStateOf<Rect?>(null)
        private set
    internal var tooltipState by mutableStateOf<PlayerControlTooltipState?>(null)
        private set
    internal var lastFocusedControl by mutableStateOf(PlayerControlFocusTarget.PlayPause)
    internal var wasControlsEnabled by mutableStateOf(false)

    internal fun resolveFocusRequester(
        target: PlayerControlFocusTarget,
        config: PlayerBottomControlBarConfig,
    ): FocusRequester {
        return when (target) {
            PlayerControlFocusTarget.Restart -> restartFocusRequester
            PlayerControlFocusTarget.Video -> {
                if (config.showVideoTracksButton) videoFocusRequester else sourcesFocusRequester
            }
            PlayerControlFocusTarget.Sources -> sourcesFocusRequester
            PlayerControlFocusTarget.PlayPause -> playPauseFocusRequester
            PlayerControlFocusTarget.NextEpisode -> {
                if (config.showNextEpisodeButton) nextEpisodeFocusRequester else playPauseFocusRequester
            }
            PlayerControlFocusTarget.Subtitles -> subtitlesFocusRequester
            PlayerControlFocusTarget.Sync -> when {
                config.showSyncButton -> syncFocusRequester
                config.showAudioTracksButton -> audioFocusRequester
                else -> aspectRatioFocusRequester
            }
            PlayerControlFocusTarget.Audio -> when {
                config.showAudioTracksButton -> audioFocusRequester
                config.showSyncButton -> syncFocusRequester
                else -> aspectRatioFocusRequester
            }
            PlayerControlFocusTarget.AspectRatio -> aspectRatioFocusRequester
        }
    }

    internal fun updateControlsBounds(bounds: Rect) {
        controlsBoundsInRoot = bounds
    }

    internal fun showTooltip(
        tooltipText: String,
        boundsInRoot: Rect,
    ) {
        val parentBounds = controlsBoundsInRoot ?: return
        tooltipState = PlayerControlTooltipState(
            text = tooltipText,
            anchorCenterXPx = boundsInRoot.center.x - parentBounds.left,
            anchorTopYPx = boundsInRoot.top - parentBounds.top,
        )
    }

    internal fun hideTooltip() {
        tooltipState = null
    }
}

@Composable
internal fun rememberPlayerBottomControlBarState(
    playPauseFocusRequester: FocusRequester,
): PlayerBottomControlBarState {
    return remember(playPauseFocusRequester) {
        PlayerBottomControlBarState(playPauseFocusRequester = playPauseFocusRequester)
    }
}

@Composable
internal fun PlayerBottomControlRestoreFocusEffect(
    state: PlayerBottomControlBarState,
    config: PlayerBottomControlBarConfig,
    controlsEnabled: Boolean,
) {
    androidx.compose.runtime.LaunchedEffect(
        controlsEnabled,
        config.showNextEpisodeButton,
        config.showSyncButton,
        config.showAudioTracksButton,
        config.showVideoTracksButton,
    ) {
        val shouldRestoreFocus = controlsEnabled && !state.wasControlsEnabled
        state.wasControlsEnabled = controlsEnabled
        if (!shouldRestoreFocus) return@LaunchedEffect

        val restoreFocusRequester = state.resolveFocusRequester(
            target = state.lastFocusedControl,
            config = config,
        )
        repeat(12) {
            if (restoreFocusRequester.requestFocus()) {
                return@LaunchedEffect
            }
            delay(16)
        }
    }

    androidx.compose.runtime.LaunchedEffect(controlsEnabled) {
        if (!controlsEnabled) {
            state.hideTooltip()
        }
    }
}
