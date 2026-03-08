package com.lagradost.cloudstream3.tv.compat

import com.lagradost.cloudstream3.ui.result.LinkLoadingResult
import com.lagradost.cloudstream3.utils.ExtractorLinkType

internal class MovieDetailsLinksCache {
    internal data class Key(
        val episodeId: Int,
        val sourceTypes: Set<ExtractorLinkType>,
        val isCasting: Boolean,
    )

    private val cachedLinks = mutableMapOf<Key, LinkLoadingResult>()

    fun get(key: Key): LinkLoadingResult? {
        return synchronized(cachedLinks) { cachedLinks[key] }
    }

    fun put(
        key: Key,
        result: LinkLoadingResult,
    ) {
        synchronized(cachedLinks) {
            cachedLinks[key] = result
        }
    }

    fun remove(key: Key) {
        synchronized(cachedLinks) {
            cachedLinks.remove(key)
        }
    }
}
