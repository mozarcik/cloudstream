package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatDownloadSnapshot
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsDownloadActionState
import com.lagradost.cloudstream3.ui.WatchType
import com.lagradost.cloudstream3.utils.VideoDownloadManager

internal val DetailsBottomDividerPadding = PaddingValues(vertical = 48.dp)

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

@Composable
internal fun detailsPlayLabel(
    mode: DetailsScreenMode,
    details: MovieDetails,
    isSeriesContent: Boolean,
): String {
    val seasonShort = stringResource(R.string.season_short)
    val episodeShort = stringResource(R.string.episode_short)
    val episodeLabel = stringResource(R.string.episode)
    val movieFallback = stringResource(R.string.movies_singular)
    val seriesFallback = stringResource(R.string.tv_series_singular)

    val currentEpisode = details.currentEpisode
    val currentSeason = details.currentSeason

    if (currentEpisode != null) {
        return if (currentSeason != null) {
            "$seasonShort$currentSeason:$episodeShort$currentEpisode"
        } else {
            "$episodeLabel $currentEpisode"
        }
    }

    return when {
        mode == DetailsScreenMode.TvSeries -> seriesFallback
        isSeriesContent -> seriesFallback
        else -> movieFallback
    }
}

@Composable
internal fun detailsTitleMetadata(details: MovieDetails): List<String> {
    val seasonLabel = stringResource(R.string.season)
    val episodesLabel = stringResource(R.string.episodes)

    return listOfNotNull(
        details.seasonCount?.let { "$seasonLabel $it" },
        details.episodeCount?.let { "$episodesLabel $it" }
    )
}

internal fun resolveDownloadActionState(
    status: VideoDownloadManager.DownloadType?,
    progressFraction: Float,
): MovieDetailsDownloadActionState {
    val normalizedProgress = progressFraction.coerceIn(0f, 1f)

    return when {
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
    val status = snapshot.status
    if (status != null) {
        return status
    }

    if (snapshot.hasPendingRequest) {
        return if (snapshot.downloadedBytes > 0L) {
            VideoDownloadManager.DownloadType.IsDownloading
        } else {
            VideoDownloadManager.DownloadType.IsPending
        }
    }

    if (snapshot.downloadedBytes > 0L && snapshot.totalBytes <= 0L) {
        return VideoDownloadManager.DownloadType.IsDownloading
    }

    if (snapshot.totalBytes <= 0L) {
        return null
    }

    val hasFinishedDownload = snapshot.totalBytes > 0L &&
        snapshot.downloadedBytes > 1024L &&
        snapshot.downloadedBytes + 1024L >= snapshot.totalBytes

    return if (hasFinishedDownload) {
        VideoDownloadManager.DownloadType.IsDone
    } else {
        VideoDownloadManager.DownloadType.IsDownloading
    }
}

internal fun calculateProgressFraction(
    downloadedBytes: Long,
    totalBytes: Long,
): Float {
    if (downloadedBytes <= 0L || totalBytes <= 0L) {
        return 0f
    }

    return (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}

internal fun Int?.toWatchType(): WatchType {
    return WatchType.entries.firstOrNull { watchType ->
        watchType.stringRes == this
    } ?: WatchType.NONE
}
