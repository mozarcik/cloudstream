package com.lagradost.cloudstream3.tv.compat

import android.content.Context
import com.lagradost.cloudstream3.tv.compat.downloads.hasActiveDownloadRequest
import com.lagradost.cloudstream3.tv.compat.downloads.hasStoredDownloadRequest
import com.lagradost.cloudstream3.tv.compat.downloads.isDownloadCheckWorkerActive
import com.lagradost.cloudstream3.tv.compat.downloads.isStaleZeroProgressDownload
import com.lagradost.cloudstream3.tv.compat.downloads.resolveNormalizedDownloadStatus
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class MovieDetailsDownloadSnapshotResolver {
    suspend fun resolve(
        context: Context?,
        target: MovieDetailsActionTarget,
    ): MovieDetailsCompatDownloadSnapshot {
        val episodeId = target.episode.id
        val downloadedFileInfo = context?.let { ctx ->
            withContext(Dispatchers.IO) {
                VideoDownloadManager.getDownloadFileInfoAndUpdateSettings(ctx, episodeId)
            }
        }
        val downloadedBytes = downloadedFileInfo?.fileLength ?: 0L
        val totalBytes = downloadedFileInfo?.totalBytes ?: 0L
        val rawStatus = VideoDownloadManager.downloadStatus[episodeId]
        val hasPendingRequest = context?.let { ctx ->
            withContext(Dispatchers.IO) {
                val hasStoredRequest = hasStoredDownloadRequest(ctx, episodeId)
                hasActiveDownloadRequest(
                    context = ctx,
                    episodeId = episodeId,
                    status = rawStatus,
                    hasStoredRequest = hasStoredRequest,
                    isDownloadCheckWorkerActive = isDownloadCheckWorkerActive(ctx),
                )
            }
        } ?: false
        val resolvedStatus = context?.let { ctx ->
            withContext(Dispatchers.IO) {
                val hasStoredRequest = hasStoredDownloadRequest(ctx, episodeId)
                resolveNormalizedDownloadStatus(
                    status = rawStatus,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
                    hasPendingRequest = hasPendingRequest,
                    isStaleZeroProgress = isStaleZeroProgressDownload(
                        status = rawStatus,
                        hasStoredRequest = hasStoredRequest,
                        hasActiveRequest = hasPendingRequest,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes,
                    ),
                )
            }
        } ?: rawStatus

        return MovieDetailsCompatDownloadSnapshot(
            episodeId = episodeId,
            status = resolvedStatus,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            hasPendingRequest = hasPendingRequest,
        )
    }
}
