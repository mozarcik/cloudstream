package com.lagradost.cloudstream3.tv.presentation.screens.library

import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.ui.library.ListSorting
import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySortOptionsTest {
    @Test
    fun `buildLibrarySortPanelOptions prepends default option and marks selected method`() {
        val options = buildLibrarySortPanelOptions(
            supportedSortingMethods = listOf(
                ListSorting.RatingHigh,
                ListSorting.ReleaseDateNew,
            ),
            selectedSortMethod = ListSorting.ReleaseDateNew,
        )

        assertEquals(
            listOf(
                "library_sort_default",
                "library_sort_RatingHigh",
                "library_sort_ReleaseDateNew",
            ),
            options.map { option -> option.id }
        )
        assertEquals(
            listOf(false, false, true),
            options.map { option -> option.selected }
        )
    }

    @Test
    fun `resolveLibraryGridRatingHundred falls back to public score when personal rating is missing`() {
        val item = testLibraryItem(
            personalRating = null,
            publicScore = Score.from10(8.4),
        )

        assertEquals(84, resolveLibraryGridRatingHundred(item))
    }

    @Test
    fun `resolveLibraryGridRatingHundred prefers personal rating over public score`() {
        val item = testLibraryItem(
            personalRating = Score.from10(9.1),
            publicScore = Score.from10(7.2),
        )

        assertEquals(91, resolveLibraryGridRatingHundred(item))
    }

    private fun testLibraryItem(
        personalRating: Score?,
        publicScore: Score?,
    ): SyncAPI.LibraryItem {
        return SyncAPI.LibraryItem(
            name = "Example",
            url = "https://example.com/example",
            syncId = "example",
            episodesCompleted = null,
            episodesTotal = null,
            personalRating = personalRating,
            lastUpdatedUnixTime = null,
            apiName = "TMDB",
            type = TvType.Movie,
            posterUrl = null,
            posterHeaders = null,
            quality = null,
            releaseDate = null,
            id = 1,
            plot = null,
            score = publicScore,
        )
    }
}
