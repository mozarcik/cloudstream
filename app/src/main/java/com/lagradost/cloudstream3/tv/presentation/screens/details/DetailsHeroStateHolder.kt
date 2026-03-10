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
    val movieFallback = stringResource(R.string.movies_singular)
    val seriesFallback = stringResource(R.string.tv_series_singular)

    val playButtonLabel = remember(
        mode,
        details.currentEpisode,
        details.currentSeason,
        isSeriesContent,
        seasonShort,
        episodeShort,
        episodeLabel,
        movieFallback,
        seriesFallback,
    ) {
        when (val currentEpisode = details.currentEpisode) {
            null -> when {
                mode == DetailsScreenMode.TvSeries -> seriesFallback
                isSeriesContent -> seriesFallback
                else -> movieFallback
            }

            else -> {
                val currentSeason = details.currentSeason
                if (currentSeason != null) {
                    "$seasonShort$currentSeason:$episodeShort$currentEpisode"
                } else {
                    "$episodeLabel $currentEpisode"
                }
            }
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
