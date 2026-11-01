package com.lagradost.cloudstream3.tv.presentation.screens.player.core

import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.tv.compat.home.refreshContinueWatchingHeader
import com.lagradost.cloudstream3.tv.compat.resume.ResumePlaybackResolver
import com.lagradost.cloudstream3.tv.compat.resume.resolveAnimePlaybackEpisodeId
import com.lagradost.cloudstream3.tv.compat.resume.resolveSeriesPlaybackEpisodeId
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerStartTarget
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerMetadata
import com.lagradost.cloudstream3.tv.presentation.screens.player.resolveEpisodeMetadata
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.buildResultEpisode
import com.lagradost.cloudstream3.ui.result.getId
import com.lagradost.cloudstream3.utils.DataStoreHelper.getDub

internal data class PlayerPlaybackTarget(
    val episode: ResultEpisode,
    val episodes: List<ResultEpisode>,
    val selectedEpisodeIndex: Int,
    val page: LoadResponse?,
    val metadata: TvPlayerMetadata,
)

internal suspend fun resolvePlayerPlaybackTarget(
    repository: APIRepository,
    url: String,
    apiName: String,
    startTarget: PlayerStartTarget,
): PlayerPlaybackTarget? {
    var loadResponse: LoadResponse? = null
    var resolvedData = (startTarget as? PlayerStartTarget.DirectEpisodeData)?.data
    var resolvedSeason: Int? = null
    var resolvedEpisode = 0
    var resolvedEpisodeTitle: String? = null
    var resolvedAnimeDubStatus: DubStatus? = null

    if (resolvedData.isNullOrBlank()) {
        loadResponse = (repository.load(url) as? Resource.Success)?.value ?: return null

        when (loadResponse) {
            is MovieLoadResponse -> {
                resolvedData = loadResponse.dataUrl
            }

            is TvSeriesLoadResponse -> {
                val resolvedStartEpisode = when (val target = startTarget) {
                    is PlayerStartTarget.ResumeEpisode -> ResumePlaybackResolver.findSeriesEpisodeById(
                        episodes = loadResponse.episodes,
                        parentId = loadResponse.getId(),
                        episodeId = target.episodeId,
                    )

                    else -> null
                } ?: loadResponse.episodes
                    .sortedWith(compareBy({ it.season ?: Int.MAX_VALUE }, { it.episode ?: Int.MAX_VALUE }))
                    .firstOrNull()
                resolvedData = resolvedStartEpisode?.data
                resolvedSeason = resolvedStartEpisode?.season
                resolvedEpisode = resolvedStartEpisode?.episode ?: 1
                resolvedEpisodeTitle = resolvedStartEpisode?.name
            }

            is AnimeLoadResponse -> {
                val mainId = loadResponse.getId()
                val resolvedStartEpisode = when (val target = startTarget) {
                    is PlayerStartTarget.ResumeEpisode -> ResumePlaybackResolver.findAnimeEpisodeById(
                        episodesByDub = loadResponse.episodes,
                        parentId = mainId,
                        episodeId = target.episodeId,
                    )

                    else -> null
                }
                if (resolvedStartEpisode != null) {
                    resolvedAnimeDubStatus = resolvedStartEpisode.dubStatus
                    resolvedData = resolvedStartEpisode.episode.data
                    resolvedSeason = resolvedStartEpisode.episode.season
                    resolvedEpisode = resolvedStartEpisode.episode.episode ?: 1
                    resolvedEpisodeTitle = resolvedStartEpisode.episode.name
                } else {
                    val preferredDubStatus = resolvePreferredAnimeDubStatus(
                        loadResponse = loadResponse,
                        mainId = mainId,
                    )
                    val preferredDubEpisodes = preferredDubStatus
                        ?.let { dubStatus -> loadResponse.episodes[dubStatus].orEmpty() }
                        .orEmpty()
                    val firstEpisode = if (preferredDubEpisodes.isNotEmpty()) {
                        resolvedAnimeDubStatus = preferredDubStatus
                        preferredDubEpisodes
                    } else {
                        val fallbackEntry = loadResponse.episodes.entries.firstOrNull { entry ->
                            entry.value.isNotEmpty()
                        }
                        resolvedAnimeDubStatus = fallbackEntry?.key
                        fallbackEntry?.value.orEmpty()
                    }.sortedWith(
                        compareBy({ it.season ?: Int.MAX_VALUE }, { it.episode ?: Int.MAX_VALUE })
                    ).firstOrNull()
                    resolvedData = firstEpisode?.data
                    resolvedSeason = firstEpisode?.season
                    resolvedEpisode = firstEpisode?.episode ?: 1
                    resolvedEpisodeTitle = firstEpisode?.name
                }
            }

            else -> {
                resolvedData = null
            }
        }
    } else {
        loadResponse = (repository.load(url) as? Resource.Success)?.value
        when (loadResponse) {
            is TvSeriesLoadResponse -> {
                val matchingEpisode = loadResponse.episodes.firstOrNull { episode ->
                    episode.data == resolvedData
                }
                resolvedSeason = matchingEpisode?.season
                resolvedEpisode = matchingEpisode?.episode ?: 1
                resolvedEpisodeTitle = matchingEpisode?.name
            }

            is AnimeLoadResponse -> {
                val matchingEntry = loadResponse.episodes.entries.firstOrNull { entry ->
                    entry.value.any { episode ->
                        episode.data == resolvedData
                    }
                }
                val matchingEpisode = matchingEntry
                    ?.value
                    ?.firstOrNull { episode ->
                        episode.data == resolvedData
                    }
                resolvedAnimeDubStatus = matchingEntry?.key
                resolvedSeason = matchingEpisode?.season
                resolvedEpisode = matchingEpisode?.episode ?: 1
                resolvedEpisodeTitle = matchingEpisode?.name
            }

            else -> Unit
        }
    }

    if (resolvedData.isNullOrBlank()) {
        return null
    }

    val resolvedType = loadResponse?.type ?: TvType.TvSeries
    val resolvedTitle = loadResponse?.name ?: apiName
    val parentId = loadResponse?.getId() ?: url.hashCode()
    val fallbackEpisode = buildFallbackPlaybackEpisode(
        loadResponse = loadResponse,
        apiName = apiName,
        url = url,
        resolvedData = resolvedData,
        resolvedSeason = resolvedSeason,
        resolvedEpisode = resolvedEpisode,
        resolvedEpisodeTitle = resolvedEpisodeTitle,
        resolvedAnimeDubStatus = resolvedAnimeDubStatus,
        resolvedType = resolvedType,
        resolvedTitle = resolvedTitle,
        parentId = parentId,
    )
    val resolvedEpisodes = buildPlaybackEpisodes(
        loadResponse = loadResponse,
        apiName = apiName,
        parentId = parentId,
        resolvedAnimeDubStatus = resolvedAnimeDubStatus,
    ).ifEmpty { listOf(fallbackEpisode) }
    val selectedEpisodeIndex = PlayerStartEpisodeSelector.resolveSelectedEpisodeIndex(
        startTarget = startTarget,
        episodes = resolvedEpisodes,
        fallbackEpisodeData = resolvedData,
    )
    val episode = resolvedEpisodes.getOrElse(selectedEpisodeIndex) { fallbackEpisode }
    val metadata = resolveEpisodeMetadata(
        baseMetadata = resolvePlayerMetadata(
            loadResponse = loadResponse,
            apiName = apiName,
        ),
        episode = episode,
    )

    loadResponse?.let { page ->
        refreshContinueWatchingHeader(
            parentId = parentId,
            apiName = apiName,
            url = page.url,
            name = page.name,
            type = resolvedType,
            posterUrl = page.posterUrl,
            backdropUrl = page.backgroundPosterUrl,
        )
    }

    return PlayerPlaybackTarget(
        episode = episode,
        episodes = resolvedEpisodes,
        selectedEpisodeIndex = selectedEpisodeIndex,
        page = loadResponse,
        metadata = metadata,
    )
}

