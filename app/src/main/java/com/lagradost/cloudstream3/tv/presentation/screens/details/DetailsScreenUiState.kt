package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DetailsLoadingPreview
import com.lagradost.cloudstream3.ui.WatchType

@Immutable
sealed interface DetailsScreenUiState {
    @Immutable
    data class Loading(
        val preview: DetailsLoadingPreview = DetailsLoadingPreview()
    ) : DetailsScreenUiState

    data object Error : DetailsScreenUiState

    @Immutable
    data class Done(
        val details: MovieDetails,
        val isSecondaryContentLoading: Boolean,
    ) : DetailsScreenUiState
}

internal fun applyDetailsLibraryOverrides(
    state: DetailsScreenUiState,
    favoriteOverrides: Map<String, Boolean>,
    bookmarkOverrides: Map<String, WatchType>,
): DetailsScreenUiState {
    val doneState = state as? DetailsScreenUiState.Done ?: return state
    val detailsId = doneState.details.id
    val overrideFavorite = favoriteOverrides[detailsId]
    val overrideBookmark = bookmarkOverrides[detailsId]

    if (overrideFavorite == null && overrideBookmark == null) {
        return state
    }

    return doneState.copy(
        details = doneState.details.withLibraryOverride(
            favorite = overrideFavorite,
            bookmark = overrideBookmark
        )
    )
}

private fun MovieDetails.withLibraryOverride(
    favorite: Boolean?,
    bookmark: WatchType?,
): MovieDetails {
    var updatedDetails = this

    if (favorite != null) {
        updatedDetails = updatedDetails.copy(isFavorite = favorite)
    }

    if (bookmark != null) {
        updatedDetails = updatedDetails.copy(
            isBookmarked = bookmark != WatchType.NONE,
            bookmarkLabelRes = bookmark.stringRes.takeIf { bookmark != WatchType.NONE }
        )
    }

    return updatedDetails
}
