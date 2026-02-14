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
import com.lagradost.cloudstream3.tv.data.mappers.toMovieDetails
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.ui.WatchType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MovieRepositoryImpl : MovieRepository {
    private companion object {
        const val DebugTag = "TvDetailsRepo"
    }
    private val loadResponseCache = mutableMapOf<String, LoadResponse>()

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

    override suspend fun getMovieDetails(url: String, apiName: String): MovieDetails {
        Log.d(DebugTag, "entry:getMovieDetails api=$apiName url=$url")
        return getDetailsInternal(url, apiName)
    }

    override suspend fun getTvSeriesDetails(url: String, apiName: String): MovieDetails {
        Log.d(DebugTag, "entry:getTvSeriesDetails api=$apiName url=$url")
        return getDetailsInternal(url, apiName)
    }

    override suspend fun getMediaDetails(url: String, apiName: String): MovieDetails {
        Log.d(DebugTag, "entry:getMediaDetails api=$apiName url=$url")
        return getDetailsInternal(url, apiName)
    }

    override suspend fun ensureMediaInFavorites(url: String, apiName: String) {
        setMediaFavorite(url = url, apiName = apiName, isFavorite = true)
    }

    override suspend fun setMediaFavorite(url: String, apiName: String, isFavorite: Boolean) {
        val cacheKey = cacheKey(url = url, apiName = apiName)
        val loadResponse = loadResponseCache[cacheKey] ?: loadResponseInternal(
            url = url,
            apiName = apiName
        ).also { loadedResponse ->
            loadResponseCache[cacheKey] = loadedResponse
        }

        if (isFavorite) {
            FavoritesCompat.addToFavorites(loadResponse)
        } else {
            FavoritesCompat.removeFromFavorites(loadResponse)
        }
    }

    override suspend fun setMediaBookmarkStatus(url: String, apiName: String, status: WatchType) {
        val cacheKey = cacheKey(url = url, apiName = apiName)
        val loadResponse = loadResponseCache[cacheKey] ?: loadResponseInternal(
            url = url,
            apiName = apiName
        ).also { loadedResponse ->
            loadResponseCache[cacheKey] = loadedResponse
        }

        FavoritesCompat.setBookmarkStatus(loadResponse, status)
    }

    private suspend fun getDetailsInternal(url: String, apiName: String): MovieDetails {
        Log.d(DebugTag, "load:start api=$apiName url=$url")
        val cacheKey = cacheKey(url = url, apiName = apiName)

        val loadResponse = loadResponseCache[cacheKey] ?: loadResponseInternal(
            url = url,
            apiName = apiName
        ).also { loadedResponse ->
            loadResponseCache[cacheKey] = loadedResponse
        }

        Log.d(
            DebugTag,
            "load:success response=${loadResponse::class.java.simpleName} type=${loadResponse.type} ${loadResponse.debugEpisodeSummary()}"
        )

        val details = FavoritesCompat.markLibraryState(
            movieDetails = loadResponse.toMovieDetails(),
            loadResponse = loadResponse,
        )
        val firstSeasonEpisodes = details.seasons.firstOrNull()?.episodes?.size ?: 0

        Log.d(
            DebugTag,
            "map:done id=${details.id} name=${details.name} isFavorite=${details.isFavorite} isBookmarked=${details.isBookmarked} bookmarkLabelRes=${details.bookmarkLabelRes} seasonCount=${details.seasonCount} episodeCount=${details.episodeCount} seasons=${details.seasons.size} firstSeasonEpisodes=$firstSeasonEpisodes currentSeason=${details.currentSeason} currentEpisode=${details.currentEpisode}"
        )

        return details
    }

    private suspend fun loadResponseInternal(url: String, apiName: String): LoadResponse {
        val api = APIHolder.getApiFromNameNull(apiName)
            ?: throw IllegalArgumentException("API provider not found: $apiName")
        val repo = APIRepository(api)

        return when (val result = repo.load(url)) {
            is Resource.Success -> result.value
            is Resource.Failure -> throw Exception("Failed to load movie details: ${result.errorString}")
            is Resource.Loading -> throw IllegalStateException("Unexpected loading state")
        }
    }

    private fun cacheKey(url: String, apiName: String): String {
        return "$apiName::$url"
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
