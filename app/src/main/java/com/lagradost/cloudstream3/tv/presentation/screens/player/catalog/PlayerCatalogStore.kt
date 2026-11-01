package com.lagradost.cloudstream3.tv.presentation.screens.player.catalog

import android.os.SystemClock
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSourceState
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSourceStatus
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.utils.AppContextUtils.sortSubs
import com.lagradost.cloudstream3.utils.ExtractorLink

internal class PlayerCatalogStore {
    private val loadedLinksByUrl = linkedMapOf<String, ExtractorLink>()
    private val loadedSubtitlesById = linkedMapOf<String, SubtitleData>()
    private val sourceStatesByUrl = linkedMapOf<String, TvPlayerSourceState>()

    var orderedLinks: List<ExtractorLink> = emptyList()
        private set

    var orderedSubtitles: List<SubtitleData> = emptyList()
        private set

    var currentLinkIndex: Int = -1
        private set

    /** Survives [reset]: it is captured on the old episode and used for the next one. */
    var preferredLink: PreferredLink? = null

    private var firstLinkAtElapsedMs: Long = 0L

    /** Set while the link that started playback was picked by [preferredLink] instead of the top. */
    var preferredAttemptUrl: String? = null
        private set

    /** A preferred link that already failed; the sequential fallback must not retry it. */
    var failedPreferredUrl: String? = null
        private set

    fun reset() {
        synchronized(loadedLinksByUrl) {
            loadedLinksByUrl.clear()
        }
        synchronized(loadedSubtitlesById) {
            loadedSubtitlesById.clear()
        }
        synchronized(sourceStatesByUrl) {
            sourceStatesByUrl.clear()
        }
        orderedLinks = emptyList()
        orderedSubtitles = emptyList()
        currentLinkIndex = -1
        preferredAttemptUrl = null
        failedPreferredUrl = null
        firstLinkAtElapsedMs = 0L
    }

    /** True while a preferred link is set, none of the loaded links matches it and [maxWaitMs] has not passed. */
    fun isWaitingForPreferredLink(maxWaitMs: Long): Boolean {
        val preferred = preferredLink ?: return false
        val hasMatch = synchronized(loadedLinksByUrl) {
            loadedLinksByUrl.values.any { link -> preferred.matchRank(link) != null }
        }
        if (hasMatch || firstLinkAtElapsedMs == 0L) return false
        return SystemClock.elapsedRealtime() - firstLinkAtElapsedMs < maxWaitMs
    }

    fun hasLoadedSources(): Boolean {
        return synchronized(loadedLinksByUrl) {
            loadedLinksByUrl.isNotEmpty()
        }
    }

    fun loadedSourcesCount(): Int {
        return synchronized(loadedLinksByUrl) {
            loadedLinksByUrl.size
        }
    }

    fun insertLink(link: ExtractorLink): Boolean {
        val inserted = synchronized(loadedLinksByUrl) {
            loadedLinksByUrl.putIfAbsent(link.url, link) == null
        }
        if (inserted) {
            if (firstLinkAtElapsedMs == 0L) firstLinkAtElapsedMs = SystemClock.elapsedRealtime()
            synchronized(sourceStatesByUrl) {
                sourceStatesByUrl.putIfAbsent(
                    link.url,
                    TvPlayerSourceState(status = TvPlayerSourceStatus.Loading),
                )
            }
        }
        return inserted
    }

    fun insertSubtitle(subtitle: SubtitleData): Boolean {
        return synchronized(loadedSubtitlesById) {
            loadedSubtitlesById.putIfAbsent(subtitle.getId(), subtitle) == null
        }
    }

