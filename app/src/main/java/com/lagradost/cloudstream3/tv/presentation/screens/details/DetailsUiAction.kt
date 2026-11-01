package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsQuickAction

internal sealed interface DetailsUiAction {
    data class HeroQuickAction(
        val action: MovieDetailsQuickAction,
    ) : DetailsUiAction

    data class EpisodeQuickAction(
        val episode: TvEpisode,
        val action: MovieDetailsQuickAction,
    ) : DetailsUiAction
}
