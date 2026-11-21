package com.lagradost.cloudstream3.homeserver

import android.content.Context
import android.net.Uri
import com.lagradost.cloudstream3.tv.compat.downloads.deleteDownloadEntry
import com.lagradost.cloudstream3.utils.DOWNLOAD_EPISODE_CACHE
import com.lagradost.cloudstream3.utils.DOWNLOAD_HEADER_CACHE
import com.lagradost.cloudstream3.utils.DataStore.getKey
import com.lagradost.cloudstream3.utils.DataStore.getKeys
import com.lagradost.cloudstream3.utils.SubtitleHelper.fromLanguageToTagIETF
import com.lagradost.cloudstream3.utils.SubtitleUtils.cleanDisplayName
import com.lagradost.cloudstream3.utils.SubtitleUtils.isMatchingSubtitle
import com.lagradost.cloudstream3.utils.downloader.DownloadFileManagement.getFolder
import com.lagradost.cloudstream3.utils.downloader.DownloadObjects
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.IOException

/** The downloaded file can not be read (deleted, or the app lost access to the download folder). */
class UploadFileException(cause: Throwable?) : Exception("cannot read the downloaded file", cause)

/**
 * Uploads one downloaded episode to the home server's S3 with presigned multipart PUTs.
 * Progress is stored after every part ([UploadStore]), so an interrupted upload continues where it stopped.
 * Network errors are retried with backoff inside a part; only an upload the server no longer knows starts over.
 */
class LibraryUploader(private val ctx: Context, private val api: HomeServerApi) {

    class Info(val title: String, val total: Long)

    /** Reads what is needed before the upload starts; null when the download is gone (nothing to upload). */
    suspend fun prepare(episodeId: Int): Pair<Info, Prepared>? {
        // The finished event arrives while the downloader is still closing the file; read it only once its size is stable.
        val fileInfo = awaitSettledFile(episodeId) ?: return null
        val stored = ctx.getKey<DownloadObjects.DownloadedFileInfo>(VideoDownloadManager.KEY_DOWNLOAD_INFO, episodeId.toString())
        val meta = buildMetadata(episodeId, stored?.displayName ?: "video.mp4", fileInfo.fileLength)
        return Info(meta.title, fileInfo.fileLength) to Prepared(episodeId, fileInfo.path, meta, stored)
    }

    class Prepared(
        val episodeId: Int,
        val uri: Uri,
        val meta: UploadMetadata,
        val stored: DownloadObjects.DownloadedFileInfo?,
    )

    suspend fun upload(p: Prepared, onSent: (Long) -> Unit) {
        val total = p.meta.size
        // Upload id of the old WorkManager uploader: it kept no parts, so it can only be dropped.
        UploadStore.legacyPending(ctx, p.episodeId)?.let { runCatching { api.delete(it) } }

        var session = UploadStore.session(ctx, p.episodeId)
        if (session != null && session.size != total) {
            // The file changed (re-downloaded); the parts on the server belong to the old one.
            dropSession(p.episodeId, session)
            session = null
        }

        val resumed = session
        val finished = if (resumed?.completed == true) {
            resumed
        } else try {
            uploadVideo(p, resumed ?: startSession(p), onSent)
        } catch (e: HomeServerHttpException) {
            // Resumed upload unknown to the server (expired / removed): start once from scratch.
            if (resumed == null || e.isRetryable) throw e
            dropSession(p.episodeId, resumed)
            uploadVideo(p, startSession(p), onSent)
        }
        onSent(total)

        uploadSubtitles(finished.id, p.stored)
        UploadStore.clearSession(ctx, p.episodeId)
        if (HomeServerConfig.deleteAfterUpload(ctx)) {
            // Removes the file and the whole download entry; the legacy delete would leave a "failed" row behind.
            deleteDownloadEntry(ctx, p.episodeId, p.meta.parentId ?: p.episodeId)
        }
    }

    private suspend fun startSession(p: Prepared): UploadSession {
        val (id, partSize) = retrying { api.startUpload(p.meta) }
        return UploadSession(id, partSize, p.meta.size).also { UploadStore.saveSession(ctx, p.episodeId, it) }
    }

    private suspend fun dropSession(episodeId: Int, session: UploadSession) {
        runCatching { api.delete(session.id) }
        UploadStore.clearSession(ctx, episodeId)
    }

