package com.lagradost.cloudstream3.tv.compat

import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.buildResultEpisode
import com.lagradost.cloudstream3.ui.result.getId

internal data class MovieDetailsActionTargetEpisode(
    val data: String,
    val season: Int,
    val episode: Int,
    val index: Int,
    val id: Int,
    val name: String? = null,
    val posterUrl: String? = null,
    val score: Score? = null,
    val description: String? = null,
    val airDate: Long? = null,
    val runTime: Int? = null,
)

internal data class MovieDetailsActionTarget(
    val loadResponse: LoadResponse,
    val episode: ResultEpisode,
    val episodesBySeason: Map<Int, List<MovieDetailsActionTargetEpisode>>,
    val allEpisodes: List<MovieDetailsActionTargetEpisode>,
)

internal fun pickPreferredMovieDetailsEpisode(
    episodes: List<MovieDetailsActionTargetEpisode>,
    preferredSeason: Int?,
    preferredEpisode: Int?,
): MovieDetailsActionTargetEpisode {
    val exact = episodes.firstOrNull { episode ->
        preferredSeason != null &&
            preferredEpisode != null &&
            episode.season == preferredSeason &&
            episode.episode == preferredEpisode
    }
    if (exact != null) return exact

    val matchingEpisode = episodes.firstOrNull { episode ->
        preferredEpisode != null && episode.episode == preferredEpisode
    }
    if (matchingEpisode != null) return matchingEpisode

    val matchingSeason = episodes.firstOrNull { episode ->
        preferredSeason != null && episode.season == preferredSeason
    }
    return matchingSeason ?: episodes.first()
}

internal fun toMovieDetailsActionTarget(
    loadResponse: LoadResponse,
    selectedEpisode: MovieDetailsActionTargetEpisode,
    allEpisodes: List<MovieDetailsActionTargetEpisode>,
    episodesBySeason: Map<Int, List<MovieDetailsActionTargetEpisode>>,
): MovieDetailsActionTarget {
    val season = selectedEpisode.season.takeIf { it > 0 }
    val episode = buildResultEpisode(
        headerName = loadResponse.name,
        name = selectedEpisode.name ?: loadResponse.name,
        poster = selectedEpisode.posterUrl ?: loadResponse.posterUrl ?: loadResponse.backgroundPosterUrl,
        episode = selectedEpisode.episode,
        seasonIndex = season,
        season = season,
        data = selectedEpisode.data,
        apiName = loadResponse.apiName,
        id = selectedEpisode.id,
        index = selectedEpisode.index,
        rating = selectedEpisode.score,
        description = selectedEpisode.description ?: loadResponse.plot,
        tvType = loadResponse.type,
        parentId = loadResponse.getId(),
        airDate = selectedEpisode.airDate,
        runTime = selectedEpisode.runTime,
    )

    return MovieDetailsActionTarget(
        loadResponse = loadResponse,
        episode = episode,
        episodesBySeason = episodesBySeason,
        allEpisodes = allEpisodes,
    )
}
