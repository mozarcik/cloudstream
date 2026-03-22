package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.lifecycle.SavedStateHandle
import com.lagradost.cloudstream3.TvType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailsScreenRouteArgsTest {
    @Test
    fun `parses tmdb resolve request and unavailable provider from saved state`() {
        val savedStateHandle = createDetailsSavedStateHandle(
            url = "https://www.themoviedb.org/movie/550",
            apiName = "TMDB",
            loadingState = DetailsLoadingState(
                title = "Fight Club",
                backdropUri = "backdrop",
                providerName = "StreamPlay",
            ),
            tmdbResolveRequest = DetailsTmdbResolveRequest(
                title = "Fight Club",
                tmdbId = 550,
                preferredApiName = "StreamPlay",
                expectedType = TvType.Movie,
            ),
        )

        val args = savedStateHandle.toDetailsScreenRouteArgs(mode = DetailsScreenMode.Movie)

        assertEquals("https://www.themoviedb.org/movie/550", args.sourceUrl)
        assertEquals("TMDB", args.sourceApiName)
        assertTrue(args.shouldShowUnavailableState)
        assertEquals("StreamPlay", args.unavailableDetails.providerName)
        assertNotNull(args.tmdbResolveRequest)
        assertEquals(550, args.tmdbResolveRequest?.tmdbId)
        assertEquals("Fight Club", args.tmdbResolveRequest?.title)
        assertEquals("StreamPlay", args.tmdbResolveRequest?.preferredApiName)
        assertEquals(TvType.Movie, args.tmdbResolveRequest?.expectedType)
    }

    @Test
    fun `consume resolve request removes it from previous state handle`() {
        val savedStateHandle = SavedStateHandle().apply {
            saveDetailsTmdbResolveRequest(
                DetailsTmdbResolveRequest(
                    title = "Fight Club",
                    tmdbId = 550,
                    preferredApiName = "StreamPlay",
                    expectedType = TvType.Movie,
                )
            )
        }

        val request = savedStateHandle.consumeDetailsTmdbResolveRequest()

        requireNotNull(request)
        assertEquals("Fight Club", request.title)
        assertEquals(550, request.tmdbId)
        assertTrue(!savedStateHandle.contains(DetailsScreenNavigation.ResolveTmdbIdBundleKey))
    }
}
