package com.lagradost.cloudstream3.tv.presentation.screens.details

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailsSearchNavigationTest {
    @Test
    fun `returns trimmed title for details search`() {
        val query = resolveDetailsSearchQuery("  Fight Club  ")

        assertEquals("Fight Club", query)
    }

    @Test
    fun `returns null for blank details title`() {
        val query = resolveDetailsSearchQuery("   ")

        assertNull(query)
    }
}
