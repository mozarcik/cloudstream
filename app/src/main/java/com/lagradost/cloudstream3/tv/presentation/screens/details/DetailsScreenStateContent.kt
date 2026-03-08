package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.presentation.common.Error
import com.lagradost.cloudstream3.tv.presentation.common.Loading
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsLoadingPlaceholder
import com.lagradost.cloudstream3.tv.presentation.screens.unavailable.UnavailableDetailsUiModel
import com.lagradost.cloudstream3.tv.presentation.screens.unavailable.UnavailableDetailsScreen
import com.lagradost.cloudstream3.ui.WatchType

@Composable
internal fun DetailsScreenLoadingStateContent(
    mode: DetailsScreenMode,
    state: DetailsScreenUiState.Loading,
    modifier: Modifier = Modifier,
) {
    val preview = state.preview
    val shouldShowPlaceholder = mode != DetailsScreenMode.Media ||
        !preview.title.isNullOrBlank() ||
        !preview.posterUri.isNullOrBlank() ||
        !preview.backdropUri.isNullOrBlank()

    if (shouldShowPlaceholder) {
        MovieDetailsLoadingPlaceholder(
            title = preview.title ?: stringResource(mode.loadingTitleRes),
            posterUri = preview.posterUri,
            backdropUri = preview.backdropUri,
            modifier = modifier.fillMaxSize()
        )
    } else {
        Loading(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

@Composable
internal fun DetailsScreenErrorStateContent(
    shouldShowUnavailableState: Boolean,
    unavailableDetails: UnavailableDetailsUiModel,
    canRemoveFromLibrary: Boolean,
    onRemoveUnavailable: () -> Unit,
    onManualSearchRequested: (String) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (shouldShowUnavailableState) {
        UnavailableDetailsScreen(
            state = unavailableDetails,
            showRemoveFromLibraryAction = canRemoveFromLibrary,
            onRemoveFromLibrary = onRemoveUnavailable,
            onManualSearch = onManualSearchRequested,
            onBackPressed = onBackPressed,
            modifier = modifier.fillMaxSize()
        )
    } else {
        Error(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

@Composable
internal fun DetailsScreenDoneStateContent(
    mode: DetailsScreenMode,
    state: DetailsScreenUiState.Done,
    actionsCompat: MovieDetailsEpisodeActionsCompat?,
    goToPlayer: (String?) -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    onFavoriteClick: () -> Unit,
    onBookmarkClick: (WatchType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseColorScheme = MaterialTheme.colorScheme
    val visualsState = rememberDetailsVisualsState(
        details = state.details,
        baseColorScheme = baseColorScheme,
    )

    MaterialTheme(
        colorScheme = visualsState.colorScheme,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
    ) {
        if (actionsCompat != null) {
            DetailsScreenContent(
                mode = mode,
                details = state.details,
                actionsCompat = actionsCompat,
                isSecondaryContentLoading = state.isSecondaryContentLoading,
                goToPlayer = goToPlayer,
                onBackPressed = onBackPressed,
                refreshScreenWithNewItem = refreshScreenWithNewItem,
                onFavoriteClick = onFavoriteClick,
                onBookmarkClick = onBookmarkClick,
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        } else {
            Loading(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    }
}
