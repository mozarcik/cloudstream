package com.lagradost.cloudstream3.tv.presentation.screens.player.core

import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.tv.compat.home.refreshContinueWatchingHeader
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerMetadata
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.buildResultEpisode
import com.lagradost.cloudstream3.ui.result.getId
import com.lagradost.cloudstream3.utils.DataStoreHelper.getDub

internal data class PlayerPlaybackTarget(
    val episode: ResultEpisode,
    val page: LoadResponse?,
    val metadata: TvPlayerMetadata,
)

internal suspend fun resolvePlayerPlaybackTarget(
    repository: APIRepository,
    url: String,
    apiName: String,
    directEpisodeData: String?,
): PlayerPlaybackTarget? {
    var loadResponse: LoadResponse? = null
    var resolvedData = directEpisodeData
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
                val firstEpisode = loadResponse.episodes
                    .sortedWith(compareBy({ it.season ?: Int.MAX_VALUE }, { it.episode ?: Int.MAX_VALUE }))
                    .firstOrNull()
                resolvedData = firstEpisode?.data
                resolvedSeason = firstEpisode?.season
                resolvedEpisode = firstEpisode?.episode ?: 1
                resolvedEpisodeTitle = firstEpisode?.name
            }

            is AnimeLoadResponse -> {
                val mainId = loadResponse.getId()
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
    val episodeId = when (val response = loadResponse) {
        is MovieLoadResponse -> parentId

        is TvSeriesLoadResponse -> {
            val matchingEpisode = response.episodes.firstOrNull { episode ->
                episode.data == resolvedData
            }
            val episodeNumber = matchingEpisode?.episode ?: resolvedEpisode.takeIf { it > 0 } ?: 1
            val seasonNumber = matchingEpisode?.season ?: resolvedSeason
            parentId + (seasonNumber?.times(100_000) ?: 0) + episodeNumber + 1
        }

        is AnimeLoadResponse -> {
            val matchingEntry = response.episodes.entries.firstOrNull { entry ->
                entry.value.any { episode ->
                    episode.data == resolvedData
                }
            }
            val matchingEpisode = matchingEntry
                ?.value
                ?.firstOrNull { episode ->
                    episode.data == resolvedData
                }
            val dubStatus = matchingEntry?.key
                ?: resolvedAnimeDubStatus
                ?: resolvePreferredAnimeDubStatus(response, parentId)
            val episodeNumber = matchingEpisode?.episode ?: resolvedEpisode.takeIf { it > 0 } ?: 1
            val seasonNumber = matchingEpisode?.season ?: resolvedSeason
            parentId + episodeNumber + ((dubStatus?.id ?: 0) * 1_000_000) + (seasonNumber?.times(10_000)
                ?: 0)
        }

        else -> "$apiName|$resolvedData".hashCode()
    }

    val episode = buildResultEpisode(
        headerName = resolvedTitle,
        name = resolvedEpisodeTitle ?: resolvedTitle,
        poster = loadResponse?.posterUrl ?: loadResponse?.backgroundPosterUrl,
        episode = resolvedEpisode,
        seasonIndex = resolvedSeason,
        season = resolvedSeason,
        data = resolvedData,
        apiName = apiName,
        id = episodeId,
        index = 0,
        tvType = resolvedType,
        parentId = parentId,
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
        page = loadResponse,
        metadata = resolvePlayerMetadata(
            loadResponse = loadResponse,
            apiName = apiName,
            resolvedSeason = resolvedSeason,
            resolvedEpisode = resolvedEpisode.takeIf { it > 0 },
            resolvedEpisodeTitle = resolvedEpisodeTitle,
        ),
    )
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
    resolvedSeason: Int?,
    resolvedEpisode: Int?,
    resolvedEpisodeTitle: String?,
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
        season = resolvedSeason,
        episode = resolvedEpisode,
        episodeTitle = resolvedEpisodeTitle,
        isEpisodeBased = isEpisodeBased,
    )
}
