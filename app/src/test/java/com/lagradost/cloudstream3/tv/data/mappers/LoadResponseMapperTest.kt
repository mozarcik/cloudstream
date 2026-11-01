package com.lagradost.cloudstream3.tv.data.mappers

import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.NextAiring
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.ShowStatus
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.TvType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadResponseMapperTest {
    @Test
    @Suppress("DEPRECATION_ERROR")
    fun `movie primary mapping keeps legacy metadata fields`() {
        val response = MovieLoadResponse(
            name = "Movie",
            url = "movie-url",
            apiName = "Test API",
            type = TvType.Movie,
            dataUrl = "movie-data",
            posterUrl = "poster-url",
            year = 2024,
            plot = "Movie plot",
            score = Score.from(84, 100),
            tags = listOf("Action", "Drama"),
            duration = 128,
            comingSoon = true,
            posterHeaders = mapOf("Referer" to "https://example.com"),
            backgroundPosterUrl = "backdrop-url",
            logoUrl = "logo-url",
            contentRating = "PG-13",
        )

        val details = response.toPrimaryMovieDetails()

        assertEquals("movie-url", details.id)
        assertEquals("Test API", details.providerName)
        assertEquals(TvType.Movie, details.type)
        assertEquals(Score.from(84, 100), details.score)
        assertTrue(details.comingSoon)
        assertEquals("logo-url", details.logoUri)
        assertEquals("poster-url", details.posterUri)
        assertEquals("backdrop-url", details.backdropUri)
        assertEquals(mapOf("Referer" to "https://example.com"), details.posterHeaders)
        assertEquals("PG-13", details.pgRating)
        assertEquals("2024", details.releaseDate)
        assertEquals(listOf("Action", "Drama"), details.categories)
        assertEquals("128min", details.duration)
    }

    @Test
    @Suppress("DEPRECATION_ERROR")
    fun `tv series primary mapping keeps status next airing and seasons`() {
        val nextAiring = NextAiring(
            episode = 3,
            season = 1,
            unixTime = 1_800_000_000L,
        )
        val response = TvSeriesLoadResponse(
            name = "Series",
            url = "series-url",
            apiName = "Series API",
            type = TvType.TvSeries,
            episodes = listOf(
                Episode(data = "ep-1", season = 1, episode = 1, name = "Episode 1"),
                Episode(data = "ep-2", season = 1, episode = 2, name = "Episode 2"),
            ),
            showStatus = ShowStatus.Ongoing,
            nextAiring = nextAiring,
            backgroundPosterUrl = "series-backdrop",
        )

        val details = response.toPrimaryMovieDetails()

        assertEquals(ShowStatus.Ongoing, details.showStatus)
        assertEquals(nextAiring, details.nextAiring)
        assertEquals(1, details.seasonCount)
        assertEquals(2, details.episodeCount)
        assertEquals(1, details.currentSeason)
        assertEquals(1, details.currentEpisode)
        assertEquals(1, details.seasons.size)
        assertEquals(2, details.seasons.first().episodes.size)
        assertEquals("series-backdrop", details.posterUri)
        assertEquals("series-backdrop", details.backdropUri)
    }

    @Test
    @Suppress("DEPRECATION_ERROR")
    fun `anime primary mapping resolves original title from alternative titles`() {
        val response = AnimeLoadResponse(
            engName = "Main Title",
            japName = "Original Title",
            name = "Main Title",
            url = "anime-url",
            apiName = "Anime API",
            type = TvType.Anime,
            episodes = mutableMapOf(
                DubStatus.Subbed to listOf(
                    Episode(data = "anime-ep-1", season = 1, episode = 1, name = "Episode 1")
                )
            ),
            synonyms = listOf("Main Title", "Other Title"),
        )

        val details = response.toPrimaryMovieDetails()

        assertEquals("Original Title", details.originalTitle)
        assertEquals(TvType.Anime, details.type)
        assertEquals(1, details.seasonCount)
        assertEquals(1, details.episodeCount)
    }
}
