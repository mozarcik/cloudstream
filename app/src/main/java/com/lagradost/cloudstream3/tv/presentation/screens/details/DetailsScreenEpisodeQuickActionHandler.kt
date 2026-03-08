package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.content.Context
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionStateHolder
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsQuickAction
import com.lagradost.cloudstream3.ui.result.ACTION_MARK_AS_WATCHED
import com.lagradost.cloudstream3.ui.result.ACTION_MARK_WATCHED_UP_TO_THIS_EPISODE
import kotlinx.coroutines.CoroutineScope

internal fun onDetailsEpisodeQuickAction(
    quickAction: MovieDetailsQuickAction,
    episode: TvEpisode,
    selectedSeason: TvSeason?,
    context: Context,
    actionsCompat: MovieDetailsEpisodeActionsCompat,
    downloadMirrorStateHolder: DownloadMirrorSelectionStateHolder,
    panelsStateHolder: DetailsPanelsStateHolder,
    episodesStateHolder: DetailsEpisodesStateHolder,
    scope: CoroutineScope,
    openDownloadPanel: (Int?, Int?) -> Unit,
    goToPlayer: (String?) -> Unit,
) {
    when (quickAction) {
        MovieDetailsQuickAction.MarkAsWatched,
        MovieDetailsQuickAction.RemoveFromWatched,
        MovieDetailsQuickAction.Bookmark -> executeDetailsEpisodeQuickAction(
            actionId = ACTION_MARK_AS_WATCHED,
            episode = episode,
            selectedSeason = selectedSeason,
            context = context,
            actionsCompat = actionsCompat,
            downloadMirrorStateHolder = downloadMirrorStateHolder,
            panelsStateHolder = panelsStateHolder,
            episodesStateHolder = episodesStateHolder,
            scope = scope,
            goToPlayer = goToPlayer,
        )

        MovieDetailsQuickAction.MarkWatchedUpToThisEpisode,
        MovieDetailsQuickAction.RemoveWatchedUpToThisEpisode,
        MovieDetailsQuickAction.Favorite -> executeDetailsEpisodeQuickAction(
            actionId = ACTION_MARK_WATCHED_UP_TO_THIS_EPISODE,
            episode = episode,
            selectedSeason = selectedSeason,
            context = context,
            actionsCompat = actionsCompat,
            downloadMirrorStateHolder = downloadMirrorStateHolder,
            panelsStateHolder = panelsStateHolder,
            episodesStateHolder = episodesStateHolder,
            scope = scope,
            goToPlayer = goToPlayer,
        )

        MovieDetailsQuickAction.Download -> handleDetailsDownloadQuickAction(
            state = episodesStateHolder.resolveEpisodeDownloadState(episode),
            preferredSeason = resolveDetailsEpisodeSeason(episode, selectedSeason),
            preferredEpisode = episode.episodeNumber,
            openDownloadPanel = openDownloadPanel,
            goToPlayer = goToPlayer,
        )

        MovieDetailsQuickAction.More -> openDetailsEpisodeDownloadPanel(
            episode = episode,
            selectedSeason = selectedSeason,
            openDownloadPanel = openDownloadPanel,
        )

        MovieDetailsQuickAction.Search -> Unit
    }
}
