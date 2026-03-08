package com.lagradost.cloudstream3.tv.data.repositories

import android.util.Log
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.tv.compat.FavoritesCompat
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.MovieList
import com.lagradost.cloudstream3.tv.data.mappers.toPrimaryMovieDetails
import com.lagradost.cloudstream3.tv.data.mappers.toSecondaryMovieDetails
import com.lagradost.cloudstream3.tv.util.tvTraceAsyncSection
import com.lagradost.cloudstream3.tv.util.tvTraceSection
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.ui.WatchType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MovieRepositoryImpl : MovieRepository {
    private companion object {
        const val DebugTag = "TvDetailsRepo"
    }

    private val loadResponseCache = mutableMapOf<String, LoadResponse>()
    private val cacheMutex = Mutex()

    override fun getTrendingMovies(): Flow<MovieList> {
        // TODO: Implement with actual data from CloudStream providers
        return flowOf(emptyList())
    }

    override fun getTop10Movies(): Flow<MovieList> {
        // TODO: Implement with actual data from CloudStream providers
        return flowOf(emptyList())
    }

    override fun getNowPlayingMovies(): Flow<MovieList> {
        // TODO: Implement with actual data from CloudStream providers
        return flowOf(emptyList())
    }

    override suspend fun getPrimaryDetails(
        url: String,
        apiName: String,
    ): DetailsPrimaryLoadResult = withContext(Dispatchers.IO) {
        Log.d(DebugTag, "entry:getPrimaryDetails api=$apiName url=$url")
        val loadResponse = getOrLoadResponse(url = url, apiName = apiName)
        logLoadedResponse(loadResponse)

        val details = tvTraceSection("details_map_primary") {
            FavoritesCompat.markLibraryState(
                movieDetails = loadResponse.toPrimaryMovieDetails(),
                loadResponse = loadResponse,
            )
        }

        Log.d(
            DebugTag,
            "map:primary id=${details.id} name=${details.name} isFavorite=${details.isFavorite} isBookmarked=${details.isBookmarked} seasonCount=${details.seasonCount} episodeCount=${details.episodeCount} seasons=${details.seasons.size} currentSeason=${details.currentSeason} currentEpisode=${details.currentEpisode}"
        )

        DetailsPrimaryLoadResult(
            details = details,
            loadResponse = loadResponse,
        )
    }

    override suspend fun getSecondaryDetails(
        url: String,
        apiName: String,
    ): DetailsSecondaryLoadResult = withContext(Dispatchers.IO) {
        Log.d(DebugTag, "entry:getSecondaryDetails api=$apiName url=$url")
        val loadResponse = getOrLoadResponse(url = url, apiName = apiName)
        val secondary = tvTraceSection("details_map_secondary") {
            loadResponse.toSecondaryMovieDetails()
        }

        Log.d(
            DebugTag,
            "map:secondary cast=${secondary.cast.size} similar=${secondary.similarMovies.size} currentSeason=${secondary.currentSeason} currentEpisode=${secondary.currentEpisode}"
        )

        secondary
    }

    override suspend fun getDetails(url: String, apiName: String): MovieDetails {
        val primary = getPrimaryDetails(url = url, apiName = apiName)
        return primary.details.mergeSecondary(
            getSecondaryDetails(url = url, apiName = apiName)
        )
    }

    override suspend fun ensureMediaInFavorites(url: String, apiName: String) {
        setMediaFavorite(url = url, apiName = apiName, isFavorite = true)
    }

    override suspend fun setMediaFavorite(url: String, apiName: String, isFavorite: Boolean) {
        val loadResponse = withContext(Dispatchers.IO) {
            getOrLoadResponse(url = url, apiName = apiName)
        }

        if (isFavorite) {
            FavoritesCompat.addToFavorites(loadResponse)
        } else {
            FavoritesCompat.removeFromFavorites(loadResponse)
        }
    }

    override suspend fun setMediaBookmarkStatus(url: String, apiName: String, status: WatchType) {
        val loadResponse = withContext(Dispatchers.IO) {
            getOrLoadResponse(url = url, apiName = apiName)
        }

        FavoritesCompat.setBookmarkStatus(loadResponse, status)
    }

    private suspend fun getOrLoadResponse(
        url: String,
        apiName: String,
    ): LoadResponse {
        Log.d(DebugTag, "load:start api=$apiName url=$url")
        val cacheKey = cacheKey(url = url, apiName = apiName)

        cacheMutex.withLock {
            loadResponseCache[cacheKey]?.let { return it }
        }

        val loadedResponse = loadResponseInternal(
            url = url,
            apiName = apiName
        )

        cacheMutex.withLock {
            return loadResponseCache.getOrPut(cacheKey) { loadedResponse }
        }
    }

    private suspend fun loadResponseInternal(url: String, apiName: String): LoadResponse {
        return tvTraceAsyncSection(
            sectionName = "details_load_provider",
            cookie = cacheKey(url = url, apiName = apiName).hashCode(),
        ) {
            val api = APIHolder.getApiFromNameNull(apiName)
                ?: throw IllegalArgumentException("API provider not found: $apiName")
            val repo = APIRepository(api)

            when (val result = repo.load(url)) {
                is Resource.Success -> result.value
                is Resource.Failure -> throw Exception("Failed to load movie details: ${result.errorString}")
                is Resource.Loading -> throw IllegalStateException("Unexpected loading state")
            }
        }
    }

    private fun cacheKey(url: String, apiName: String): String {
        return "$apiName::$url"
    }

    private fun logLoadedResponse(loadResponse: LoadResponse) {
        Log.d(
            DebugTag,
            "load:success response=${loadResponse::class.java.simpleName} type=${loadResponse.type} ${loadResponse.debugEpisodeSummary()}"
        )
    }
}

private fun LoadResponse.debugEpisodeSummary(): String {
    return when (this) {
        is TvSeriesLoadResponse -> {
            "tvSeriesEpisodes=${this.episodes.size} seasonNames=${this.seasonNames?.size ?: 0}"
        }

        is AnimeLoadResponse -> {
            val perDub = this.episodes.entries.joinToString(separator = ",") { (dub, episodes) ->
                "${dub.name}:${episodes.size}"
            }
            "animeEpisodeBuckets={${perDub}} seasonNames=${this.seasonNames?.size ?: 0}"
        }

        is MovieLoadResponse -> {
            "movieDataUrlBlank=${this.dataUrl.isBlank()}"
        }

        else -> "noEpisodeSummary"
    }
}
