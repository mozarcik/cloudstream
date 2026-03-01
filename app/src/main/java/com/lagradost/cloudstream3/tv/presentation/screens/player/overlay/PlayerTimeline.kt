package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.screens.player.rememberPlaybackTimelineUiState

@Composable
internal fun PlaybackTimelineSection(
    exoPlayer: ExoPlayer,
    controlsEnabled: Boolean,
    timelineFocusRequester: FocusRequester,
    onPlaybackProgress: (Long, Long) -> Unit,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
) {
    val timelineUiState = rememberPlaybackTimelineUiState(
        player = exoPlayer,
        onPlaybackProgress = onPlaybackProgress,
        updateIntervalMs = 500L,
    )
    val timelineState = timelineUiState.value
    val progressFraction by remember(timelineState.positionMs, timelineState.durationMs) {
        derivedStateOf {
            if (timelineState.durationMs > 0L) {
                (timelineState.positionMs.toFloat() / timelineState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatPlaybackTime(timelineState.positionMs),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.width(14.dp))
        PlaybackTimeline(
            progressFraction = progressFraction,
            controlsEnabled = controlsEnabled,
            focusRequester = timelineFocusRequester,
            onControlsEvent = onControlsEvent,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = formatPlaybackTime(timelineState.durationMs),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
internal fun PlaybackTimeline(
    progressFraction: Float,
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeProgress = progressFraction.coerceIn(0f, 1f)
    var isFocused by remember { mutableStateOf(false) }
    val focusScale = animateFloatAsState(
        targetValue = if (isFocused) {
            PlayerControlsTokens.TimelineFocusedTrackHeight.value /
                PlayerControlsTokens.TimelineInactiveTrackHeight.value
        } else {
            1f
        },
        animationSpec = tween(durationMillis = PlayerControlsTokens.TimelineFocusAnimationMs),
        label = "playback_timeline_focus_scale",
    ).value
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackAlpha = if (isFocused) 0.34f else 0.20f

    Box(
        modifier = modifier
            .height(PlayerControlsTokens.TimelineContainerHeight)
            .focusRequester(focusRequester)
            .focusProperties {
                canFocus = controlsEnabled
            }
            .onPreviewKeyEvent { event ->
                if (!controlsEnabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onControlsEvent(TvPlayerControlsEvent.SeekBackward)
                        true
                    }

                    Key.DirectionRight -> {
                        onControlsEvent(TvPlayerControlsEvent.SeekForward)
                        true
                    }

                    else -> false
                }
            }
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .focusable(enabled = controlsEnabled)
            .padding(vertical = PlayerControlsTokens.TimelineTrackPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlayerControlsTokens.TimelineInactiveTrackHeight)
                .graphicsLayer {
                    scaleY = focusScale
                }
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = trackAlpha)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(safeProgress)
                    .background(primaryColor)
            )
        }
    }
}

internal fun formatPlaybackTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
