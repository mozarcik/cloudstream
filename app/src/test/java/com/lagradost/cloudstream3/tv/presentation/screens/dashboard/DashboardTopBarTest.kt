package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardTopBarTest {
    @Test
    fun `skips dashboard focus restore during programmatic search navigation`() {
        val shouldRestore = shouldRestoreDashboardFocusAfterReturn(
            isComingBackFromDifferentScreen = true,
            isProgrammaticSearchNavigationPending = true,
        )

        assertEquals(false, shouldRestore)
    }

    @Test
    fun `restores dashboard focus when returning normally`() {
        val shouldRestore = shouldRestoreDashboardFocusAfterReturn(
            isComingBackFromDifferentScreen = true,
            isProgrammaticSearchNavigationPending = false,
        )

        assertEquals(true, shouldRestore)
    }

    @Test
    fun `clears stale pending focus when selected screen changes programmatically`() {
        val shouldClear = shouldClearPendingFocusedScreen(
            previousSelectedScreen = Screens.Library,
            selectedScreen = Screens.Search,
            pendingFocusedScreen = Screens.Library,
        )

        assertEquals(true, shouldClear)
    }

    @Test
    fun `does not clear pending focus when selected screen did not change`() {
        val shouldClear = shouldClearPendingFocusedScreen(
            previousSelectedScreen = Screens.Library,
            selectedScreen = Screens.Library,
            pendingFocusedScreen = Screens.Library,
        )

        assertEquals(false, shouldClear)
    }

    @Test
    fun `does not navigate from stale pending focus when tab row is not focused`() {
        val target = resolveDelayedTopBarNavigationTarget(
            pendingFocusedScreen = Screens.Library,
            selectedScreen = Screens.Search,
            isTabRowFocused = false,
            suppressDelayedNavigation = false,
        )

        assertNull(target)
    }

    @Test
    fun `does not navigate when pending screen already matches selected screen`() {
        val target = resolveDelayedTopBarNavigationTarget(
            pendingFocusedScreen = Screens.Search,
            selectedScreen = Screens.Search,
            isTabRowFocused = true,
            suppressDelayedNavigation = false,
        )

        assertNull(target)
    }

    @Test
    fun `does not navigate while delayed navigation is suppressed`() {
        val target = resolveDelayedTopBarNavigationTarget(
            pendingFocusedScreen = Screens.Library,
            selectedScreen = Screens.Search,
            isTabRowFocused = true,
            suppressDelayedNavigation = true,
        )

        assertNull(target)
    }

    @Test
    fun `navigates only when tab row is focused and target differs`() {
        val target = resolveDelayedTopBarNavigationTarget(
            pendingFocusedScreen = Screens.Search,
            selectedScreen = Screens.Library,
            isTabRowFocused = true,
            suppressDelayedNavigation = false,
        )

        assertEquals(Screens.Search, target)
    }
}
