package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import kotlin.math.max
import kotlin.math.min

internal const val DetailsEpisodeInitialWindowSize = 8
internal const val DetailsEpisodeHydrationBackBuffer = 2
internal const val DetailsEpisodeHydrationForwardBuffer = 4

internal fun resolveRequestedDetailEpisodes(
    episodes: List<TvEpisode>,
    visibleEpisodeIds: List<String>,
): List<TvEpisode> {
    val totalCount = episodes.size
    if (totalCount == 0) return emptyList()

    val episodeIndexById = episodes.mapIndexed { index, episode -> episode.id to index }.toMap()
    val visibleIndexes = visibleEpisodeIds
        .mapNotNull { episodeId -> episodeIndexById[episodeId] }
        .sorted()

    val hydrationRange = resolveDetailsEpisodeHydrationRange(
        totalCount = totalCount,
        visibleIndexes = visibleIndexes,
    ) ?: return emptyList()
    return episodes.subList(hydrationRange.first, hydrationRange.last + 1)
}

internal fun filterRequestedDetailEpisodes(
    episodes: List<TvEpisode>,
    visibleItemKeys: List<Any>,
    episodeDownloadStates: Map<String, DetailsDownloadButtonUiState>,
    episodeWatchedStates: Map<String, Boolean>,
    loadingEpisodeStateIds: Set<String>,
): List<TvEpisode> {
    val visibleEpisodeIds = visibleItemKeys.mapNotNull { key -> key as? String }
    return resolveRequestedDetailEpisodes(
        episodes = episodes,
        visibleEpisodeIds = visibleEpisodeIds,
    ).filterNot { episode ->
        episodeDownloadStates.containsKey(episode.id) && episodeWatchedStates.containsKey(episode.id)
    }.filterNot { episode ->
        loadingEpisodeStateIds.contains(episode.id)
    }
}

internal fun resolveDetailsEpisodeHydrationRange(
    totalCount: Int,
    visibleIndexes: List<Int>,
    initialWindowSize: Int = DetailsEpisodeInitialWindowSize,
    backBuffer: Int = DetailsEpisodeHydrationBackBuffer,
    forwardBuffer: Int = DetailsEpisodeHydrationForwardBuffer,
): IntRange? {
    if (totalCount <= 0) {
        return null
    }

    val lastIndex = totalCount - 1
    val startIndex = if (visibleIndexes.isEmpty()) {
        0
    } else {
        max(0, visibleIndexes.first() - backBuffer)
    }
    val endIndex = if (visibleIndexes.isEmpty()) {
        min(lastIndex, initialWindowSize - 1)
    } else {
        min(lastIndex, visibleIndexes.last() + forwardBuffer)
    }

    if (startIndex > endIndex) {
        return null
    }
    return startIndex..endIndex
}
