package com.lagradost.cloudstream3.homeserver

import com.lagradost.cloudstream3.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okio.buffer
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** HTTP error answered by the server or S3; [code] tells "try again" (5xx, 408, 429) from "gone" (other 4xx). */
class HomeServerHttpException(val code: Int, message: String) : IOException(message) {
    val isRetryable: Boolean get() = code >= 500 || code == 408 || code == 429
}

/** Minimal client of cloudstream-server. All URLs it returns are presigned (no headers needed). */
class HomeServerApi(private val baseUrl: String) {
    private val client get() = app.baseClient
    private val json = "application/json".toMediaType()

    suspend fun health(): Boolean = runCatching { get("/health"); true }.getOrDefault(false)

    suspend fun library(): List<ServerLibraryItem> =
        get("/library").getJSONArray("items").mapObjects(::parseItem)

    suspend fun playable(id: String): ServerPlayable {
        val o = get("/library/$id")
        val subs = o.getJSONArray("subtitles").mapObjects { ServerSubtitle(it.getString("lang"), it.getString("url")) }
        return ServerPlayable(parseItem(o), o.getJSONObject("video").getString("url"), subs)
    }

    suspend fun delete(id: String) {
        call(Request.Builder().url("$baseUrl/api/v1/library/$id").delete().build())
    }

    // ---- backup (settings + data store) ----

    data class BackupMeta(val version: Int, val hash: String)

    suspend fun backupMeta(): BackupMeta {
        val o = get("/backup")
        return BackupMeta(o.getInt("version"), o.optString("hash"))
    }

    /** The stored backup document as is, or null when the server has none. */
    suspend fun backupData(): String? = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url("$baseUrl/api/v1/backup/data").get().build()).execute().use {
            when {
                it.code == 404 -> null
                !it.isSuccessful -> throw IOException("Server ${it.code} backup")
                else -> it.body.string()
            }
        }
    }

    /** Stores [json] if [base] is still the server's version; null when it moved on (no overwrite, no merge). */
    suspend fun putBackup(json: String, base: Int): BackupMeta? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$baseUrl/api/v1/backup?base=$base")
            .put(json.toRequestBody(this@HomeServerApi.json)).build()
        client.newCall(req).execute().use {
            when {
                it.code == 409 -> null
                !it.isSuccessful -> throw IOException("Server ${it.code} backup")
                else -> JSONObject(it.body.string()).let { o -> BackupMeta(o.getInt("version"), o.optString("hash")) }
            }
        }
    }

    // ---- upload (phone side) ----

    /** @return pair(id, partSize) */
    suspend fun startUpload(m: UploadMetadata): Pair<String, Int> {
        val body = JSONObject()
            .put("title", m.title).put("filename", m.filename).put("size", m.size)
            .put("poster", m.poster).put("parent_id", m.parentId).put("episode_id", m.episodeId)
            .put("api_name", m.apiName).put("url", m.url)
            .put("season", m.season).put("episode", m.episode)
        val o = post("/library/uploads", body)
        return o.getString("id") to o.getInt("part_size")
    }

    suspend fun presignPart(id: String, n: Int): String = post("/library/$id/parts/$n", null).getString("url")

    suspend fun complete(id: String, parts: List<Pair<Int, String>>, size: Long) {
        val arr = JSONArray().also { a -> parts.forEach { (n, etag) -> a.put(JSONObject().put("n", n).put("etag", etag)) } }
        post("/library/$id/complete", JSONObject().put("parts", arr).put("size", size))
    }

    suspend fun presignSubtitle(id: String, lang: String, filename: String): String =
        post("/library/$id/subtitles", JSONObject().put("lang", lang).put("filename", filename)).getString("url")

    /** PUT bytes to a presigned URL; returns the ETag header (needed for multipart parts). */
    suspend fun putBytes(
        url: String,
        bytes: ByteArray,
        contentType: String? = null,
        onProgress: ((Long) -> Unit)? = null,
    ): String? =
        withContext(Dispatchers.IO) {
            val plain = bytes.toRequestBody(contentType?.toMediaType())
            val body = if (onProgress == null) plain else ProgressBody(plain, onProgress)
            val req = Request.Builder().url(url).put(body).build()
            uploadClient.newCall(req).execute().use {
                if (!it.isSuccessful) throw HomeServerHttpException(it.code, "S3 PUT ${it.code}")
                it.header("ETag")
            }
        }

    // ---- internals ----

    private suspend fun get(path: String) =
        call(Request.Builder().url("$baseUrl/api/v1$path").get().build())

    private suspend fun post(path: String, body: JSONObject?): JSONObject {
        val rb: RequestBody = (body?.toString() ?: "{}").toRequestBody(json)
        return call(Request.Builder().url("$baseUrl/api/v1$path").post(rb).build())
    }

    private suspend fun call(req: Request): JSONObject = withContext(Dispatchers.IO) {
        client.newCall(req).execute().use {
            if (!it.isSuccessful) throw HomeServerHttpException(it.code, "Server ${it.code} ${req.url.encodedPath}")
            val text = it.body.string()
            if (text.isBlank()) JSONObject() else JSONObject(text)
        }
    }

    private companion object {
        /** Parts are tens of MB and S3 may take a while to confirm one; the default 10 s timeouts are too short. */
        val uploadClient by lazy {
            app.baseClient.newBuilder()
                .writeTimeout(2, TimeUnit.MINUTES)
                .readTimeout(2, TimeUnit.MINUTES)
                .build()
        }
    }

    private fun parseItem(o: JSONObject) = ServerLibraryItem(
        id = o.getString("id"),
        title = o.getString("title"),
        poster = o.optString("poster").takeIf { it.isNotEmpty() && it != "null" },
        parentId = o.optIntOrNull("parent_id"),
        episodeId = o.optIntOrNull("episode_id"),
        apiName = o.optString("api_name"),
        url = o.optString("url"),
        season = o.optIntOrNull("season"),
        episode = o.optIntOrNull("episode"),
        size = o.optLong("size"),
        createdAt = o.optLong("created_at"),
    )

    private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key)) null else optInt(key)

    private fun <T> JSONArray.mapObjects(f: (JSONObject) -> T): List<T> =
        (0 until length()).map { f(getJSONObject(it)) }

    /** Reports the bytes handed to the socket so far (cumulative for this request). */
    private class ProgressBody(
        private val delegate: RequestBody,
        private val onProgress: (Long) -> Unit,
    ) : RequestBody() {
        override fun contentType() = delegate.contentType()
        override fun contentLength() = delegate.contentLength()
        override fun writeTo(sink: okio.BufferedSink) {
            val counting = object : okio.ForwardingSink(sink) {
                var written = 0L
                override fun write(source: okio.Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    written += byteCount
                    onProgress(written)
                }
            }
            val buffered = counting.buffer()
            delegate.writeTo(buffered)
            buffered.flush()
        }
    }
}
