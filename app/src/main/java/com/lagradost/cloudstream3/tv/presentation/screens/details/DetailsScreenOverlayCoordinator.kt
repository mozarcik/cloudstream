package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.content.Context
import androidx.compose.runtime.Composable
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionStateHolder
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionUiState
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.ui.WatchType
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun DetailsScreenOverlayCoordinator(
    mode: DetailsScreenMode,
    details: MovieDetails,
    actionsCompat: MovieDetailsEpisodeActionsCompat,
    panelsStateHolder: DetailsPanelsStateHolder,
    downloadMirrorState: DownloadMirrorSelectionUiState,
    downloadMirrorStateHolder: DownloadMirrorSelectionStateHolder,
    downloadButtonViewModel: DetailsDownloadButtonViewModel,
    context: Context,
    scope: CoroutineScope,
    closeDownloadPanel: () -> Unit,
    goToPlayer: (String?) -> Unit,
    onBookmarkClick: (WatchType) -> Unit,
) {
    DetailsOverlayPanels(
        mode = mode,
        details = details,
        panelsStateHolder = panelsStateHolder,
        downloadMirrorState = downloadMirrorState,
        onActionSelected = { actionId ->
            executeDetailsAction(
                mode = mode,
                actionId = actionId,
                context = context,
                details = details,
                actionsCompat = actionsCompat,
                panelsStateHolder = panelsStateHolder,
                scope = scope,
                goToPlayer = goToPlayer,
            )
        },
        onDownloadActionSelected = { actionId ->
            executeDetailsDownloadSelection(
                mode = mode,
                actionId = actionId,
                context = context,
                downloadMirrorStateHolder = downloadMirrorStateHolder,
                downloadButtonViewModel = downloadButtonViewModel,
                panelsStateHolder = panelsStateHolder,
                scope = scope,
                onHandleDownloadActionOutcome = { outcome ->
                    handleDetailsDownloadActionOutcome(
                        mode = mode,
                        outcome = outcome,
                        downloadMirrorStateHolder = downloadMirrorStateHolder,
                        closeDownloadPanel = closeDownloadPanel,
                    )
                },
            )
        },
        onCloseActionsPanel = { navigateDetailsActionsBack(panelsStateHolder) },
        onCloseDownloadPanel = closeDownloadPanel,
        onSkipDownloadLoading = { skipDetailsDownloadLoading(downloadMirrorStateHolder) },
        onCloseBookmarkPanel = panelsStateHolder::closeBookmarkPanel,
        onBookmarkSelected = onBookmarkClick,
    )
}
