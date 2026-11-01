package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.delay

@Stable
internal class PlayerBottomControlBarState(
    internal val playPauseFocusRequester: FocusRequester,
) {
    private val requesters = PlayerControlFocusTarget.entries.associateWith { target ->
        if (target == PlayerControlFocusTarget.PlayPause) playPauseFocusRequester else FocusRequester()
    }
    internal var lastFocusedControl by mutableStateOf(PlayerControlFocusTarget.PlayPause)
    internal var wasControlsEnabled by mutableStateOf(false)
    /** Controls were hidden (auto-hide, Back) since they were last shown: the next showing focuses Play. */
    internal var hiddenSinceShown = false

    internal fun requesterFor(target: PlayerControlFocusTarget): FocusRequester =
        requesters.getValue(target)

    /** Last focused control, or its nearest visible neighbour when that control is hidden now. */
    internal fun resolveFocusRequester(
        target: PlayerControlFocusTarget,
        config: PlayerBottomControlBarConfig,
    ): FocusRequester {
        val visible = when (target) {
            PlayerControlFocusTarget.Video -> config.showVideoTracksButton
            PlayerControlFocusTarget.NextEpisode -> config.showNextEpisodeButton
            PlayerControlFocusTarget.Sync -> config.showSyncButton
            PlayerControlFocusTarget.Audio -> config.showAudioTracksButton
            else -> true
        }
        return requesterFor(if (visible) target else PlayerControlFocusTarget.PlayPause)
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
    controlsShown: Boolean,
    panelCloseCount: Int,
) {
    // Explicit restore after a closed panel, independent of the enabled-state transition below.
    LaunchedEffect(panelCloseCount) {
        if (panelCloseCount == 0) return@LaunchedEffect
        val requester = state.resolveFocusRequester(target = state.lastFocusedControl, config = config)
        repeat(60) {
            if (requester.requestFocus()) return@LaunchedEffect
            delay(16)
        }
    }
    // A closed panel keeps the controls shown, so only a real hide (auto-hide, Back) counts here.
    LaunchedEffect(controlsShown) {
        if (!controlsShown) state.hiddenSinceShown = true
    }
    LaunchedEffect(
        controlsEnabled,
        config.showNextEpisodeButton,
        config.showSyncButton,
        config.showAudioTracksButton,
        config.showVideoTracksButton,
    ) {
        val shouldRestoreFocus = controlsEnabled && !state.wasControlsEnabled
        state.wasControlsEnabled = controlsEnabled
        if (!shouldRestoreFocus) return@LaunchedEffect

        // Shown again after a hide (auto-hide, Back): Play. Closing a panel: the control that opened it.
        val target = if (state.hiddenSinceShown) PlayerControlFocusTarget.PlayPause else state.lastFocusedControl
        state.hiddenSinceShown = false
        val restoreFocusRequester = state.resolveFocusRequester(target = target, config = config)
        // The panel that had focus is removed in the same frame; retry until the row is composed again.
        repeat(60) {
            if (restoreFocusRequester.requestFocus()) {
                return@LaunchedEffect
            }
            delay(16)
        }
    }
}
