package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TrailerData
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.data.entities.MovieCast
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.repositories.DetailsPrimaryLoadResult
import com.lagradost.cloudstream3.tv.data.repositories.DetailsSecondaryLoadResult
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository
import com.lagradost.cloudstream3.ui.WatchType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailsScreenLoadCoordinatorTest {
    @Test
    fun `load emits primary stage before secondary success`() = runBlocking {
        val primaryDetails = fakeMovieDetails()
        val repository = FakeMovieRepository(
            primaryResult = DetailsPrimaryLoadResult(
                details = primaryDetails,
                loadResponse = FakeLoadResponse(),
            ),
            secondaryResult = DetailsSecondaryLoadResult(
                cast = listOf(MovieCast("cast-1", "Hero", "Actor", "avatar")),
                similarMovies = listOf(Movie("similar-1", "poster", "Similar", "Description")),
                currentSeason = 2,
                currentEpisode = 5,
            ),
        )
        val coordinator = DetailsScreenLoadCoordinator(repository)
        val emittedStages = mutableListOf<String>()

        val outcome = coordinator.load(url = "url", apiName = "api") { primary ->
            emittedStages += "primary:${primary.details.id}:${primary.details.currentSeason}:${primary.details.currentEpisode}"
        }

        assertEquals(listOf("primary:details-1:null:null"), emittedStages)
        assertTrue(outcome is DetailsScreenLoadOutcome.Success)
        val success = outcome as DetailsScreenLoadOutcome.Success
        assertEquals(2, success.secondary.details.currentSeason)
        assertEquals(5, success.secondary.details.currentEpisode)
        assertEquals(1, success.secondary.details.cast.size)
        assertEquals(1, success.secondary.details.similarMovies.size)
    }

    @Test
    fun `load preserves primary stage when secondary fails`() = runBlocking {
        val primaryDetails = fakeMovieDetails()
        val repository = FakeMovieRepository(
            primaryResult = DetailsPrimaryLoadResult(
                details = primaryDetails,
                loadResponse = FakeLoadResponse(),
            ),
            secondaryError = IllegalStateException("secondary failed"),
        )
        val coordinator = DetailsScreenLoadCoordinator(repository)
        var primaryCallbackCount = 0

        val outcome = coordinator.load(url = "url", apiName = "api") {
            primaryCallbackCount += 1
        }

        assertEquals(1, primaryCallbackCount)
        assertTrue(outcome is DetailsScreenLoadOutcome.SecondaryFailure)
        val failure = outcome as DetailsScreenLoadOutcome.SecondaryFailure
        assertEquals(primaryDetails, failure.primary.details)
        assertEquals("secondary failed", failure.error.message)
    }

    private fun fakeMovieDetails(): MovieDetails {
        return MovieDetails(
            id = "details-1",
            name = "Details",
            description = "Description",
            posterUri = "poster",
        )
    }
}

private class FakeMovieRepository(
    private val primaryResult: DetailsPrimaryLoadResult? = null,
    private val secondaryResult: DetailsSecondaryLoadResult = DetailsSecondaryLoadResult(),
    private val primaryError: Throwable? = null,
    private val secondaryError: Throwable? = null,
) : MovieRepository {
    override fun getTrendingMovies(): Flow<List<Movie>> = flowOf(emptyList())

    override fun getTop10Movies(): Flow<List<Movie>> = flowOf(emptyList())

    override fun getNowPlayingMovies(): Flow<List<Movie>> = flowOf(emptyList())

    override suspend fun getPrimaryDetails(url: String, apiName: String): DetailsPrimaryLoadResult {
        primaryError?.let { throw it }
        return requireNotNull(primaryResult)
    }

    override suspend fun getSecondaryDetails(url: String, apiName: String): DetailsSecondaryLoadResult {
        secondaryError?.let { throw it }
        return secondaryResult
    }

    override suspend fun ensureMediaInFavorites(url: String, apiName: String) = Unit

    override suspend fun setMediaFavorite(url: String, apiName: String, isFavorite: Boolean) = Unit

    override suspend fun setMediaBookmarkStatus(url: String, apiName: String, status: WatchType) = Unit
}

private class FakeLoadResponse : LoadResponse {
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
