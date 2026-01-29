package com.lagradost.cloudstream3.tv.compat.home

import com.lagradost.cloudstream3.AnimeSearchResponse
import com.lagradost.cloudstream3.LiveSearchResponse
import com.lagradost.cloudstream3.MovieSearchResponse
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TorrentSearchResponse
import com.lagradost.cloudstream3.TvSeriesSearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.isEpisodeBased

/**
 * Mapper object for converting SearchResponse types to MediaItemCompat
 */
object SearchResponseMapper {
    
    /**
     * Extension function to convert any SearchResponse to MediaItemCompat
     * Generates a unique ID based on URL and API name
     */
    fun SearchResponse.toMediaItemCompat(): MediaItemCompat {
        val generatedId = "${this.apiName}_${this.url.hashCode()}"
        
        return when (this) {
            is MovieSearchResponse -> {
                val mediaType = this.type ?: TvType.Movie
                val inferredSeriesType = this.inferSeriesTypeFromUrl()
                val shouldTreatAsSeries = mediaType.isEpisodeBased() || inferredSeriesType != null

                if (shouldTreatAsSeries) {
                    MediaItemCompat.TvSeries(
                        id = generatedId,
                        posterUri = this.posterUrl ?: "",
                        name = this.name,
                        url = this.url,
                        apiName = this.apiName,
                        type = if (mediaType.isEpisodeBased()) mediaType else inferredSeriesType ?: TvType.TvSeries,
                        year = this.year,
                        episodes = null
                    )
                } else {
                    MediaItemCompat.Movie(
                        id = generatedId,
                        posterUri = this.posterUrl ?: "",
                        name = this.name,
                        url = this.url,
                        apiName = this.apiName,
                        type = mediaType,
                        year = this.year
                    )
                }
            }
            
            is TvSeriesSearchResponse -> {
                MediaItemCompat.TvSeries(
                    id = generatedId,
                    posterUri = this.posterUrl ?: "",
                    name = this.name,
                    url = this.url,
                    apiName = this.apiName,
                    type = this.type ?: TvType.TvSeries,
                    year = this.year,
                    episodes = this.episodes
                )
            }
            
            is AnimeSearchResponse -> {
                // For anime, episodes is a map of DubStatus to episode count
                // We'll use the total count (sum of all dub statuses)
                val totalEpisodes = this.episodes.values.maxOrNull()
                
                MediaItemCompat.TvSeries(
                    id = generatedId,
                    posterUri = this.posterUrl ?: "",
                    name = this.name,
                    url = this.url,
                    apiName = this.apiName,
                    type = this.type ?: TvType.Anime,
                    year = this.year,
                    episodes = totalEpisodes
                )
            }
            
            is LiveSearchResponse -> {
                // Live streams as "Other" type
                MediaItemCompat.Other(
                    id = generatedId,
                    posterUri = this.posterUrl ?: "",
                    name = this.name,
                    url = this.url,
                    apiName = this.apiName,
                    type = this.type ?: TvType.Live
                )
            }
            
            is TorrentSearchResponse -> {
                // Torrents as "Other" type
                MediaItemCompat.Other(
                    id = generatedId,
                    posterUri = this.posterUrl ?: "",
                    name = this.name,
                    url = this.url,
                    apiName = this.apiName,
                    type = this.type ?: TvType.Torrent
                )
            }
            
            else -> {
                val mediaType = this.type ?: TvType.Others
                val inferredSeriesType = this.inferSeriesTypeFromUrl()
                val shouldTreatAsSeries = mediaType.isEpisodeBased() || inferredSeriesType != null

                if (shouldTreatAsSeries) {
                    MediaItemCompat.TvSeries(
                        id = generatedId,
                        posterUri = this.posterUrl ?: "",
                        name = this.name,
                        url = this.url,
                        apiName = this.apiName,
                        type = if (mediaType.isEpisodeBased()) mediaType else inferredSeriesType ?: TvType.TvSeries,
                        episodes = null
                    )
                } else {
                    // Fallback for unknown types
                    MediaItemCompat.Other(
                        id = generatedId,
                        posterUri = this.posterUrl ?: "",
                        name = this.name,
                        url = this.url,
                        apiName = this.apiName,
                        type = mediaType
                    )
                }
            }
        }
    }

    private fun SearchResponse.inferSeriesTypeFromUrl(): TvType? {
        val normalizedUrl = this.url.lowercase()

        return when {
            "\"type\":\"tv\"" in normalizedUrl -> TvType.TvSeries
            "\"type\":\"tvseries\"" in normalizedUrl -> TvType.TvSeries
            "\"type\":\"series\"" in normalizedUrl -> TvType.TvSeries
            "\"type\":\"anime\"" in normalizedUrl -> TvType.Anime
            "\"type\":\"ova\"" in normalizedUrl -> TvType.OVA
            else -> null
        }
    }
}
