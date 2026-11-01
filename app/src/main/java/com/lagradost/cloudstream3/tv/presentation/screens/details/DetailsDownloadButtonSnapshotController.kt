package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.content.Context
import android.util.Log
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class DetailsDownloadButtonSnapshotController(
    private val compatRegistry: DetailsDownloadButtonCompatRegistry,
    private val logTracker: DetailsDownloadButtonLogTracker,
    private val publishState: (DetailsDownloadButtonUiState) -> Unit,
    private val resetState: () -> Unit,
) {
    suspend fun refreshSnapshot(
        context: Context?,
        reason: String,
        compat: MovieDetailsEpisodeActionsCompat?,
    ) {
        compatRegistry.updateContext(context)
        if (compat == null) {
            Log.d(TAG, "download snapshot[$reason]: compat unavailable")
            resetState()
            return
        }

        val publication = loadDetailsDownloadSnapshotPublication(
            context = context,
            compat = compat,
        )
        if (publication == null) {
            Log.d(TAG, "download snapshot[$reason]: null")
            resetState()
            return
        }

        Log.d(
            TAG,
            "download snapshot[$reason]: id=${publication.snapshot.episodeId} status=${publication.snapshot.status} normalizedStatus=${publication.uiState.status} downloaded=${publication.snapshot.downloadedBytes} total=${publication.snapshot.totalBytes} pending=${publication.snapshot.hasPendingRequest} progress=${publication.uiState.progressFraction.toProgressPercent()}%"
        )
        publishState(publication.uiState)
        logTracker.onSnapshotPublished(publication.uiState.progressFraction)
    }

    fun requestPendingOrDefaultSnapshotRefresh(
        scope: CoroutineScope,
        reason: String,
    ) {
        val context = compatRegistry.resolveAppContext() ?: return
        val compat = compatRegistry.resolvePendingOrDefaultCompat() ?: return
        scope.launch {
            refreshSnapshot(
                context = context,
                reason = reason,
                compat = compat,
            )
        }
    }

    private companion object {
        private const val TAG = "TvDetailsDownloadVM"
    }
}
