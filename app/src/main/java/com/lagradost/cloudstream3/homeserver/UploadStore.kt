package com.lagradost.cloudstream3.homeserver

import android.content.Context
import androidx.core.content.edit
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson

/** One multipart upload on the server; kept until it is finished so an interrupted upload resumes instead of restarting. */
data class UploadSession(
    @JsonProperty("id") val id: String,
    @JsonProperty("partSize") val partSize: Int,
    @JsonProperty("size") val size: Long,
    @JsonProperty("parts") val parts: List<UploadedPart> = emptyList(),
    /** The server already assembled the video; only subtitles / local cleanup are left. Never delete it then. */
    @JsonProperty("completed") val completed: Boolean = false,
)

data class UploadedPart(
    @JsonProperty("n") val n: Int,
    @JsonProperty("etag") val etag: String,
)

/** Persistent upload queue (ordered episode ids) and the sessions of started uploads. Survives process death. */
object UploadStore {
    private const val PREFS = "home_server_uploads"
    private const val KEY_QUEUE = "queue"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun sessionKey(episodeId: Int) = "session_$episodeId"

    /** Server id written by the old WorkManager uploader; such an upload can not be resumed. */
    private fun legacyPendingKey(episodeId: Int) = "pending_$episodeId"

    @Synchronized
    fun queue(context: Context): List<Int> =
        prefs(context).getString(KEY_QUEUE, null).orEmpty().split(',').mapNotNull { it.toIntOrNull() }

    /** @return false when the episode is already queued. */
    @Synchronized
    fun add(context: Context, episodeId: Int): Boolean {
        val current = queue(context)
        if (episodeId in current) return false
        writeQueue(context, current + episodeId)
        return true
    }

    @Synchronized
    fun remove(context: Context, episodeId: Int) = writeQueue(context, queue(context) - episodeId)

    private fun writeQueue(context: Context, ids: List<Int>) =
        prefs(context).edit { putString(KEY_QUEUE, ids.joinToString(",")) }

    fun session(context: Context, episodeId: Int): UploadSession? =
        prefs(context).getString(sessionKey(episodeId), null)?.let { runCatching { parseJson<UploadSession>(it) }.getOrNull() }

    /** commit(), not apply(): the session must be on disk before the next part starts, or a kill would lose it. */
    fun saveSession(context: Context, episodeId: Int, session: UploadSession) {
        prefs(context).edit(commit = true) { putString(sessionKey(episodeId), session.toJson()) }
    }

    fun clearSession(context: Context, episodeId: Int) {
        prefs(context).edit(commit = true) { remove(sessionKey(episodeId)).remove(legacyPendingKey(episodeId)) }
    }

    fun legacyPending(context: Context, episodeId: Int): String? = prefs(context).getString(legacyPendingKey(episodeId), null)
}
