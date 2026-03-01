package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.lagradost.cloudstream3.R

@Immutable
internal data class PlayerBottomControlBarConfig(
    val showAudioTracksButton: Boolean,
    val showVideoTracksButton: Boolean,
    val showSyncButton: Boolean,
    val showNextEpisodeButton: Boolean,
)

@Immutable
internal data class PlayerBottomControlFocusGraph(
    val playRightFocusRequester: FocusRequester,
    val subtitlesLeftFocusRequester: FocusRequester,
    val subtitlesRightFocusRequester: FocusRequester,
    val syncRightFocusRequester: FocusRequester,
    val audioLeftFocusRequester: FocusRequester,
    val aspectLeftFocusRequester: FocusRequester,
    val restartRightFocusRequester: FocusRequester,
    val sourceLeftFocusRequester: FocusRequester,
)

@Immutable
internal data class PlayerBottomControlOffsets(
    val sourceOffsetX: Dp,
    val videoOffsetX: Dp,
    val restartOffsetX: Dp,
    val nextOffsetX: Dp,
)

internal fun buildPlayerBottomControlFocusGraph(
    config: PlayerBottomControlBarConfig,
    state: PlayerBottomControlBarState,
): PlayerBottomControlFocusGraph {
    val playRightFocusRequester = if (config.showNextEpisodeButton) {
        state.nextEpisodeFocusRequester
    } else {
        state.subtitlesFocusRequester
    }
    val subtitlesLeftFocusRequester = if (config.showNextEpisodeButton) {
        state.nextEpisodeFocusRequester
    } else {
        state.playPauseFocusRequester
    }
    val subtitlesRightFocusRequester = when {
        config.showSyncButton -> state.syncFocusRequester
        config.showAudioTracksButton -> state.audioFocusRequester
        else -> state.aspectRatioFocusRequester
    }
    val syncRightFocusRequester = if (config.showAudioTracksButton) {
        state.audioFocusRequester
    } else {
        state.aspectRatioFocusRequester
    }
    val audioLeftFocusRequester = if (config.showSyncButton) {
        state.syncFocusRequester
    } else {
        state.subtitlesFocusRequester
    }
    val aspectLeftFocusRequester = when {
        config.showAudioTracksButton -> state.audioFocusRequester
        config.showSyncButton -> state.syncFocusRequester
        else -> state.subtitlesFocusRequester
    }
    val restartRightFocusRequester = if (config.showVideoTracksButton) {
        state.videoFocusRequester
    } else {
        state.sourcesFocusRequester
    }
    val sourceLeftFocusRequester = if (config.showVideoTracksButton) {
        state.videoFocusRequester
    } else {
        state.restartFocusRequester
    }
    return PlayerBottomControlFocusGraph(
        playRightFocusRequester = playRightFocusRequester,
        subtitlesLeftFocusRequester = subtitlesLeftFocusRequester,
        subtitlesRightFocusRequester = subtitlesRightFocusRequester,
        syncRightFocusRequester = syncRightFocusRequester,
        audioLeftFocusRequester = audioLeftFocusRequester,
        aspectLeftFocusRequester = aspectLeftFocusRequester,
        restartRightFocusRequester = restartRightFocusRequester,
        sourceLeftFocusRequester = sourceLeftFocusRequester,
    )
}

internal fun buildPlayerBottomControlOffsets(
    config: PlayerBottomControlBarConfig,
): PlayerBottomControlOffsets {
    val playToNearButtonOffset = (PlayerControlsTokens.PlayButtonSize / 2) +
        PlayerControlsTokens.ButtonsSpacing +
        (PlayerControlsTokens.SecondaryButtonSize / 2)
    val sourceOffsetX = -playToNearButtonOffset
    val videoOffsetX = sourceOffsetX -
        PlayerControlsTokens.SecondaryButtonSize -
        PlayerControlsTokens.ButtonsSpacing
    val restartOffsetX = if (config.showVideoTracksButton) {
        videoOffsetX -
            PlayerControlsTokens.SecondaryButtonSize -
            PlayerControlsTokens.ButtonsSpacing
    } else {
        videoOffsetX
    }
    val nextOffsetX = playToNearButtonOffset

    return PlayerBottomControlOffsets(
        sourceOffsetX = sourceOffsetX,
        videoOffsetX = videoOffsetX,
        restartOffsetX = restartOffsetX,
        nextOffsetX = nextOffsetX,
    )
}

