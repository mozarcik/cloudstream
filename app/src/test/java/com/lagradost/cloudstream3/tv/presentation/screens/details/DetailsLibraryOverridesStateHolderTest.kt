package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DetailsLoadingPreview
import com.lagradost.cloudstream3.ui.WatchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailsLibraryOverridesStateHolderTest {
    @Test
    fun `applyDetailsLibraryOverrides updates done state details`() {
        val stateHolder = DetailsLibraryOverridesStateHolder()
        stateHolder.updateFavorite(detailsId = "details-1", isFavorite = true)
        stateHolder.updateBookmark(detailsId = "details-1", status = WatchType.WATCHING)

        val updatedState = applyDetailsLibraryOverrides(
            state = DetailsScreenUiState.Done(
                details = MovieDetails(
                    id = "details-1",
                    name = "Details",
                    description = "Description",
                    posterUri = "poster",
                ),
                isSecondaryContentLoading = true,
            ),
            favoriteOverrides = stateHolder.favoriteOverrides.value,
            bookmarkOverrides = stateHolder.bookmarkOverrides.value,
        ) as DetailsScreenUiState.Done

        assertTrue(updatedState.details.isFavorite)
        assertTrue(updatedState.details.isBookmarked)
        assertEquals(WatchType.WATCHING.stringRes, updatedState.details.bookmarkLabelRes)
    }

    @Test
    fun `clear removes stored overrides`() {
        val stateHolder = DetailsLibraryOverridesStateHolder()
        stateHolder.updateFavorite(detailsId = "details-1", isFavorite = true)
        stateHolder.updateBookmark(detailsId = "details-1", status = WatchType.COMPLETED)

        stateHolder.clear()

        assertTrue(stateHolder.favoriteOverrides.value.isEmpty())
        assertTrue(stateHolder.bookmarkOverrides.value.isEmpty())

        val unchangedState = applyDetailsLibraryOverrides(
            state = DetailsScreenUiState.Done(
                details = MovieDetails(
                    id = "details-1",
                    name = "Details",
                    description = "Description",
                    posterUri = "poster",
                ),
                isSecondaryContentLoading = false,
            ),
            favoriteOverrides = stateHolder.favoriteOverrides.value,
            bookmarkOverrides = stateHolder.bookmarkOverrides.value,
        ) as DetailsScreenUiState.Done

        assertFalse(unchangedState.details.isFavorite)
        assertFalse(unchangedState.details.isBookmarked)
    }

    @Test
    fun `applyDetailsLibraryOverrides leaves loading state unchanged`() {
        val updatedState = applyDetailsLibraryOverrides(
            state = DetailsScreenUiState.Loading(preview = DetailsLoadingPreview(title = "Loading")),
            favoriteOverrides = mapOf("details-1" to true),
            bookmarkOverrides = mapOf("details-1" to WatchType.WATCHING),
        )

        assertTrue(updatedState is DetailsScreenUiState.Loading)
    }
}
