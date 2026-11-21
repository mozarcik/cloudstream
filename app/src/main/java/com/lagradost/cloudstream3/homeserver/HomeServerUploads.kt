package com.lagradost.cloudstream3.homeserver

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager.DownloadType

/** Queues an upload for every finished download when "auto upload" is enabled and resumes unfinished ones. */
object HomeServerUploads {
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        (app as? Application)?.let(HomeServerSync::install)
        HomeServerUploadService.restoreQueuedState(app)
        // Uploads used to be WorkManager jobs; drop leftovers of the removed worker (WorkManager tags them by class).
        runCatching { androidx.work.WorkManager.getInstance(app).cancelAllWorkByTag("com.lagradost.cloudstream3.homeserver.LibraryUploadWorker") }
        VideoDownloadManager.downloadStatusEvent += { (id, type) ->
            if (type == DownloadType.IsDone && HomeServerConfig.autoUpload(app)) {
                HomeServerUploadService.enqueue(app, id)
            }
        }
        // A visible activity may start a foreground service: pick up uploads interrupted by a kill or a reboot.
        (app as? Application)?.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (HomeServerConfig.isEnabled(app)) HomeServerUploadService.resumePending(app)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
