package com.lagradost.cloudstream3.tv.compat.resume

import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.ui.result.VideoWatchState
import com.lagradost.cloudstream3.ui.result.getId
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.VideoDownloadHelper

private const val SeriesSeasonEpisodeIdOffset = 100_000
private const val AnimeDubEpisodeIdOffset = 1_000_000
private const val AnimeSeasonEpisodeIdOffset = 10_000
private const val EpisodeIdOffset = 1

internal data class ResolvedAnimePlaybackEpisode(
    val dubStatus: DubStatus?,
    val episode: Episode,
)

internal object ResumePlaybackResolver {
    fun resolveContext(
        parentId: Int?,
        resumeProvider: (Int?) -> VideoDownloadHelper.ResumeWatching? = DataStoreHelper::getLastWatched,
    ): ResumePlaybackContext? {
        val resume = resumeProvider(parentId) ?: return null
        if (resume.parentId != parentId) return null

        return ResumePlaybackContext(
            parentId = resume.parentId,
            episodeId = resume.episodeId,
            season = resume.season,
            episode = resume.episode,
            isFromDownload = resume.isFromDownload,
        )
    }

    fun resolveCurrentSeriesEpisode(
        response: TvSeriesLoadResponse,
        resumeProvider: (Int?) -> VideoDownloadHelper.ResumeWatching? = DataStoreHelper::getLastWatched,
        watchStateProvider: (Int?) -> VideoWatchState? = DataStoreHelper::getVideoWatchState,
    ): Episode? {
        val parentId = response.getId()
        return resolveCurrentSeriesEpisode(
            episodes = response.episodes,
            parentId = parentId,
            resumeContext = resolveContext(parentId, resumeProvider),
            watchStateProvider = watchStateProvider,
        )
    }

    fun resolveCurrentSeriesEpisode(
        episodes: List<Episode>,
        parentId: Int,
        resumeContext: ResumePlaybackContext?,
        watchStateProvider: (Int?) -> VideoWatchState? = DataStoreHelper::getVideoWatchState,
    ): Episode? {
        val sortedEpisodes = episodes.toSortedEpisodeEntries()
        if (sortedEpisodes.isEmpty()) return null

        return findSeriesEpisodeByResumeContext(
            episodes = sortedEpisodes,
            parentId = parentId,
            resumeContext = resumeContext,
        ) ?: findNextUnwatchedSeriesEpisode(
            episodes = sortedEpisodes,
            parentId = parentId,
            watchStateProvider = watchStateProvider,
        ) ?: sortedEpisodes.firstOrNull()?.episode
    }

    fun resolveCurrentAnimeEpisode(
        response: AnimeLoadResponse,
        resumeProvider: (Int?) -> VideoDownloadHelper.ResumeWatching? = DataStoreHelper::getLastWatched,
        watchStateProvider: (Int?) -> VideoWatchState? = DataStoreHelper::getVideoWatchState,
        preferredDubProvider: (Int) -> DubStatus? = DataStoreHelper::getDub,
    ): ResolvedAnimePlaybackEpisode? {
        val parentId = response.getId()
        return resolveCurrentAnimeEpisode(
            episodesByDub = response.episodes,
            parentId = parentId,
            preferredDubStatus = preferredDubProvider(parentId),
            resumeContext = resolveContext(parentId, resumeProvider),
            watchStateProvider = watchStateProvider,
        )
    }

    fun resolveCurrentAnimeEpisode(
        episodesByDub: Map<DubStatus, List<Episode>>,
        parentId: Int,
        preferredDubStatus: DubStatus?,
        resumeContext: ResumePlaybackContext?,
        watchStateProvider: (Int?) -> VideoWatchState? = DataStoreHelper::getVideoWatchState,
    ): ResolvedAnimePlaybackEpisode? {
        findAnimeEpisodeByResumeContext(
            episodesByDub = episodesByDub,
            parentId = parentId,
            resumeContext = resumeContext,
        )?.let { resolvedEpisode ->
            return resolvedEpisode
        }

        val selectedDubStatus = resolvePreferredAnimeDubStatus(
            episodesByDub = episodesByDub,
            preferredDubStatus = preferredDubStatus,
        )
        val fallbackEntry = episodesByDub.entries.firstOrNull { entry -> entry.value.isNotEmpty() }
        val selectedEntry = selectedDubStatus
            ?.let { dubStatus -> dubStatus to episodesByDub[dubStatus].orEmpty() }
            ?.takeIf { (_, episodes) -> episodes.isNotEmpty() }
            ?: fallbackEntry?.let { entry -> entry.key to entry.value }
        val selectedEpisodes = selectedEntry?.second.orEmpty()
        val fallbackDubStatus = selectedEntry?.first
        val sortedEpisodes = selectedEpisodes.toSortedEpisodeEntries()
        if (sortedEpisodes.isEmpty()) return null

        val resolvedEpisode = findEpisodeBySeasonAndEpisode(
            episodes = sortedEpisodes,
            resumeContext = resumeContext,
        ) ?: findNextUnwatchedAnimeEpisode(
            episodes = sortedEpisodes,
            parentId = parentId,
            dubStatus = fallbackDubStatus,
            watchStateProvider = watchStateProvider,
        ) ?: sortedEpisodes.firstOrNull()?.episode

        return resolvedEpisode?.let { episode ->
            ResolvedAnimePlaybackEpisode(
                dubStatus = fallbackDubStatus,
                episode = episode,
            )
        }
    }

