package com.lagradost.cloudstream3.tv.presentation.screens.player

import com.lagradost.cloudstream3.ui.result.ResultEpisode

private const val NextEpisodePrefetchThresholdPercent = 85L

internal fun resolveNextEpisodeIndex(
    currentEpisodeIndex: Int,
    totalEpisodes: Int,
): Int? {
    if (currentEpisodeIndex < 0 || currentEpisodeIndex >= totalEpisodes - 1) {
        return null
    }
    return currentEpisodeIndex + 1
}

internal fun resolveEpisodeMetadata(
    baseMetadata: TvPlayerMetadata,
    episode: ResultEpisode?,
): TvPlayerMetadata {
    val resolvedSeason = episode?.season?.takeIf { season -> season > 0 }
    val resolvedEpisode = episode?.episode?.takeIf { episodeNumber -> episodeNumber > 0 }
    val resolvedEpisodeTitle = episode?.name
        ?.takeIf { name ->
            name.isNotBlank() && !name.equals(baseMetadata.title, ignoreCase = true)
        }

    return baseMetadata.copy(
        season = resolvedSeason,
        episode = resolvedEpisode,
        episodeTitle = resolvedEpisodeTitle,
    )
}

internal fun shouldPrefetchNextEpisode(
    positionMs: Long,
    durationMs: Long,
): Boolean {
    if (durationMs <= 0L) return false
    return positionMs.coerceAtLeast(0L) * 100L / durationMs >= NextEpisodePrefetchThresholdPercent
}
