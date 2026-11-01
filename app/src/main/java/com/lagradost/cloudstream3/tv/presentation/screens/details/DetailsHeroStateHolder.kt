package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsDownloadActionState

@Immutable
internal data class DetailsHeroUiState(
    val playButtonLabel: String,
    val downloadActionState: MovieDetailsDownloadActionState,
)

@Composable
internal fun rememberDetailsHeroUiState(
    mode: DetailsScreenMode,
    details: MovieDetails,
    isSeriesContent: Boolean,
    downloadButtonState: DetailsDownloadButtonUiState,
): DetailsHeroUiState {
    val seasonShort = stringResource(R.string.season_short)
    val episodeShort = stringResource(R.string.episode_short)
    val episodeLabel = stringResource(R.string.episode)
    val play = stringResource(R.string.home_play)

    // "Play" for a movie, "Play S3:E2" when the series has a current episode (resume target or next unwatched).
    val playButtonLabel = remember(
        details.currentEpisode,
        details.currentSeason,
        seasonShort,
        episodeShort,
        episodeLabel,
        play,
    ) {
        val currentEpisode = details.currentEpisode
        val currentSeason = details.currentSeason
        when {
            currentEpisode == null -> play
            currentSeason != null -> "$play $seasonShort$currentSeason:$episodeShort$currentEpisode"
            else -> "$play $episodeLabel $currentEpisode"
        }
    }

    val downloadActionState = remember(
        downloadButtonState.status,
        downloadButtonState.progressFraction,
    ) {
        resolveDownloadActionState(
            status = downloadButtonState.status,
            progressFraction = downloadButtonState.progressFraction,
        )
    }

    return remember(
        playButtonLabel,
        downloadActionState,
    ) {
        DetailsHeroUiState(
            playButtonLabel = playButtonLabel,
            downloadActionState = downloadActionState,
        )
    }
}
