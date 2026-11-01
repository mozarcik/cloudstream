package com.lagradost.cloudstream3.tv.compat

import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.ui.result.getId
import com.lagradost.cloudstream3.utils.DataStoreHelper.getDub

internal fun buildMovieDetailsMovieTarget(
    loadResponse: MovieLoadResponse,
): MovieDetailsActionTarget? {
    val data = loadResponse.dataUrl.takeIf { it.isNotBlank() } ?: return null
    val mainId = loadResponse.getId()
    val episode = MovieDetailsActionTargetEpisode(
        data = data,
        season = 0,
        episode = 0,
        index = 0,
        id = mainId,
        name = loadResponse.name,
        posterUrl = loadResponse.posterUrl ?: loadResponse.backgroundPosterUrl,
        description = loadResponse.plot,
    )
    return toMovieDetailsActionTarget(
        loadResponse = loadResponse,
        selectedEpisode = episode,
        allEpisodes = listOf(episode),
        episodesBySeason = mapOf(episode.season to listOf(episode)),
    )
}

internal fun buildMovieDetailsSeriesTarget(
    loadResponse: TvSeriesLoadResponse,
    preferredSeason: Int?,
    preferredEpisode: Int?,
): MovieDetailsActionTarget? {
    val mainId = loadResponse.getId()
    val missingSeasonBucket = loadResponse.episodes.resolveMovieDetailsMissingSeasonBucket()
    val episodes = loadResponse.episodes
        .sortedBy { episode ->
            episode.movieDetailsSeasonSortOrder(missingSeasonBucket) * 10_000 + (episode.episode ?: 0)
        }
        .mapIndexed { index, episode ->
            val episodeIndex = episode.episode ?: (index + 1)
            val normalizedSeason = episode.season?.takeIf { it > 0 } ?: missingSeasonBucket
            MovieDetailsActionTargetEpisode(
                data = episode.data,
                season = normalizedSeason,
                episode = episodeIndex,
                index = index,
                id = mainId + normalizedSeason * 100_000 + episodeIndex + 1,
                name = episode.name,
                posterUrl = episode.posterUrl,
                score = episode.score,
                description = episode.description,
                airDate = episode.date,
                runTime = episode.runTime,
            )
        }
    if (episodes.isEmpty()) return null

    val episodesBySeason = episodes
        .groupBy { it.season }
        .mapValues { (_, episodesInSeason) -> episodesInSeason.sortedBy { it.episode } }

    return toMovieDetailsActionTarget(
        loadResponse = loadResponse,
        selectedEpisode = pickPreferredMovieDetailsEpisode(
            episodes = episodes,
            preferredSeason = preferredSeason,
            preferredEpisode = preferredEpisode,
        ),
        allEpisodes = episodes,
        episodesBySeason = episodesBySeason,
    )
}

internal fun buildMovieDetailsAnimeTarget(
    loadResponse: AnimeLoadResponse,
    preferredSeason: Int?,
    preferredEpisode: Int?,
): MovieDetailsActionTarget? {
    val mainId = loadResponse.getId()
    val preferredDub = resolvePreferredMovieDetailsDubStatus(loadResponse, mainId) ?: return null
    val dubEpisodes = loadResponse.episodes[preferredDub].orEmpty()
    if (dubEpisodes.isEmpty()) return null

    val missingSeasonBucket = dubEpisodes.resolveMovieDetailsMissingSeasonBucket()
    val episodes = dubEpisodes
        .sortedBy { episode ->
            episode.movieDetailsSeasonSortOrder(missingSeasonBucket) * 10_000 + (episode.episode ?: 0)
        }
        .mapIndexed { index, episode ->
        val episodeIndex = episode.episode ?: (index + 1)
        val normalizedSeason = episode.season?.takeIf { it > 0 } ?: missingSeasonBucket
        MovieDetailsActionTargetEpisode(
            data = episode.data,
            season = normalizedSeason,
            episode = episodeIndex,
            index = index,
            id = mainId + episodeIndex + preferredDub.id * 1_000_000 + (normalizedSeason * 10_000),
            name = episode.name,
            posterUrl = episode.posterUrl,
            score = episode.score,
            description = episode.description,
            airDate = episode.date,
            runTime = episode.runTime,
        )
    }

    val episodesBySeason = episodes
        .groupBy { it.season }
        .mapValues { (_, episodesInSeason) -> episodesInSeason.sortedBy { it.episode } }

    return toMovieDetailsActionTarget(
        loadResponse = loadResponse,
        selectedEpisode = pickPreferredMovieDetailsEpisode(
            episodes = episodes,
            preferredSeason = preferredSeason,
            preferredEpisode = preferredEpisode,
        ),
        allEpisodes = episodes,
        episodesBySeason = episodesBySeason,
    )
}

private fun resolvePreferredMovieDetailsDubStatus(
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

private fun List<com.lagradost.cloudstream3.Episode>.resolveMovieDetailsMissingSeasonBucket(): Int {
    return if (any { episode -> (episode.season ?: 0) > 0 }) 0 else 1
}

private fun com.lagradost.cloudstream3.Episode.movieDetailsSeasonSortOrder(
    missingSeasonBucket: Int,
): Int {
    val normalizedSeason = season?.takeIf { it > 0 } ?: missingSeasonBucket
    return normalizedSeason.takeIf { it > 0 } ?: Int.MAX_VALUE
}
