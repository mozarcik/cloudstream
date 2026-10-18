package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerStartTarget

@Composable
fun DetailsScreen(
    mode: DetailsScreenMode,
    goToPlayer: (PlayerStartTarget) -> Unit,
    onBackPressed: () -> Unit,
    onManualSearchRequested: (String) -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    detailsScreenViewModel: DetailsScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState = detailsScreenViewModel.uiState.collectAsStateWithLifecycle().value
    val actionsCompat = detailsScreenViewModel.actionsCompat

    when (uiState) {
        is DetailsScreenUiState.Loading -> DetailsScreenLoadingStateContent(
            mode = mode,
            state = uiState,
            modifier = modifier,
        )

        DetailsScreenUiState.Unavailable -> DetailsScreenErrorStateContent(
            unavailableDetails = detailsScreenViewModel.unavailableDetails,
            canRemoveFromLibrary = detailsScreenViewModel.canRemoveFromLibrary,
            onRemoveUnavailable = {
                detailsScreenViewModel.removeUnavailableItemFromLibrary()
                onBackPressed()
            },
            onManualSearchRequested = onManualSearchRequested,
            onBackPressed = onBackPressed,
            modifier = modifier,
        )

        is DetailsScreenUiState.Error -> DetailsScreenFailureStateContent(
            state = uiState,
            title = detailsScreenViewModel.unavailableDetails.title,
            onRetry = detailsScreenViewModel::retry,
            onBackPressed = onBackPressed,
            modifier = modifier,
        )

        is DetailsScreenUiState.Done -> DetailsScreenDoneStateContent(
            mode = mode,
            state = uiState,
            actionsCompat = actionsCompat,
            goToPlayer = goToPlayer,
            onBackPressed = onBackPressed,
            onManualSearchRequested = onManualSearchRequested,
            refreshScreenWithNewItem = refreshScreenWithNewItem,
            onFavoriteClick = detailsScreenViewModel::onFavoriteClick,
            onBookmarkClick = detailsScreenViewModel::onBookmarkClick,
            modifier = modifier,
        )
    }
}
