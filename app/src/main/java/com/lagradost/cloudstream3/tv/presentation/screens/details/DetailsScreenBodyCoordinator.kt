package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionStateHolder
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerStartTarget
import com.lagradost.cloudstream3.tv.presentation.utils.Padding
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsQuickAction
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun DetailsScreenBodyCoordinator(
    mode: DetailsScreenMode,
    details: MovieDetails,
    isSeriesContent: Boolean,
    isSecondaryContentLoading: Boolean,
    listState: LazyListState,
    childPadding: Padding,
    seasons: List<TvSeason>,
    selectedSeasonId: String?,
    selectedSeason: TvSeason?,
    selectedEpisodes: List<TvEpisode>,
    seasonTabFocusRequesters: List<FocusRequester>,
    heroState: DetailsHeroUiState,
    downloadButtonState: DetailsDownloadButtonUiState,
    episodesStateHolder: DetailsEpisodesStateHolder,
    panelsStateHolder: DetailsPanelsStateHolder,
    downloadMirrorStateHolder: DownloadMirrorSelectionStateHolder,
    actionsCompat: MovieDetailsEpisodeActionsCompat,
    hasAdditionalInfo: Boolean,
    context: Context,
    scope: CoroutineScope,
    closeDownloadPanel: () -> Unit,
    openDownloadPanel: (Int?, Int?) -> Unit,
    onFavoriteClick: () -> Unit,
    onManualSearchRequested: (String) -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    goToPlayer: (PlayerStartTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailsScreenBody(
        mode = mode,
        details = details,
        isSeriesContent = isSeriesContent,
        isSecondaryContentLoading = isSecondaryContentLoading,
        listState = listState,
        childPadding = childPadding,
        seasons = seasons,
        selectedSeasonId = selectedSeasonId,
        selectedEpisodes = selectedEpisodes,
        seasonTabFocusRequesters = seasonTabFocusRequesters,
        heroState = heroState,
        downloadButtonState = downloadButtonState,
        hasAdditionalInfo = hasAdditionalInfo,
        resolveEpisodeDownloadState = episodesStateHolder::resolveEpisodeDownloadState,
        resolveEpisodeWatchedState = episodesStateHolder::resolveEpisodeWatchedState,
        toDownloadUiState = DetailsDownloadButtonUiState::toMovieDetailsDownloadActionState,
        onAction = { action ->
            when (action) {
                is DetailsUiAction.HeroQuickAction -> when (action.action) {
                    MovieDetailsQuickAction.Bookmark -> openDetailsBookmarkPanel(
                        mode = mode,
                        panelsStateHolder = panelsStateHolder,
                        closeDownloadPanel = closeDownloadPanel,
                    )

                    MovieDetailsQuickAction.Favorite -> onFavoriteClick()
                    MovieDetailsQuickAction.Download -> handleDetailsDownloadQuickAction(
                        state = downloadButtonState,
                        preferredSeason = null,
                        preferredEpisode = null,
                        openDownloadPanel = openDownloadPanel,
                        goToPlayer = goToPlayer,
                    )

                    MovieDetailsQuickAction.More -> openDetailsActionsPanel(
                        mode = mode,
                        context = context,
                        actionsCompat = actionsCompat,
                        panelsStateHolder = panelsStateHolder,
                        scope = scope,
                        closeDownloadPanel = closeDownloadPanel,
                        onPlayInApp = { episodeData ->
                            goToPlayer(
                                episodeData
                                    ?.let(PlayerStartTarget::DirectEpisodeData)
                                    ?: PlayerStartTarget.Default
                            )
                        },
                    )

                    MovieDetailsQuickAction.Search -> {
                        resolveDetailsSearchQuery(details.name)?.let(onManualSearchRequested)
                    }

                    MovieDetailsQuickAction.MarkAsWatched,
                    MovieDetailsQuickAction.MarkWatchedUpToThisEpisode,
                    MovieDetailsQuickAction.RemoveFromWatched,
                    MovieDetailsQuickAction.RemoveWatchedUpToThisEpisode -> Unit
                }

                is DetailsUiAction.EpisodeQuickAction -> onDetailsEpisodeQuickAction(
                    quickAction = action.action,
                    episode = action.episode,
                    selectedSeason = selectedSeason,
                    context = context,
                    actionsCompat = actionsCompat,
                    downloadMirrorStateHolder = downloadMirrorStateHolder,
                    panelsStateHolder = panelsStateHolder,
                    episodesStateHolder = episodesStateHolder,
                    scope = scope,
                    openDownloadPanel = openDownloadPanel,
                    openActionsPanel = { preferredSeason, preferredEpisode, title ->
                        openDetailsActionsPanel(
                            mode = mode,
                            context = context,
                            actionsCompat = actionsCompat,
                            panelsStateHolder = panelsStateHolder,
                            scope = scope,
                            closeDownloadPanel = closeDownloadPanel,
                            preferredSeason = preferredSeason,
                            preferredEpisode = preferredEpisode,
                            title = title,
                            onPlayInApp = { episodeData ->
                                goToPlayer(
                                    episodeData
                                        ?.let(PlayerStartTarget::DirectEpisodeData)
                                        ?: PlayerStartTarget.Default
                                )
                            },
                        )
                    },
                    goToPlayer = goToPlayer,
                )
            }
        },
        onSeasonSelected = { season -> episodesStateHolder.onSeasonSelected(season.id) },
        onEpisodeSelected = { episode -> goToPlayer(PlayerStartTarget.DirectEpisodeData(episode.data)) },
        refreshScreenWithNewItem = refreshScreenWithNewItem,
        goToPlayer = goToPlayer,
        modifier = modifier,
    )
}
