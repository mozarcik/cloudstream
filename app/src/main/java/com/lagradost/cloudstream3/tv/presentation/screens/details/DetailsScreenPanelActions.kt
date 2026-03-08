package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.content.Context
import android.widget.Toast
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionEvent
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionStateHolder
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatActionOutcome
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun closeDetailsDownloadPanel(
    mode: DetailsScreenMode,
    downloadMirrorStateHolder: DownloadMirrorSelectionStateHolder,
    downloadButtonViewModel: DetailsDownloadButtonViewModel,
    panelsStateHolder: DetailsPanelsStateHolder,
) {
    android.util.Log.d(DetailsDebugTag, "close download panel mode=$mode")
    downloadMirrorStateHolder.onEvent(DownloadMirrorSelectionEvent.Close)
    downloadButtonViewModel.clearPendingCompat()
    panelsStateHolder.updateActionInProgress(false)
}

internal fun navigateDetailsActionsBack(
    panelsStateHolder: DetailsPanelsStateHolder,
) {
    panelsStateHolder.navigateActionsBack()
}

internal fun handleDetailsActionOutcome(
    outcome: MovieDetailsCompatActionOutcome,
    panelsStateHolder: DetailsPanelsStateHolder,
) {
    when (outcome) {
        MovieDetailsCompatActionOutcome.Completed -> panelsStateHolder.closeActionsPanel()
        is MovieDetailsCompatActionOutcome.OpenSelection -> {
            panelsStateHolder.showActionSelection(outcome.request)
        }
    }
}

internal fun executeDetailsAction(
    mode: DetailsScreenMode,
    actionId: Int,
    context: Context,
    details: MovieDetails,
    actionsCompat: MovieDetailsEpisodeActionsCompat,
    panelsStateHolder: DetailsPanelsStateHolder,
    scope: CoroutineScope,
    goToPlayer: (String?) -> Unit,
) {
    if (!mode.allowsExtendedActions ||
        panelsStateHolder.isActionInProgress ||
        panelsStateHolder.isPanelLoading
    ) {
        return
    }

    scope.launch {
        panelsStateHolder.updateActionInProgress(true)
        try {
            val selection = panelsStateHolder.panelSelection
            val outcome = if (selection != null) {
                selection.onOptionSelected(actionId)
            } else {
                actionsCompat.execute(
                    actionId = actionId,
                    context = context,
                    onPlayInApp = { goToPlayer(resolveDefaultEpisodeData(details)) },
                )
            }
            handleDetailsActionOutcome(
                outcome = outcome,
                panelsStateHolder = panelsStateHolder,
            )
        } finally {
            panelsStateHolder.updateActionInProgress(false)
        }
    }
}

internal fun openDetailsActionsPanel(
    mode: DetailsScreenMode,
    context: Context,
    actionsCompat: MovieDetailsEpisodeActionsCompat,
    panelsStateHolder: DetailsPanelsStateHolder,
    scope: CoroutineScope,
    closeDownloadPanel: () -> Unit,
) {
    if (!mode.allowsExtendedActions ||
        panelsStateHolder.isPanelLoading ||
        panelsStateHolder.isActionInProgress
    ) {
        return
    }

    panelsStateHolder.closeBookmarkPanel()
    closeDownloadPanel()
    panelsStateHolder.openActionsPanel()

    scope.launch {
        panelsStateHolder.updatePanelLoading(true)
        try {
            val loadedActions = withContext(Dispatchers.IO) {
                actionsCompat.loadPanelActions(context)
            }
            panelsStateHolder.updatePanelItems(loadedActions)

            if (loadedActions.isEmpty()) {
                panelsStateHolder.closeActionsPanel()
                CommonActivity.showToast(R.string.no_links_found_toast, Toast.LENGTH_SHORT)
            }
        } finally {
            panelsStateHolder.updatePanelLoading(false)
        }
    }
}

internal fun openDetailsBookmarkPanel(
    mode: DetailsScreenMode,
    panelsStateHolder: DetailsPanelsStateHolder,
    closeDownloadPanel: () -> Unit,
) {
    if (!mode.allowsBookmark ||
        panelsStateHolder.isPanelLoading ||
        panelsStateHolder.isActionInProgress
    ) {
        return
    }

    panelsStateHolder.closeActionsPanel()
    closeDownloadPanel()
    panelsStateHolder.openBookmarkPanel()
}
