package com.lagradost.cloudstream3.tv.presentation.screens.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

internal object HomeFocusStore {
    private const val FocusDebugTag = "TvHomeFocus"
    const val ContinueWatchingResume = "home:continue_watching:resume"
    const val ContinueWatchingDetails = "home:continue_watching:details"
    const val ContinueWatchingRemove = "home:continue_watching:remove"
    const val Featured = "home:featured"
    const val SourcesMore = "home:sources:more"

    var pendingRestoreTargetId by mutableStateOf<String?>(null)
        private set

    private var lastFocusedTargetId: String? = null

    fun onTargetFocused(targetId: String) {
        lastFocusedTargetId = targetId
        Log.d(FocusDebugTag, "onTargetFocused target=$targetId")
    }

    fun scheduleRestoreToLastFocused() {
        pendingRestoreTargetId = lastFocusedTargetId
        Log.d(FocusDebugTag, "scheduleRestoreToLastFocused target=$pendingRestoreTargetId")
    }

    fun scheduleRestoreTo(targetId: String) {
        pendingRestoreTargetId = targetId
        Log.d(FocusDebugTag, "scheduleRestoreTo target=$targetId")
    }

    fun clearPendingRestore(targetId: String? = pendingRestoreTargetId) {
        if (pendingRestoreTargetId == targetId) {
            Log.d(FocusDebugTag, "clearPendingRestore target=$targetId")
            pendingRestoreTargetId = null
        }
    }

    fun continueWatchingCard(item: MediaItemCompat): String {
        return "home:continue_watching:card:${mediaIdentity(item)}"
    }

    fun feedPoster(feedId: String, item: MediaItemCompat): String {
        return "home:feed:$feedId:item:${mediaIdentity(item)}"
    }

    fun feedShowMore(feedId: String): String {
        return "home:feed:$feedId:show_more"
    }

    fun isContinueWatchingTarget(targetId: String?): Boolean {
        return targetId == ContinueWatchingResume ||
            targetId == ContinueWatchingDetails ||
            targetId == ContinueWatchingRemove ||
            targetId?.startsWith("home:continue_watching:card:") == true
    }

    fun feedIdFromTarget(targetId: String?): String? {
        val value = targetId ?: return null
        if (!value.startsWith("home:feed:")) return null

        val suffix = value.removePrefix("home:feed:")
        return when {
            ":item:" in suffix -> suffix.substringBefore(":item:")
            suffix.endsWith(":show_more") -> suffix.removeSuffix(":show_more")
            else -> null
        }
    }

    private fun mediaIdentity(item: MediaItemCompat): String {
        return "${item.apiName}|${item.id}|${item.url}"
    }
}
