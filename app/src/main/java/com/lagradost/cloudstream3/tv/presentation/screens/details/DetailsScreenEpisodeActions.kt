package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.content.Context
import android.util.Log
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionStateHolder
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun openDetailsEpisodeDownloadPanel(
    episode: TvEpisode,
    selectedSeason: TvSeason?,
    openDownloadPanel: (Int?, Int?) -> Unit,
) {
    openDownloadPanel(
        resolveDetailsEpisodeSeason(episode = episode, selectedSeason = selectedSeason),
        episode.episodeNumber,
    )
}

internal fun executeDetailsEpisodeQuickAction(
    actionId: Int,
    episode: TvEpisode,
    selectedSeason: TvSeason?,
    context: Context,
    actionsCompat: MovieDetailsEpisodeActionsCompat,
    downloadMirrorStateHolder: DownloadMirrorSelectionStateHolder,
    panelsStateHolder: DetailsPanelsStateHolder,
    episodesStateHolder: DetailsEpisodesStateHolder,
    scope: CoroutineScope,
    goToPlayer: (String?) -> Unit,
) {
    if (panelsStateHolder.isActionInProgress ||
        panelsStateHolder.isPanelLoading ||
        downloadMirrorStateHolder.uiState.value.isLoading
    ) {
        return
    }

    val targetSeason = resolveDetailsEpisodeSeason(
        episode = episode,
        selectedSeason = selectedSeason,
    )
    val targetEpisode = episode.episodeNumber

    scope.launch {
        panelsStateHolder.updateActionInProgress(true)
        try {
            actionsCompat.executeForEpisode(
                actionId = actionId,
                context = context,
                preferredSeason = targetSeason,
                preferredEpisode = targetEpisode,
                onPlayInApp = goToPlayer,
            )
        } catch (error: Throwable) {
            Log.e(
                DetailsDebugTag,
                "episode quick action failed actionId=$actionId season=$targetSeason episode=$targetEpisode",
                error,
            )
        } finally {
            episodesStateHolder.invalidateEpisodeStates()
            panelsStateHolder.updateActionInProgress(false)
        }
    }
}
