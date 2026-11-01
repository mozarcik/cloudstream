package com.lagradost.cloudstream3.tv.presentation.screens.player.state

import com.lagradost.cloudstream3.ui.player.ExtractorUri
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.player.VideoGenerator
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType

/**
 * Upstream generators are stateless (the index is passed on every call), the TV player keeps
 * track of the current episode itself, so this small wrapper holds the index.
 */
internal class TvPlaybackGenerator(
    private val delegate: VideoGenerator<*>,
    var index: Int = 0,
) {
    val hasCache: Boolean get() = delegate.hasCache

    fun hasNext(): Boolean = delegate.hasNext(index)

    fun goto(newIndex: Int) {
        index = newIndex.coerceIn(0, maxOf(0, delegate.videos.lastIndex))
    }

    suspend fun generateLinks(
        clearCache: Boolean,
        sourceTypes: Set<ExtractorLinkType>,
        callback: (Pair<ExtractorLink?, ExtractorUri?>) -> Unit,
        subtitleCallback: (SubtitleData) -> Unit,
        offset: Int = 0,
        isCasting: Boolean = false,
    ): Boolean = delegate.generateLinks(
        clearCache = clearCache,
        sourceTypes = sourceTypes,
        callback = callback,
        subtitleCallback = subtitleCallback,
        offset = index + offset,
        isCasting = isCasting,
    )
}
