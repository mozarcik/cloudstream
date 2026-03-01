package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

@Composable
internal fun PlayerBottomControlBar(
    isPlaying: Boolean,
    controlsEnabled: Boolean,
    showAudioTracksButton: Boolean,
    showVideoTracksButton: Boolean,
    showSyncButton: Boolean,
    showNextEpisodeButton: Boolean,
    playPauseFocusRequester: androidx.compose.ui.focus.FocusRequester,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
) {
    val config = remember(
        showAudioTracksButton,
        showVideoTracksButton,
        showSyncButton,
        showNextEpisodeButton,
    ) {
        PlayerBottomControlBarConfig(
            showAudioTracksButton = showAudioTracksButton,
            showVideoTracksButton = showVideoTracksButton,
            showSyncButton = showSyncButton,
            showNextEpisodeButton = showNextEpisodeButton,
        )
    }
    val state = rememberPlayerBottomControlBarState(playPauseFocusRequester)
    val focusGraph = remember(config, state) {
        buildPlayerBottomControlFocusGraph(
            config = config,
            state = state,
        )
    }
    val offsets = remember(config) {
        buildPlayerBottomControlOffsets(config)
    }

    PlayerBottomControlRestoreFocusEffect(
        state = state,
        config = config,
        controlsEnabled = controlsEnabled,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                state.updateControlsBounds(coordinates.boundsInRoot())
            },
    ) {
        PlayerBottomTrailingControls(
            config = config,
            state = state,
            focusGraph = focusGraph,
            controlsEnabled = controlsEnabled,
            onControlsEvent = onControlsEvent,
            modifier = Modifier.align(Alignment.CenterEnd),
        )

        PlayerBottomCenteredControls(
            config = config,
            state = state,
            focusGraph = focusGraph,
            offsets = offsets,
            isPlaying = isPlaying,
            controlsEnabled = controlsEnabled,
            onControlsEvent = onControlsEvent,
        )

        AnimatedVisibility(
            visible = state.tooltipState != null,
            enter = fadeIn(animationSpec = tween(durationMillis = PlayerControlsTokens.TooltipFadeInMs)),
            exit = fadeOut(animationSpec = tween(durationMillis = PlayerControlsTokens.TooltipFadeOutMs)),
            modifier = Modifier
                .fillMaxWidth()
                .height(PlayerControlsTokens.PlayButtonSize),
        ) {
            state.tooltipState?.let { activeTooltip ->
                PlayerGlobalTooltip(
                    tooltipState = activeTooltip,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
