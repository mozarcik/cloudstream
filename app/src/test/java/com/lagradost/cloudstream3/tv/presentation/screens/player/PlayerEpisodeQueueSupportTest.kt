package com.lagradost.cloudstream3.tv.presentation.screens.player

import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.VideoWatchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerEpisodeQueueSupportTest {
    @Test
    fun `resolveNextEpisodeIndex returns next index when available`() {
        assertEquals(2, resolveNextEpisodeIndex(currentEpisodeIndex = 1, totalEpisodes = 4))
    }

    @Test
    fun `resolveNextEpisodeIndex returns null for last episode`() {
        assertNull(resolveNextEpisodeIndex(currentEpisodeIndex = 3, totalEpisodes = 4))
    }

    @Test
    fun `resolveEpisodeMetadata maps season episode and title from queue entry`() {
        val metadata = resolveEpisodeMetadata(
            baseMetadata = TvPlayerMetadata(
                title = "Show",
                subtitle = "2024 . API",
                backdropUri = "backdrop",
                year = 2024,
                apiName = "API",
                isEpisodeBased = true,
            ),
            episode = fakeEpisode(
                id = 7,
                season = 2,
                episode = 3,
                name = "Pilot",
            ),
        )

        assertEquals(2, metadata.season)
        assertEquals(3, metadata.episode)
        assertEquals("Pilot", metadata.episodeTitle)
        assertEquals("2024 . API", metadata.subtitle)
    }

    @Test
    fun `resolveEpisodeMetadata hides duplicate episode title`() {
        val metadata = resolveEpisodeMetadata(
            baseMetadata = TvPlayerMetadata(
                title = "Show",
                subtitle = "2024 . API",
                backdropUri = "backdrop",
                isEpisodeBased = true,
            ),
            episode = fakeEpisode(
                id = 8,
                season = 1,
                episode = 1,
                name = "Show",
            ),
        )

        assertNull(metadata.episodeTitle)
    }

    @Test
    fun `shouldPrefetchNextEpisode starts at eighty five percent`() {
        assertFalse(shouldPrefetchNextEpisode(positionMs = 84L, durationMs = 100L))
        assertTrue(shouldPrefetchNextEpisode(positionMs = 85L, durationMs = 100L))
    }
}

private fun fakeEpisode(
    id: Int,
    season: Int,
    episode: Int,
    name: String,
): ResultEpisode {
    return ResultEpisode(
        headerName = "Show",
        name = name,
        poster = null,
        episode = episode,
        seasonIndex = season,
        season = season,
        data = "data-$id",
        apiName = "api",
        id = id,
        index = episode - 1,
        position = 0L,
        duration = 0L,
        score = null,
        description = null,
        isFiller = null,
        tvType = TvType.TvSeries,
        parentId = 1,
        videoWatchState = VideoWatchState.None,
    )
}