    fun findSeriesEpisodeById(
        episodes: List<Episode>,
        parentId: Int,
        episodeId: Int?,
    ): Episode? {
        val targetEpisodeId = episodeId ?: return null
        return episodes.toSortedEpisodeEntries().firstOrNull { episode ->
            resolveSeriesPlaybackEpisodeId(
                parentId = parentId,
                seasonNumber = episode.seasonNumber,
                episodeNumber = episode.episodeNumber,
            ) == targetEpisodeId
        }?.episode
    }

    fun findAnimeEpisodeById(
        episodesByDub: Map<DubStatus, List<Episode>>,
        parentId: Int,
        episodeId: Int?,
    ): ResolvedAnimePlaybackEpisode? {
        val targetEpisodeId = episodeId ?: return null

        episodesByDub.forEach { (dubStatus, episodes) ->
            val matchingEpisode = episodes.toSortedEpisodeEntries().firstOrNull { episode ->
                resolveAnimePlaybackEpisodeId(
                    parentId = parentId,
                    seasonNumber = episode.seasonNumber,
                    episodeNumber = episode.episodeNumber,
                    dubStatus = dubStatus,
                ) == targetEpisodeId
            }?.episode
            if (matchingEpisode != null) {
                return ResolvedAnimePlaybackEpisode(
                    dubStatus = dubStatus,
                    episode = matchingEpisode,
                )
            }
        }

        return null
    }

    private fun findSeriesEpisodeByResumeContext(
        episodes: List<SortedEpisodeEntry>,
        parentId: Int,
        resumeContext: ResumePlaybackContext?,
    ): Episode? {
        val resume = resumeContext ?: return null
        if (!resume.isFromDownload) {
            findSeriesEpisodeById(
                episodes = episodes.map(SortedEpisodeEntry::episode),
                parentId = parentId,
                episodeId = resume.episodeId,
            )?.let { return it }
        }

        return findEpisodeBySeasonAndEpisode(
            episodes = episodes,
            resumeContext = resume,
        )
    }

    private fun findAnimeEpisodeByResumeContext(
        episodesByDub: Map<DubStatus, List<Episode>>,
        parentId: Int,
        resumeContext: ResumePlaybackContext?,
    ): ResolvedAnimePlaybackEpisode? {
        val resume = resumeContext ?: return null
        if (!resume.isFromDownload) {
            findAnimeEpisodeById(
                episodesByDub = episodesByDub,
                parentId = parentId,
                episodeId = resume.episodeId,
            )?.let { return it }
        }

        episodesByDub.forEach { (dubStatus, episodes) ->
            val matchingEpisode = findEpisodeBySeasonAndEpisode(
                episodes = episodes.toSortedEpisodeEntries(),
                resumeContext = resume,
            )
            if (matchingEpisode != null) {
                return ResolvedAnimePlaybackEpisode(
                    dubStatus = dubStatus,
                    episode = matchingEpisode,
                )
            }
        }

        return null
    }

    private fun findNextUnwatchedSeriesEpisode(
        episodes: List<SortedEpisodeEntry>,
        parentId: Int,
        watchStateProvider: (Int?) -> VideoWatchState?,
    ): Episode? {
        val watchedFlags = episodes.map { episode ->
            watchStateProvider(
                resolveSeriesPlaybackEpisodeId(
                    parentId = parentId,
                    seasonNumber = episode.seasonNumber,
                    episodeNumber = episode.episodeNumber,
                )
            ) == VideoWatchState.Watched
        }
        val lastWatchedIndex = watchedFlags.indexOfLast { watched -> watched }

        return episodes.getOrNull(lastWatchedIndex + 1)?.episode
    }

