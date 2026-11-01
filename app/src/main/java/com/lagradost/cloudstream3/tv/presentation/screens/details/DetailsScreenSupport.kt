package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatDownloadSnapshot
import com.lagradost.cloudstream3.tv.compat.downloads.calculateDownloadProgressFraction
import com.lagradost.cloudstream3.tv.compat.downloads.resolveNormalizedDownloadStatus
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsDownloadActionState
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerStartTarget
import com.lagradost.cloudstream3.ui.WatchType
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager

private val ActiveDownloadStates = setOf(
    VideoDownloadManager.DownloadType.IsDownloading,
    VideoDownloadManager.DownloadType.IsPending,
    VideoDownloadManager.DownloadType.IsPaused
)

internal fun resolveDefaultEpisodeData(details: MovieDetails): String? {
    if (details.seasons.isEmpty()) return null

    val selectedSeason = details.currentSeason?.let { currentSeason ->
        details.seasons.firstOrNull { season ->
            season.displaySeasonNumber == currentSeason || season.seasonNumber == currentSeason
        }
    } ?: details.seasons.firstOrNull()

    val selectedEpisode = details.currentEpisode?.let { currentEpisode ->
        selectedSeason?.episodes?.firstOrNull { episode ->
            episode.episodeNumber == currentEpisode
        }
    } ?: selectedSeason?.episodes?.firstOrNull()

    return selectedEpisode?.data
}

internal fun resolveDefaultPlaybackTarget(details: MovieDetails): PlayerStartTarget {
    return resolveDefaultEpisodeData(details)
        ?.let(PlayerStartTarget::DirectEpisodeData)
        ?: PlayerStartTarget.Default
}

internal fun resolveDownloadActionState(
    status: VideoDownloadManager.DownloadType?,
    progressFraction: Float,
): MovieDetailsDownloadActionState {
    val normalizedProgress = progressFraction.coerceIn(0f, 1f)

    return when {
        status == VideoDownloadManager.DownloadType.IsFailed ->
            MovieDetailsDownloadActionState.Failed

        status == VideoDownloadManager.DownloadType.IsDone || normalizedProgress >= 0.999f ->
            MovieDetailsDownloadActionState.Downloaded

        status in ActiveDownloadStates ->
            MovieDetailsDownloadActionState.Downloading(progress = normalizedProgress)

        else -> MovieDetailsDownloadActionState.Idle
    }
}

internal fun normalizeDownloadStatus(
    snapshot: MovieDetailsCompatDownloadSnapshot,
): VideoDownloadManager.DownloadType? {
    return resolveNormalizedDownloadStatus(
        status = snapshot.status,
        downloadedBytes = snapshot.downloadedBytes,
        totalBytes = snapshot.totalBytes,
        hasPendingRequest = snapshot.hasPendingRequest,
        isStaleZeroProgress = false,
    )
}

internal fun calculateProgressFraction(
    downloadedBytes: Long,
    totalBytes: Long,
): Float {
    return calculateDownloadProgressFraction(
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
    )
}

internal fun resolveDetailsEpisodeSeason(
    episode: TvEpisode,
    selectedSeason: TvSeason?,
): Int? {
    return episode.seasonNumber
        ?: selectedSeason?.displaySeasonNumber
        ?: selectedSeason?.seasonNumber
}

internal fun DetailsDownloadButtonUiState.toMovieDetailsDownloadActionState(): MovieDetailsDownloadActionState {
    return resolveDownloadActionState(
        status = status,
        progressFraction = progressFraction,
    )
}

internal fun Int?.toWatchType(): WatchType {
    return WatchType.entries.firstOrNull { watchType ->
        watchType.stringRes == this
    } ?: WatchType.NONE
}

/** "Season 4 Episode 3 will be released in" and "3d 4h 12m", or null when nothing is upcoming. */
@Composable
internal fun detailsNextAiringParts(nextAiring: com.lagradost.cloudstream3.NextAiring): Pair<String, String>? {
    val now = com.lagradost.cloudstream3.APIHolder.unixTime
    if (nextAiring.unixTime <= now) return null

    val seconds = nextAiring.unixTime - now
    val days = java.util.concurrent.TimeUnit.SECONDS.toDays(seconds)
    val hours = java.util.concurrent.TimeUnit.SECONDS.toHours(seconds) - days * 24
    val minutes = java.util.concurrent.TimeUnit.SECONDS.toMinutes(seconds) -
        java.util.concurrent.TimeUnit.SECONDS.toHours(seconds) * 60
    val timeLabel = when {
        days > 0 -> stringResource(R.string.next_episode_time_day_format, days, hours, minutes)
        hours > 0 -> stringResource(R.string.next_episode_time_hour_format, hours, minutes)
        minutes > 0 -> stringResource(R.string.next_episode_time_min_format, minutes)
        else -> return null
    }
    val episodeLabel = when (val season = nextAiring.season) {
        null -> stringResource(R.string.next_episode_format, nextAiring.episode)
        else -> stringResource(R.string.next_season_episode_format, season, nextAiring.episode)
    }
    return episodeLabel to timeLabel
}
