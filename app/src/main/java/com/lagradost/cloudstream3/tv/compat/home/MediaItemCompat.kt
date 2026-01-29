package com.lagradost.cloudstream3.tv.compat.home

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
    
    data class Movie(
        override val id: String,
        override val url: String,
        override val apiName: String,
        override val name: String,
        override val posterUri: String,
        override val type: TvType?,
        val year: Int? = null,
    ) : MediaItemCompat()
    
    data class TvSeries(
        override val id: String,
        override val url: String,
        override val apiName: String,
        override val name: String,
        override val posterUri: String,
        override val type: TvType?,
        val year: Int? = null,
        val episodes: Int? = null,
    ) : MediaItemCompat()
    
    data class Other(
        override val id: String,
        override val url: String,
        override val apiName: String,
        override val name: String,
        override val posterUri: String,
        override val type: TvType?,
    ) : MediaItemCompat()
}

typealias MediaListCompat = List<MediaItemCompat>