    private fun findNextUnwatchedAnimeEpisode(
        episodes: List<SortedEpisodeEntry>,
        parentId: Int,
        dubStatus: DubStatus?,
        watchStateProvider: (Int?) -> VideoWatchState?,
    ): Episode? {
        val watchedFlags = episodes.map { episode ->
            watchStateProvider(
                resolveAnimePlaybackEpisodeId(
                    parentId = parentId,
                    seasonNumber = episode.seasonNumber,
                    episodeNumber = episode.episodeNumber,
                    dubStatus = dubStatus,
                )
            ) == VideoWatchState.Watched
        }
        val lastWatchedIndex = watchedFlags.indexOfLast { watched -> watched }

        return episodes.getOrNull(lastWatchedIndex + 1)?.episode
    }

    private fun findEpisodeBySeasonAndEpisode(
        episodes: List<SortedEpisodeEntry>,
        resumeContext: ResumePlaybackContext?,
    ): Episode? {
        val resume = resumeContext ?: return null
        val resumeEpisode = resume.episode?.takeIf { episode -> episode > 0 } ?: return null
        val resumeSeason = resume.season?.takeIf { season -> season > 0 }

        return episodes.firstOrNull { episode ->
            episode.episodeNumber == resumeEpisode &&
                (resumeSeason == null || episode.seasonNumber == resumeSeason)
        }?.episode
    }

    private fun resolvePreferredAnimeDubStatus(
        episodesByDub: Map<DubStatus, List<Episode>>,
        preferredDubStatus: DubStatus?,
    ): DubStatus? {
        val available = episodesByDub.keys
        if (available.isEmpty()) return null

        if (preferredDubStatus != null && available.contains(preferredDubStatus)) {
            val preferredEpisodes = episodesByDub[preferredDubStatus].orEmpty()
            if (preferredEpisodes.isNotEmpty()) {
                return preferredDubStatus
            }
        }
        if (available.contains(DubStatus.Dubbed)) {
            return DubStatus.Dubbed
        }

        return episodesByDub.entries.firstOrNull { entry -> entry.value.isNotEmpty() }?.key
    }
}

internal fun resolveSeriesPlaybackEpisodeId(
    parentId: Int,
    seasonNumber: Int?,
    episodeNumber: Int,
): Int {
    return parentId + (seasonNumber?.times(SeriesSeasonEpisodeIdOffset) ?: 0) + episodeNumber + EpisodeIdOffset
}

internal fun resolveAnimePlaybackEpisodeId(
    parentId: Int,
    seasonNumber: Int?,
    episodeNumber: Int,
    dubStatus: DubStatus?,
): Int {
    return parentId +
        episodeNumber +
        ((dubStatus?.id ?: 0) * AnimeDubEpisodeIdOffset) +
        (seasonNumber?.times(AnimeSeasonEpisodeIdOffset) ?: 0)
}

private data class SortedEpisodeEntry(
    val episode: Episode,
    val seasonNumber: Int?,
    val episodeNumber: Int,
)

private fun List<Episode>.toSortedEpisodeEntries(): List<SortedEpisodeEntry> {
    val missingSeasonBucket = resolveMissingSeasonBucket(this)

    return asSequence()
        .filter { episode -> episode.season != null || episode.episode != null }
        .sortedWith(
            compareBy<Episode>(
                { episode -> resolveEpisodeSortOrder(episode.season, missingSeasonBucket) },
                { episode -> episode.episode ?: Int.MAX_VALUE },
            )
        )
        .mapIndexed { index, episode ->
            val normalizedSeason = episode.season?.takeIf { season -> season > 0 } ?: missingSeasonBucket
            SortedEpisodeEntry(
                episode = episode,
                seasonNumber = normalizedSeason.takeIf { season -> season > 0 },
                episodeNumber = episode.episode ?: (index + 1),
            )
        }
        .toList()
}

private fun resolveMissingSeasonBucket(episodes: List<Episode>): Int {
    return if (episodes.any { episode -> (episode.season ?: 0) > 0 }) 0 else 1
}

private fun resolveEpisodeSortOrder(
    rawSeason: Int?,
    missingSeasonBucket: Int,
): Int {
    val normalizedSeason = rawSeason?.takeIf { season -> season > 0 } ?: missingSeasonBucket
    return normalizedSeason.takeIf { season -> season > 0 } ?: Int.MAX_VALUE
}
