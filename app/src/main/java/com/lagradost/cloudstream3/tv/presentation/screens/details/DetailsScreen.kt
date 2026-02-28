package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.presentation.common.Error
import com.lagradost.cloudstream3.tv.presentation.common.Loading
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsLoadingPlaceholder
import com.lagradost.cloudstream3.tv.presentation.screens.unavailable.UnavailableDetailsScreen

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

    when (val state = uiState) {
        is DetailsScreenUiState.Loading -> {
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

        DetailsScreenUiState.Error -> {
            if (detailsScreenViewModel.shouldShowUnavailableState) {
                val unavailableDetails = detailsScreenViewModel.unavailableDetails
                UnavailableDetailsScreen(
                    state = unavailableDetails,
                    showRemoveFromLibraryAction = detailsScreenViewModel.canRemoveFromLibrary,
                    onRemoveFromLibrary = {
                        detailsScreenViewModel.removeUnavailableItemFromLibrary()
                        onBackPressed()
                    },
                    onManualSearch = { query ->
                        onManualSearchRequested(query)
                    },
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

        is DetailsScreenUiState.Done -> {
            val baseColorScheme = MaterialTheme.colorScheme
            val artworkKey = remember(state.details.id, state.details.posterUri) {
                "details:${state.details.id}:${state.details.posterUri}"
            }
            val dynamicColorScheme = rememberDetailsDynamicColorScheme(
                artworkKey = artworkKey,
                artworkUrl = state.details.posterUri.takeIf { it.isNotBlank() },
                baseColorScheme = baseColorScheme,
            )

            MaterialTheme(
                colorScheme = dynamicColorScheme,
                shapes = MaterialTheme.shapes,
                typography = MaterialTheme.typography,
            ) {
                DetailsScreenContent(
                    mode = mode,
                    details = state.details,
                    sourceUrl = state.sourceUrl,
                    apiName = state.apiName,
                    goToPlayer = goToPlayer,
                    onBackPressed = onBackPressed,
                    refreshScreenWithNewItem = refreshScreenWithNewItem,
                    onFavoriteClick = detailsScreenViewModel::onFavoriteClick,
                    onBookmarkClick = detailsScreenViewModel::onBookmarkClick,
                    modifier = modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}
