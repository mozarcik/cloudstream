package com.lagradost.cloudstream3.tv.compat.home

import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.syncproviders.providers.TMDB_PROVIDER_NAME
import com.lagradost.cloudstream3.tv.compat.home.SearchResponseMapper.toMediaItemCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResponseMapperTest {
    @Test
    fun `maps tmdb library item backdrop and tmdb id`() {
        val item = SyncAPI.LibraryItem(
            name = "Fight Club",
            url = "https://www.themoviedb.org/movie/550",
            syncId = "movie:550",
            episodesCompleted = null,
            episodesTotal = null,
            personalRating = null,
            lastUpdatedUnixTime = null,
            apiName = TMDB_PROVIDER_NAME,
            type = TvType.Movie,
            posterUrl = "https://image.tmdb.org/t/p/w500/poster.jpg",
            posterHeaders = null,
            quality = null,
            releaseDate = null,
            id = 550,
            plot = "desc",
            score = null,
            backgroundPosterUrl = "https://image.tmdb.org/t/p/w500/backdrop.jpg",
        )

        val mediaItem = item.toMediaItemCompat()

        assertTrue(mediaItem is MediaItemCompat.Movie)
        assertEquals("https://image.tmdb.org/t/p/w500/backdrop.jpg", mediaItem.backdropUri)
        assertEquals(550, mediaItem.tmdbId)
    }
}