    fun replaceEmbeddedSubtitles(subtitles: Iterable<SubtitleData>): Boolean {
        val normalizedSubtitles = linkedMapOf<String, SubtitleData>()
        subtitles.forEach { subtitle ->
            if (subtitle.origin == com.lagradost.cloudstream3.ui.player.SubtitleOrigin.EMBEDDED_IN_VIDEO) {
                normalizedSubtitles[subtitle.getId()] = subtitle
            }
        }

        return synchronized(loadedSubtitlesById) {
            val currentEmbeddedSubtitles = loadedSubtitlesById.entries
                .filter { (_, subtitle) ->
                    subtitle.origin == com.lagradost.cloudstream3.ui.player.SubtitleOrigin.EMBEDDED_IN_VIDEO
                }
                .associate { (id, subtitle) ->
                    id to subtitle
                }
            if (currentEmbeddedSubtitles == normalizedSubtitles) {
                return@synchronized false
            }

            val iterator = loadedSubtitlesById.entries.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().value.origin == com.lagradost.cloudstream3.ui.player.SubtitleOrigin.EMBEDDED_IN_VIDEO) {
                    iterator.remove()
                }
            }
            normalizedSubtitles.forEach { (id, subtitle) ->
                loadedSubtitlesById[id] = subtitle
            }
            true
        }
    }

    fun refreshOrderedSubtitles() {
        orderedSubtitles = synchronized(loadedSubtitlesById) {
            sortSubs(loadedSubtitlesById.values.toSet())
        }
    }

    fun rebuildOrderedData(currentUrl: String? = currentLink()?.url): Boolean {
        val candidateLinks = synchronized(loadedLinksByUrl) {
            loadedLinksByUrl.values.sortedWith(PlayerCatalogLinkComparator)
        }
        if (candidateLinks.isEmpty()) {
            return false
        }

        ensureSourceStateEntries(candidateLinks)
        orderedLinks = candidateLinks
        refreshOrderedSubtitles()
        currentLinkIndex = resolveCurrentIndex(currentUrl)
        return true
    }

    fun snapshotSourceStates(): Map<String, TvPlayerSourceState> {
        return synchronized(sourceStatesByUrl) {
            sourceStatesByUrl.toMap()
        }
    }

    fun updateSourceState(
        url: String,
        state: TvPlayerSourceState,
    ): Boolean {
        return synchronized(sourceStatesByUrl) {
            val currentState = sourceStatesByUrl[url]
            if (currentState == state) {
                false
            } else {
                sourceStatesByUrl[url] = state
                true
            }
        }
    }

    fun setCurrentLinkIndex(index: Int) {
        currentLinkIndex = index
    }

    fun currentLink(): ExtractorLink? {
        return orderedLinks.getOrNull(currentLinkIndex)
    }

    fun linkAt(index: Int): ExtractorLink? {
        return orderedLinks.getOrNull(index)
    }

    fun indexOfUrl(url: String): Int {
        return orderedLinks.indexOfFirst { link ->
            link.url == url
        }
    }

    /**
     * The list order never changes (best quality on top). Playback starts from the link matching
     * [preferredLink] when the list has one, otherwise from the top.
     */
    fun markInitialLinkLoading(): ExtractorLink? {
        val preferred = preferredLink
        // Best rank wins; ties keep list order. Index 0 means "already on top": nothing special.
        val preferredIndex = preferred
            ?.let { wanted ->
                orderedLinks.withIndex()
                    .mapNotNull { (index, link) -> wanted.matchRank(link)?.let { rank -> rank to index } }
                    .minWithOrNull(compareBy({ it.first }, { it.second }))
                    ?.second
            }
            ?.takeIf { index -> index > 0 }
        val index = preferredIndex ?: 0
        val link = orderedLinks.getOrNull(index) ?: return null
        updateSourceState(
            url = link.url,
            state = TvPlayerSourceState(status = TvPlayerSourceStatus.Loading),
        )
        currentLinkIndex = index
        preferredAttemptUrl = link.url.takeIf { preferredIndex != null }
        return link
    }

    /** Index to try after [failedUrl] failed: the top of the list if a preferred link was tried. */
    fun nextIndexAfterFailure(failedUrl: String?): Int {
        if (failedUrl != null && failedUrl == preferredAttemptUrl) {
            failedPreferredUrl = failedUrl
            preferredAttemptUrl = null
            return 0
        }
        var next = currentLinkIndex + 1
        if (linkAt(next)?.url == failedPreferredUrl) next += 1
        return next
    }

    private fun ensureSourceStateEntries(links: List<ExtractorLink>) {
        synchronized(sourceStatesByUrl) {
            links.forEach { link ->
                sourceStatesByUrl.putIfAbsent(
                    link.url,
                    TvPlayerSourceState(status = TvPlayerSourceStatus.Loading),
                )
            }
        }
    }

    private fun resolveCurrentIndex(currentUrl: String?): Int {
        return currentUrl
            ?.let(::indexOfUrl)
            ?.takeIf { index -> index >= 0 }
            ?: currentLinkIndex.takeIf { index -> index in orderedLinks.indices }
            ?: 0
    }
}

private val PlayerCatalogLinkComparator =
    compareByDescending<ExtractorLink> { link ->
        if (link.quality > 0) link.quality else 0
    }.thenBy { link ->
        link.isM3u8
    }
