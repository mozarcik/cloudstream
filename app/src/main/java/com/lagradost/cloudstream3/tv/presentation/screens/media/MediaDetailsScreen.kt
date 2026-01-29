/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lagradost.cloudstream3.tv.presentation.screens.media

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
import com.lagradost.cloudstream3.tv.presentation.screens.movies.rememberChildPadding
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.AdditionalInfoSection
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.EpisodeCard
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.EpisodesSectionHeader
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.NoEpisodesRow
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.SeasonSelectorRow
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.SeasonsSectionHeader
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.resolveInitialSeasonId

private const val DebugTag = "TvMediaDetailsUI"

object MediaDetailsScreen {
    const val UrlBundleKey = "url"
    const val ApiNameBundleKey = "apiName"
}

@Composable
fun MediaDetailsScreen(
    goToPlayer: (String?) -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    mediaDetailsScreenViewModel: MediaDetailsScreenViewModel
) {
    val uiState by mediaDetailsScreenViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is MediaDetailsScreenUiState.Loading -> {
            Loading(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        is MediaDetailsScreenUiState.Error -> {
            Error(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        is MediaDetailsScreenUiState.Done -> {
            Details(
                mediaDetails = s.mediaDetails,
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
    mediaDetails: MovieDetails,
    goToPlayer: (String?) -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    modifier: Modifier = Modifier,
) {
    val childPadding = rememberChildPadding()
    val seasons = mediaDetails.seasons
    val isSeriesContent = mediaDetails.seasonCount != null || mediaDetails.episodeCount != null || seasons.isNotEmpty()
    var selectedSeasonId by rememberSaveable(mediaDetails.id) {
        mutableStateOf(resolveInitialSeasonId(seasons, mediaDetails.currentSeason))
    }
    val selectedSeason = seasons.firstOrNull { season -> season.id == selectedSeasonId } ?: seasons.firstOrNull()
    val selectedEpisodes = selectedSeason?.episodes.orEmpty()
    val hasAdditionalInfo = listOf(
        mediaDetails.status,
        mediaDetails.originalLanguage,
        mediaDetails.budget,
        mediaDetails.revenue
    ).any { value -> value.isNotBlank() }

    LaunchedEffect(seasons, mediaDetails.currentSeason) {
        if (seasons.none { season -> season.id == selectedSeasonId }) {
            selectedSeasonId = resolveInitialSeasonId(seasons, mediaDetails.currentSeason)
        }
    }
    LaunchedEffect(
        mediaDetails.id,
        isSeriesContent,
        mediaDetails.seasonCount,
        mediaDetails.episodeCount,
        seasons.size,
        selectedSeasonId,
        selectedEpisodes.size
    ) {
        Log.d(
            DebugTag,
            "render id=${mediaDetails.id} name=${mediaDetails.name} isSeries=$isSeriesContent seasonCount=${mediaDetails.seasonCount} episodeCount=${mediaDetails.episodeCount} seasons=${seasons.size} selectedSeason=$selectedSeasonId selectedEpisodes=${selectedEpisodes.size} cast=${mediaDetails.cast.size} similar=${mediaDetails.similarMovies.size}"
        )
        if (isSeriesContent && seasons.isEmpty()) {
            Log.d(
                DebugTag,
                "render:series metadata exists but seasons list is empty"
            )
        }
    }

    BackHandler(onBack = onBackPressed)
    LazyColumn(
        contentPadding = PaddingValues(bottom = 135.dp),
        modifier = modifier,
    ) {
        item {
            MovieDetails(
                movieDetails = mediaDetails,
                goToMoviePlayer = { goToPlayer(resolveDefaultEpisodeData(mediaDetails)) },
                playButtonLabel = mediaDetailsPlayLabel(mediaDetails),
                titleMetadata = mediaDetailsTitleMetadata(mediaDetails)
            )
        }

        if (isSeriesContent) {
            if (seasons.isNotEmpty()) {
                item {
                    SeasonsSectionHeader(
                        modifier = Modifier.padding(start = childPadding.start)
                    )
                    SeasonSelectorRow(
                        seasons = seasons,
                        selectedSeasonId = selectedSeasonId,
                        onSeasonSelected = { season -> selectedSeasonId = season.id }
                    )
                }
            }

            item {
                EpisodesSectionHeader(
                    modifier = Modifier.padding(start = childPadding.start)
                )
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
                        fallbackDescription = mediaDetails.description,
                        onEpisodeSelected = { selectedEpisode ->
                            goToPlayer(selectedEpisode.data)
                        },
                        modifier = Modifier
                            .padding(start = childPadding.start, end = childPadding.end)
                            .padding(bottom = 12.dp)
                    )
                }
            }
        }

        if (mediaDetails.cast.isNotEmpty()) {
            item {
                CastAndCrewList(
                    castAndCrew = mediaDetails.cast
                )
            }
        }

        if (mediaDetails.similarMovies.isNotEmpty()) {
            item {
                MoviesRow(
                    title = StringConstants
                        .Composable
                        .movieDetailsScreenSimilarTo(mediaDetails.name),
                    titleStyle = MaterialTheme.typography.titleMedium,
                    movieList = mediaDetails.similarMovies,
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
                    tvSeriesDetails = mediaDetails
                )
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
private fun mediaDetailsPlayLabel(mediaDetails: MovieDetails): String {
    val seasonShort = stringResource(R.string.season_short)
    val episodeShort = stringResource(R.string.episode_short)
    val episodeLabel = stringResource(R.string.episode)
    val movieFallback = stringResource(R.string.movies_singular)
    val seriesFallback = stringResource(R.string.tv_series_singular)

    val currentEpisode = mediaDetails.currentEpisode
    val currentSeason = mediaDetails.currentSeason

    if (currentEpisode != null) {
        return if (currentSeason != null) {
            "$seasonShort$currentSeason:$episodeShort$currentEpisode"
        } else {
            "$episodeLabel $currentEpisode"
        }
    }

    val hasSeriesMetadata = mediaDetails.seasonCount != null || mediaDetails.episodeCount != null
    return if (hasSeriesMetadata) seriesFallback else movieFallback
}

@Composable
private fun mediaDetailsTitleMetadata(mediaDetails: MovieDetails): List<String> {
    val seasonLabel = stringResource(R.string.season)
    val episodesLabel = stringResource(R.string.episodes)

    return listOfNotNull(
        mediaDetails.seasonCount?.let { "$seasonLabel $it" },
        mediaDetails.episodeCount?.let { "$episodesLabel $it" }
    )
}
