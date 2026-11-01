package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardBackNavigationTest {
    private val home = 0
    private val library = 2

    private fun resolve(
        isOnTopBarTab: Boolean = true,
        hasPrevious: Boolean = true,
        isTopBarVisible: Boolean = true,
        selectedTab: Int = library,
        isTopBarFocused: Boolean = false,
    ) = resolveDashboardBackAction(
        isOnTopBarTab = isOnTopBarTab,
        hasPreviousBackStackEntry = hasPrevious,
        isTopBarVisible = isTopBarVisible,
        currentTopBarSelectedTabIndex = selectedTab,
        homeTabIndex = home,
        isTopBarFocused = isTopBarFocused,
    )

    @Test
    fun `back from a scrolled tab with hidden top bar shows it and focuses the selected tab`() {
        assertEquals(DashboardBackAction.ShowTopBarAndFocusSelectedTab, resolve(isTopBarVisible = false))
    }

    @Test
    fun `back from tab content with visible top bar focuses the selected tab`() {
        assertEquals(DashboardBackAction.FocusSelectedTab, resolve())
    }

    @Test
    fun `back from a grid screen pops the back stack`() {
        assertEquals(DashboardBackAction.PopBackStack, resolve(isOnTopBarTab = false, isTopBarVisible = false))
    }

    @Test
    fun `back on a non home tab with focused top bar goes to home`() {
        assertEquals(DashboardBackAction.FocusHomeTab, resolve(isTopBarFocused = true))
    }

    @Test
    fun `back on home with focused top bar exits the app`() {
        assertEquals(DashboardBackAction.ExitApp, resolve(selectedTab = home, isTopBarFocused = true))
    }

    @Test
    fun `back to the top bar resets the content and its remembered focus`() {
        val entry = DashboardContentEntry()
        var resets = 0
        entry.resetToTop = { resets++ }

        entry.resetToTop?.invoke()

        assertEquals(1, resets)
    }

    @Test
    fun `content entry has no reset until a screen registers one`() {
        assertNull(DashboardContentEntry().resetToTop)
    }

    @Test
    fun `reset actions are the ones that return focus to the top bar`() {
        val resetting = setOf(
            DashboardBackAction.ShowTopBarAndFocusSelectedTab,
            DashboardBackAction.FocusSelectedTab,
        )
        assertTrue(resolve(isTopBarVisible = false) in resetting)
        assertTrue(resolve() in resetting)
        assertTrue(resolve(isOnTopBarTab = false) !in resetting)
    }
}
