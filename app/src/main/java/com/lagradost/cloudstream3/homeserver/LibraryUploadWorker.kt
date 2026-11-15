package com.lagradost.cloudstream3.homeserver

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.utils.AppContextUtils.createNotificationChannel
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.lagradost.cloudstream3.tv.compat.downloads.deleteDownloadEntry
import com.lagradost.cloudstream3.utils.DOWNLOAD_EPISODE_CACHE
import com.lagradost.cloudstream3.utils.DOWNLOAD_HEADER_CACHE
import com.lagradost.cloudstream3.utils.DataStore.getFolderName
import com.lagradost.cloudstream3.utils.DataStore.getKey
import com.lagradost.cloudstream3.utils.DataStore.getKeys
import com.lagradost.cloudstream3.utils.SubtitleHelper.fromLanguageToTagIETF
import com.lagradost.cloudstream3.utils.SubtitleUtils.cleanDisplayName
import com.lagradost.cloudstream3.utils.SubtitleUtils.isMatchingSubtitle
import com.lagradost.cloudstream3.utils.downloader.DownloadFileManagement.getFolder
import com.lagradost.cloudstream3.utils.downloader.DownloadObjects
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Uploads an already downloaded episode (legacy downloader handles plugin headers/HLS) to the home server's S3
 * with presigned multipart PUTs. Failures retry from scratch; an orphaned server item is removed first.
 */
class LibraryUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val notificationId = NOTIFICATION_BASE + (inputData.getInt(KEY_EPISODE_ID, 0) and 0xFFFF)
    private var lastNotifyMs = 0L

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val episodeId = inputData.getInt(KEY_EPISODE_ID, 0)
        val base = HomeServerConfig.baseUrl(ctx) ?: return Result.failure()
        val api = HomeServerApi(base)
        // The finished event arrives while the downloader is still closing the file; read it only once its size is stable.
        val fileInfo = awaitSettledFile(ctx, episodeId) ?: return Result.failure()
        val stored = ctx.getKey<DownloadObjects.DownloadedFileInfo>(VideoDownloadManager.KEY_DOWNLOAD_INFO, episodeId.toString())
        val meta = buildMetadata(ctx, episodeId, stored?.displayName ?: "video.mp4", fileInfo.fileLength)
            ?: return Result.failure()
        val total = fileInfo.fileLength

        ctx.createNotificationChannel(
            CHANNEL_ID,
            ctx.getString(R.string.home_server_notification_channel),
            ctx.getString(R.string.home_server_notification_channel),
        )
        progress(ctx, episodeId, meta.title, 0L, total)
        runCatching { setForeground(foregroundInfo(ctx, meta.title, 0L, total)) }

        return try {
            val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.getString(pendingKey(episodeId), null)?.let { runCatching { api.delete(it) } }
            val (id, partSize) = api.startUpload(meta)
            prefs.edit().putString(pendingKey(episodeId), id).apply()

            val parts = uploadVideo(api, ctx, id, fileInfo.path, partSize) { sent ->
                progress(ctx, episodeId, meta.title, sent, total)
            }
            api.complete(id, parts, total)
            uploadSubtitles(api, ctx, id, stored)
            prefs.edit().remove(pendingKey(episodeId)).apply()
            if (HomeServerConfig.deleteAfterUpload(ctx)) {
                // Removes the file and the whole download entry; the legacy delete would leave a "failed" row behind.
                deleteDownloadEntry(ctx, episodeId, meta.parentId ?: episodeId)
            }
            UploadProgress.clear(episodeId)
            notifyResult(ctx, meta.title, ok = true)
            Result.success()
        } catch (t: kotlinx.coroutines.CancellationException) {
            UploadProgress.clear(episodeId)
            throw t
        } catch (t: Throwable) {
            if (runAttemptCount >= MAX_ATTEMPTS) {
                UploadProgress.set(episodeId, UploadState(UploadPhase.Failed, 0L, total))
                notifyResult(ctx, meta.title, ok = false)
                Result.failure()
            } else {
                UploadProgress.set(episodeId, UploadState(UploadPhase.Queued, 0L, total))
                Result.retry()
            }
        }
    }

    private suspend fun awaitSettledFile(ctx: Context, episodeId: Int): DownloadObjects.DownloadedFileInfoResult? {
        var last = -1L
        repeat(SETTLE_CHECKS) {
            val info = VideoDownloadManager.getDownloadFileInfo(ctx, episodeId) ?: return null
            if (info.fileLength == last && VideoDownloadManager.downloadStatus[episodeId] != VideoDownloadManager.DownloadType.IsDownloading) {
                return info
            }
            last = info.fileLength
            kotlinx.coroutines.delay(1000)
        }
        return VideoDownloadManager.getDownloadFileInfo(ctx, episodeId)
    }

    /** Updates the UI state at once and the notification at most twice a second. */
    private fun progress(ctx: Context, episodeId: Int, title: String, sent: Long, total: Long) {
        UploadProgress.set(episodeId, UploadState(UploadPhase.Uploading, sent, total))
        val now = System.currentTimeMillis()
        if (now - lastNotifyMs < 500 && sent < total) return
        lastNotifyMs = now
        ctx.getSystemService(NotificationManager::class.java)
            ?.notify(notificationId, buildNotification(ctx, title, sent, total))
    }

    private fun buildNotification(ctx: Context, title: String, sent: Long, total: Long): Notification {
        val percent = if (total > 0) (sent * 100 / total).toInt() else 0
        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cloudstream_monochrome_big)
            .setContentTitle(ctx.getString(R.string.home_server_uploading))
            .setContentText("$title \u2022 " + ctx.getString(R.string.home_server_upload_percent, percent))
            .setProgress(100, percent, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                ctx.getString(R.string.cancel),
                WorkManager.getInstance(ctx).createCancelPendingIntent(id),
            )
            .build()
    }

    private fun foregroundInfo(ctx: Context, title: String, sent: Long, total: Long): ForegroundInfo {
        val notification = buildNotification(ctx, title, sent, total)
        return if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun notifyResult(ctx: Context, title: String, ok: Boolean) {
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cloudstream_monochrome_big)
            .setContentTitle(ctx.getString(if (ok) R.string.home_server_upload_done else R.string.home_server_upload_failed))
            .setContentText(title)
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java)?.notify(notificationId, notification)
    }

    private suspend fun uploadVideo(
        api: HomeServerApi, ctx: Context, id: String, uri: Uri, partSize: Int, onSent: (Long) -> Unit,
    ): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
        var done = 0L
        val parts = ArrayList<Pair<Int, String>>()
        val input = ctx.contentResolver.openInputStream(uri) ?: error("cannot open $uri")
        input.use { stream ->
            val buf = ByteArray(partSize)
            var n = 1
            while (true) {
                val read = stream.readFully(buf)
                if (read == 0) break
                val bytes = if (read == buf.size) buf else buf.copyOf(read)
                val before = done
                val etag = api.putBytes(api.presignPart(id, n), bytes, onProgress = { onSent(before + it) })
                    ?: error("missing ETag")
                done += read
                onSent(done)
                parts += n to etag
                n++
                if (read < buf.size) break
            }
        }
        parts
    }

    private suspend fun uploadSubtitles(api: HomeServerApi, ctx: Context, id: String, info: DownloadObjects.DownloadedFileInfo?) {
        val relative = info?.relativePath ?: return
        val display = info.displayName
        val clean = cleanDisplayName(display)
        getFolder(ctx, relative, info.basePath)?.forEach { (name, uri) ->
            if (!isMatchingSubtitle(name, display, clean)) return@forEach
            runCatching {
                val langName = cleanDisplayName(name).removePrefix(clean).trim()
                val lang = fromLanguageToTagIETF(langName, true) ?: "und"
                val bytes = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                    ?: return@runCatching
                api.putBytes(api.presignSubtitle(id, lang, name), bytes, "text/plain; charset=utf-8")
            }
        }
    }

    private fun buildMetadata(ctx: Context, episodeId: Int, displayName: String, size: Long): UploadMetadata? {
        val episode = ctx.getKeys(DOWNLOAD_EPISODE_CACHE)
            .firstNotNullOfOrNull { key -> ctx.getKey<DownloadObjects.DownloadEpisodeCached>(key)?.takeIf { it.id == episodeId } }
        val parentId = episode?.parentId ?: episodeId // movies use the header id as episode id
        val header = ctx.getKey<DownloadObjects.DownloadHeaderCached>(DOWNLOAD_HEADER_CACHE, parentId.toString())
            ?: return null
        return UploadMetadata(
            title = header.name, filename = displayName, size = size,
            poster = episode?.poster ?: header.poster,
            parentId = header.id, episodeId = episodeId,
            apiName = header.apiName, url = header.url,
            season = episode?.season, episode = episode?.episode,
        )
    }

    private fun java.io.InputStream.readFully(buf: ByteArray): Int {
        var total = 0
        while (total < buf.size) {
            val r = read(buf, total, buf.size - total)
            if (r < 0) break
            total += r
        }
        return total
    }

    companion object {
        private const val KEY_EPISODE_ID = "episode_id"
        private const val PREFS = "home_server_uploads"
        private const val MAX_ATTEMPTS = 5
        private const val SETTLE_CHECKS = 15
        private const val CHANNEL_ID = "home_server_upload"
        private const val NOTIFICATION_BASE = 74000

        private fun pendingKey(episodeId: Int) = "pending_$episodeId"

        fun enqueue(context: Context, episodeId: Int) {
            UploadProgress.set(episodeId, UploadState(UploadPhase.Queued))
            val request = OneTimeWorkRequestBuilder<LibraryUploadWorker>()
                .setInputData(Data.Builder().putInt(KEY_EPISODE_ID, episodeId).build())
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("home_server_upload_$episodeId", ExistingWorkPolicy.KEEP, request)
        }
    }
}
