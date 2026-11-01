package com.lagradost.cloudstream3.tv.compat

import com.lagradost.cloudstream3.ui.result.VideoWatchState
import com.lagradost.cloudstream3.utils.DataStoreHelper.getVideoWatchState
import com.lagradost.cloudstream3.utils.DataStoreHelper.setVideoWatchState

internal class MovieDetailsWatchStateSupport {
    fun isEpisodeWatched(target: MovieDetailsActionTarget): Boolean {
        return getVideoWatchState(target.episode.id) == VideoWatchState.Watched
    }

    fun toggleWatched(target: MovieDetailsActionTarget) {
        val nextState = if (isEpisodeWatched(target)) {
            VideoWatchState.None
        } else {
            VideoWatchState.Watched
        }
        setVideoWatchState(target.episode.id, nextState)
    }

    fun toggleWatchedUpTo(target: MovieDetailsActionTarget) {
        val nextState = if (isEpisodeWatched(target)) {
            VideoWatchState.None
        } else {
            VideoWatchState.Watched
        }

        resolveEpisodesUpToTarget(target).forEach { episode ->
            setVideoWatchState(episode.id, nextState)
        }
    }
}

internal fun resolveEpisodesUpToTarget(
    target: MovieDetailsActionTarget,
): List<MovieDetailsActionTargetEpisode> {
    val clickSeason = target.episode.season ?: 0
    val clickEpisode = target.episode.episode

    return buildList {
        target.episodesBySeason
            .toSortedMap()
            .forEach { (season, episodes) ->
                if (season > clickSeason) return@forEach
                if (clickSeason != 0 && season == 0) return@forEach

                if (season == clickSeason) {
                    addAll(episodes.filter { episode -> episode.episode <= clickEpisode })
                } else {
                    addAll(episodes)
                }
            }
    }
}
