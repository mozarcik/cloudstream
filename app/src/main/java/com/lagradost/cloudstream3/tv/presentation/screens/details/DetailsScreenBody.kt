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
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsQuickAction
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
    selectedSeasonFocusRequester: FocusRequester?,
    heroState: DetailsHeroUiState,
    downloadButtonState: DetailsDownloadButtonUiState,
    hasAdditionalInfo: Boolean,
    resolveEpisodeDownloadState: (TvEpisode) -> DetailsDownloadButtonUiState,
    resolveEpisodeWatchedState: (TvEpisode) -> Boolean,
    toDownloadUiState: (DetailsDownloadButtonUiState) -> com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsDownloadActionState,
    onFavoriteClick: () -> Unit,
    onOpenBookmarkPanel: () -> Unit,
    onOpenActionsPanel: () -> Unit,
    onHandleDownloadQuickAction: (DetailsDownloadButtonUiState, Int?, Int?) -> Unit,
    onSeasonSelected: (TvSeason) -> Unit,
    onEpisodeSelected: (TvEpisode) -> Unit,
    onEpisodeQuickActionClick: (TvEpisode, MovieDetailsQuickAction) -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    goToPlayer: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier) {
        MovieDetailsBackdrop(
            posterUri = details.posterUri,
            title = details.name,
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
                    goToMoviePlayer = { goToPlayer(resolveDefaultEpisodeData(details)) },
                    playButtonLabel = heroState.playButtonLabel,
                    titleMetadata = heroState.titleMetadata,
                    downloadActionState = heroState.downloadActionState,
                    downFocusRequester = selectedSeasonFocusRequester,
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
                        when (quickAction) {
                            MovieDetailsQuickAction.Bookmark -> onOpenBookmarkPanel()
                            MovieDetailsQuickAction.Favorite -> onFavoriteClick()
                            MovieDetailsQuickAction.Download -> onHandleDownloadQuickAction(
                                downloadButtonState,
                                null,
                                null,
                            )
                            MovieDetailsQuickAction.More -> onOpenActionsPanel()
                            MovieDetailsQuickAction.Search -> Unit
                            MovieDetailsQuickAction.MarkAsWatched -> Unit
                            MovieDetailsQuickAction.MarkWatchedUpToThisEpisode -> Unit
                            MovieDetailsQuickAction.RemoveFromWatched -> Unit
                            MovieDetailsQuickAction.RemoveWatchedUpToThisEpisode -> Unit
                        }
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
                    onEpisodeQuickActionClick = onEpisodeQuickActionClick,
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
