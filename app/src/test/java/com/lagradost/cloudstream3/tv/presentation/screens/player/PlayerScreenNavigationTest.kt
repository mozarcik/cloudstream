package com.lagradost.cloudstream3.tv.presentation.screens.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerScreenNavigationTest {
    @Test
    fun `navigation arg round-trips downloaded episode target`() {
        val encoded = PlayerScreenNavigation.toNavigationArg(
            PlayerStartTarget.DownloadedEpisode(1234)
        )

        assertEquals(
            PlayerStartTarget.DownloadedEpisode(1234),
            PlayerScreenNavigation.fromNavigationArg(encoded),
        )
    }

    @Test
    fun `navigation arg round-trips resume episode target`() {
        val encoded = PlayerScreenNavigation.toNavigationArg(
            PlayerStartTarget.ResumeEpisode(5678)
        )

        assertEquals(
            PlayerStartTarget.ResumeEpisode(5678),
            PlayerScreenNavigation.fromNavigationArg(encoded),
        )
    }

    @Test
    fun `navigation arg keeps direct episode data untouched`() {
        val payload = "episode-data"

        assertEquals(
            PlayerStartTarget.DirectEpisodeData(payload),
            PlayerScreenNavigation.fromNavigationArg(payload),
        )
    }

    @Test
    fun `blank navigation arg resolves to default target`() {
        assertEquals(
            PlayerStartTarget.Default,
            PlayerScreenNavigation.fromNavigationArg(""),
        )
    }
}
