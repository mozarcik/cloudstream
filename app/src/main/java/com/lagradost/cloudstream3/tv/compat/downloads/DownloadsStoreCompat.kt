package com.lagradost.cloudstream3.tv.compat.downloads

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.lagradost.cloudstream3.utils.DOWNLOAD_CHECK
import com.lagradost.cloudstream3.utils.DOWNLOAD_EPISODE_CACHE
import com.lagradost.cloudstream3.utils.DataStore.getFolderName
import com.lagradost.cloudstream3.utils.DataStore.getKey
import com.lagradost.cloudstream3.utils.DataStore.removeKey
import com.lagradost.cloudstream3.utils.VideoDownloadHelper
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal data class DownloadArtwork(
    val posterUrl: String?,
    val backdropUrl: String?,
)

private val ActiveWorkStates = setOf(
    WorkInfo.State.ENQUEUED,
    WorkInfo.State.RUNNING,
    WorkInfo.State.BLOCKED,
)

internal fun resolveDownloadArtwork(
    header: VideoDownloadHelper.DownloadHeaderCached,
    episode: VideoDownloadHelper.DownloadEpisodeCached,
): DownloadArtwork {
    val episodePoster = episode.poster?.takeIf { it.isNotBlank() }
    val headerPoster = header.poster?.takeIf { it.isNotBlank() }
    val headerBackdrop = header.backdrop?.takeIf { it.isNotBlank() }

    return DownloadArtwork(
        posterUrl = episodePoster ?: headerPoster ?: headerBackdrop,
        backdropUrl = headerBackdrop ?: headerPoster ?: episodePoster,
    )
}

internal fun hasStoredDownloadRequest(
    context: Context,
    episodeId: Int,
): Boolean {
    val key = episodeId.toString()
    val hasWorkerInfo = context.getKey<VideoDownloadManager.DownloadInfo>(
        VideoDownloadManager.WORK_KEY_INFO,
        key
    ) != null
    val hasWorkerPackage = context.getKey<VideoDownloadManager.DownloadResumePackage>(
        VideoDownloadManager.WORK_KEY_PACKAGE,
        key
    ) != null
    val hasResumePackage = VideoDownloadManager.getDownloadResumePackage(context, episodeId) != null

    return hasWorkerInfo || hasWorkerPackage || hasResumePackage
}

internal fun hasActiveDownloadRequest(
    context: Context,
    episodeId: Int,
    status: VideoDownloadManager.DownloadType?,
    hasStoredRequest: Boolean,
    isDownloadCheckWorkerActive: Boolean,
): Boolean {
    if (isEpisodeWorkerActive(context, episodeId)) {
        return true
    }

    if (VideoDownloadManager.downloadQueue.any { pkg -> pkg.item.ep.id == episodeId }) {
        return true
    }

    if (!hasStoredRequest) {
        return false
    }

    return isDownloadCheckWorkerActive && when (status) {
        null,
        VideoDownloadManager.DownloadType.IsPending,
        VideoDownloadManager.DownloadType.IsDownloading -> true
        else -> false
    }
}

internal fun isStaleZeroProgressDownload(
    status: VideoDownloadManager.DownloadType?,
    hasStoredRequest: Boolean,
    hasActiveRequest: Boolean,
    downloadedBytes: Long,
    totalBytes: Long,
): Boolean {
    if (!hasStoredRequest || hasActiveRequest) {
        return false
    }

    if (downloadedBytes > 0L || totalBytes > 0L) {
        return false
    }

    return when (status) {
        null,
        VideoDownloadManager.DownloadType.IsPending,
        VideoDownloadManager.DownloadType.IsDownloading -> true
        else -> false
    }
}

internal fun resolveNormalizedDownloadStatus(
    status: VideoDownloadManager.DownloadType?,
    downloadedBytes: Long,
    totalBytes: Long,
    hasPendingRequest: Boolean,
    isStaleZeroProgress: Boolean,
): VideoDownloadManager.DownloadType? {
    if (isStaleZeroProgress) {
        return VideoDownloadManager.DownloadType.IsFailed
    }

    if (status != null) {
        return status
    }

    if (hasPendingRequest) {
        return if (downloadedBytes > 0L) {
            VideoDownloadManager.DownloadType.IsDownloading
        } else {
            VideoDownloadManager.DownloadType.IsPending
        }
    }

    if (downloadedBytes > 0L && totalBytes <= 0L) {
        return VideoDownloadManager.DownloadType.IsDownloading
    }

    if (totalBytes <= 0L) {
        return null
    }

    val isDone = downloadedBytes > 1024L &&
        downloadedBytes + 1024L >= totalBytes

    return if (isDone) {
        VideoDownloadManager.DownloadType.IsDone
    } else {
        VideoDownloadManager.DownloadType.IsDownloading
    }
}

