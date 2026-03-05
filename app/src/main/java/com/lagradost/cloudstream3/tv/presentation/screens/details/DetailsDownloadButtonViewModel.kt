package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatDownloadSnapshot
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DetailsDownloadButtonViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsDownloadButtonUiState())
    val uiState: StateFlow<DetailsDownloadButtonUiState> = _uiState.asStateFlow()

    private var appContext: Context? = null
    private var defaultActionsCompat: MovieDetailsEpisodeActionsCompat? = null
    private var pendingActionsCompat: MovieDetailsEpisodeActionsCompat? = null
    private var lastLoggedProgressPercent = -1
    private var hasLoggedPendingWarning = false

    private val statusObserver: (Pair<Int, VideoDownloadManager.DownloadType>) -> Unit =
        { (id, status) ->
            handleStatusChanged(id = id, status = status)
        }

    private val progressObserver: (Triple<Int, Long, Long>) -> Unit =
        { (id, downloaded, total) ->
            handleProgressChanged(
                id = id,
                downloadedBytes = downloaded,
                totalBytes = total
            )
        }

    init {
        VideoDownloadManager.downloadStatusEvent += statusObserver
        VideoDownloadManager.downloadProgressEvent += progressObserver
    }

    fun setDefaultCompat(compat: MovieDetailsEpisodeActionsCompat) {
        defaultActionsCompat = compat
        pendingActionsCompat = null
        resetUiState()
    }

    fun setPendingCompat(compat: MovieDetailsEpisodeActionsCompat) {
        pendingActionsCompat = compat
    }

    fun clearPendingCompat() {
        pendingActionsCompat = null
    }

    fun markPending() {
        hasLoggedPendingWarning = false
        lastLoggedProgressPercent = 0
        _uiState.value = DetailsDownloadButtonUiState(
            status = VideoDownloadManager.DownloadType.IsPending,
            progressFraction = 0f
        )
    }

    fun markFailed() {
        hasLoggedPendingWarning = false
        lastLoggedProgressPercent = -1
        _uiState.value = _uiState.value.copy(
            status = VideoDownloadManager.DownloadType.IsFailed,
            progressFraction = 0f
        )
    }

    suspend fun refreshDefaultSnapshot(
        context: Context?,
        reason: String,
    ) {
        refreshSnapshot(
            context = context,
            reason = reason,
            compat = defaultActionsCompat
        )
    }

    suspend fun refreshPendingOrDefaultSnapshot(
        context: Context?,
        reason: String,
    ) {
        refreshSnapshot(
            context = context,
            reason = reason,
            compat = pendingActionsCompat ?: defaultActionsCompat
        )
    }

    override fun onCleared() {
        VideoDownloadManager.downloadStatusEvent -= statusObserver
        VideoDownloadManager.downloadProgressEvent -= progressObserver
        super.onCleared()
    }

    private suspend fun refreshSnapshot(
        context: Context?,
        reason: String,
        compat: MovieDetailsEpisodeActionsCompat?,
    ) {
        if (context != null) {
            appContext = context.applicationContext
        }

        if (compat == null) {
            Log.d(TAG, "download snapshot[$reason]: compat unavailable")
            resetUiState()
            return
        }

        val snapshot = withContext(Dispatchers.IO) {
            compat.getDownloadSnapshot(context)
        }
        publishSnapshot(snapshot = snapshot, reason = reason)
    }

    private fun publishSnapshot(
        snapshot: MovieDetailsCompatDownloadSnapshot?,
        reason: String,
    ) {
        if (snapshot == null) {
            Log.d(TAG, "download snapshot[$reason]: null")
            resetUiState()
            return
        }

        val normalizedStatus = normalizeDownloadStatus(snapshot)
        val normalizedProgress = when (normalizedStatus) {
            VideoDownloadManager.DownloadType.IsDone -> 1f
            else -> calculateProgressFraction(snapshot.downloadedBytes, snapshot.totalBytes)
        }

        Log.d(
            TAG,
            "download snapshot[$reason]: id=${snapshot.episodeId} status=${snapshot.status} normalizedStatus=$normalizedStatus downloaded=${snapshot.downloadedBytes} total=${snapshot.totalBytes} pending=${snapshot.hasPendingRequest} progress=${(normalizedProgress * 100f).toInt()}%"
        )

        _uiState.value = DetailsDownloadButtonUiState(
            episodeId = snapshot.episodeId,
            status = normalizedStatus,
            progressFraction = normalizedProgress
        )
        lastLoggedProgressPercent = (normalizedProgress * 100f).toInt()
        hasLoggedPendingWarning = false
    }

    private fun handleStatusChanged(
        id: Int,
        status: VideoDownloadManager.DownloadType,
    ) {
        val currentState = _uiState.value
        if (currentState.episodeId != id) return

        val nextProgress = when (status) {
            VideoDownloadManager.DownloadType.IsDone -> 1f
            VideoDownloadManager.DownloadType.IsStopped,
            VideoDownloadManager.DownloadType.IsFailed -> 0f
            else -> currentState.progressFraction
        }

        _uiState.value = currentState.copy(
            status = status,
            progressFraction = nextProgress
        )
        requestSnapshotRefresh(reason = "status_event_$status")

        if (status == VideoDownloadManager.DownloadType.IsPending &&
            nextProgress <= 0f &&
            !hasLoggedPendingWarning
        ) {
            hasLoggedPendingWarning = true
            Log.w(
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

        val progress = calculateProgressFraction(downloadedBytes, totalBytes)
        val nextProgress = when {
            currentState.status == VideoDownloadManager.DownloadType.IsDone -> 1f
            progress > 0f -> progress
            else -> currentState.progressFraction
        }

        _uiState.value = currentState.copy(progressFraction = nextProgress)

        val progressPercent = (nextProgress * 100f).toInt()
        val shouldLogProgress = progressPercent != lastLoggedProgressPercent &&
            (progressPercent <= 5 || progressPercent % 5 == 0 || progressPercent == 100)

        if (!shouldLogProgress) return

        lastLoggedProgressPercent = progressPercent
        Log.d(
            TAG,
            "download progress event: id=$id progress=$progressPercent% bytes=$downloadedBytes/$totalBytes"
        )
    }

    private fun resetUiState() {
        _uiState.value = DetailsDownloadButtonUiState()
        lastLoggedProgressPercent = -1
        hasLoggedPendingWarning = false
    }

    private fun requestSnapshotRefresh(reason: String) {
        val context = appContext ?: return
        val compat = pendingActionsCompat ?: defaultActionsCompat ?: return
        viewModelScope.launch {
            refreshSnapshot(
                context = context,
                reason = reason,
                compat = compat
            )
        }
    }

    private companion object {
        private const val TAG = "TvDetailsDownloadVM"
    }
}
