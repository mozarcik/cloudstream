package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.content.Context
import android.widget.Toast
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionEvent
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionStateHolder
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatActionOutcome
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
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
    panelsStateHolder: DetailsPanelsStateHolder,
    scope: CoroutineScope,
    onCompleted: suspend () -> Unit = {},
) {
    if (!mode.allowsExtendedActions ||
        panelsStateHolder.isActionInProgress ||
        panelsStateHolder.isPanelLoading
    ) {
        return
    }

    val selection = panelsStateHolder.currentActionSelection ?: return

    scope.launch {
        panelsStateHolder.updateActionInProgress(true)
        try {
            val outcome = selection.onOptionSelected(actionId)
            handleDetailsActionOutcome(
                outcome = outcome,
                panelsStateHolder = panelsStateHolder,
            )
            if (outcome == MovieDetailsCompatActionOutcome.Completed) {
                onCompleted()
            }
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
    preferredSeason: Int? = null,
    preferredEpisode: Int? = null,
    title: String? = null,
    onPlayInApp: (String?) -> Unit,
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
            val request = withContext(Dispatchers.IO) {
                actionsCompat.buildActionMenuRequest(
                    context = context,
                    preferredSeason = preferredSeason,
                    preferredEpisode = preferredEpisode,
                    title = title,
                    onPlayInApp = onPlayInApp,
                )
            }

            if (!panelsStateHolder.isActionsPanelVisible) {
                return@launch
            }

            if (request == null || request.options.isEmpty()) {
                panelsStateHolder.closeActionsPanel()
                CommonActivity.showToast(R.string.no_links_found_toast, Toast.LENGTH_SHORT)
            } else {
                panelsStateHolder.showRootActionSelection(request)
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
