package com.lagradost.cloudstream3.tv.presentation.screens.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerScreenNavigationTest {
    @Test
    fun `parseDownloadedEpisodeId parses positive id`() {
        val encoded = PlayerScreenNavigation.buildDownloadedEpisodeData(1234)

        assertEquals(1234, PlayerScreenNavigation.parseDownloadedEpisodeId(encoded))
    }

    @Test
    fun `parseDownloadedEpisodeId parses negative id`() {
        val encoded = PlayerScreenNavigation.buildDownloadedEpisodeData(-1234)

        assertEquals(-1234, PlayerScreenNavigation.parseDownloadedEpisodeId(encoded))
    }

    @Test
    fun `parseDownloadedEpisodeId returns null for non download payload`() {
        assertNull(PlayerScreenNavigation.parseDownloadedEpisodeId("episode-data"))
    }
}
