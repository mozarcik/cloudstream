package com.lagradost.cloudstream3.tv.compat

import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope

class ResolvedTrailer(
    val sourceUrl: String,
    val links: List<ExtractorLink>,
)

/** Same resolution the old UI does (ResultViewModel2.getTrailers): up to [MaxTrailers], extracted three at a time. */
object TrailerResolver {
    private const val MaxTrailers = 3

    suspend fun resolve(loadResponse: LoadResponse): List<ResolvedTrailer> = coroutineScope {
        val resolved = ArrayList<ResolvedTrailer>()
        for (batch in loadResponse.trailers.chunked(MaxTrailers)) {
            batch.amap { trailer ->
                try {
                    val links = ArrayList<ExtractorLink>()
                    val extracted = loadExtractor(trailer.extractorUrl, trailer.referer, {}, { links.add(it) })
                    if (!extracted && trailer.raw) {
                        links.add(
                            newExtractorLink("", "Trailer", trailer.extractorUrl, type = INFER_TYPE) {
                                this.referer = trailer.referer ?: ""
                                this.quality = Qualities.Unknown.value
                                this.headers = trailer.headers
                            }
                        )
                    }
                    ResolvedTrailer(trailer.extractorUrl, links).takeIf { links.isNotEmpty() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    logError(e)
                    null
                }
            }.filterNotNull().let(resolved::addAll)
            if (resolved.size >= MaxTrailers) break
        }
        resolved.take(MaxTrailers)
    }
}

/**
 * Hands resolved trailer links to the TV player, which is started through navigation args (strings only).
 * Holds the last details page that resolved trailers.
 */
object TrailerPlaybackStore {
    class Entry(val title: String, val trailers: List<ResolvedTrailer>)

    @Volatile
    private var current: Pair<String, Entry>? = null

    fun put(detailsUrl: String, title: String, trailers: List<ResolvedTrailer>) {
        current = detailsUrl to Entry(title, trailers)
    }

    fun get(detailsUrl: String): Entry? = current?.takeIf { it.first == detailsUrl }?.second
}
