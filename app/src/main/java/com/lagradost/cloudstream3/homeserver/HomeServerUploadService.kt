package com.lagradost.cloudstream3.homeserver

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.AppContextUtils.createNotificationChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service (like the download queue) that uploads queued downloads to the home server one by one.
 * The queue and the progress of each upload are persistent ([UploadStore]): after a kill or a restart the
 * service continues with the next part instead of starting the file again.
 */
class HomeServerUploadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var queueJob: Job? = null
    @Volatile private var currentId: Int? = null
    @Volatile private var currentJob: Job? = null
    private var lastNotifyMs = 0L
    @Volatile private var lastStartId = 0

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createChannel(this)
        val notification = progressNotification(getString(R.string.home_server_uploading), null, 0L, 0L)
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(FOREGROUND_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(FOREGROUND_ID, notification)
            }
        } catch (e: IllegalStateException) {
            // Not allowed from the background right now; the queue stays and is resumed on the next app start.
            logError(e)
            showWaitingNotification(this)
            scope.cancel()
            stopSelf()
            return
        }
        NotificationManagerCompat.from(this).cancel(WAITING_ID)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        if (!scope.isActive) return START_NOT_STICKY
        if (intent?.action == ACTION_CANCEL) cancel(intent.getIntExtra(EXTRA_EPISODE_ID, 0))
        if (queueJob?.isActive != true) queueJob = scope.launch { runQueue() }
        return START_STICKY // restarted after a kill; the stored progress lets it continue
    }

    private suspend fun runQueue() {
        try {
            while (scope.isActive) {
                val id = UploadStore.queue(this).firstOrNull() ?: break
                currentId = id
                val job = scope.launch { process(id) }
                currentJob = job
                job.join() // throws when the service itself is stopped: the item then stays queued
                currentId = null
                UploadStore.remove(this, id)
            }
        } finally {
            // An upload queued while the loop was ending must not be left behind by stopping the service.
            if (scope.isActive) {
                if (UploadStore.queue(this).isNotEmpty() || !stopSelfResult(lastStartId)) {
                    queueJob = scope.launch { runQueue() }
                }
            }
        }
    }

    private suspend fun process(episodeId: Int) {
        val base = HomeServerConfig.baseUrl(this) ?: return UploadProgress.clear(episodeId)
        val uploader = LibraryUploader(this, HomeServerApi(base))
        var title = ""
        var total = 0L
        try {
            val (info, prepared) = uploader.prepare(episodeId) ?: return UploadProgress.clear(episodeId)
            title = info.title
            total = info.total
            uploader.upload(prepared) { sent -> progress(episodeId, title, sent, total) }
            UploadProgress.clear(episodeId)
            notifyResult(episodeId, title, ok = true)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "Upload of $episodeId failed", t)
            logError(t)
            val reason = describeError(t)
            // The session stays stored: "Send" again (or the next app start) resumes from the last part.
            UploadProgress.set(episodeId, UploadState(UploadPhase.Failed, 0L, total, reason))
            notifyResult(episodeId, title, ok = false, reason = reason)
        }
    }

    /** UI state at once, the notification at most twice a second. */
    private fun progress(episodeId: Int, title: String, sent: Long, total: Long) {
        UploadProgress.set(episodeId, UploadState(UploadPhase.Uploading, sent, total))
        val now = System.currentTimeMillis()
        if (now - lastNotifyMs < 500 && sent < total) return
        lastNotifyMs = now
        notify(FOREGROUND_ID, progressNotification(title, episodeId, sent, total))
    }

    private fun cancel(episodeId: Int) {
        UploadStore.remove(this, episodeId)
        UploadProgress.clear(episodeId)
        if (currentId == episodeId) currentJob?.cancel()
        // Cancelled by the user: the parts already sent are not needed any more.
        val session = UploadStore.session(this, episodeId)
        UploadStore.clearSession(this, episodeId)
        val base = HomeServerConfig.baseUrl(this)
        if (session != null && !session.completed && base != null) {
            scope.launch { runCatching { HomeServerApi(base).delete(session.id) } }
        }
    }

    private fun progressNotification(title: String, episodeId: Int?, sent: Long, total: Long): Notification {
        val percent = if (total > 0) (sent * 100 / total).toInt() else 0
        val queued = (UploadStore.queue(this).size - 1).coerceAtLeast(0)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cloudstream_monochrome_big)
            .setContentTitle(getString(R.string.home_server_uploading))
            .setContentText(if (episodeId == null) null else "$title • " + getString(R.string.home_server_upload_percent, percent))
            .setSubText(if (queued > 0) resources.getQuantityString(R.plurals.home_server_uploads_queued, queued, queued) else null)
            .setProgress(100, percent, episodeId == null || total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(openAppIntent(this))
            .apply {
                if (episodeId != null) addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    getString(R.string.cancel),
                    PendingIntentCompat.getService(
                        this@HomeServerUploadService, episodeId,
                        Intent(this@HomeServerUploadService, HomeServerUploadService::class.java)
                            .setAction(ACTION_CANCEL).putExtra(EXTRA_EPISODE_ID, episodeId),
                        PendingIntent.FLAG_UPDATE_CURRENT, false,
                    ),
                )
            }
            .build()
    }

    /** Short reason for the user; the full stack trace goes to logcat (tag [TAG]). */
    private fun describeError(t: Throwable): String = when (t) {
        is HomeServerHttpException -> listOfNotNull(getString(R.string.home_server_upload_error_http, t.code), t.body).joinToString(": ")
        is UploadFileException -> getString(R.string.home_server_upload_error_file)
        is java.io.IOException -> getString(R.string.home_server_upload_error_network, t.message ?: t.javaClass.simpleName)
        else -> listOfNotNull(t.javaClass.simpleName, t.message).joinToString(": ")
    }

    private fun notifyResult(episodeId: Int, title: String, ok: Boolean, reason: String? = null) {
        val text = listOfNotNull(title.takeIf { it.isNotBlank() }, reason).joinToString("\n")
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cloudstream_monochrome_big)
            .setContentTitle(getString(if (ok) R.string.home_server_upload_done else R.string.home_server_upload_failed))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(this))
            .setAutoCancel(true)
            .build()
        notify(RESULT_BASE_ID + (episodeId and 0xFFFF), notification)
    }

    private fun stopSelfSafely() {
        runCatching {
            if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
            stopSelf()
        }
    }

    /** Android 15+: dataSync services get ~6 h a day. Stop now; the stored progress is picked up on the next start. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.w(TAG, "Upload service timed out")
        scope.cancel()
        stopSelfSafely()
    }

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notify(id: Int, notification: Notification) = notify(this, id, notification)

    companion object {
        private const val TAG = "HomeServerUpload"
        private const val CHANNEL_ID = "home_server_upload"
        private const val FOREGROUND_ID = 74_999
        private const val WAITING_ID = 74_998
        private const val RESULT_BASE_ID = 75_000
        private const val ACTION_CANCEL = "cancel"
        private const val EXTRA_EPISODE_ID = "episode_id"

        @Volatile
        var isRunning = false
            private set

        /** Queues the episode and starts the service. A failed upload queued again resumes where it stopped. */
        fun enqueue(context: Context, episodeId: Int) {
            val app = context.applicationContext
            UploadStore.add(app, episodeId)
            UploadProgress.set(episodeId, UploadState(UploadPhase.Queued))
            start(app)
        }

        /** Continues queued uploads (app start, user tap); no-op when the queue is empty or the service runs. */
        fun resumePending(context: Context) {
            val app = context.applicationContext
            val queue = UploadStore.queue(app)
            if (queue.isEmpty() || isRunning) return
            queue.forEach { UploadProgress.set(it, UploadState(UploadPhase.Queued)) }
            start(app)
        }

        /** Marks queued uploads in the UI after a process start, without starting anything. */
        fun restoreQueuedState(context: Context) {
            UploadStore.queue(context.applicationContext).forEach { UploadProgress.set(it, UploadState(UploadPhase.Queued)) }
        }

        private fun start(app: Context) {
            try {
                ContextCompat.startForegroundService(app, Intent(app, HomeServerUploadService::class.java))
            } catch (e: IllegalStateException) {
                // Android 12+ refuses to start a foreground service while the app is in the background
                // (e.g. a download finished with the screen off). Ask the user instead: a tap is allowed to start it.
                logError(e)
                showWaitingNotification(app)
            }
        }

        private fun showWaitingNotification(app: Context) {
            createChannel(app)
            val startIntent = Intent(app, HomeServerUploadService::class.java)
            val tap = if (Build.VERSION.SDK_INT >= 26) {
                PendingIntent.getForegroundService(app, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            } else {
                PendingIntentCompat.getService(app, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT, false)
            }
            val notification = NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloudstream_monochrome_big)
                .setContentTitle(app.getString(R.string.home_server_upload_waiting))
                .setContentText(app.getString(R.string.home_server_upload_tap_to_start))
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build()
            notify(app, WAITING_ID, notification)
        }

        private fun createChannel(context: Context) = context.createNotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.home_server_notification_channel),
            context.getString(R.string.home_server_notification_channel),
        )

        private fun openAppIntent(context: Context): PendingIntent? =
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
                PendingIntentCompat.getActivity(context, 0, it, 0, false)
            }

        private fun notify(context: Context, id: Int, notification: Notification) {
            if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
        }
    }
}
