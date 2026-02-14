package com.lagradost.cloudstream3.tv.compat.home

import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.TvType

/**
 * Sealed class dla elementów media w grid view.
 * Kompatybilna z CloudStream SearchResponse.
 */
sealed class MediaItemCompat {
    abstract val id: String
    abstract val url: String
    abstract val apiName: String
    abstract val name: String
    abstract val posterUri: String
    abstract val type: TvType?
    abstract val score: Score?
    open val continueWatchingProgress: Float? = null
    open val continueWatchingRemainingMs: Long? = null
    open val continueWatchingSeason: Int? = null
    open val continueWatchingEpisode: Int? = null
    open val continueWatchingHasBackdrop: Boolean = false
    
    data class Movie(
        override val id: String,
        override val url: String,
        override val apiName: String,
        override val name: String,
        override val posterUri: String,
        override val type: TvType?,
        override val score: Score?,
        val year: Int? = null,
        override val continueWatchingProgress: Float? = null,
        override val continueWatchingRemainingMs: Long? = null,
        override val continueWatchingSeason: Int? = null,
        override val continueWatchingEpisode: Int? = null,
        override val continueWatchingHasBackdrop: Boolean = false,
    ) : MediaItemCompat()
    
    data class TvSeries(
        override val id: String,
        override val url: String,
        override val apiName: String,
        override val name: String,
        override val posterUri: String,
        override val type: TvType?,
        override val score: Score?,
        val year: Int? = null,
        val episodes: Int? = null,
        override val continueWatchingProgress: Float? = null,
        override val continueWatchingRemainingMs: Long? = null,
        override val continueWatchingSeason: Int? = null,
        override val continueWatchingEpisode: Int? = null,
        override val continueWatchingHasBackdrop: Boolean = false,
    ) : MediaItemCompat()
    
    data class Other(
        override val id: String,
        override val url: String,
        override val apiName: String,
        override val name: String,
        override val posterUri: String,
        override val type: TvType?,
        override val score: Score?,
        override val continueWatchingProgress: Float? = null,
        override val continueWatchingRemainingMs: Long? = null,
        override val continueWatchingSeason: Int? = null,
        override val continueWatchingEpisode: Int? = null,
        override val continueWatchingHasBackdrop: Boolean = false,
    ) : MediaItemCompat()
}

typealias MediaListCompat = List<MediaItemCompat>
