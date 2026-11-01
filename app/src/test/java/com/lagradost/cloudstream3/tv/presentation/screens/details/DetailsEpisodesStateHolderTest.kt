package com.lagradost.cloudstream3.tv.presentation.screens.details

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailsEpisodesStateHolderTest {
    @Test
    fun `resolveDetailsEpisodeHydrationRange returns initial window when nothing is visible`() {
        val range = resolveDetailsEpisodeHydrationRange(
            totalCount = 20,
            visibleIndexes = emptyList(),
        )

        assertEquals(0..7, range)
    }

    @Test
    fun `resolveDetailsEpisodeHydrationRange expands around visible items with buffers`() {
        val range = resolveDetailsEpisodeHydrationRange(
            totalCount = 20,
            visibleIndexes = listOf(5, 6),
        )

        assertEquals(3..10, range)
    }

    @Test
    fun `resolveDetailsEpisodeHydrationRange clamps near list end`() {
        val range = resolveDetailsEpisodeHydrationRange(
            totalCount = 20,
            visibleIndexes = listOf(18, 19),
        )

        assertEquals(16..19, range)
    }

    @Test
    fun `resolveDetailsEpisodeHydrationRange returns null for empty list`() {
        val range = resolveDetailsEpisodeHydrationRange(
            totalCount = 0,
            visibleIndexes = listOf(0),
        )

        assertNull(range)
    }
}
