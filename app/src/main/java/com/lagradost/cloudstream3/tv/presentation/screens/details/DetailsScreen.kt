package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.presentation.screens.unavailable.UnavailableDetailsUiModel
import com.lagradost.cloudstream3.ui.WatchType

@Composable
fun DetailsScreen(
    mode: DetailsScreenMode,
    goToPlayer: (String?) -> Unit,
    onBackPressed: () -> Unit,
    onManualSearchRequested: (String) -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    detailsScreenViewModel: DetailsScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState = detailsScreenViewModel.uiState.collectAsStateWithLifecycle().value
    val actionsCompat = detailsScreenViewModel.actionsCompat

    DetailsScreenRouteContent(
        mode = mode,
        uiState = uiState,
        actionsCompat = actionsCompat,
        shouldShowUnavailableState = detailsScreenViewModel.shouldShowUnavailableState,
        unavailableDetails = detailsScreenViewModel.unavailableDetails,
        canRemoveFromLibrary = detailsScreenViewModel.canRemoveFromLibrary,
        goToPlayer = goToPlayer,
        onBackPressed = onBackPressed,
        onManualSearchRequested = onManualSearchRequested,
        refreshScreenWithNewItem = refreshScreenWithNewItem,
        onFavoriteClick = detailsScreenViewModel::onFavoriteClick,
        onBookmarkClick = detailsScreenViewModel::onBookmarkClick,
        onRemoveUnavailable = {
            detailsScreenViewModel.removeUnavailableItemFromLibrary()
            onBackPressed()
        },
        modifier = modifier,
    )
}

@Composable
internal fun DetailsScreenRouteContent(
    mode: DetailsScreenMode,
    uiState: DetailsScreenUiState,
    actionsCompat: MovieDetailsEpisodeActionsCompat?,
    shouldShowUnavailableState: Boolean,
    unavailableDetails: UnavailableDetailsUiModel,
    canRemoveFromLibrary: Boolean,
    goToPlayer: (String?) -> Unit,
    onBackPressed: () -> Unit,
    onManualSearchRequested: (String) -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    onFavoriteClick: () -> Unit,
    onBookmarkClick: (WatchType) -> Unit,
    onRemoveUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val state = uiState) {
        is DetailsScreenUiState.Loading -> DetailsScreenLoadingStateContent(
            mode = mode,
            state = state,
            modifier = modifier,
        )

        DetailsScreenUiState.Error -> DetailsScreenErrorStateContent(
            shouldShowUnavailableState = shouldShowUnavailableState,
            unavailableDetails = unavailableDetails,
            canRemoveFromLibrary = canRemoveFromLibrary,
            onRemoveUnavailable = onRemoveUnavailable,
            onManualSearchRequested = onManualSearchRequested,
            onBackPressed = onBackPressed,
            modifier = modifier,
        )

        is DetailsScreenUiState.Done -> DetailsScreenDoneStateContent(
            mode = mode,
            state = state,
            actionsCompat = actionsCompat,
            goToPlayer = goToPlayer,
            onBackPressed = onBackPressed,
            refreshScreenWithNewItem = refreshScreenWithNewItem,
            onFavoriteClick = onFavoriteClick,
            onBookmarkClick = onBookmarkClick,
            modifier = modifier,
        )
    }
}
