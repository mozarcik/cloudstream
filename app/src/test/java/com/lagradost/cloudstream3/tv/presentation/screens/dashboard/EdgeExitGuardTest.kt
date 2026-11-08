package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeExitGuardTest {
    private val gap = EDGE_EXIT_DEBOUNCE_MS

    @Test
    fun `first press is allowed`() {
        val guard = EdgeExitGuard()
        guard.onHorizontalKey(eventTimeMs = 1_000, repeatCount = 0)
        assertFalse(guard.blocked)
    }

    @Test
    fun `a held key is always blocked`() {
        val guard = EdgeExitGuard()
        guard.onHorizontalKey(0, 0)
        guard.onHorizontalKey(10_000, 1)
        assertTrue(guard.blocked)
    }

    @Test
    fun `a press right after another horizontal press is blocked`() {
        val guard = EdgeExitGuard()
        guard.onHorizontalKey(1_000, 0)
        guard.onHorizontalKey(1_000 + gap - 1, 0)
        assertTrue(guard.blocked)
    }

    @Test
    fun `a press after the pause is allowed`() {
        val guard = EdgeExitGuard()
        guard.onHorizontalKey(1_000, 0)
        guard.onHorizontalKey(1_000 + gap, 0)
        assertFalse(guard.blocked)
    }

    @Test
    fun `a blocked press still extends the burst`() {
        val guard = EdgeExitGuard()
        guard.onHorizontalKey(1_000, 0)
        guard.onHorizontalKey(1_000 + gap - 10, 0) // blocked
        guard.onHorizontalKey(1_000 + 2 * gap - 20, 0) // < gap after the blocked one
        assertTrue(guard.blocked)
    }

    @Test
    fun `right counts as horizontal movement`() {
        // The guard is fed Left and Right alike; the caller never feeds Up / Down.
        val guard = EdgeExitGuard()
        guard.onHorizontalKey(1_000, 0) // Right
        guard.onHorizontalKey(1_100, 0) // Left
        assertTrue(guard.blocked)
    }

    @Test
    fun `bounce plays once per blocked series`() {
        val guard = EdgeExitGuard()
        guard.onHorizontalKey(1_000, 0)
        assertFalse(guard.consumeBounce())
        guard.onHorizontalKey(1_050, 0)
        assertTrue(guard.consumeBounce())
        guard.onHorizontalKey(1_100, 1)
        assertFalse(guard.consumeBounce())
        guard.onHorizontalKey(5_000, 0) // series over
        guard.onHorizontalKey(5_050, 0)
        assertTrue(guard.consumeBounce())
    }
}
