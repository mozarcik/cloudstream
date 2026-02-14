package com.lagradost.cloudstream3.tv.compat.home

import com.lagradost.cloudstream3.AnimeSearchResponse
import com.lagradost.cloudstream3.LiveSearchResponse
import com.lagradost.cloudstream3.MovieSearchResponse
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TorrentSearchResponse
import com.lagradost.cloudstream3.TvSeriesSearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.utils.DataStoreHelper.ResumeWatchingResult
import com.lagradost.cloudstream3.utils.DataStoreHelper.fixVisual

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
            is ResumeWatchingResult -> {
                this.toContinueWatchingMediaItem(generatedId)
            }

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
                        score = this.score,
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
                        score = this.score,
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
                    score = this.score,
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
                    score = this.score,
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
                    type = this.type ?: TvType.Live,
                    score = this.score
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
                    type = this.type ?: TvType.Torrent,
                    score = this.score
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
                        score = this.score,
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
                        type = mediaType,
                        score = this.score
                    )
                }
            }
        }
    }

    private fun ResumeWatchingResult.toContinueWatchingMediaItem(generatedId: String): MediaItemCompat {
        val mediaType = this.type ?: TvType.Movie
        val hasBackdrop = !this.backdropUrl.isNullOrBlank()
        val artworkUri = this.backdropUrl
            ?.takeIf { it.isNotBlank() }
            ?: this.posterUrl?.takeIf { it.isNotBlank() }
            .orEmpty()
        val inferredSeriesType = this.inferSeriesTypeFromUrl()
        val hasSeriesResumeMarker = (season ?: 0) > 0 || (episode ?: 0) > 0
        val shouldTreatAsSeries =
            mediaType.isEpisodeBased() || inferredSeriesType != null || hasSeriesResumeMarker
        val normalizedWatchPos = watchPos?.fixVisual()
        val progress = normalizedWatchPos?.let { posDur ->
            if (posDur.duration <= 0L) null else (posDur.position.toFloat() / posDur.duration.toFloat()).coerceIn(0f, 1f)
        }
        val remainingMs = normalizedWatchPos?.let { posDur ->
            (posDur.duration - posDur.position).coerceAtLeast(0L)
        }

        if (shouldTreatAsSeries) {
            return MediaItemCompat.TvSeries(
                id = generatedId,
                posterUri = artworkUri,
                name = this.name,
                url = this.url,
                apiName = this.apiName,
                type = if (mediaType.isEpisodeBased()) mediaType else inferredSeriesType ?: TvType.TvSeries,
                score = this.score,
                episodes = null,
                continueWatchingProgress = progress,
                continueWatchingRemainingMs = remainingMs,
                continueWatchingSeason = this.season,
                continueWatchingEpisode = this.episode,
                continueWatchingHasBackdrop = hasBackdrop
            )
        }

        if (mediaType == TvType.Movie) {
            return MediaItemCompat.Movie(
                id = generatedId,
                posterUri = artworkUri,
                name = this.name,
                url = this.url,
                apiName = this.apiName,
                type = mediaType,
                score = this.score,
                continueWatchingProgress = progress,
                continueWatchingRemainingMs = remainingMs,
                continueWatchingSeason = this.season,
                continueWatchingEpisode = this.episode,
                continueWatchingHasBackdrop = hasBackdrop
            )
        }

        return MediaItemCompat.Other(
            id = generatedId,
            posterUri = artworkUri,
            name = this.name,
            url = this.url,
            apiName = this.apiName,
            type = mediaType,
            score = this.score,
            continueWatchingProgress = progress,
            continueWatchingRemainingMs = remainingMs,
            continueWatchingSeason = this.season,
            continueWatchingEpisode = this.episode,
            continueWatchingHasBackdrop = hasBackdrop
        )
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
