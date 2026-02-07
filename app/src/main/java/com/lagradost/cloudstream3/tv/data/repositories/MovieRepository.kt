package com.lagradost.cloudstream3.tv.data.repositories

import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.MovieList
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun getTrendingMovies(): Flow<MovieList>
    fun getTop10Movies(): Flow<MovieList>
    fun getNowPlayingMovies(): Flow<MovieList>
    suspend fun getMovieDetails(url: String, apiName: String): MovieDetails
    suspend fun getTvSeriesDetails(url: String, apiName: String): MovieDetails
    suspend fun getMediaDetails(url: String, apiName: String): MovieDetails
}
