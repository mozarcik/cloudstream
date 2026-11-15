package com.lagradost.cloudstream3.homeserver

import android.content.Context
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager.DownloadType

/** Queues an upload for every finished download when "auto upload" is enabled. */
object HomeServerUploads {
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        VideoDownloadManager.downloadStatusEvent += { (id, type) ->
            if (type == DownloadType.IsDone && HomeServerConfig.autoUpload(app)) {
                LibraryUploadWorker.enqueue(app, id)
            }
        }
    }
}
