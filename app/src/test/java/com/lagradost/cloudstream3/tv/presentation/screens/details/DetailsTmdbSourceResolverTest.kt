package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addTMDbId
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SearchResponseList
import com.lagradost.cloudstream3.TrailerData
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newSearchResponseList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailsTmdbSourceResolverTest {
    @Test
    fun `returns resolved source when candidate load has matching tmdb id`() = runBlocking {
        val candidateUrl = "https://streamplay.example/fight-club"
        val client = FakeDetailsTmdbSearchClient(
            apiName = "StreamPlay",
            searchPages = mapOf(
                1 to newSearchResponseList(
                    listOf(testApi.newMovieSearchResponse("Fight Club", candidateUrl, fix = false)),
                    hasNext = false,
                )
            ),
            loadResponses = mapOf(
                candidateUrl to FakeResolverLoadResponse(tmdbId = "550")
            ),
        )
        val resolver = DetailsTmdbSourceResolver(
            clientFactory = { apiName ->
                if (apiName == "StreamPlay") client else null
            }
        )

        val resolvedSource = resolver.resolve(
            DetailsTmdbResolveRequest(
                title = "Fight Club",
                tmdbId = 550,
                preferredApiName = "StreamPlay",
                expectedType = TvType.Movie,
            )
        )

        requireNotNull(resolvedSource)
        assertEquals(candidateUrl, resolvedSource.url)
        assertEquals("StreamPlay", resolvedSource.apiName)
    }

    @Test
    fun `continues to next search page until exact tmdb match is found`() = runBlocking {
        val firstCandidateUrl = "https://streamplay.example/other-fight-club"
        val secondCandidateUrl = "https://streamplay.example/fight-club"
        val client = FakeDetailsTmdbSearchClient(
            apiName = "StreamPlay",
            searchPages = mapOf(
                1 to newSearchResponseList(
                    listOf(testApi.newMovieSearchResponse("Fight Club", firstCandidateUrl, fix = false)),
                    hasNext = true,
                ),
                2 to newSearchResponseList(
                    listOf(testApi.newMovieSearchResponse("Fight Club", secondCandidateUrl, fix = false)),
                    hasNext = false,
                ),
            ),
            loadResponses = mapOf(
                firstCandidateUrl to FakeResolverLoadResponse(tmdbId = "123"),
                secondCandidateUrl to FakeResolverLoadResponse(tmdbId = "550"),
            ),
        )
        val resolver = DetailsTmdbSourceResolver(
            clientFactory = { apiName ->
                if (apiName == "StreamPlay") client else null
            },
            maxSearchPages = 3,
        )

        val resolvedSource = resolver.resolve(
            DetailsTmdbResolveRequest(
                title = "Fight Club",
                tmdbId = 550,
                preferredApiName = "StreamPlay",
                expectedType = TvType.Movie,
            )
        )

        requireNotNull(resolvedSource)
        assertEquals(secondCandidateUrl, resolvedSource.url)
    }

    @Test
    fun `uses tmdb id extracted from search payload when load response has no sync tmdb id`() = runBlocking {
        val candidateUrl = """{"id":550,"type":"movie"}"""
        val client = FakeDetailsTmdbSearchClient(
            apiName = "StreamPlay",
            searchPages = mapOf(
                1 to newSearchResponseList(
                    listOf(testApi.newMovieSearchResponse("Fight Club", candidateUrl, fix = false)),
                    hasNext = false,
                )
            ),
            loadResponses = mapOf(
                candidateUrl to FakeResolverLoadResponse(tmdbId = null)
            ),
        )
        val resolver = DetailsTmdbSourceResolver(
            clientFactory = { apiName ->
                if (apiName == "StreamPlay") client else null
            }
        )

        val resolvedSource = resolver.resolve(
            DetailsTmdbResolveRequest(
                title = "Fight Club",
                tmdbId = 550,
                preferredApiName = "StreamPlay",
                expectedType = TvType.Movie,
            )
        )

        requireNotNull(resolvedSource)
        assertEquals(candidateUrl, resolvedSource.url)
        assertEquals("StreamPlay", resolvedSource.apiName)
    }

    @Test
    fun `resolves tv series when provider marks search response as movie but url payload says tv`() = runBlocking {
        val candidateUrl = """{"id":1399,"type":"tv"}"""
        val client = FakeDetailsTmdbSearchClient(
            apiName = "StreamPlay",
            searchPages = mapOf(
                1 to newSearchResponseList(
                    listOf(testApi.newMovieSearchResponse("Game of Thrones", candidateUrl, fix = false)),
                    hasNext = false,
                )
            ),
            loadResponses = mapOf(
                candidateUrl to FakeResolverLoadResponse(tmdbId = null, type = TvType.TvSeries)
            ),
        )
        val resolver = DetailsTmdbSourceResolver(
            clientFactory = { apiName ->
                if (apiName == "StreamPlay") client else null
            }
        )

        val resolvedSource = resolver.resolve(
            DetailsTmdbResolveRequest(
                title = "Game of Thrones",
                tmdbId = 1399,
                preferredApiName = "StreamPlay",
                expectedType = TvType.TvSeries,
            )
        )

        requireNotNull(resolvedSource)
        assertEquals(candidateUrl, resolvedSource.url)
        assertEquals("StreamPlay", resolvedSource.apiName)
    }

    @Test
    fun `returns null when preferred provider cannot be resolved`() = runBlocking {
        val resolver = DetailsTmdbSourceResolver(
            clientFactory = { _ -> null }
        )

        val resolvedSource = resolver.resolve(
            DetailsTmdbResolveRequest(
                title = "Fight Club",
                tmdbId = 550,
                preferredApiName = "MissingProvider",
                expectedType = TvType.Movie,
            )
        )

        assertNull(resolvedSource)
    }
}

private class FakeDetailsTmdbSearchClient(
    override val apiName: String,
    private val searchPages: Map<Int, SearchResponseList>,
    private val loadResponses: Map<String, LoadResponse>,
) : DetailsTmdbSearchClient {
    override suspend fun search(
        query: String,
        page: Int,
    ): SearchResponseList? {
        return searchPages[page]
    }

    override suspend fun load(url: String): LoadResponse? {
        return loadResponses[url]
    }
}

private class FakeResolverLoadResponse(
    tmdbId: String? = null,
    type: TvType = TvType.Movie,
) : LoadResponse {
    override var name: String = "Fake title"
    override var url: String = "fake-url"
    override var apiName: String = "fake-api"
    override var type: TvType = type
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

    init {
        addTMDbId(tmdbId)
    }
}

private val testApi = object : MainAPI() {
    override var name: String = "StreamPlay"
    override var lang: String = "en"
    override val supportedTypes: Set<TvType> = setOf(TvType.Movie, TvType.TvSeries)
}
