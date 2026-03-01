package com.lagradost.cloudstream3.tv.presentation.screens.player.catalog

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

    fun insertSubtitles(subtitles: Iterable<SubtitleData>): Boolean {
        var insertedAny = false
        synchronized(loadedSubtitlesById) {
            subtitles.forEach { subtitle ->
                if (loadedSubtitlesById.putIfAbsent(subtitle.getId(), subtitle) == null) {
                    insertedAny = true
                }
            }
        }
        return insertedAny
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

    fun markFirstLinkLoading(): ExtractorLink? {
        val firstLink = orderedLinks.firstOrNull() ?: return null
        updateSourceState(
            url = firstLink.url,
            state = TvPlayerSourceState(status = TvPlayerSourceStatus.Loading),
        )
        currentLinkIndex = 0
        return firstLink
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