internal fun calculateDownloadProgressFraction(
    downloadedBytes: Long,
    totalBytes: Long,
): Float {
    if (downloadedBytes <= 0L || totalBytes <= 0L) {
        return 0f
    }

    return (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}

internal fun isDownloadCheckWorkerActive(context: Context): Boolean {
    return isUniqueWorkActive(context, DOWNLOAD_CHECK)
}

internal suspend fun deleteDownloadEntry(
    context: Context,
    episodeId: Int,
    parentId: Int,
): Boolean = withContext(Dispatchers.IO) {
    val fileUri = VideoDownloadManager.getDownloadFileInfoAndUpdateSettings(context, episodeId)?.path
    val deletedByManager = awaitLegacyDelete(context, episodeId)
    val deletedByUri = if (!deletedByManager) {
        deleteDownloadUri(context, fileUri)
    } else {
        false
    }
    val shouldForceRemove = deletedByManager || deletedByUri || fileUri == null

    if (shouldForceRemove) {
        forceRemoveDownloadState(
            context = context,
            episodeId = episodeId,
            parentId = parentId,
        )
    }

    if (!shouldForceRemove) {
        return@withContext false
    }

    deletedByManager || deletedByUri || !hasCachedDownloadEntry(
        context = context,
        episodeId = episodeId,
        parentId = parentId,
    )
}

private suspend fun awaitLegacyDelete(
    context: Context,
    episodeId: Int,
): Boolean = suspendCancellableCoroutine { continuation ->
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    VideoDownloadManager.deleteFilesAndUpdateSettings(
        context = context,
        ids = setOf(episodeId),
        scope = scope,
    ) { successfulIds ->
        if (continuation.isActive) {
            continuation.resume(episodeId in successfulIds)
        }
        scope.cancel()
    }

    continuation.invokeOnCancellation {
        scope.cancel()
    }
}

private fun forceRemoveDownloadState(
    context: Context,
    episodeId: Int,
    parentId: Int,
) {
    val key = episodeId.toString()

    VideoDownloadManager.downloadEvent.invoke(
        episodeId to VideoDownloadManager.DownloadActionType.Stop
    )
    WorkManager.getInstance(context).cancelUniqueWork(key)

    val queueIterator = VideoDownloadManager.downloadQueue.iterator()
    while (queueIterator.hasNext()) {
        if (queueIterator.next().item.ep.id == episodeId) {
            queueIterator.remove()
        }
    }

    context.removeKey(VideoDownloadManager.KEY_RESUME_PACKAGES, key)
    context.removeKey(VideoDownloadManager.WORK_KEY_INFO, key)
    context.removeKey(VideoDownloadManager.WORK_KEY_PACKAGE, key)
    context.removeKey(VideoDownloadManager.KEY_DOWNLOAD_INFO, key)
    context.removeKey(
        getFolderName(DOWNLOAD_EPISODE_CACHE, parentId.toString()),
        episodeId.toString()
    )

    VideoDownloadManager.downloadStatus.remove(episodeId)
    VideoDownloadManager.downloadProgressEvent.invoke(Triple(episodeId, 0L, 0L))
    VideoDownloadManager.downloadDeleteEvent.invoke(episodeId)
}

private fun hasCachedDownloadEntry(
    context: Context,
    episodeId: Int,
    parentId: Int,
): Boolean {
    return context.getKey<VideoDownloadHelper.DownloadEpisodeCached>(
        getFolderName(DOWNLOAD_EPISODE_CACHE, parentId.toString()),
        episodeId.toString()
    ) != null
}

private fun isEpisodeWorkerActive(
    context: Context,
    episodeId: Int,
): Boolean {
    return isUniqueWorkActive(context, episodeId.toString())
}

private fun isUniqueWorkActive(
    context: Context,
    key: String,
): Boolean {
    return runCatching {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(key)
            .get()
            .any { workInfo -> workInfo.state in ActiveWorkStates }
    }.getOrDefault(false)
}

private fun deleteDownloadUri(
    context: Context,
    uri: Uri?,
): Boolean {
    if (uri == null) {
        return false
    }

    return runCatching {
        when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> {
                val file = File(uri.path ?: return@runCatching false)
                !file.exists() || file.delete()
            }

            else -> {
                val deletedByResolver = context.contentResolver.delete(uri, null, null) > 0
                if (deletedByResolver) {
                    true
                } else {
                    DocumentFile.fromSingleUri(context, uri)?.let { file ->
                        !file.exists() || file.delete()
                    } ?: false
                }
            }
        }
    }.getOrDefault(false)
}
