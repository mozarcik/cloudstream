package com.lagradost.cloudstream3.tv.data.mappers

import android.net.Uri
import com.lagradost.cloudstream3.tv.compat.ResolvedTrailer
import com.lagradost.cloudstream3.tv.data.entities.MovieTrailer

internal fun List<ResolvedTrailer>.toMovieTrailers(): List<MovieTrailer> = mapIndexed { index, trailer ->
    val best = trailer.links.maxBy { it.quality }
    MovieTrailer(
        index = index,
        host = runCatching { Uri.parse(trailer.sourceUrl).host }.getOrNull()
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
            ?: trailer.sourceUrl,
        qualityLabel = if (best.quality > 0) "${best.quality}p" else "",
        isHls = best.isM3u8,
    )
}
