package com.lagradost.cloudstream3.tv.presentation.screens.tvseries

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.lagradost.cloudstream3.tv.presentation.screens.movies.CastAndCrewList
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetails
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsBackdrop
import com.lagradost.cloudstream3.tv.presentation.screens.movies.rememberChildPadding

private const val DebugTag = "TvSeriesDetailsUI"

object TvSeriesDetailsScreen {
    const val UrlBundleKey = "url"
    const val ApiNameBundleKey = "apiName"
}

@Composable
fun TvSeriesDetailsScreen(
    goToPlayer: (String?) -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    tvSeriesDetailsScreenViewModel: TvSeriesDetailsScreenViewModel
) {
    val uiState by tvSeriesDetailsScreenViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is TvSeriesDetailsScreenUiState.Loading -> {
            Loading(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        is TvSeriesDetailsScreenUiState.Error -> {
            Error(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        is TvSeriesDetailsScreenUiState.Done -> {
            Details(
                tvSeriesDetails = s.tvSeriesDetails,
                goToPlayer = goToPlayer,
                onBackPressed = onBackPressed,
                refreshScreenWithNewItem = refreshScreenWithNewItem,
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
    tvSeriesDetails: MovieDetails,
    goToPlayer: (String?) -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    modifier: Modifier = Modifier,
) {
    val childPadding = rememberChildPadding()
    val seasons = tvSeriesDetails.seasons
    var selectedSeasonId by rememberSaveable(tvSeriesDetails.id) {
        mutableStateOf(resolveInitialSeasonId(seasons, tvSeriesDetails.currentSeason))
    }
    val selectedSeason = seasons.firstOrNull { season -> season.id == selectedSeasonId } ?: seasons.firstOrNull()
    val selectedEpisodes = selectedSeason?.episodes.orEmpty()
    val hasAdditionalInfo = listOf(
        tvSeriesDetails.status,
        tvSeriesDetails.originalLanguage,
        tvSeriesDetails.budget,
        tvSeriesDetails.revenue
    ).any { value -> value.isNotBlank() }

    LaunchedEffect(seasons, tvSeriesDetails.currentSeason) {
        if (seasons.none { season -> season.id == selectedSeasonId }) {
            selectedSeasonId = resolveInitialSeasonId(seasons, tvSeriesDetails.currentSeason)
        }
    }
    LaunchedEffect(
        tvSeriesDetails.id,
        tvSeriesDetails.seasonCount,
        tvSeriesDetails.episodeCount,
        seasons.size,
        selectedSeasonId,
        selectedEpisodes.size
    ) {
        Log.d(
            DebugTag,
            "render id=${tvSeriesDetails.id} name=${tvSeriesDetails.name} seasonCount=${tvSeriesDetails.seasonCount} episodeCount=${tvSeriesDetails.episodeCount} seasons=${seasons.size} selectedSeason=$selectedSeasonId selectedEpisodes=${selectedEpisodes.size} cast=${tvSeriesDetails.cast.size} similar=${tvSeriesDetails.similarMovies.size}"
        )
        if (seasons.isEmpty()) {
            Log.d(DebugTag, "render:seasons list is empty")
        }
    }

    BackHandler(onBack = onBackPressed)
    Box(modifier = modifier) {
        MovieDetailsBackdrop(
            posterUri = tvSeriesDetails.posterUri,
            title = tvSeriesDetails.name,
            modifier = Modifier.matchParentSize(),
            gradientColor = MaterialTheme.colorScheme.background
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 135.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                MovieDetails(
                    movieDetails = tvSeriesDetails,
                    goToMoviePlayer = { goToPlayer(resolveDefaultEpisodeData(tvSeriesDetails)) },
                    playButtonLabel = tvSeriesPlayLabel(tvSeriesDetails),
                    titleMetadata = tvSeriesTitleMetadata(tvSeriesDetails)
                )
            }

            if (seasons.isNotEmpty()) {
                item {
                    SeasonSelectorRow(
                        seasons = seasons,
                        selectedSeasonId = selectedSeasonId,
                        onSeasonSelected = { season -> selectedSeasonId = season.id },
                        modifier = Modifier
                            .padding(start = childPadding.start, end = childPadding.end)
                            .padding(bottom = 8.dp)
                    )
                }
            }

            if (selectedEpisodes.isEmpty()) {
                item {
                    NoEpisodesRow(
                        modifier = Modifier
                            .padding(start = childPadding.start, end = childPadding.end)
                            .padding(bottom = 8.dp)
                    )
                }
            } else {
                items(
                    items = selectedEpisodes,
                    key = { episode -> episode.id }
                ) { episode ->
                    EpisodeCard(
                        episode = episode,
                        fallbackDescription = tvSeriesDetails.description,
                        onEpisodeSelected = { selectedEpisode ->
                            goToPlayer(selectedEpisode.data)
                        },
                        modifier = Modifier
                            .padding(start = childPadding.start, end = childPadding.end)
                            .padding(bottom = 12.dp)
                    )
                }
            }

            if (tvSeriesDetails.cast.isNotEmpty()) {
                item {
                    CastAndCrewList(
                        castAndCrew = tvSeriesDetails.cast
                    )
                }
            }

            if (tvSeriesDetails.similarMovies.isNotEmpty()) {
                item {
                    MoviesRow(
                        title = StringConstants
                            .Composable
                            .movieDetailsScreenSimilarTo(tvSeriesDetails.name),
                        titleStyle = MaterialTheme.typography.titleMedium,
                        movieList = tvSeriesDetails.similarMovies,
                        itemDirection = ItemDirection.Horizontal,
                        onMovieSelected = refreshScreenWithNewItem
                    )
                }
            }

            if (hasAdditionalInfo) {
                item {
                    Box(
                        modifier = Modifier
                            .padding(start = childPadding.start, end = childPadding.end)
                            .padding(BottomDividerPadding)
                            .fillMaxWidth()
                            .height(1.dp)
                            .alpha(0.15f)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                }

                item {
                    AdditionalInfoSection(
                        tvSeriesDetails = tvSeriesDetails
                    )
                }
            }
        }
    }
}

private val BottomDividerPadding = PaddingValues(vertical = 48.dp)

private fun resolveDefaultEpisodeData(details: MovieDetails): String? {
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
private fun tvSeriesPlayLabel(tvSeriesDetails: MovieDetails): String {
    val seasonShort = stringResource(R.string.season_short)
    val episodeShort = stringResource(R.string.episode_short)
    val episodeLabel = stringResource(R.string.episode)
    val fallback = stringResource(R.string.tv_series_singular)

    val currentEpisode = tvSeriesDetails.currentEpisode ?: return fallback
    val currentSeason = tvSeriesDetails.currentSeason

    return if (currentSeason != null) {
        "$seasonShort$currentSeason:$episodeShort$currentEpisode"
    } else {
        "$episodeLabel $currentEpisode"
    }
}

@Composable
private fun tvSeriesTitleMetadata(tvSeriesDetails: MovieDetails): List<String> {
    val seasonLabel = stringResource(R.string.season)
    val episodesLabel = stringResource(R.string.episodes)

    return listOfNotNull(
        tvSeriesDetails.seasonCount?.let { "$seasonLabel $it" },
        tvSeriesDetails.episodeCount?.let { "$episodesLabel $it" }
    )
}
