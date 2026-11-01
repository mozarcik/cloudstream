package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerStartTarget

private const val DetailsCrossfadeMs = 300

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

    AnimatedContent(
        targetState = uiState,
        contentKey = { it::class },
        transitionSpec = {
            // The final screen fades in over the placeholder, which is dropped only once it is fully covered.
            fadeIn(tween(DetailsCrossfadeMs)) togetherWith
                fadeOut(tween(durationMillis = 1, delayMillis = DetailsCrossfadeMs))
        },
        label = "DetailsStateCrossfade",
        modifier = modifier,
    ) { uiState ->
    when (uiState) {
        is DetailsScreenUiState.Loading -> DetailsScreenLoadingStateContent(
            mode = mode,
            state = uiState,
            modifier = Modifier,
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
            modifier = Modifier,
        )

        is DetailsScreenUiState.Error -> DetailsScreenFailureStateContent(
            state = uiState,
            title = detailsScreenViewModel.unavailableDetails.title,
            onRetry = detailsScreenViewModel::retry,
            onBackPressed = onBackPressed,
            modifier = Modifier,
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
            modifier = Modifier,
        )
    }
    }
}
