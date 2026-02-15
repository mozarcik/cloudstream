package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Immutable
internal data class PlaybackTimelineUiState(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

@Composable
internal fun rememberPlaybackTimelineUiState(
    player: ExoPlayer,
    onPlaybackProgress: (Long, Long) -> Unit,
    updateIntervalMs: Long,
): State<PlaybackTimelineUiState> {
    val latestOnPlaybackProgress by rememberUpdatedState(onPlaybackProgress)

    return produceState(
        initialValue = PlaybackTimelineUiState(),
        key1 = player,
        key2 = updateIntervalMs,
    ) {
        while (currentCoroutineContext().isActive) {
            val positionMs = player.currentPosition.coerceAtLeast(0L)
            val rawDuration = player.duration
            val durationMs = if (rawDuration == C.TIME_UNSET || rawDuration < 0L) {
                0L
            } else {
                rawDuration
            }

            if (positionMs != value.positionMs || durationMs != value.durationMs) {
                value = PlaybackTimelineUiState(
                    positionMs = positionMs,
                    durationMs = durationMs,
                )
            }

            latestOnPlaybackProgress(positionMs, durationMs)
            delay(updateIntervalMs)
        }
    }
}