private fun buildPlaybackEpisodes(
    loadResponse: LoadResponse?,
    apiName: String,
    parentId: Int,
    resolvedAnimeDubStatus: DubStatus?,
): List<ResultEpisode> {
    return when (loadResponse) {
        is MovieLoadResponse -> listOf(
            buildResultEpisode(
                headerName = loadResponse.name,
                name = loadResponse.name,
                poster = loadResponse.posterUrl ?: loadResponse.backgroundPosterUrl,
                episode = 0,
                seasonIndex = null,
                season = null,
                data = loadResponse.dataUrl,
                apiName = apiName,
                id = parentId,
                index = 0,
                description = loadResponse.plot,
                rating = loadResponse.score,
                tvType = loadResponse.type,
                parentId = parentId,
            )
        )

        is TvSeriesLoadResponse -> buildSeriesPlaybackEpisodes(
            loadResponse = loadResponse,
            apiName = apiName,
            parentId = parentId,
        )

        is AnimeLoadResponse -> buildAnimePlaybackEpisodes(
            loadResponse = loadResponse,
            apiName = apiName,
            parentId = parentId,
            resolvedAnimeDubStatus = resolvedAnimeDubStatus,
        )

        else -> emptyList()
    }
}

private fun buildSeriesPlaybackEpisodes(
    loadResponse: TvSeriesLoadResponse,
    apiName: String,
    parentId: Int,
): List<ResultEpisode> {
    val missingSeasonBucket = resolveMissingSeasonBucket(loadResponse.episodes)
    return loadResponse.episodes
        .sortedBy { episode ->
            resolveEpisodeSortOrder(
                rawSeason = episode.season,
                missingSeasonBucket = missingSeasonBucket,
            ) * 10_000 + (episode.episode ?: 0)
        }
        .mapIndexed { index, episode ->
            val normalizedSeason = episode.season?.takeIf { season -> season > 0 } ?: missingSeasonBucket
            val displaySeason = normalizedSeason.takeIf { season -> season > 0 }
            val episodeNumber = episode.episode ?: (index + 1)
            buildResultEpisode(
                headerName = loadResponse.name,
                name = episode.name ?: loadResponse.name,
                poster = episode.posterUrl ?: loadResponse.posterUrl ?: loadResponse.backgroundPosterUrl,
                episode = episodeNumber,
                seasonIndex = displaySeason,
                season = displaySeason,
                data = episode.data,
                apiName = apiName,
                id = resolveSeriesPlaybackEpisodeId(
                    parentId = parentId,
                    seasonNumber = displaySeason,
                    episodeNumber = episodeNumber,
                ),
                index = index,
                description = episode.description ?: loadResponse.plot,
                rating = episode.score,
                tvType = loadResponse.type,
                parentId = parentId,
                airDate = episode.date,
                runTime = episode.runTime,
            )
        }
}

