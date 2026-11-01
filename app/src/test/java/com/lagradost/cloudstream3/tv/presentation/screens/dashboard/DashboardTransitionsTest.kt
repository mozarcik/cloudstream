package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardTransitionsTest {
    private fun direction(from: Screens, to: Screens) = tabDirection(from.name, to.name)

    @Test
    fun `moving to a tab on the right enters from the right`() {
        assertEquals(1, direction(Screens.Home, Screens.Library))
        assertEquals(1, direction(Screens.Library, Screens.Settings))
    }

    @Test
    fun `moving to a tab on the left enters from the left`() {
        assertEquals(-1, direction(Screens.Library, Screens.Home))
        assertEquals(-1, direction(Screens.Settings, Screens.Home))
    }

    @Test
    fun `search is the last tab in the top bar`() {
        assertEquals(1, direction(Screens.Settings, Screens.Search))
        assertEquals(-1, direction(Screens.Search, Screens.Downloads))
    }

    @Test
    fun `feed grids belong to their tab and only fade`() {
        assertEquals(0, direction(Screens.Home, Screens.HomeFeedGrid))
        assertEquals(0, direction(Screens.LibraryFeedGrid, Screens.Library))
        assertEquals(0, direction(Screens.Search, Screens.SearchFeedGrid))
    }

    @Test
    fun `a feed grid still moves sideways relative to another tab`() {
        assertEquals(1, direction(Screens.HomeFeedGrid, Screens.Library))
        assertEquals(-1, direction(Screens.Library, Screens.HomeFeedGrid))
    }

    @Test
    fun `routes outside the tabs do not slide`() {
        assertEquals(0, tabDirection(Screens.Home.name, Screens.Profile.name))
        assertEquals(0, tabDirection(null, Screens.Home.name))
        assertEquals(0, direction(Screens.Home, Screens.Home))
    }
}