    /** Sends the parts after the ones already on the server, then assembles the video. @return the completed session */
    private suspend fun uploadVideo(p: Prepared, start: UploadSession, onSent: (Long) -> Unit): UploadSession =
        withContext(Dispatchers.IO) {
            var session = start
            val partSize = session.partSize
            var done = session.parts.size.toLong() * partSize
            onSent(done)
            // Not retried: a missing file or a lost folder permission does not fix itself.
            val fd = try {
                ctx.contentResolver.openFileDescriptor(p.uri, "r")
            } catch (e: Exception) {
                throw UploadFileException(e)
            } ?: throw UploadFileException(null)
            fd.use {
                FileInputStream(fd.fileDescriptor).use { stream ->
                    stream.channel.position(done)
                    val buf = ByteArray(partSize)
                    var n = session.parts.size + 1
                    while (done < session.size) {
                        val read = stream.readFully(buf)
                        if (read == 0) break
                        val bytes = if (read == buf.size) buf else buf.copyOf(read)
                        val before = done
                        val etag = retrying {
                            // Presigned per attempt: an expired URL is the usual cause of a failing retry.
                            api.putBytes(api.presignPart(session.id, n), bytes, onProgress = { onSent(before + it) })
                                ?: throw IOException("missing ETag")
                        }
                        done += read
                        session = session.copy(parts = session.parts + UploadedPart(n, etag))
                        UploadStore.saveSession(ctx, p.episodeId, session)
                        onSent(done)
                        n++
                        if (read < buf.size) break
                    }
                }
            }
            retrying { api.complete(session.id, session.parts.map { it.n to it.etag }, session.size) }
            session.copy(completed = true).also { UploadStore.saveSession(ctx, p.episodeId, it) }
        }

    private suspend fun uploadSubtitles(id: String, info: DownloadObjects.DownloadedFileInfo?) {
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
                retrying { api.putBytes(api.presignSubtitle(id, lang, name), bytes, "text/plain; charset=utf-8") }
            }
        }
    }

    /** Retries network errors and 5xx/408/429 with backoff; other HTTP errors are final and thrown at once. */
    private suspend fun <T> retrying(block: suspend () -> T): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: IOException) {
                if (e is HomeServerHttpException && !e.isRetryable) throw e
                if (attempt >= RETRY_DELAYS_MS.size) throw e
                delay(RETRY_DELAYS_MS[attempt++])
            }
        }
    }

    private suspend fun awaitSettledFile(episodeId: Int): DownloadObjects.DownloadedFileInfoResult? {
        var last = -1L
        repeat(SETTLE_CHECKS) {
            val info = VideoDownloadManager.getDownloadFileInfo(ctx, episodeId) ?: return null
            if (info.fileLength == last && VideoDownloadManager.downloadStatus[episodeId] != VideoDownloadManager.DownloadType.IsDownloading) {
                return info
            }
            last = info.fileLength
            delay(1000)
        }
        return VideoDownloadManager.getDownloadFileInfo(ctx, episodeId)
    }

    /**
     * Without the title header (removed together with a continue watching entry by older versions) the file is
     * still uploaded, named after the file; the server item then just has no link back to the provider page.
     */
    private fun buildMetadata(episodeId: Int, displayName: String, size: Long): UploadMetadata {
        val episode = ctx.getKeys(DOWNLOAD_EPISODE_CACHE)
            .firstNotNullOfOrNull { key -> ctx.getKey<DownloadObjects.DownloadEpisodeCached>(key)?.takeIf { it.id == episodeId } }
        val parentId = episode?.parentId ?: episodeId // movies use the header id as episode id
        val header = ctx.getKey<DownloadObjects.DownloadHeaderCached>(DOWNLOAD_HEADER_CACHE, parentId.toString())
        return UploadMetadata(
            title = header?.name ?: displayName.substringBeforeLast('.').ifBlank { episode?.name ?: displayName },
            filename = displayName, size = size,
            poster = episode?.poster ?: header?.poster,
            parentId = parentId, episodeId = episodeId,
            apiName = header?.apiName.orEmpty(), url = header?.url.orEmpty(),
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

    private companion object {
        const val SETTLE_CHECKS = 15
        /** ~6 minutes in total: long enough to ride out a Wi-Fi switch or a server restart. */
        val RETRY_DELAYS_MS = longArrayOf(3_000, 10_000, 30_000, 60_000, 120_000, 120_000)
    }
}
