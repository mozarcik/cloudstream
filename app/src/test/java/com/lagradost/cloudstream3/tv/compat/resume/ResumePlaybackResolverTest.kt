package com.lagradost.cloudstream3.tv.compat.resume

import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.ui.result.VideoWatchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

@Suppress("DEPRECATION_ERROR")
class ResumePlaybackResolverTest {
    @Test
    fun `series resume prefers exact saved episode id`() {
        val parentId = 1000
        val episodes = listOf(
            Episode(data = "s8e1", season = 8, episode = 1, name = "S08E01"),
            Episode(data = "s1e3", season = 1, episode = 3, name = "S01E03"),
        )
        val resumeContext = ResumePlaybackContext(
            parentId = parentId,
            episodeId = resolveSeriesPlaybackEpisodeId(parentId, seasonNumber = 1, episodeNumber = 3),
            season = 1,
            episode = 3,
            isFromDownload = false,
        )

        val resolvedEpisode = ResumePlaybackResolver.resolveCurrentSeriesEpisode(
            episodes = episodes,
            parentId = parentId,
            resumeContext = resumeContext,
            watchStateProvider = { null },
        )

        assertEquals("s1e3", resolvedEpisode?.data)
    }

    @Test
    fun `series resume falls back to next unwatched episode when saved resume is missing`() {
        val parentId = 1000
        val episodes = listOf(
            Episode(data = "s1e1", season = 1, episode = 1, name = "S01E01"),
            Episode(data = "s1e2", season = 1, episode = 2, name = "S01E02"),
        )

        val resolvedEpisode = ResumePlaybackResolver.resolveCurrentSeriesEpisode(
            episodes = episodes,
            parentId = parentId,
            resumeContext = null,
            watchStateProvider = { episodeId ->
                when (episodeId) {
                    resolveSeriesPlaybackEpisodeId(parentId, seasonNumber = 1, episodeNumber = 1) ->
                        VideoWatchState.Watched

                    else -> VideoWatchState.None
                }
            },
        )

        assertEquals("s1e2", resolvedEpisode?.data)
    }

    @Test
    fun `anime resume keeps exact dub group for saved episode id`() {
        val parentId = 2000
        val dubbedEpisode = Episode(data = "dub-s1e1", season = 1, episode = 1, name = "Dub 1")
        val subbedEpisode = Episode(data = "sub-s1e3", season = 1, episode = 3, name = "Sub 3")
        val resumeContext = ResumePlaybackContext(
            parentId = parentId,
            episodeId = resolveAnimePlaybackEpisodeId(
                parentId = parentId,
                seasonNumber = 1,
                episodeNumber = 3,
                dubStatus = DubStatus.Subbed,
            ),
            season = 1,
            episode = 3,
            isFromDownload = false,
        )

        val resolvedEpisode = ResumePlaybackResolver.resolveCurrentAnimeEpisode(
            episodesByDub = mapOf(
                DubStatus.Dubbed to listOf(dubbedEpisode),
                DubStatus.Subbed to listOf(subbedEpisode),
            ),
            parentId = parentId,
            preferredDubStatus = DubStatus.Dubbed,
            resumeContext = resumeContext,
            watchStateProvider = { null },
        )

        assertNotNull(resolvedEpisode)
        assertEquals(DubStatus.Subbed, resolvedEpisode?.dubStatus)
        assertEquals("sub-s1e3", resolvedEpisode?.episode?.data)
    }
}
