package com.lagradost.cloudstream3.tv.compat

import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TrailerData
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.VideoWatchState
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieDetailsWatchStateSupportTest {
    @Test
    fun `resolveEpisodesUpToTarget includes previous seasons and current season up to clicked episode`() {
        val target = fakeTarget(
            selectedEpisode = fakeResultEpisode(id = 204, season = 2, episode = 4),
            episodesBySeason = mapOf(
                1 to listOf(
                    fakeEpisode(id = 101, season = 1, episode = 1),
                    fakeEpisode(id = 102, season = 1, episode = 2),
                ),
                2 to listOf(
                    fakeEpisode(id = 201, season = 2, episode = 1),
                    fakeEpisode(id = 202, season = 2, episode = 2),
                    fakeEpisode(id = 203, season = 2, episode = 3),
                    fakeEpisode(id = 204, season = 2, episode = 4),
                    fakeEpisode(id = 205, season = 2, episode = 5),
                ),
                3 to listOf(
                    fakeEpisode(id = 301, season = 3, episode = 1),
                ),
            ),
        )

        val resolvedIds = resolveEpisodesUpToTarget(target).map { it.id }

        assertEquals(listOf(101, 102, 201, 202, 203, 204), resolvedIds)
    }

    @Test
    fun `resolveEpisodesUpToTarget skips season zero when target is regular season`() {
        val target = fakeTarget(
            selectedEpisode = fakeResultEpisode(id = 202, season = 2, episode = 2),
            episodesBySeason = mapOf(
                0 to listOf(
                    fakeEpisode(id = 1, season = 0, episode = 1),
                    fakeEpisode(id = 2, season = 0, episode = 2),
                ),
                1 to listOf(
                    fakeEpisode(id = 101, season = 1, episode = 1),
                ),
                2 to listOf(
                    fakeEpisode(id = 201, season = 2, episode = 1),
                    fakeEpisode(id = 202, season = 2, episode = 2),
                    fakeEpisode(id = 203, season = 2, episode = 3),
                ),
            ),
        )

        val resolvedIds = resolveEpisodesUpToTarget(target).map { it.id }

        assertEquals(listOf(101, 201, 202), resolvedIds)
    }
}

private fun fakeTarget(
    selectedEpisode: ResultEpisode,
    episodesBySeason: Map<Int, List<MovieDetailsActionTargetEpisode>>,
): MovieDetailsActionTarget {
    return MovieDetailsActionTarget(
        loadResponse = FakeWatchStateLoadResponse(),
        episode = selectedEpisode,
        episodesBySeason = episodesBySeason,
        allEpisodes = episodesBySeason.values.flatten(),
    )
}

private fun fakeEpisode(
    id: Int,
    season: Int,
    episode: Int,
): MovieDetailsActionTargetEpisode {
    return MovieDetailsActionTargetEpisode(
        data = "data-$id",
        season = season,
        episode = episode,
        index = episode - 1,
        id = id,
        name = "Episode $episode",
    )
}

private fun fakeResultEpisode(
    id: Int,
    season: Int,
    episode: Int,
): ResultEpisode {
    return ResultEpisode(
        headerName = "Details",
        name = "Episode $episode",
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

private class FakeWatchStateLoadResponse : LoadResponse {
    override var name: String = "Fake title"
    override var url: String = "fake-url"
    override var apiName: String = "fake-api"
    override var type: TvType = TvType.TvSeries
    override var posterUrl: String? = "poster"
    override var year: Int? = 2024
    override var plot: String? = "plot"
    override var score: Score? = null
    override var tags: List<String>? = emptyList()
    override var duration: Int? = null
    override var trailers: MutableList<TrailerData> = mutableListOf()
    override var recommendations: List<SearchResponse>? = emptyList()
    override var actors: List<ActorData>? = emptyList()
    override var comingSoon: Boolean = false
    override var syncData: MutableMap<String, String> = mutableMapOf()
    override var posterHeaders: Map<String, String>? = emptyMap()
    override var backgroundPosterUrl: String? = null
    override var logoUrl: String? = null
    override var contentRating: String? = null
    override var uniqueUrl: String = "unique-url"
}
