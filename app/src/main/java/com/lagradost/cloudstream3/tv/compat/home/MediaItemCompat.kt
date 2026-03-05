package com.lagradost.cloudstream3.tv.compat.home

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.TvType
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Sealed class dla elementów media w grid view.
 * Kompatybilna z CloudStream SearchResponse.
 */
@Immutable
sealed class MediaItemCompat {
    abstract val id: String
    abstract val url: String
    abstract val apiName: String
    abstract val gridKeySeed: Long
    abstract val name: String
    abstract val posterUri: String
    abstract val type: TvType?
    abstract val score: Score?
    open val backdropUri: String? = null
    open val description: String? = null
    open val continueWatchingProgress: Float? = null
    open val continueWatchingRemainingMs: Long? = null
    open val continueWatchingSeason: Int? = null
    open val continueWatchingEpisode: Int? = null
    open val continueWatchingHasBackdrop: Boolean = false
    open val continueWatchingParentId: Int? = null
    
    @Immutable
    data class Movie(
        override val id: String,
        override val url: String,
        override val apiName: String,
        override val gridKeySeed: Long = computeMediaGridKeySeed(
            apiName = apiName,
            id = id,
            url = url
        ),
        override val name: String,
        override val posterUri: String,
        override val type: TvType?,
        override val score: Score?,
        override val backdropUri: String? = null,
        override val description: String? = null,
        val year: Int? = null,
        override val continueWatchingProgress: Float? = null,
        override val continueWatchingRemainingMs: Long? = null,
        override val continueWatchingSeason: Int? = null,
        override val continueWatchingEpisode: Int? = null,
        override val continueWatchingHasBackdrop: Boolean = false,
        override val continueWatchingParentId: Int? = null,
    ) : MediaItemCompat()
    
    @Immutable
    data class TvSeries(
        override val id: String,
        override val url: String,
        override val apiName: String,
        override val gridKeySeed: Long = computeMediaGridKeySeed(
            apiName = apiName,
            id = id,
            url = url
        ),
        override val name: String,
        override val posterUri: String,
        override val type: TvType?,
        override val score: Score?,
        override val backdropUri: String? = null,
        override val description: String? = null,
        val year: Int? = null,
        val episodes: Int? = null,
        override val continueWatchingProgress: Float? = null,
        override val continueWatchingRemainingMs: Long? = null,
        override val continueWatchingSeason: Int? = null,
        override val continueWatchingEpisode: Int? = null,
        override val continueWatchingHasBackdrop: Boolean = false,
        override val continueWatchingParentId: Int? = null,
    ) : MediaItemCompat()
    
    @Immutable
    data class Other(
        override val id: String,
        override val url: String,
        override val apiName: String,
        override val gridKeySeed: Long = computeMediaGridKeySeed(
            apiName = apiName,
            id = id,
            url = url
        ),
        override val name: String,
        override val posterUri: String,
        override val type: TvType?,
        override val score: Score?,
        override val backdropUri: String? = null,
        override val description: String? = null,
        override val continueWatchingProgress: Float? = null,
        override val continueWatchingRemainingMs: Long? = null,
        override val continueWatchingSeason: Int? = null,
        override val continueWatchingEpisode: Int? = null,
        override val continueWatchingHasBackdrop: Boolean = false,
        override val continueWatchingParentId: Int? = null,
    ) : MediaItemCompat()
}

typealias MediaListCompat = List<MediaItemCompat>

private fun computeMediaGridKeySeed(
    apiName: String,
    id: String,
    url: String,
): Long {
    var key = 1_469_598_103_934_665_603L
    key = (31L * key) + apiName.hashCode().toLong()
    key = (31L * key) + id.hashCode().toLong()
    key = (31L * key) + url.hashCode().toLong()
    return key
}

fun MediaItemCompat.ratingOrNull(maxScore: Int = 10): Float? {
    val ratingValue = score?.toDouble(maxScore = maxScore) ?: return null
    if (ratingValue < 1.0) {
        return null
    }
    return ratingValue.toFloat()
}

fun MediaItemCompat.ratingLabelOrNull(maxScore: Int = 10): String? {
    val rating = ratingOrNull(maxScore = maxScore) ?: return null
    val rounded = (rating * 10f).roundToInt()
    val integerPart = rounded / 10
    val decimalPart = abs(rounded % 10)
    return "$integerPart.$decimalPart"
}
