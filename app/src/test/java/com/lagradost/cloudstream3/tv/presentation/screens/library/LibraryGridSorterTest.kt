package com.lagradost.cloudstream3.tv.presentation.screens.library

import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.ui.library.ListSorting
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryGridSorterTest {
    @Test
    fun `sortLibraryGridItems keeps original order for default sort`() {
        val items = listOf(
            testGridItem(name = "Gamma", originalIndex = 2),
            testGridItem(name = "Alpha", originalIndex = 0),
            testGridItem(name = "Beta", originalIndex = 1),
        )

        val sortedItems = sortLibraryGridItems(items, method = null)

        assertEquals(listOf("Alpha", "Beta", "Gamma"), sortedItems.map { item -> item.name })
    }

    @Test
    fun `sortLibraryGridItems sorts by rating descending`() {
        val items = listOf(
            testGridItem(name = "Alpha", originalIndex = 0, personalRatingHundred = 72),
            testGridItem(name = "Beta", originalIndex = 1, personalRatingHundred = 91),
            testGridItem(name = "Gamma", originalIndex = 2, personalRatingHundred = null),
        )

        val sortedItems = sortLibraryGridItems(items, method = ListSorting.RatingHigh)

        assertEquals(listOf("Beta", "Alpha", "Gamma"), sortedItems.map { item -> item.name })
    }

    @Test
    fun `sortLibraryGridItems keeps null updated timestamps first for updated new`() {
        val items = listOf(
            testGridItem(name = "Alpha", originalIndex = 0, lastUpdatedUnixTime = 200L),
            testGridItem(name = "Beta", originalIndex = 1, lastUpdatedUnixTime = null),
            testGridItem(name = "Gamma", originalIndex = 2, lastUpdatedUnixTime = 100L),
        )

        val sortedItems = sortLibraryGridItems(items, method = ListSorting.UpdatedNew)

        assertEquals(listOf("Beta", "Alpha", "Gamma"), sortedItems.map { item -> item.name })
    }

    @Test
    fun `resolveSupportedLibrarySortingMethods removes query and uses enum order`() {
        val supportedMethods = linkedSetOf(
            ListSorting.ReleaseDateOld,
            ListSorting.Query,
            ListSorting.AlphabeticalZ,
            ListSorting.UpdatedOld,
        )

        val resolvedMethods = resolveSupportedLibrarySortingMethods(supportedMethods)

        assertEquals(
            listOf(
                ListSorting.UpdatedOld,
                ListSorting.AlphabeticalZ,
                ListSorting.ReleaseDateOld,
            ),
            resolvedMethods
        )
    }

    private fun testGridItem(
        name: String,
        originalIndex: Int,
        personalRatingHundred: Int? = null,
        lastUpdatedUnixTime: Long? = null,
        releaseDateUnixTimeMs: Long? = null,
    ): LibraryGridItemUiState {
        return LibraryGridItemUiState(
            mediaItem = MediaItemCompat.Movie(
                id = "id_$name",
                url = "https://example.com/$name",
                apiName = "Test API",
                name = name,
                posterUri = "https://example.com/$name.jpg",
                type = TvType.Movie,
                score = null,
            ),
            originalIndex = originalIndex,
            name = name,
            personalRatingHundred = personalRatingHundred,
            lastUpdatedUnixTime = lastUpdatedUnixTime,
            releaseDateUnixTimeMs = releaseDateUnixTimeMs,
        )
    }
}
