package com.lagradost.cloudstream3.tv.presentation.screens.movies

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.util.StringConstants
import com.lagradost.cloudstream3.tv.presentation.common.Error
import com.lagradost.cloudstream3.tv.presentation.common.ItemDirection
import com.lagradost.cloudstream3.tv.presentation.common.Loading
import com.lagradost.cloudstream3.tv.presentation.common.MoviesRow

private const val DebugTag = "TvMovieDetailsUI"

object MovieDetailsScreen {
    const val UrlBundleKey = "url"
    const val ApiNameBundleKey = "apiName"
}

@Composable
fun MovieDetailsScreen(
    goToMoviePlayer: () -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewMovie: (Movie) -> Unit,
    movieDetailsScreenViewModel: MovieDetailsScreenViewModel
) {
    val uiState by movieDetailsScreenViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is MovieDetailsScreenUiState.Loading -> {
            Loading(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        is MovieDetailsScreenUiState.Error -> {
            Error(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        is MovieDetailsScreenUiState.Done -> {
            Details(
                movieDetails = s.movieDetails,
                goToMoviePlayer = goToMoviePlayer,
                onBackPressed = onBackPressed,
                refreshScreenWithNewMovie = refreshScreenWithNewMovie,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .animateContentSize()
            )
        }
    }
}

@Composable
private fun Details(
    movieDetails: MovieDetails,
    goToMoviePlayer: () -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewMovie: (Movie) -> Unit,
    modifier: Modifier = Modifier,
) {
    val childPadding = rememberChildPadding()
    LaunchedEffect(
        movieDetails.id,
        movieDetails.seasonCount,
        movieDetails.episodeCount,
        movieDetails.seasons.size
    ) {
        Log.d(
            DebugTag,
            "render id=${movieDetails.id} name=${movieDetails.name} seasonCount=${movieDetails.seasonCount} episodeCount=${movieDetails.episodeCount} seasons=${movieDetails.seasons.size} cast=${movieDetails.cast.size} similar=${movieDetails.similarMovies.size}"
        )
    }

    BackHandler(onBack = onBackPressed)
    Box(modifier = modifier) {
        MovieDetailsBackdrop(
            posterUri = movieDetails.posterUri,
            title = movieDetails.name,
            modifier = Modifier.matchParentSize(),
            gradientColor = MaterialTheme.colorScheme.background
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 135.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                MovieDetails(
                    movieDetails = movieDetails,
                    goToMoviePlayer = goToMoviePlayer,
                    playButtonLabel = movieDetailsPlayLabel(movieDetails),
                    titleMetadata = movieDetailsTitleMetadata(movieDetails)
                )
            }

            item {
                CastAndCrewList(
                    castAndCrew = movieDetails.cast
                )
            }

            if (movieDetails.similarMovies.isNotEmpty()) {
                item {
                    MoviesRow(
                        title = StringConstants
                            .Composable
                            .movieDetailsScreenSimilarTo(movieDetails.name),
                        titleStyle = MaterialTheme.typography.titleMedium,
                        movieList = movieDetails.similarMovies,
                        itemDirection = ItemDirection.Horizontal,
                        onMovieSelected = refreshScreenWithNewMovie
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = childPadding.start)
                        .padding(BottomDividerPadding)
                        .fillMaxWidth()
                        .height(1.dp)
                        .alpha(0.15f)
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = childPadding.start),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val itemModifier = Modifier.width(192.dp)

                    TitleValueText(
                        modifier = itemModifier,
                        title = stringResource(R.string.status),
                        value = movieDetails.status
                    )
                    TitleValueText(
                        modifier = itemModifier,
                        title = stringResource(R.string.status),
                        value = movieDetails.originalLanguage
                    )
                    TitleValueText(
                        modifier = itemModifier,
                        title = stringResource(R.string.status),
                        value = movieDetails.budget
                    )
                    TitleValueText(
                        modifier = itemModifier,
                        title = stringResource(R.string.status),
                        value = movieDetails.revenue
                    )
                }
            }
        }
    }
}

private val BottomDividerPadding = PaddingValues(vertical = 48.dp)

@Composable
private fun movieDetailsPlayLabel(movieDetails: MovieDetails): String {
    val seasonShort = stringResource(R.string.season_short)
    val episodeShort = stringResource(R.string.episode_short)
    val episodeLabel = stringResource(R.string.episode)
    val movieFallback = stringResource(R.string.movies_singular)
    val seriesFallback = stringResource(R.string.tv_series_singular)

    val currentEpisode = movieDetails.currentEpisode
    val currentSeason = movieDetails.currentSeason

    if (currentEpisode != null) {
        return if (currentSeason != null) {
            "$seasonShort$currentSeason:$episodeShort$currentEpisode"
        } else {
            "$episodeLabel $currentEpisode"
        }
    }

    val hasSeriesMetadata = movieDetails.seasonCount != null || movieDetails.episodeCount != null
    return if (hasSeriesMetadata) seriesFallback else movieFallback
}

@Composable
private fun movieDetailsTitleMetadata(movieDetails: MovieDetails): List<String> {
    val seasonLabel = stringResource(R.string.season)
    val episodesLabel = stringResource(R.string.episodes)

    return listOfNotNull(
        movieDetails.seasonCount?.let { "$seasonLabel $it" },
        movieDetails.episodeCount?.let { "$episodesLabel $it" }
    )
}
