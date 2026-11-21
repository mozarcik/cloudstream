package com.lagradost.cloudstream3.tv.compat

import android.content.Context
import com.lagradost.cloudstream3.tv.compat.downloads.hasActiveDownloadRequest
import com.lagradost.cloudstream3.tv.compat.downloads.hasStoredDownloadRequest
import com.lagradost.cloudstream3.tv.compat.downloads.isDownloadCheckWorkerActive
import com.lagradost.cloudstream3.tv.compat.downloads.isStaleZeroProgressDownload
import com.lagradost.cloudstream3.tv.compat.downloads.resolveNormalizedDownloadStatus
import com.lagradost.cloudstream3.ui.result.getId
import com.lagradost.cloudstream3.utils.DOWNLOAD_HEADER_CACHE
import com.lagradost.cloudstream3.utils.DataStore.containsKey
import com.lagradost.cloudstream3.utils.DataStore.setKey
import com.lagradost.cloudstream3.utils.downloader.DownloadObjects
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager
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
                VideoDownloadManager.getDownloadFileInfo(ctx, episodeId)
            }
        }
        if (context != null && downloadedFileInfo != null) restoreMissingHeader(context, target)
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

    /**
     * The downloads list needs the title header; it used to be deleted together with a continue watching entry,
     * which hid the download while the file stayed. Write it back from the loaded page.
     */
    private fun restoreMissingHeader(context: Context, target: MovieDetailsActionTarget) {
        val page = target.loadResponse
        val parentId = page.getId()
        if (context.containsKey(DOWNLOAD_HEADER_CACHE, parentId.toString())) return
        context.setKey(
            DOWNLOAD_HEADER_CACHE,
            parentId.toString(),
            DownloadObjects.DownloadHeaderCached(
                apiName = page.apiName,
                url = page.url,
                type = page.type,
                name = page.name,
                poster = page.posterUrl ?: page.backgroundPosterUrl,
                backdrop = page.backgroundPosterUrl ?: page.posterUrl,
                cacheTime = System.currentTimeMillis(),
                id = parentId,
            ),
        )
    }
}
