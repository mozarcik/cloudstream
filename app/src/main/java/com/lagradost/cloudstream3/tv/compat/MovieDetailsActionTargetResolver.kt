package com.lagradost.cloudstream3.tv.compat

import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse

internal class MovieDetailsActionTargetResolver(
    private val loadResponse: LoadResponse,
    private val preferredSeason: Int? = null,
    private val preferredEpisode: Int? = null,
) {
    fun buildBaseTarget(): MovieDetailsActionTarget? {
        return when (loadResponse) {
            is MovieLoadResponse -> buildMovieDetailsMovieTarget(loadResponse)
            is TvSeriesLoadResponse -> buildMovieDetailsSeriesTarget(
                loadResponse = loadResponse,
                preferredSeason = preferredSeason,
                preferredEpisode = preferredEpisode,
            )
            is AnimeLoadResponse -> buildMovieDetailsAnimeTarget(
                loadResponse = loadResponse,
                preferredSeason = preferredSeason,
                preferredEpisode = preferredEpisode,
            )
            else -> null
        }
    }

    fun selectPreferredTarget(
        baseTarget: MovieDetailsActionTarget,
        preferredSeason: Int?,
        preferredEpisode: Int?,
    ): MovieDetailsActionTarget {
        if (preferredSeason == null && preferredEpisode == null) {
            return baseTarget
        }

        val selectedEpisode = pickPreferredMovieDetailsEpisode(
            episodes = baseTarget.allEpisodes,
            preferredSeason = preferredSeason,
            preferredEpisode = preferredEpisode,
        )
        if (selectedEpisode.id == baseTarget.episode.id) {
            return baseTarget
        }

        return toMovieDetailsActionTarget(
            loadResponse = baseTarget.loadResponse,
            selectedEpisode = selectedEpisode,
            allEpisodes = baseTarget.allEpisodes,
            episodesBySeason = baseTarget.episodesBySeason,
        )
    }
}
