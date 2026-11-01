package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionStateHolder
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat

@Stable
internal class DetailsDownloadPanelActions(
    val closePanel: () -> Unit,
    val openPanel: (Int?, Int?) -> Unit,
)

@Composable
internal fun rememberDetailsDownloadPanelActions(
    mode: DetailsScreenMode,
    context: Context,
    actionsCompat: MovieDetailsEpisodeActionsCompat,
    downloadButtonViewModel: DetailsDownloadButtonViewModel,
    downloadMirrorStateHolder: DownloadMirrorSelectionStateHolder,
    panelsStateHolder: DetailsPanelsStateHolder,
): DetailsDownloadPanelActions {
    return remember(
        mode,
        context,
        actionsCompat,
        downloadButtonViewModel,
        downloadMirrorStateHolder,
        panelsStateHolder,
    ) {
        DetailsDownloadPanelActions(
            closePanel = {
                closeDetailsDownloadPanel(
                    mode = mode,
                    downloadMirrorStateHolder = downloadMirrorStateHolder,
                    downloadButtonViewModel = downloadButtonViewModel,
                    panelsStateHolder = panelsStateHolder,
                )
            },
            openPanel = { preferredSeason, preferredEpisode ->
                openDetailsDownloadPanel(
                    mode = mode,
                    context = context,
                    actionsCompat = actionsCompat,
                    downloadButtonViewModel = downloadButtonViewModel,
                    downloadMirrorStateHolder = downloadMirrorStateHolder,
                    panelsStateHolder = panelsStateHolder,
                    preferredSeason = preferredSeason,
                    preferredEpisode = preferredEpisode,
                )
            },
        )
    }
}
