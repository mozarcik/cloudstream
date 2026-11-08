package com.lagradost.cloudstream3.tv.presentation.screens.grid

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

/**
 * Remembers which poster of the "Show more" grid had focus when the user opened a details screen,
 * so Back can put focus on it again. One instance per grid source (home / library / search).
 */
class TvGridFocusStore(private val keyPrefix: String) {
    var pendingRestoreTargetId by mutableStateOf<String?>(null)
        private set
    var pendingRestoreIndex by mutableStateOf<Int?>(null)
        private set

    private var lastFocusedTargetId: String? = null
    private var lastFocusedIndex: Int? = null

    /** Poster that had focus last, or null when none has yet. */
    val lastIndex: Int? get() = lastFocusedIndex

    fun targetId(item: MediaItemCompat): String =
        "$keyPrefix:item:${item.apiName}|${item.id}|${item.url}"

    fun onItemFocused(item: MediaItemCompat, index: Int) {
        lastFocusedTargetId = targetId(item)
        lastFocusedIndex = index
    }

    fun scheduleRestoreToLastFocused() {
        pendingRestoreTargetId = lastFocusedTargetId
        pendingRestoreIndex = lastFocusedIndex
    }

    fun clearPendingRestore(targetId: String? = pendingRestoreTargetId) {
        if (pendingRestoreTargetId == targetId) {
            pendingRestoreTargetId = null
            pendingRestoreIndex = null
        }
    }

    /** A new list was opened: forget everything about the previous one. */
    fun reset() {
        pendingRestoreTargetId = null
        pendingRestoreIndex = null
        lastFocusedTargetId = null
        lastFocusedIndex = null
    }
}
