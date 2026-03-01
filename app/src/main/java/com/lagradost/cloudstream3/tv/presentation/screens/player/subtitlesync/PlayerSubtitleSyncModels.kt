package com.lagradost.cloudstream3.tv.presentation.screens.player.subtitlesync

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.ui.player.SubtitleCue
import java.util.Locale

internal object PlayerSubtitleSyncTokens {
    const val DebugTag = "TvSubtitleSync"
    const val EmptyCuesPollLogIntervalMs = 3_000L
    const val PositionTickMs = 250L
    const val CuesPollMs = 600L
    const val SmallStepMs = 100L
    const val LargeStepMs = 1_000L
    const val ActiveLinesPaddingCount = 4
    const val PanelWidthFraction = 0.58f
    const val ProgressFocusAnimationMs = 120

    val PanelOuterPadding = 12.dp
    val PanelInnerPadding = 20.dp
    val ColumnsSpacing = 18.dp
    val ColumnWeightDialogs = 0.58f
    val ColumnWeightControls = 0.42f
    val DialogListTopPadding = 10.dp
    val DialogListMaxHeight = 620.dp
    val DelayValueTopPadding = 12.dp
    val DelayValueBottomPadding = 18.dp
    val ControlsButtonsSpacing = 8.dp
    val ActiveProgressHeight = 5.dp
    val InactiveProgressHeight = 3.dp
    val ActiveProgressTopPadding = 6.dp
    val ButtonsContentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    val EmptyStateMinHeight = 220.dp
    val EmptyStateTopPadding = 16.dp
}

@Immutable
internal data class PlayerSubtitleCueUiModel(
    val cue: SubtitleCue,
    val joinedText: String,
    val timestampRange: String,
)

internal fun LazyListState.isItemOutsideViewport(index: Int): Boolean {
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return true
    val firstVisible = visibleItems.first().index
    val lastVisible = visibleItems.last().index
    return index < firstVisible || index > lastVisible
}

internal fun Modifier.subtitleSyncProgressTrack(
    color: Color,
): Modifier {
    return background(
        color = color,
        shape = CircleShape,
    )
}

internal fun formatSubtitleDelayLabel(delayMs: Long): String {
    return String.format(
        Locale.US,
        "%+.1f s",
        delayMs / 1000f,
    )
}

internal fun findActiveCueIndex(
    cues: List<SubtitleCue>,
    playbackPositionMs: Long,
): Int {
    if (cues.isEmpty()) return -1
    val activeIndex = cues.indexOfFirst { cue ->
        playbackPositionMs in cue.startTimeMs until cue.endTimeMs
    }
    if (activeIndex >= 0) return activeIndex

    val nearestFutureIndex = cues.indexOfFirst { cue ->
        cue.startTimeMs > playbackPositionMs
    }
    return if (nearestFutureIndex >= 0) {
        nearestFutureIndex
    } else {
        cues.lastIndex
    }
}

internal fun SubtitleCue.progressFor(playbackPositionMs: Long): Float {
    if (durationMs <= 0L) return 0f
    val rawProgress = (playbackPositionMs - startTimeMs).toFloat() / durationMs.toFloat()
    return rawProgress.coerceIn(0f, 1f)
}

internal fun SubtitleCue.formatTimestampRange(): String {
    return "${formatTimestamp(startTimeMs)} - ${formatTimestamp(endTimeMs)}"
}

private fun formatTimestamp(timeMs: Long): String {
    val absolute = timeMs.coerceAtLeast(0L)
    val totalSeconds = absolute / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val millis = absolute % 1000L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    } else {
        String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
    }
}
