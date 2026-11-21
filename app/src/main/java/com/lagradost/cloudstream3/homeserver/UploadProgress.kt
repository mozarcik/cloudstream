package com.lagradost.cloudstream3.homeserver

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class UploadPhase { Queued, Uploading, Failed }

/** Upload of one downloaded episode; [total] is 0 until the file size is known, [error] says why it failed. */
data class UploadState(val phase: UploadPhase, val sent: Long = 0L, val total: Long = 0L, val error: String? = null) {
    val fraction: Float get() = if (total > 0) (sent.toFloat() / total).coerceIn(0f, 1f) else 0f
}

/** In-memory progress of running uploads, keyed by episode id, for the UI (the notification has its own copy). */
object UploadProgress {
    private val _state = MutableStateFlow<Map<Int, UploadState>>(emptyMap())
    val state: StateFlow<Map<Int, UploadState>> = _state.asStateFlow()

    fun set(episodeId: Int, value: UploadState) = _state.update { it + (episodeId to value) }

    fun clear(episodeId: Int) = _state.update { it - episodeId }
}
