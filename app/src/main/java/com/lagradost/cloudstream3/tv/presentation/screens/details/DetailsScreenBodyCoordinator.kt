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
import com.lagradost.cloudstream3.tv.presentation.utils.Padding
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
    selectedSeasonFocusRequester: FocusRequester?,
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
    refreshScreenWithNewItem: (Movie) -> Unit,
    goToPlayer: (String?) -> Unit,
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
        selectedSeasonFocusRequester = selectedSeasonFocusRequester,
        heroState = heroState,
        downloadButtonState = downloadButtonState,
        hasAdditionalInfo = hasAdditionalInfo,
        resolveEpisodeDownloadState = episodesStateHolder::resolveEpisodeDownloadState,
        resolveEpisodeWatchedState = episodesStateHolder::resolveEpisodeWatchedState,
        toDownloadUiState = DetailsDownloadButtonUiState::toMovieDetailsDownloadActionState,
        onFavoriteClick = onFavoriteClick,
        onOpenBookmarkPanel = {
            openDetailsBookmarkPanel(
                mode = mode,
                panelsStateHolder = panelsStateHolder,
                closeDownloadPanel = closeDownloadPanel,
            )
        },
        onOpenActionsPanel = {
            openDetailsActionsPanel(
                mode = mode,
                context = context,
                actionsCompat = actionsCompat,
                panelsStateHolder = panelsStateHolder,
                scope = scope,
                closeDownloadPanel = closeDownloadPanel,
            )
        },
        onHandleDownloadQuickAction = { state, preferredSeason, preferredEpisode ->
            handleDetailsDownloadQuickAction(
                state = state,
                preferredSeason = preferredSeason,
                preferredEpisode = preferredEpisode,
                openDownloadPanel = openDownloadPanel,
                goToPlayer = goToPlayer,
            )
        },
        onSeasonSelected = { season -> episodesStateHolder.onSeasonSelected(season.id) },
        onEpisodeSelected = { episode -> goToPlayer(episode.data) },
        onEpisodeQuickActionClick = { episode, quickAction ->
            onDetailsEpisodeQuickAction(
                quickAction = quickAction,
                episode = episode,
                selectedSeason = selectedSeason,
                context = context,
                actionsCompat = actionsCompat,
                downloadMirrorStateHolder = downloadMirrorStateHolder,
                panelsStateHolder = panelsStateHolder,
                episodesStateHolder = episodesStateHolder,
                scope = scope,
                openDownloadPanel = openDownloadPanel,
                goToPlayer = goToPlayer,
            )
        },
        refreshScreenWithNewItem = refreshScreenWithNewItem,
        goToPlayer = goToPlayer,
        modifier = modifier,
    )
}
