package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.content.Context
import android.util.Log
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionEvent
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionStateHolder
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatActionOutcome
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun handleDetailsDownloadActionOutcome(
    mode: DetailsScreenMode,
    outcome: MovieDetailsCompatActionOutcome,
    downloadMirrorStateHolder: DownloadMirrorSelectionStateHolder,
    closeDownloadPanel: () -> Unit,
) {
    when (outcome) {
        MovieDetailsCompatActionOutcome.Completed -> {
            Log.d(DetailsDebugTag, "download outcome: completed mode=$mode")
            closeDownloadPanel()
        }

        is MovieDetailsCompatActionOutcome.OpenSelection -> {
            Log.d(
                DetailsDebugTag,
                "download outcome: open selection mode=$mode options=${outcome.request.options.size}"
            )
            downloadMirrorStateHolder.updateSelectionRequest(outcome.request)
        }
    }
}

internal fun executeDetailsDownloadSelection(
    mode: DetailsScreenMode,
    actionId: Int,
    context: Context,
    downloadMirrorStateHolder: DownloadMirrorSelectionStateHolder,
    downloadButtonViewModel: DetailsDownloadButtonViewModel,
    panelsStateHolder: DetailsPanelsStateHolder,
    scope: CoroutineScope,
    onHandleDownloadActionOutcome: (MovieDetailsCompatActionOutcome) -> Unit,
) {
    val selection = downloadMirrorStateHolder.uiState.value.selectionRequest ?: return
    if (panelsStateHolder.isActionInProgress) return

    scope.launch {
        panelsStateHolder.updateActionInProgress(true)
        downloadButtonViewModel.markPending()
        var shouldClearPendingCompat = false
        try {
            val outcome = withContext(Dispatchers.IO) {
                selection.onOptionSelected(actionId)
            }
            when (outcome) {
                MovieDetailsCompatActionOutcome.Completed -> {
                    Log.d(DetailsDebugTag, "download outcome: completed mode=$mode")
                    downloadMirrorStateHolder.onEvent(DownloadMirrorSelectionEvent.Close)
                    shouldClearPendingCompat = true
                    downloadButtonViewModel.refreshPendingOrDefaultSnapshot(
                        context = context,
                        reason = "after_source_selected"
                    )
                    delay(DetailsDownloadSelectionRefreshDelayMs)
                    downloadButtonViewModel.refreshPendingOrDefaultSnapshot(
                        context = context,
                        reason = "after_source_selected_delayed"
                    )
                }

                is MovieDetailsCompatActionOutcome.OpenSelection -> {
                    onHandleDownloadActionOutcome(outcome)
                }
            }
        } catch (error: Throwable) {
            Log.e(DetailsDebugTag, "download selection failed mode=$mode actionId=$actionId", error)
            downloadButtonViewModel.markFailed()
            downloadMirrorStateHolder.onEvent(DownloadMirrorSelectionEvent.Close)
            shouldClearPendingCompat = true
        } finally {
            if (shouldClearPendingCompat) {
                downloadButtonViewModel.clearPendingCompat()
            }
            panelsStateHolder.updateActionInProgress(false)
        }
    }
}

internal fun skipDetailsDownloadLoading(
    downloadMirrorStateHolder: DownloadMirrorSelectionStateHolder,
) {
    val state = downloadMirrorStateHolder.uiState.value
    if (!state.isLoading || state.loadedSourcesCount <= 0) return
    downloadMirrorStateHolder.onEvent(DownloadMirrorSelectionEvent.SkipLoadingUi)
}

internal fun openDetailsDownloadPanel(
    mode: DetailsScreenMode,
    context: Context,
    actionsCompat: MovieDetailsEpisodeActionsCompat,
    downloadButtonViewModel: DetailsDownloadButtonViewModel,
    downloadMirrorStateHolder: DownloadMirrorSelectionStateHolder,
    panelsStateHolder: DetailsPanelsStateHolder,
    preferredSeason: Int? = null,
    preferredEpisode: Int? = null,
) {
    val downloadMirrorState = downloadMirrorStateHolder.uiState.value
    if (panelsStateHolder.isActionInProgress ||
        panelsStateHolder.isPanelLoading ||
        downloadMirrorState.isLoading
    ) {
        return
    }

    if (mode.allowsBookmark) {
        panelsStateHolder.closeBookmarkPanel()
    }
    if (mode.allowsExtendedActions) {
        panelsStateHolder.closeActionsPanel()
    }
    downloadButtonViewModel.setPendingCompat(actionsCompat)
    downloadMirrorStateHolder.onEvent(
        DownloadMirrorSelectionEvent.Open(
            compat = actionsCompat,
            context = context,
            preferredSeason = preferredSeason,
            preferredEpisode = preferredEpisode,
        )
    )
}