private fun buildAnimePlaybackEpisodes(
    loadResponse: AnimeLoadResponse,
    apiName: String,
    parentId: Int,
    resolvedAnimeDubStatus: DubStatus?,
): List<ResultEpisode> {
    val (dubStatus, episodes) = resolveAnimePlaybackEpisodes(
        loadResponse = loadResponse,
        parentId = parentId,
        resolvedAnimeDubStatus = resolvedAnimeDubStatus,
    )
    if (episodes.isEmpty()) {
        return emptyList()
    }

    val missingSeasonBucket = resolveMissingSeasonBucket(episodes)
    return episodes
        .sortedBy { episode ->
            resolveEpisodeSortOrder(
                rawSeason = episode.season,
                missingSeasonBucket = missingSeasonBucket,
            ) * 10_000 + (episode.episode ?: 0)
        }
        .mapIndexed { index, episode ->
            val normalizedSeason = episode.season?.takeIf { season -> season > 0 } ?: missingSeasonBucket
            val displaySeason = normalizedSeason.takeIf { season -> season > 0 }
            val episodeNumber = episode.episode ?: (index + 1)
            buildResultEpisode(
                headerName = loadResponse.name,
                name = episode.name ?: loadResponse.name,
                poster = episode.posterUrl ?: loadResponse.posterUrl ?: loadResponse.backgroundPosterUrl,
                episode = episodeNumber,
                seasonIndex = displaySeason,
                season = displaySeason,
                data = episode.data,
                apiName = apiName,
                id = resolveAnimePlaybackEpisodeId(
                    parentId = parentId,
                    seasonNumber = displaySeason,
                    episodeNumber = episodeNumber,
                    dubStatus = dubStatus,
                ),
                index = index,
                description = episode.description ?: loadResponse.plot,
                rating = episode.score,
                tvType = loadResponse.type,
                parentId = parentId,
                airDate = episode.date,
                runTime = episode.runTime,
            )
        }
}

