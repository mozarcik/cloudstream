package com.lagradost.cloudstream3.tv.presentation.screens.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object HomeFeedGridSelectionStore {
    private const val FocusDebugTag = "TvHomeFocus"
    private val _selectedFeed = MutableStateFlow<FeedCategory?>(null)
    val selectedFeed: StateFlow<FeedCategory?> = _selectedFeed.asStateFlow()

    var pendingRestoreTargetId by mutableStateOf<String?>(null)
        private set
    var pendingRestoreIndex by mutableStateOf<Int?>(null)
        private set

    private var lastFocusedTargetId: String? = null
    private var lastFocusedIndex: Int? = null

    fun setSelectedFeed(feed: FeedCategory) {
        _selectedFeed.value = feed
        Log.d(FocusDebugTag, "setSelectedFeed id=${feed.id}")
    }

    fun onTargetFocused(item: MediaItemCompat, index: Int) {
        lastFocusedTargetId = mediaTarget(item)
        lastFocusedIndex = index
        Log.d(FocusDebugTag, "grid onTargetFocused target=$lastFocusedTargetId index=$index")
    }

    fun scheduleRestoreToLastFocused() {
        pendingRestoreTargetId = lastFocusedTargetId
        pendingRestoreIndex = lastFocusedIndex
        Log.d(
            FocusDebugTag,
            "grid scheduleRestoreToLastFocused target=$pendingRestoreTargetId index=$pendingRestoreIndex"
        )
    }

    fun clearPendingRestore(targetId: String? = pendingRestoreTargetId) {
        if (pendingRestoreTargetId == targetId) {
            Log.d(
                FocusDebugTag,
                "grid clearPendingRestore target=$targetId index=$pendingRestoreIndex"
            )
            pendingRestoreTargetId = null
            pendingRestoreIndex = null
        }
    }

    fun mediaTarget(item: MediaItemCompat): String {
        return "home_feed_grid:item:${item.apiName}|${item.id}|${item.url}"
    }
}
