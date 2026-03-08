package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TrailerData
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DetailsLoadingPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailsRouteStateHolderTest {
    @Test
    fun `applyPrimary publishes done state and actions compat`() {
        val loadingPreview = DetailsLoadingPreview(title = "Loading")
        val stateHolder = DetailsRouteStateHolder(loadingPreview = loadingPreview)
        val primaryActionsCompat = fakeActionsCompat()

        stateHolder.applyPrimary(
            DetailsPrimaryStageResult(
                details = fakeMovieDetails(),
                loadResponse = FakeDetailsLoadResponse(),
                actionsCompat = primaryActionsCompat,
            )
        )

        val currentState = stateHolder.baseUiState.value as DetailsScreenUiState.Done
        assertEquals("details-1", currentState.details.id)
        assertTrue(currentState.isSecondaryContentLoading)
        assertEquals(primaryActionsCompat, stateHolder.actionsCompat)
    }

    @Test
    fun `applySecondary replaces details and ends loading`() {
        val stateHolder = DetailsRouteStateHolder(
            loadingPreview = DetailsLoadingPreview(title = "Loading")
        )
        stateHolder.applyPrimary(
            DetailsPrimaryStageResult(
                details = fakeMovieDetails(),
                loadResponse = FakeDetailsLoadResponse(),
                actionsCompat = fakeActionsCompat(),
            )
        )
        val secondaryActionsCompat = fakeActionsCompat(preferredSeason = 2, preferredEpisode = 4)

        stateHolder.applySecondary(
            DetailsSecondaryStageResult(
                details = fakeMovieDetails(currentSeason = 2, currentEpisode = 4),
                actionsCompat = secondaryActionsCompat,
            )
        )

        val currentState = stateHolder.baseUiState.value as DetailsScreenUiState.Done
        assertEquals(2, currentState.details.currentSeason)
        assertEquals(4, currentState.details.currentEpisode)
        assertTrue(!currentState.isSecondaryContentLoading)
        assertEquals(secondaryActionsCompat, stateHolder.actionsCompat)
    }

    @Test
    fun `showLoading resets stale actions compat`() {
        val loadingPreview = DetailsLoadingPreview(title = "Loading")
        val stateHolder = DetailsRouteStateHolder(loadingPreview = loadingPreview)
        stateHolder.applyPrimary(
            DetailsPrimaryStageResult(
                details = fakeMovieDetails(),
                loadResponse = FakeDetailsLoadResponse(),
                actionsCompat = fakeActionsCompat(),
            )
        )

        stateHolder.showLoading()

        val currentState = stateHolder.baseUiState.value as DetailsScreenUiState.Loading
        assertEquals(loadingPreview, currentState.preview)
        assertNull(stateHolder.actionsCompat)
    }

    @Test
    fun `finishSecondaryLoading keeps current details`() {
        val stateHolder = DetailsRouteStateHolder(
            loadingPreview = DetailsLoadingPreview(title = "Loading")
        )
        stateHolder.applyPrimary(
            DetailsPrimaryStageResult(
                details = fakeMovieDetails(),
                loadResponse = FakeDetailsLoadResponse(),
                actionsCompat = fakeActionsCompat(),
            )
        )

        stateHolder.finishSecondaryLoading()

        val currentState = stateHolder.baseUiState.value as DetailsScreenUiState.Done
        assertEquals("details-1", currentState.details.id)
        assertTrue(!currentState.isSecondaryContentLoading)
    }
}

private fun fakeMovieDetails(
    currentSeason: Int? = null,
    currentEpisode: Int? = null,
): MovieDetails {
    return MovieDetails(
        id = "details-1",
        name = "Details",
        description = "Description",
        posterUri = "poster",
        currentSeason = currentSeason,
        currentEpisode = currentEpisode,
    )
}

private fun fakeActionsCompat(
    preferredSeason: Int? = null,
    preferredEpisode: Int? = null,
): MovieDetailsEpisodeActionsCompat {
    return MovieDetailsEpisodeActionsCompat(
        loadResponse = FakeDetailsLoadResponse(),
        preferredSeason = preferredSeason,
        preferredEpisode = preferredEpisode,
    )
}

private class FakeDetailsLoadResponse : LoadResponse {
    override var name: String = "Fake title"
    override var url: String = "fake-url"
    override var apiName: String = "fake-api"
    override var type: TvType = TvType.Movie
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
