package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardBackNavigationTest {
    private fun resolve(
        isMenuFocused: Boolean = false,
        isOnHome: Boolean = true,
        isOnTopLevelScreen: Boolean = true,
        hasPrevious: Boolean = true,
        contentHandledBack: Boolean = false,
    ) = resolveDashboardBackAction(
        isMenuFocused = isMenuFocused,
        isOnHome = isOnHome,
        isOnTopLevelScreen = isOnTopLevelScreen,
        hasPreviousBackStackEntry = hasPrevious,
        contentHandledBack = contentHandledBack,
    )

    @Test
    fun `back in the menu on home exits right away`() {
        assertEquals(DashboardBackAction.ExitApp, resolve(isMenuFocused = true))
    }

    @Test
    fun `back in the menu on another section switches to home`() {
        assertEquals(DashboardBackAction.MenuGoHome, resolve(isMenuFocused = true, isOnHome = false))
    }

    @Test
    fun `the menu wins over everything underneath it`() {
        assertEquals(
            DashboardBackAction.ExitApp,
            resolve(isMenuFocused = true, isOnTopLevelScreen = false, contentHandledBack = true),
        )
    }

    @Test
    fun `back from a sub screen pops it`() {
        assertEquals(DashboardBackAction.PopBackStack, resolve(isOnHome = false, isOnTopLevelScreen = false))
    }

    @Test
    fun `scrolled content consumes back and stays in content`() {
        assertEquals(DashboardBackAction.ContentHandled, resolve(contentHandledBack = true))
    }

    @Test
    fun `content at the top of any section opens the menu`() {
        assertEquals(DashboardBackAction.OpenMenu, resolve())
        assertEquals(DashboardBackAction.OpenMenu, resolve(isOnHome = false))
    }

    @Test
    fun `a sub screen without a previous entry falls back to the menu`() {
        assertEquals(
            DashboardBackAction.OpenMenu,
            resolve(isOnHome = false, isOnTopLevelScreen = false, hasPrevious = false),
        )
    }

    @Test
    fun `focus is restored after returning from another screen`() {
        assertTrue(shouldRestoreDashboardFocusAfterReturn(true, false))
        assertFalse(shouldRestoreDashboardFocusAfterReturn(true, true))
        assertFalse(shouldRestoreDashboardFocusAfterReturn(false, false))
    }
}
