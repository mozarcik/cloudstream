package com.lagradost.cloudstream3.tv.data.repositories

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.tv.data.entities.MovieCast
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.MovieList
import com.lagradost.cloudstream3.ui.WatchType
import kotlinx.coroutines.flow.Flow

@Immutable
data class DetailsPrimaryLoadResult(
    val details: MovieDetails,
    val loadResponse: LoadResponse,
)

@Immutable
data class DetailsSecondaryLoadResult(
    val cast: List<MovieCast> = emptyList(),
    val similarMovies: MovieList = emptyList(),
    val currentSeason: Int? = null,
    val currentEpisode: Int? = null,
)

interface MovieRepository {
    fun getTrendingMovies(): Flow<MovieList>
    fun getTop10Movies(): Flow<MovieList>
    fun getNowPlayingMovies(): Flow<MovieList>
    suspend fun getPrimaryDetails(url: String, apiName: String): DetailsPrimaryLoadResult
    suspend fun getSecondaryDetails(url: String, apiName: String): DetailsSecondaryLoadResult
    suspend fun getDetails(url: String, apiName: String): MovieDetails {
        val primary = getPrimaryDetails(url, apiName)
        return primary.details.mergeSecondary(getSecondaryDetails(url, apiName))
    }
    suspend fun getMovieDetails(url: String, apiName: String): MovieDetails = getDetails(url, apiName)
    suspend fun getTvSeriesDetails(url: String, apiName: String): MovieDetails = getDetails(url, apiName)
    suspend fun getMediaDetails(url: String, apiName: String): MovieDetails = getDetails(url, apiName)
    suspend fun ensureMediaInFavorites(url: String, apiName: String)
    suspend fun setMediaFavorite(url: String, apiName: String, isFavorite: Boolean)
    suspend fun setMediaBookmarkStatus(url: String, apiName: String, status: WatchType)
}

fun MovieDetails.mergeSecondary(secondary: DetailsSecondaryLoadResult): MovieDetails {
    return copy(
        cast = secondary.cast,
        similarMovies = secondary.similarMovies,
        currentSeason = secondary.currentSeason ?: currentSeason,
        currentEpisode = secondary.currentEpisode ?: currentEpisode,
    )
}
