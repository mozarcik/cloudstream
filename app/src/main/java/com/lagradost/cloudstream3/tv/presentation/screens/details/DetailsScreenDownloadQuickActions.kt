package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.util.Log
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsDownloadActionState
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerScreenNavigation

internal fun playDownloadedDetailsByState(
    state: DetailsDownloadButtonUiState,
    goToPlayer: (String?) -> Unit,
): Boolean {
    val episodeId = state.episodeId ?: run {
        Log.d(DetailsDebugTag, "playDownloadedByState skipped: state without episodeId")
        return false
    }
    goToPlayer(PlayerScreenNavigation.buildDownloadedEpisodeData(episodeId))
    Log.d(
        DetailsDebugTag,
        "playDownloadedByState episodeId=$episodeId status=${state.status} played=true via compose player route"
    )
    return true
}

internal fun handleDetailsDownloadQuickAction(
    state: DetailsDownloadButtonUiState,
    preferredSeason: Int?,
    preferredEpisode: Int?,
    openDownloadPanel: (Int?, Int?) -> Unit,
    goToPlayer: (String?) -> Unit,
) {
    when (state.toMovieDetailsDownloadActionState()) {
        is MovieDetailsDownloadActionState.Downloading -> {
            Log.d(
                DetailsDebugTag,
                "handleDownloadQuickAction ignored (downloading) season=$preferredSeason episode=$preferredEpisode"
            )
            Unit
        }

        MovieDetailsDownloadActionState.Downloaded -> {
            Log.d(
                DetailsDebugTag,
                "handleDownloadQuickAction downloaded season=$preferredSeason episode=$preferredEpisode episodeId=${state.episodeId}"
            )
            val isPlayed = playDownloadedDetailsByState(
                state = state,
                goToPlayer = goToPlayer,
            )
            if (!isPlayed) {
                Log.d(DetailsDebugTag, "handleDownloadQuickAction fallback to source panel (play failed)")
                openDownloadPanel(preferredSeason, preferredEpisode)
            }
        }

        MovieDetailsDownloadActionState.Idle,
        MovieDetailsDownloadActionState.Failed -> {
            Log.d(
                DetailsDebugTag,
                "handleDownloadQuickAction open panel state=${state.toMovieDetailsDownloadActionState()} season=$preferredSeason episode=$preferredEpisode"
            )
            openDownloadPanel(preferredSeason, preferredEpisode)
        }
    }
}