@Composable
internal fun PlayerBottomTrailingControls(
    config: PlayerBottomControlBarConfig,
    state: PlayerBottomControlBarState,
    focusGraph: PlayerBottomControlFocusGraph,
    controlsEnabled: Boolean,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(PlayerControlsTokens.ButtonsSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerControlButton(
            icon = Icons.Default.Subtitles,
            tooltipText = stringResource(R.string.player_subtitles_settings),
            style = PlayerControlButtonStyle.Secondary,
            controlsEnabled = controlsEnabled,
            focusRequester = state.subtitlesFocusRequester,
            leftFocusRequester = focusGraph.subtitlesLeftFocusRequester,
            rightFocusRequester = focusGraph.subtitlesRightFocusRequester,
            onClick = { onControlsEvent(TvPlayerControlsEvent.OpenSubtitles) },
            onTooltipVisible = state::showTooltip,
            onTooltipHidden = state::hideTooltip,
            onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.Subtitles },
        )
        if (config.showSyncButton) {
            PlayerControlButton(
                icon = Icons.Default.Sync,
                tooltipText = stringResource(R.string.subtitle_offset),
                style = PlayerControlButtonStyle.Secondary,
                controlsEnabled = controlsEnabled,
                focusRequester = state.syncFocusRequester,
                leftFocusRequester = state.subtitlesFocusRequester,
                rightFocusRequester = focusGraph.syncRightFocusRequester,
                onClick = { onControlsEvent(TvPlayerControlsEvent.SyncSubtitles) },
                onTooltipVisible = state::showTooltip,
                onTooltipHidden = state::hideTooltip,
                onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.Sync },
            )
        }
        if (config.showAudioTracksButton) {
            PlayerControlButton(
                icon = Icons.Default.Audiotrack,
                tooltipText = stringResource(R.string.audio_tracks),
                style = PlayerControlButtonStyle.Secondary,
                controlsEnabled = controlsEnabled,
                focusRequester = state.audioFocusRequester,
                leftFocusRequester = focusGraph.audioLeftFocusRequester,
                rightFocusRequester = state.aspectRatioFocusRequester,
                onClick = { onControlsEvent(TvPlayerControlsEvent.OpenTracks) },
                onTooltipVisible = state::showTooltip,
                onTooltipHidden = state::hideTooltip,
                onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.Audio },
            )
        }
        PlayerControlButton(
            icon = Icons.Default.AspectRatio,
            tooltipText = stringResource(R.string.video_aspect_ratio_resize),
            style = PlayerControlButtonStyle.Secondary,
            controlsEnabled = controlsEnabled,
            focusRequester = state.aspectRatioFocusRequester,
            leftFocusRequester = focusGraph.aspectLeftFocusRequester,
            rightFocusRequester = null,
            onClick = { onControlsEvent(TvPlayerControlsEvent.ToggleResizeMode) },
            onTooltipVisible = state::showTooltip,
            onTooltipHidden = state::hideTooltip,
            onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.AspectRatio },
        )
    }
}

@Composable
internal fun BoxScope.PlayerBottomCenteredControls(
    config: PlayerBottomControlBarConfig,
    state: PlayerBottomControlBarState,
    focusGraph: PlayerBottomControlFocusGraph,
    offsets: PlayerBottomControlOffsets,
    isPlaying: Boolean,
    controlsEnabled: Boolean,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
) {
    if (config.showNextEpisodeButton) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = offsets.nextOffsetX),
        ) {
            PlayerControlButton(
                icon = Icons.Default.SkipNext,
                tooltipText = stringResource(R.string.next_episode),
                style = PlayerControlButtonStyle.Secondary,
                controlsEnabled = controlsEnabled,
                focusRequester = state.nextEpisodeFocusRequester,
                leftFocusRequester = state.playPauseFocusRequester,
                rightFocusRequester = state.subtitlesFocusRequester,
                onClick = { onControlsEvent(TvPlayerControlsEvent.NextEpisode) },
                onTooltipVisible = state::showTooltip,
                onTooltipHidden = state::hideTooltip,
                onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.NextEpisode },
            )
        }
    }

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(x = offsets.sourceOffsetX),
    ) {
        PlayerControlButton(
            icon = Icons.Default.Source,
            tooltipText = stringResource(R.string.sources),
            style = PlayerControlButtonStyle.Secondary,
            controlsEnabled = controlsEnabled,
            focusRequester = state.sourcesFocusRequester,
            leftFocusRequester = focusGraph.sourceLeftFocusRequester,
            rightFocusRequester = state.playPauseFocusRequester,
            onClick = { onControlsEvent(TvPlayerControlsEvent.OpenSources) },
            onTooltipVisible = state::showTooltip,
            onTooltipHidden = state::hideTooltip,
            onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.Sources },
        )
    }

    if (config.showVideoTracksButton) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = offsets.videoOffsetX),
        ) {
            PlayerControlButton(
                icon = Icons.Default.HighQuality,
                tooltipText = stringResource(R.string.video_tracks),
                style = PlayerControlButtonStyle.Secondary,
                controlsEnabled = controlsEnabled,
                focusRequester = state.videoFocusRequester,
                leftFocusRequester = state.restartFocusRequester,
                rightFocusRequester = state.sourcesFocusRequester,
                onClick = { onControlsEvent(TvPlayerControlsEvent.OpenVideoTracks) },
                onTooltipVisible = state::showTooltip,
                onTooltipHidden = state::hideTooltip,
                onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.Video },
            )
        }
    }

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(x = offsets.restartOffsetX),
    ) {
        PlayerControlButton(
            icon = Icons.Default.Replay,
            tooltipText = stringResource(R.string.restart),
            style = PlayerControlButtonStyle.Secondary,
            controlsEnabled = controlsEnabled,
            focusRequester = state.restartFocusRequester,
            leftFocusRequester = null,
            rightFocusRequester = focusGraph.restartRightFocusRequester,
            onClick = { onControlsEvent(TvPlayerControlsEvent.Restart) },
            onTooltipVisible = state::showTooltip,
            onTooltipHidden = state::hideTooltip,
            onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.Restart },
        )
    }

    Box(modifier = Modifier.align(Alignment.Center)) {
        PlayerControlButton(
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            tooltipText = if (isPlaying) {
                stringResource(R.string.pause)
            } else {
                stringResource(R.string.home_play)
            },
            style = PlayerControlButtonStyle.Primary,
            controlsEnabled = controlsEnabled,
            focusRequester = state.playPauseFocusRequester,
            leftFocusRequester = state.sourcesFocusRequester,
            rightFocusRequester = focusGraph.playRightFocusRequester,
            onClick = { onControlsEvent(TvPlayerControlsEvent.PlayPause) },
            onTooltipVisible = state::showTooltip,
            onTooltipHidden = state::hideTooltip,
            onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.PlayPause },
        )
    }
}
