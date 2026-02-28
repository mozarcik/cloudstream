package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.lazy.LazyListState
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

internal fun LazyListState.isItemOutsideViewport(targetIndex: Int): Boolean {
    val targetItem = layoutInfo.visibleItemsInfo.firstOrNull { item ->
        item.index == targetIndex
    } ?: return true

    return targetItem.offset < layoutInfo.viewportStartOffset ||
        (targetItem.offset + targetItem.size) > layoutInfo.viewportEndOffset
}

internal fun MediaItemCompat.continueWatchingMetadataLabel(remainingSuffix: String): String? {
    val isSeriesContent = this is MediaItemCompat.TvSeries || this.type?.isEpisodeBased() == true
    val safeSeason = continueWatchingSeason?.takeIf { it > 0 }
    val safeEpisode = continueWatchingEpisode?.takeIf { it > 0 }
    val seasonEpisode = when {
        isSeriesContent && safeSeason != null && safeEpisode != null -> {
            "S${safeSeason}:E${safeEpisode}"
        }

        isSeriesContent && safeSeason != null -> "S${safeSeason}"
        isSeriesContent && safeEpisode != null -> "E${safeEpisode}"
        else -> null
    }
    val remaining = continueWatchingRemainingMs?.toRemainingLabel(remainingSuffix = remainingSuffix)

    return when {
        !seasonEpisode.isNullOrBlank() && !remaining.isNullOrBlank() -> "$seasonEpisode • $remaining"
        !seasonEpisode.isNullOrBlank() -> seasonEpisode
        !remaining.isNullOrBlank() -> remaining
        else -> null
    }
}

private fun Long.toRemainingLabel(remainingSuffix: String): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0) {
        "${hours}h ${minutes}m $remainingSuffix"
    } else {
        String.format("%d:%02d %s", minutes, seconds, remainingSuffix)
    }
}
