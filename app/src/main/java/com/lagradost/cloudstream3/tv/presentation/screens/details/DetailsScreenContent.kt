package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionStateHolder
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.presentation.focus.rememberFocusRequesters
import com.lagradost.cloudstream3.tv.presentation.screens.movies.rememberChildPadding
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.resolveInitialSeasonId
import com.lagradost.cloudstream3.ui.WatchType

@Composable
internal fun DetailsScreenContent(
    mode: DetailsScreenMode,
    details: MovieDetails,
    actionsCompat: MovieDetailsEpisodeActionsCompat,
    isSecondaryContentLoading: Boolean,
    goToPlayer: (String?) -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    onFavoriteClick: () -> Unit,
    onBookmarkClick: (WatchType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val childPadding = rememberChildPadding()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val downloadButtonViewModel: DetailsDownloadButtonViewModel = viewModel()
    val downloadButtonState by downloadButtonViewModel.uiState.collectAsStateWithLifecycle()
    val seasons = details.seasons
    val seasonTabFocusRequesters = rememberFocusRequesters(count = seasons.size)
    val downloadMirrorStateHolder = remember(details.id, coroutineScope) {
        DownloadMirrorSelectionStateHolder(scope = coroutineScope)
    }
    val downloadMirrorState by downloadMirrorStateHolder.uiState.collectAsStateWithLifecycle()
    val panelsStateHolder = rememberDetailsPanelsStateHolder(details.id, mode)
    val episodesStateHolder = rememberDetailsEpisodesStateHolder(
        detailsId = details.id,
        context = context,
        actionsCompat = actionsCompat,
        initialSeasonId = resolveInitialSeasonId(seasons, details.currentSeason),
        scope = coroutineScope,
    )
    val selectedSeasonId = episodesStateHolder.selectedSeasonId
    val selectedSeason = seasons.firstOrNull { season -> season.id == selectedSeasonId } ?: seasons.firstOrNull()
    val selectedEpisodes = selectedSeason?.episodes.orEmpty()
    val selectedSeasonIndex = seasons.indexOfFirst { season -> season.id == selectedSeasonId }
        .takeIf { it >= 0 } ?: 0
    val selectedSeasonFocusRequester = seasonTabFocusRequesters.getOrNull(selectedSeasonIndex)
    val isSeriesContent = mode.isSeriesContent(details)
    val heroState = rememberDetailsHeroUiState(
        mode = mode,
        details = details,
        isSeriesContent = isSeriesContent,
        downloadButtonState = downloadButtonState,
    )
    val downloadPanelActions = rememberDetailsDownloadPanelActions(
        mode = mode,
        context = context,
        actionsCompat = actionsCompat,
        downloadButtonViewModel = downloadButtonViewModel,
        downloadMirrorStateHolder = downloadMirrorStateHolder,
        panelsStateHolder = panelsStateHolder,
    )
    DetailsScreenContentEffects(
        detailsId = details.id,
        detailsCurrentSeason = details.currentSeason,
        seasons = seasons,
        selectedSeasonId = selectedSeasonId,
        selectedEpisodes = selectedEpisodes,
        context = context,
        listState = listState,
        defaultActionsCompat = actionsCompat,
        downloadButtonViewModel = downloadButtonViewModel,
        episodesStateHolder = episodesStateHolder,
        downloadMirrorStateHolder = downloadMirrorStateHolder,
        onResolveEpisodeSeason = { episode -> resolveDetailsEpisodeSeason(episode, selectedSeason) },
        onHandleDownloadActionOutcome = { outcome ->
            handleDetailsDownloadActionOutcome(
                mode = mode,
                outcome = outcome,
                downloadMirrorStateHolder = downloadMirrorStateHolder,
                closeDownloadPanel = downloadPanelActions.closePanel,
            )
        },
    )
    BackHandler(
        enabled = !panelsStateHolder.isActionsPanelVisible &&
            !panelsStateHolder.isBookmarkPanelVisible &&
            !downloadMirrorState.isVisible,
        onBack = onBackPressed,
    )
    DetailsScreenBodyCoordinator(
        mode = mode,
        details = details,
        isSeriesContent = isSeriesContent,
        isSecondaryContentLoading = isSecondaryContentLoading,
        listState = listState,
        childPadding = childPadding,
        seasons = seasons,
        selectedSeasonId = selectedSeasonId,
        selectedSeason = selectedSeason,
        selectedEpisodes = selectedEpisodes,
        seasonTabFocusRequesters = seasonTabFocusRequesters,
        selectedSeasonFocusRequester = selectedSeasonFocusRequester,
        heroState = heroState,
        downloadButtonState = downloadButtonState,
        episodesStateHolder = episodesStateHolder,
        panelsStateHolder = panelsStateHolder,
        downloadMirrorStateHolder = downloadMirrorStateHolder,
        actionsCompat = actionsCompat,
        hasAdditionalInfo = details.hasDetailsAdditionalInfo(),
        context = context,
        scope = coroutineScope,
        closeDownloadPanel = downloadPanelActions.closePanel,
        openDownloadPanel = downloadPanelActions.openPanel,
        onFavoriteClick = onFavoriteClick,
        refreshScreenWithNewItem = refreshScreenWithNewItem,
        goToPlayer = goToPlayer,
        modifier = modifier,
    )
    DetailsScreenOverlayCoordinator(
        mode = mode,
        details = details,
        actionsCompat = actionsCompat,
        panelsStateHolder = panelsStateHolder,
        downloadMirrorState = downloadMirrorState,
        downloadMirrorStateHolder = downloadMirrorStateHolder,
        downloadButtonViewModel = downloadButtonViewModel,
        context = context,
        scope = coroutineScope,
        closeDownloadPanel = downloadPanelActions.closePanel,
        goToPlayer = goToPlayer,
        onBookmarkClick = onBookmarkClick,
    )
}
