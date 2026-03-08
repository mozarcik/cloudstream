package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TrailerData
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.repositories.DetailsPrimaryLoadResult
import com.lagradost.cloudstream3.tv.data.repositories.DetailsSecondaryLoadResult
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository
import com.lagradost.cloudstream3.ui.WatchType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailsSessionRepositoryTest {
    @Test
    fun `session caches primary and secondary stages`() = runBlocking {
        val repository = CountingMovieRepository()
        val sessionRepository = DefaultDetailsSessionRepository(repository)
        val session = sessionRepository.openSession(url = "url", apiName = "api")

        session.loadPrimaryStage()
        session.loadPrimaryStage()
        val primary = session.loadPrimaryStage()
        session.loadSecondaryStage(primary)
        session.loadSecondaryStage(primary)

        assertEquals(1, repository.primaryCalls)
        assertEquals(1, repository.secondaryCalls)
    }
}

private class CountingMovieRepository : MovieRepository {
    var primaryCalls: Int = 0
        private set
    var secondaryCalls: Int = 0
        private set

    override fun getTrendingMovies(): Flow<List<com.lagradost.cloudstream3.tv.data.entities.Movie>> = flowOf(emptyList())

    override fun getTop10Movies(): Flow<List<com.lagradost.cloudstream3.tv.data.entities.Movie>> = flowOf(emptyList())

    override fun getNowPlayingMovies(): Flow<List<com.lagradost.cloudstream3.tv.data.entities.Movie>> = flowOf(emptyList())

    override suspend fun getPrimaryDetails(url: String, apiName: String): DetailsPrimaryLoadResult {
        primaryCalls += 1
        return DetailsPrimaryLoadResult(
            details = MovieDetails(
                id = "details",
                name = "Details",
                description = "Description",
                posterUri = "poster",
            ),
            loadResponse = CountingLoadResponse(),
        )
    }

    override suspend fun getSecondaryDetails(url: String, apiName: String): DetailsSecondaryLoadResult {
        secondaryCalls += 1
        return DetailsSecondaryLoadResult(currentSeason = 1, currentEpisode = 2)
    }

    override suspend fun ensureMediaInFavorites(url: String, apiName: String) = Unit

    override suspend fun setMediaFavorite(url: String, apiName: String, isFavorite: Boolean) = Unit

    override suspend fun setMediaBookmarkStatus(url: String, apiName: String, status: WatchType) = Unit
}

private class CountingLoadResponse : LoadResponse {
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
