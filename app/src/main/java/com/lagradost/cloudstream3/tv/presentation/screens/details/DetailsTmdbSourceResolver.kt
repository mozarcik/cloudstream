package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.util.Log
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.getImdbId
import com.lagradost.cloudstream3.LoadResponse.Companion.getTMDbId
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SearchResponseList
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.APIRepository
import kotlinx.coroutines.CancellationException

internal interface DetailsTmdbSearchClient {
    val apiName: String

    suspend fun search(
        query: String,
        page: Int,
    ): SearchResponseList?

    suspend fun load(url: String): LoadResponse?
}

internal class DetailsTmdbSourceResolver(
    private val clientFactory: (String) -> DetailsTmdbSearchClient? = ::defaultDetailsTmdbSearchClient,
    private val maxSearchPages: Int = 3,
    private val maxCandidateLoads: Int = 12,
) {
    suspend fun resolve(request: DetailsTmdbResolveRequest): DetailsRouteSource? {
        val preferredApiName = request.preferredApiName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: run {
                DetailsTmdbResolverLogger.logMissingPreferredApi(request)
                return null
            }
        val client = clientFactory(preferredApiName) ?: run {
            DetailsTmdbResolverLogger.logMissingClient(preferredApiName, request)
            return null
        }

        var currentPage = 1
        var verifiedCandidates = 0

        while (currentPage <= maxSearchPages && verifiedCandidates < maxCandidateLoads) {
            val searchPage = try {
                client.search(
                    query = request.title,
                    page = currentPage,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                DetailsTmdbResolverLogger.logSearchFailure(
                    request = request,
                    apiName = client.apiName,
                    page = currentPage,
                    error = error,
                )
                return null
            } ?: run {
                DetailsTmdbResolverLogger.logEmptySearchPage(
                    request = request,
                    apiName = client.apiName,
                    page = currentPage,
                )
                return null
            }

            DetailsTmdbResolverLogger.logSearchPage(
                request = request,
                apiName = client.apiName,
                page = currentPage,
                resultCount = searchPage.items.size,
                hasNext = searchPage.hasNext,
            )

            val candidates = searchPage.items.asSequence()
                .filter { candidate ->
                    candidate.matchesExpectedType(request.expectedType)
                }

            for (candidate in candidates) {
                if (verifiedCandidates >= maxCandidateLoads) {
                    break
                }
                val searchCandidateTmdbId = candidate.resolveCandidateTmdbIdOrNull()
                verifiedCandidates += 1

                DetailsTmdbResolverLogger.logCandidate(
                    request = request,
                    apiName = client.apiName,
                    page = currentPage,
                    verifiedCandidates = verifiedCandidates,
                    candidate = candidate,
                    resolvedCandidateType = candidate.resolveCandidateType(),
                    searchCandidateTmdbId = searchCandidateTmdbId,
                )

                if (searchCandidateTmdbId != null && searchCandidateTmdbId != request.tmdbId) {
                    DetailsTmdbResolverLogger.logSkippedCandidate(
                        apiName = client.apiName,
                        candidateName = candidate.name,
                        expectedTmdbId = request.tmdbId,
                        candidateTmdbId = searchCandidateTmdbId,
                    )
                    continue
                }

                val loadResponse = try {
                    client.load(candidate.url)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    DetailsTmdbResolverLogger.logLoadFailure(
                        apiName = client.apiName,
                        candidateName = candidate.name,
                        candidateUrl = candidate.url,
                        error = error,
                    )
                    null
                } ?: continue

                val loadTmdbId = loadResponse.getTMDbId()?.toIntOrNull()
                val resolvedCandidateTmdbId = loadTmdbId ?: searchCandidateTmdbId
                DetailsTmdbResolverLogger.logLoadedCandidate(
                    apiName = client.apiName,
                    candidateName = candidate.name,
                    candidateUrl = candidate.url,
                    loadResponse = loadResponse,
                    searchCandidateTmdbId = searchCandidateTmdbId,
                    loadTmdbId = loadTmdbId,
                    resolvedCandidateTmdbId = resolvedCandidateTmdbId,
                )
                if (resolvedCandidateTmdbId != request.tmdbId) {
                    continue
                }

                return DetailsRouteSource(
                    url = candidate.url,
                    apiName = client.apiName,
                )
            }

            if (!searchPage.hasNext) {
                break
            }
            currentPage += 1
        }

        return null
    }
}

private fun defaultDetailsTmdbSearchClient(
    apiName: String,
): DetailsTmdbSearchClient? {
    val api = APIHolder.getApiFromNameNull(apiName) ?: return null
    return ApiRepositoryDetailsTmdbSearchClient(
        repository = APIRepository(api)
    )
}

private class ApiRepositoryDetailsTmdbSearchClient(
    private val repository: APIRepository,
) : DetailsTmdbSearchClient {
    override val apiName: String = repository.name

    override suspend fun search(
        query: String,
        page: Int,
    ): SearchResponseList? {
        return when (val result = repository.search(query = query, page = page)) {
            is Resource.Success -> result.value
            is Resource.Failure -> null
            is Resource.Loading -> null
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        return when (val result = repository.load(url)) {
            is Resource.Success -> result.value
            is Resource.Failure -> null
            is Resource.Loading -> null
        }
    }
}

private fun SearchResponse.matchesExpectedType(expectedType: TvType?): Boolean {
    val normalizedExpectedType = expectedType ?: return true
    if (normalizedExpectedType == TvType.Others) {
        return true
    }

    val candidateType = resolveCandidateType() ?: return true
    return candidateType.isEpisodeBased() == normalizedExpectedType.isEpisodeBased()
}

private fun SearchResponse.resolveCandidateType(): TvType? {
    val explicitType = type
    val inferredType = inferSeriesTypeFromUrl()

    return when {
        explicitType?.isEpisodeBased() == true -> explicitType
        inferredType != null -> inferredType
        else -> explicitType
    }
}

private fun SearchResponse.resolveCandidateTmdbIdOrNull(): Int? {
    val tmdbUrlMatch = TmdbResolverTmdbUrlRegex.find(url)
    if (tmdbUrlMatch != null) {
        return tmdbUrlMatch.groupValues.getOrNull(2)?.toIntOrNull()
    }

    val jsonIdMatch = TmdbResolverJsonIdRegex.find(url)
    if (jsonIdMatch != null) {
        return jsonIdMatch.groupValues.getOrNull(1)?.toIntOrNull()
    }

    return null
}

private fun SearchResponse.inferSeriesTypeFromUrl(): TvType? {
    val normalizedUrl = url.lowercase()

    return when {
        "\"type\":\"tv\"" in normalizedUrl -> TvType.TvSeries
        "\"type\":\"tvseries\"" in normalizedUrl -> TvType.TvSeries
        "\"type\":\"series\"" in normalizedUrl -> TvType.TvSeries
        "\"type\":\"anime\"" in normalizedUrl -> TvType.Anime
        "\"type\":\"ova\"" in normalizedUrl -> TvType.OVA
        else -> null
    }
}

private val TmdbResolverJsonIdRegex = Regex("\"id\"\\s*:\\s*(\\d+)")
private val TmdbResolverTmdbUrlRegex =
    Regex("""https?://(?:www\.)?themoviedb\.org/(movie|tv)/(\d+)""")

private object DetailsTmdbResolverLogger {
    private const val Tag = "TvTmdbResolve"

    fun logMissingPreferredApi(request: DetailsTmdbResolveRequest) {
        Log.w(
            Tag,
            "resolve aborted: missing preferred api title=${request.title} tmdbId=${request.tmdbId}"
        )
    }

    fun logMissingClient(
        preferredApiName: String,
        request: DetailsTmdbResolveRequest,
    ) {
        Log.w(
            Tag,
            "resolve aborted: missing client api=$preferredApiName title=${request.title} tmdbId=${request.tmdbId}"
        )
    }

    fun logSearchPage(
        request: DetailsTmdbResolveRequest,
        apiName: String,
        page: Int,
        resultCount: Int,
        hasNext: Boolean,
    ) {
        Log.d(
            Tag,
            "search page api=$apiName page=$page title=${request.title} tmdbId=${request.tmdbId} results=$resultCount hasNext=$hasNext"
        )
    }

    fun logEmptySearchPage(
        request: DetailsTmdbResolveRequest,
        apiName: String,
        page: Int,
    ) {
        Log.w(
            Tag,
            "search returned null api=$apiName page=$page title=${request.title} tmdbId=${request.tmdbId}"
        )
    }

    fun logSearchFailure(
        request: DetailsTmdbResolveRequest,
        apiName: String,
        page: Int,
        error: Throwable,
    ) {
        Log.e(
            Tag,
            "search failed api=$apiName page=$page title=${request.title} tmdbId=${request.tmdbId}",
            error
        )
    }

    fun logCandidate(
        request: DetailsTmdbResolveRequest,
        apiName: String,
        page: Int,
        verifiedCandidates: Int,
        candidate: SearchResponse,
        resolvedCandidateType: TvType?,
        searchCandidateTmdbId: Int?,
    ) {
        Log.d(
            Tag,
            "candidate api=$apiName page=$page index=$verifiedCandidates name=${candidate.name} type=${candidate.type} resolvedType=$resolvedCandidateType searchTmdbId=$searchCandidateTmdbId expectedTmdbId=${request.tmdbId} url=${candidate.url}"
        )
    }

    fun logSkippedCandidate(
        apiName: String,
        candidateName: String,
        expectedTmdbId: Int,
        candidateTmdbId: Int,
    ) {
        Log.d(
            Tag,
            "candidate skipped api=$apiName name=$candidateName candidateTmdbId=$candidateTmdbId expectedTmdbId=$expectedTmdbId"
        )
    }

    fun logLoadFailure(
        apiName: String,
        candidateName: String,
        candidateUrl: String,
        error: Throwable,
    ) {
        Log.e(
            Tag,
            "candidate load failed api=$apiName name=$candidateName url=$candidateUrl",
            error
        )
    }

    fun logLoadedCandidate(
        apiName: String,
        candidateName: String,
        candidateUrl: String,
        loadResponse: LoadResponse,
        searchCandidateTmdbId: Int?,
        loadTmdbId: Int?,
        resolvedCandidateTmdbId: Int?,
    ) {
        Log.d(
            Tag,
            "candidate loaded api=$apiName name=$candidateName candidateUrl=$candidateUrl loadUrl=${loadResponse.url} loadType=${loadResponse.type} searchTmdbId=$searchCandidateTmdbId loadTmdbId=$loadTmdbId resolvedTmdbId=$resolvedCandidateTmdbId imdbId=${loadResponse.getImdbId()}"
        )
    }
}
