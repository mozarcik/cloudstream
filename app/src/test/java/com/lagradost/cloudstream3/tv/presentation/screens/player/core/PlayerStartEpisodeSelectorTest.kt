package com.lagradost.cloudstream3.tv.presentation.screens.player.core

import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerStartTarget
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.VideoWatchState
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerStartEpisodeSelectorTest {
    @Test
    fun `resume target prefers exact episode id over fallback data`() {
        val episodes = listOf(
            createEpisode(id = 101, data = "s8e1"),
            createEpisode(id = 202, data = "s1e3"),
        )

        val selectedIndex = PlayerStartEpisodeSelector.resolveSelectedEpisodeIndex(
            startTarget = PlayerStartTarget.ResumeEpisode(202),
            episodes = episodes,
            fallbackEpisodeData = "s8e1",
        )

        assertEquals(1, selectedIndex)
    }

    @Test
    fun `direct episode data falls back to matching payload`() {
        val episodes = listOf(
            createEpisode(id = 101, data = "s8e1"),
            createEpisode(id = 202, data = "s1e3"),
        )

        val selectedIndex = PlayerStartEpisodeSelector.resolveSelectedEpisodeIndex(
            startTarget = PlayerStartTarget.DirectEpisodeData("missing-data"),
            episodes = episodes,
            fallbackEpisodeData = "s1e3",
        )

        assertEquals(1, selectedIndex)
    }

    private fun createEpisode(
        id: Int,
        data: String,
    ): ResultEpisode {
        return ResultEpisode(
            headerName = "Series",
            name = data,
            poster = null,
            episode = 1,
            seasonIndex = 1,
            season = 1,
            data = data,
            apiName = "Test API",
            id = id,
            index = 0,
            position = 0,
            duration = 0,
            score = null,
            description = null,
            isFiller = null,
            tvType = TvType.TvSeries,
            parentId = 99,
            videoWatchState = VideoWatchState.None,
        )
    }
}
