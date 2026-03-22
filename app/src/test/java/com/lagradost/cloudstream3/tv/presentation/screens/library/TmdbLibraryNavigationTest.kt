package com.lagradost.cloudstream3.tv.presentation.screens.library

import com.lagradost.cloudstream3.syncproviders.providers.TMDB_PROVIDER_NAME
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbLibraryNavigationTest {
    @Test
    fun `returns resolve request for tmdb items`() {
        val request = TmdbLibraryNavigation.resolveDetailsRequest(
            MediaItemCompat.Movie(
                id = "1",
                url = "https://www.themoviedb.org/movie/550",
                apiName = TMDB_PROVIDER_NAME,
                name = " Fight Club ",
                posterUri = "",
                type = null,
                score = null,
                tmdbId = 550,
            ),
            preferredApiName = "StreamPlay",
        )

        requireNotNull(request)
        assertEquals("Fight Club", request.title)
        assertEquals(550, request.tmdbId)
        assertEquals("StreamPlay", request.preferredApiName)
    }

    @Test
    fun `ignores non tmdb items`() {
        val request = TmdbLibraryNavigation.resolveDetailsRequest(
            MediaItemCompat.Movie(
                id = "1",
                url = "https://example.com/movie/1",
                apiName = "Local",
                name = "Fight Club",
                posterUri = "",
                type = null,
                score = null,
            ),
            preferredApiName = "StreamPlay",
        )

        assertNull(request)
    }

    @Test
    fun `requires tmdb id`() {
        val request = TmdbLibraryNavigation.resolveDetailsRequest(
            MediaItemCompat.Movie(
                id = "1",
                url = "https://www.themoviedb.org/movie/550",
                apiName = TMDB_PROVIDER_NAME,
                name = "Fight Club",
                posterUri = "",
                type = null,
                score = null,
            ),
            preferredApiName = "StreamPlay",
        )

        assertNull(request)
    }
}
