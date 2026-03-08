package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class DetailsDownloadButtonViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DetailsDownloadButtonUiState())
    val uiState: StateFlow<DetailsDownloadButtonUiState> = _uiState.asStateFlow()

    private val compatRegistry = DetailsDownloadButtonCompatRegistry()
    private val logTracker = DetailsDownloadButtonLogTracker()
    private val snapshotController = DetailsDownloadButtonSnapshotController(
        compatRegistry = compatRegistry,
        logTracker = logTracker,
        publishState = { state -> _uiState.value = state },
        resetState = ::resetUiState,
    )

    private val statusObserver: (Pair<Int, VideoDownloadManager.DownloadType>) -> Unit =
        { (id, status) -> handleStatusChanged(id = id, status = status) }
    private val progressObserver: (Triple<Int, Long, Long>) -> Unit =
        { (id, downloaded, total) ->
            handleProgressChanged(
                id = id,
                downloadedBytes = downloaded,
                totalBytes = total,
            )
        }

    init {
        VideoDownloadManager.downloadStatusEvent += statusObserver
        VideoDownloadManager.downloadProgressEvent += progressObserver
    }

    fun setDefaultCompat(compat: MovieDetailsEpisodeActionsCompat) {
        compatRegistry.setDefaultCompat(compat)
        resetUiState()
    }

    fun setPendingCompat(compat: MovieDetailsEpisodeActionsCompat) {
        compatRegistry.setPendingCompat(compat)
    }

    fun clearPendingCompat() {
        compatRegistry.clearPendingCompat()
    }

    fun markPending() {
        logTracker.markPending()
        _uiState.value = PendingDetailsDownloadButtonUiState
    }

    fun markFailed() {
        logTracker.markFailed()
        _uiState.value = _uiState.value.copy(
            status = VideoDownloadManager.DownloadType.IsFailed,
            progressFraction = 0f,
        )
    }

    suspend fun refreshDefaultSnapshot(
        context: Context?,
        reason: String,
    ) = snapshotController.refreshSnapshot(
            context = context,
            reason = reason,
            compat = compatRegistry.resolveDefaultCompat(),
        )

    suspend fun refreshPendingOrDefaultSnapshot(
        context: Context?,
        reason: String,
    ) = snapshotController.refreshSnapshot(
            context = context,
            reason = reason,
            compat = compatRegistry.resolvePendingOrDefaultCompat(),
        )

    override fun onCleared() {
        VideoDownloadManager.downloadStatusEvent -= statusObserver
        VideoDownloadManager.downloadProgressEvent -= progressObserver
        super.onCleared()
    }

    private fun handleStatusChanged(
        id: Int,
        status: VideoDownloadManager.DownloadType,
    ) {
        val currentState = _uiState.value
        if (currentState.episodeId != id) return

        val nextState = currentState.withStatusUpdate(status)
        _uiState.value = nextState
        snapshotController.requestPendingOrDefaultSnapshotRefresh(
            scope = viewModelScope,
            reason = "status_event_$status",
        )

        if (logTracker.shouldLogPendingWarning(status = status, nextProgress = nextState.progressFraction)) {
            android.util.Log.w(
                TAG,
                "download pending with 0% progress. If it persists, check battery optimization/background restrictions for CloudStream."
            )
        }
    }

    private fun handleProgressChanged(
        id: Int,
        downloadedBytes: Long,
        totalBytes: Long,
    ) {
        val currentState = _uiState.value
        if (currentState.episodeId != id) return

        val nextState = currentState.withProgressUpdate(
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
        )
        _uiState.value = nextState

        if (!logTracker.shouldLogProgress(nextState.progressFraction)) return

        android.util.Log.d(
            TAG,
            "download progress event: id=$id progress=${nextState.progressFraction.toProgressPercent()}% bytes=$downloadedBytes/$totalBytes"
        )
    }

    private fun resetUiState() {
        _uiState.value = DetailsDownloadButtonUiState()
        logTracker.reset()
    }

    private companion object {
        private const val TAG = "TvDetailsDownloadVM"
    }
}
