package com.lagradost.cloudstream3.tv.data.repositories

import android.util.Log
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.MovieList
import com.lagradost.cloudstream3.tv.data.mappers.toMovieDetails
import com.lagradost.cloudstream3.ui.APIRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MovieRepositoryImpl : MovieRepository {
    private companion object {
        const val DebugTag = "TvDetailsRepo"
    }

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

    private suspend fun getDetailsInternal(url: String, apiName: String): MovieDetails {
        Log.d(DebugTag, "load:start api=$apiName url=$url")

        val api = APIHolder.getApiFromNameNull(apiName)
            ?: throw IllegalArgumentException("API provider not found: $apiName")

        val repo = APIRepository(api)
        
        val loadResponse = when (val result = repo.load(url)) {
            is Resource.Success -> result.value
            is Resource.Failure -> throw Exception("Failed to load movie details: ${result.errorString}")
            is Resource.Loading -> throw IllegalStateException("Unexpected loading state")
        }

        Log.d(
            DebugTag,
            "load:success response=${loadResponse::class.java.simpleName} type=${loadResponse.type} ${loadResponse.debugEpisodeSummary()}"
        )

        val details = loadResponse.toMovieDetails()
        val firstSeasonEpisodes = details.seasons.firstOrNull()?.episodes?.size ?: 0

        Log.d(
            DebugTag,
            "map:done id=${details.id} name=${details.name} seasonCount=${details.seasonCount} episodeCount=${details.episodeCount} seasons=${details.seasons.size} firstSeasonEpisodes=$firstSeasonEpisodes currentSeason=${details.currentSeason} currentEpisode=${details.currentEpisode}"
        )

        return details
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
