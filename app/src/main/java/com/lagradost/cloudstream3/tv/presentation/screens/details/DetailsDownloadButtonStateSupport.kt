package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatDownloadSnapshot
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class DetailsDownloadButtonLogTracker {
    private var lastLoggedProgressPercent = -1
    private var hasLoggedPendingWarning = false

    fun markPending() {
        hasLoggedPendingWarning = false
        lastLoggedProgressPercent = 0
    }

    fun markFailed() {
        hasLoggedPendingWarning = false
        lastLoggedProgressPercent = -1
    }

    fun reset() {
        lastLoggedProgressPercent = -1
        hasLoggedPendingWarning = false
    }

    fun onSnapshotPublished(progressFraction: Float) {
        lastLoggedProgressPercent = progressFraction.toProgressPercent()
        hasLoggedPendingWarning = false
    }

    fun shouldLogProgress(progressFraction: Float): Boolean {
        val progressPercent = progressFraction.toProgressPercent()
        val shouldLog = progressPercent != lastLoggedProgressPercent &&
            (progressPercent <= 5 || progressPercent % 5 == 0 || progressPercent == 100)
        if (shouldLog) {
            lastLoggedProgressPercent = progressPercent
        }
        return shouldLog
    }

    fun shouldLogPendingWarning(
        status: VideoDownloadManager.DownloadType,
        nextProgress: Float,
    ): Boolean {
        val shouldLog = status == VideoDownloadManager.DownloadType.IsPending &&
            nextProgress <= 0f &&
            !hasLoggedPendingWarning
        if (shouldLog) {
            hasLoggedPendingWarning = true
        }
        return shouldLog
    }
}

internal data class DetailsDownloadSnapshotPublication(
    val snapshot: MovieDetailsCompatDownloadSnapshot,
    val uiState: DetailsDownloadButtonUiState,
)

internal val PendingDetailsDownloadButtonUiState = DetailsDownloadButtonUiState(
    status = VideoDownloadManager.DownloadType.IsPending,
    progressFraction = 0f,
)

internal fun DetailsDownloadButtonUiState.withStatusUpdate(
    status: VideoDownloadManager.DownloadType,
): DetailsDownloadButtonUiState {
    val nextProgress = when (status) {
        VideoDownloadManager.DownloadType.IsDone -> 1f
        VideoDownloadManager.DownloadType.IsStopped,
        VideoDownloadManager.DownloadType.IsFailed -> 0f
        else -> progressFraction
    }
    return copy(
        status = status,
        progressFraction = nextProgress,
    )
}

internal fun DetailsDownloadButtonUiState.withProgressUpdate(
    downloadedBytes: Long,
    totalBytes: Long,
): DetailsDownloadButtonUiState {
    val progress = calculateProgressFraction(downloadedBytes, totalBytes)
    val nextProgress = when {
        status == VideoDownloadManager.DownloadType.IsDone -> 1f
        progress > 0f -> progress
        else -> progressFraction
    }
    return copy(progressFraction = nextProgress)
}

internal fun MovieDetailsCompatDownloadSnapshot.toDetailsDownloadButtonUiState(): DetailsDownloadButtonUiState {
    val normalizedStatus = normalizeDownloadStatus(this)
    val normalizedProgress = when (normalizedStatus) {
        VideoDownloadManager.DownloadType.IsDone -> 1f
        else -> calculateProgressFraction(downloadedBytes, totalBytes)
    }
    return DetailsDownloadButtonUiState(
        episodeId = episodeId,
        status = normalizedStatus,
        progressFraction = normalizedProgress,
    )
}

internal suspend fun loadDetailsDownloadSnapshotPublication(
    context: android.content.Context?,
    compat: MovieDetailsEpisodeActionsCompat,
): DetailsDownloadSnapshotPublication? {
    val snapshot = withContext(Dispatchers.IO) {
        compat.getDownloadSnapshot(context)
    } ?: return null
    return DetailsDownloadSnapshotPublication(
        snapshot = snapshot,
        uiState = snapshot.toDetailsDownloadButtonUiState(),
    )
}

internal fun Float.toProgressPercent(): Int {
    return (coerceIn(0f, 1f) * 100f).toInt()
}
