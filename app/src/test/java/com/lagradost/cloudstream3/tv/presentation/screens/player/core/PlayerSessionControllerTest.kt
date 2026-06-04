package com.lagradost.cloudstream3.tv.presentation.screens.player.core

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerSessionControllerTest {
    @Test
    fun `resolve subtitle refresh seek target prefers requested direction when within bounds`() {
        val targetPositionMs = resolveSubtitleRefreshSeekTarget(
            currentPositionMs = 10_000L,
            durationMs = 20_000L,
            preferredDeltaMs = 1L,
        )

        assertEquals(10_001L, targetPositionMs)
    }

    @Test
    fun `resolve subtitle refresh seek target flips direction at stream start`() {
        val targetPositionMs = resolveSubtitleRefreshSeekTarget(
            currentPositionMs = 0L,
            durationMs = 20_000L,
            preferredDeltaMs = -1L,
        )

        assertEquals(1L, targetPositionMs)
    }

    @Test
    fun `resolve subtitle refresh seek target flips direction at stream end`() {
        val targetPositionMs = resolveSubtitleRefreshSeekTarget(
            currentPositionMs = 20_000L,
            durationMs = 20_000L,
            preferredDeltaMs = 1L,
        )

        assertEquals(19_999L, targetPositionMs)
    }

    @Test
    fun `resolve subtitle refresh seek target supports unknown duration`() {
        val targetPositionMs = resolveSubtitleRefreshSeekTarget(
            currentPositionMs = 0L,
            durationMs = C.TIME_UNSET,
            preferredDeltaMs = -1L,
        )

        assertEquals(1L, targetPositionMs)
    }

    @Test
    fun `resolve subtitle refresh seek target returns null when both directions collapse`() {
        val targetPositionMs = resolveSubtitleRefreshSeekTarget(
            currentPositionMs = 0L,
            durationMs = 0L,
            preferredDeltaMs = 1L,
        )

        assertNull(targetPositionMs)
    }
}
