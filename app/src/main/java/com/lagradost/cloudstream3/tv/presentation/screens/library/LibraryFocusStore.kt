package com.lagradost.cloudstream3.tv.presentation.screens.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

internal object LibraryFocusStore {
    var pendingRestoreTargetId by mutableStateOf<String?>(null)
        private set

    private var lastFocusedTargetId: String? = null

    fun onTargetFocused(targetId: String) {
        lastFocusedTargetId = targetId
    }

    fun scheduleRestoreToLastFocused() {
        pendingRestoreTargetId = lastFocusedTargetId
    }

    fun clearPendingRestore(targetId: String? = pendingRestoreTargetId) {
        if (pendingRestoreTargetId == targetId) {
            pendingRestoreTargetId = null
        }
    }

    fun feedPoster(sectionId: String, item: MediaItemCompat): String {
        return "library:section:$sectionId:item:${mediaIdentity(item)}"
    }

    fun feedShowMore(sectionId: String): String {
        return "library:section:$sectionId:show_more"
    }

    private fun mediaIdentity(item: MediaItemCompat): String {
        return "${item.apiName}|${item.id}|${item.url}"
    }
}
