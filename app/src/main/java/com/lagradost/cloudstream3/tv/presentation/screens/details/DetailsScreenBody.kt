package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetails
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsBackdrop
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerStartTarget
import com.lagradost.cloudstream3.tv.presentation.utils.Padding
import kotlinx.coroutines.launch

@Composable
internal fun DetailsScreenBody(
    mode: DetailsScreenMode,
    details: MovieDetails,
    isSeriesContent: Boolean,
    isSecondaryContentLoading: Boolean,
    listState: LazyListState,
    childPadding: Padding,
    seasons: List<TvSeason>,
    selectedSeasonId: String?,
    selectedEpisodes: List<TvEpisode>,
    seasonTabFocusRequesters: List<FocusRequester>,
    heroState: DetailsHeroUiState,
    downloadButtonState: DetailsDownloadButtonUiState,
    hasAdditionalInfo: Boolean,
    resolveEpisodeDownloadState: (TvEpisode) -> DetailsDownloadButtonUiState,
    resolveEpisodeWatchedState: (TvEpisode) -> Boolean,
    toDownloadUiState: (DetailsDownloadButtonUiState) -> com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsDownloadActionState,
    onAction: (DetailsUiAction) -> Unit,
    onSeasonSelected: (TvSeason) -> Unit,
    onEpisodeSelected: (TvEpisode) -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    goToPlayer: (PlayerStartTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier) {
        MovieDetailsBackdrop(
            posterUri = details.backdropUri.ifBlank { details.posterUri },
            title = details.name,
            headers = details.posterHeaders,
            modifier = Modifier.fillMaxSize(),
            gradientColor = MaterialTheme.colorScheme.background
        )

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 135.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                MovieDetails(
                    movieDetails = details,
                    goToMoviePlayer = { goToPlayer(resolveDefaultPlaybackTarget(details)) },
                    playButtonLabel = heroState.playButtonLabel,
                    downloadActionState = heroState.downloadActionState,
                    onPrimaryActionsFocused = {
                        if (listState.firstVisibleItemIndex == 0 &&
                            listState.firstVisibleItemScrollOffset == 0
                        ) {
                            return@MovieDetails
                        }
                        if (listState.isScrollInProgress) return@MovieDetails
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    onQuickActionClick = { quickAction ->
                        onAction(DetailsUiAction.HeroQuickAction(quickAction))
                    },
                )
            }

            if (isSeriesContent) {
                detailsSeriesItems(
                    mode = mode,
                    seasons = seasons,
                    selectedSeasonId = selectedSeasonId,
                    selectedEpisodes = selectedEpisodes,
                    detailsDescription = details.description,
                    childPadding = childPadding,
                    seasonTabFocusRequesters = seasonTabFocusRequesters,
                    onSeasonSelected = onSeasonSelected,
                    resolveEpisodeDownloadState = resolveEpisodeDownloadState,
                    resolveEpisodeWatchedState = resolveEpisodeWatchedState,
                    toDownloadUiState = toDownloadUiState,
                    onEpisodeSelected = onEpisodeSelected,
                    onEpisodeQuickActionClick = { episode, action ->
                        onAction(DetailsUiAction.EpisodeQuickAction(episode, action))
                    },
                )
            }

            detailsSecondaryItems(
                isSecondaryContentLoading = isSecondaryContentLoading,
                hasAdditionalInfo = hasAdditionalInfo,
                detailsName = details.name,
                detailsStatus = details.status,
                detailsOriginalLanguage = details.originalLanguage,
                detailsBudget = details.budget,
                detailsRevenue = details.revenue,
                cast = details.cast,
                similarMovies = details.similarMovies,
                details = details,
                childPadding = childPadding,
                refreshScreenWithNewItem = refreshScreenWithNewItem,
            )
        }
    }
}
