package com.lagradost.cloudstream3.tv.compat

import com.lagradost.cloudstream3.sortUrls
import com.lagradost.cloudstream3.ui.result.LinkLoadingResult
import com.lagradost.cloudstream3.utils.AppContextUtils.sortSubs

internal class MovieDetailsLinksBatchPublisher(
    private val callback: ((MovieDetailsLinksBatchUpdate) -> Unit)?,
) {
    private companion object {
        const val DownloadUiBatchSize = 10
    }

    private var lastPublishedBatchCount = 0

    fun emit(
        links: Set<com.lagradost.cloudstream3.utils.ExtractorLink>,
        subtitles: Set<com.lagradost.cloudstream3.ui.player.SubtitleData>,
        isCompleted: Boolean,
    ) {
        val resolvedCallback = callback ?: return
        val loadedCount = links.size

        val shouldEmit = when {
            isCompleted -> loadedCount != lastPublishedBatchCount || loadedCount == 0
            loadedCount == 1 && lastPublishedBatchCount == 0 -> true
            loadedCount - lastPublishedBatchCount >= DownloadUiBatchSize -> true
            else -> false
        }
        if (!shouldEmit) {
            return
        }

        lastPublishedBatchCount = loadedCount
        resolvedCallback(
            MovieDetailsLinksBatchUpdate(
                loadedCount = loadedCount,
                links = sortUrls(links),
                subtitles = sortSubs(subtitles),
                isCompleted = isCompleted,
            )
        )
    }

    fun emitCompleted(result: LinkLoadingResult) {
        emit(
            links = LinkedHashSet(result.links),
            subtitles = LinkedHashSet(result.subs),
            isCompleted = true,
        )
    }
}