private fun resolveAnimePlaybackEpisodes(
    loadResponse: AnimeLoadResponse,
    parentId: Int,
    resolvedAnimeDubStatus: DubStatus?,
): Pair<DubStatus?, List<Episode>> {
    val preferredDubStatus = resolvedAnimeDubStatus ?: resolvePreferredAnimeDubStatus(
        loadResponse = loadResponse,
        mainId = parentId,
    )
    val preferredEpisodes = preferredDubStatus
        ?.let { dubStatus -> loadResponse.episodes[dubStatus].orEmpty() }
        .orEmpty()
    if (preferredEpisodes.isNotEmpty()) {
        return preferredDubStatus to preferredEpisodes
    }

    val fallbackEntry = loadResponse.episodes.entries.firstOrNull { entry ->
        entry.value.isNotEmpty()
    }
    return fallbackEntry?.key to fallbackEntry?.value.orEmpty()
}

private fun buildFallbackPlaybackEpisode(
    loadResponse: LoadResponse?,
    apiName: String,
    url: String,
    resolvedData: String,
    resolvedSeason: Int?,
    resolvedEpisode: Int,
    resolvedEpisodeTitle: String?,
    resolvedAnimeDubStatus: DubStatus?,
    resolvedType: TvType,
    resolvedTitle: String,
    parentId: Int,
): ResultEpisode {
    val resolvedEpisodeId = when (val response = loadResponse) {
        is MovieLoadResponse -> parentId
        is TvSeriesLoadResponse -> resolveSeriesPlaybackEpisodeId(
            parentId = parentId,
            seasonNumber = resolvedSeason,
            episodeNumber = resolvedEpisode.takeIf { episode -> episode > 0 } ?: 1,
        )
        is AnimeLoadResponse -> resolveAnimePlaybackEpisodeId(
            parentId = parentId,
            seasonNumber = resolvedSeason,
            episodeNumber = resolvedEpisode.takeIf { episode -> episode > 0 } ?: 1,
            dubStatus = resolvedAnimeDubStatus ?: resolvePreferredAnimeDubStatus(response, parentId),
        )
        else -> "$apiName|$url|$resolvedData".hashCode()
    }

    return buildResultEpisode(
        headerName = resolvedTitle,
        name = resolvedEpisodeTitle ?: resolvedTitle,
        poster = loadResponse?.posterUrl ?: loadResponse?.backgroundPosterUrl,
        episode = resolvedEpisode,
        seasonIndex = resolvedSeason,
        season = resolvedSeason,
        data = resolvedData,
        apiName = apiName,
        id = resolvedEpisodeId,
        index = 0,
        description = loadResponse?.plot,
        rating = loadResponse?.score,
        tvType = resolvedType,
        parentId = parentId,
    )
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

private fun resolvePreferredAnimeDubStatus(
    loadResponse: AnimeLoadResponse,
    mainId: Int,
): DubStatus? {
    val available = loadResponse.episodes.keys
    if (available.isEmpty()) return null

    val stored = getDub(mainId)
    if (stored != null && available.contains(stored)) {
        return stored
    }

    return when {
        available.contains(DubStatus.Dubbed) -> DubStatus.Dubbed
        available.contains(DubStatus.Subbed) -> DubStatus.Subbed
        available.contains(DubStatus.None) -> DubStatus.None
        else -> available.firstOrNull()
    }
}

private fun resolvePlayerMetadata(
    loadResponse: LoadResponse?,
    apiName: String,
): TvPlayerMetadata {
    if (loadResponse == null) {
        return TvPlayerMetadata(
            title = apiName,
            subtitle = "",
            backdropUri = null,
            apiName = apiName,
        )
    }

    val year = when (loadResponse) {
        is MovieLoadResponse -> loadResponse.year
        is TvSeriesLoadResponse -> loadResponse.year
        is AnimeLoadResponse -> loadResponse.year
        else -> null
    }
    val subtitleChunks = listOfNotNull(year?.toString(), apiName)
    val isEpisodeBased = loadResponse is TvSeriesLoadResponse || loadResponse is AnimeLoadResponse

    return TvPlayerMetadata(
        title = loadResponse.name,
        subtitle = subtitleChunks.joinToString(separator = " . "),
        backdropUri = loadResponse.backgroundPosterUrl ?: loadResponse.posterUrl,
        year = year,
        apiName = apiName,
        season = null,
        episode = null,
        episodeTitle = null,
        isEpisodeBased = isEpisodeBased,
    )
}
