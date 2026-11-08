package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import org.junit.Assert.assertEquals
import org.junit.Test

class TvNavModelTest {
    @Test
    fun `sub screens mark their parent entry`() {
        assertEquals(TvNavSelection.Screen(TvNavTarget.Home), navSelectionForRoute(Screens.HomeFeedGrid.name, null))
        assertEquals(TvNavSelection.Screen(TvNavTarget.Search), navSelectionForRoute(Screens.SearchFeedGrid.name, null))
    }

    @Test
    fun `an open library list marks that list`() {
        assertEquals(
            TvNavSelection.Library("TMDB/list_0"),
            navSelectionForRoute(Screens.LibraryFeedGrid.name, "TMDB/list_0"),
        )
    }

    @Test
    fun `the profile screen marks nothing`() {
        assertEquals(TvNavSelection.None, navSelectionForRoute(Screens.Profile.name, null))
    }

    @Test
    fun `no route yet means home`() {
        assertEquals(TvNavSelection.Screen(TvNavTarget.Home), navSelectionForRoute(null, null))
    }
}
