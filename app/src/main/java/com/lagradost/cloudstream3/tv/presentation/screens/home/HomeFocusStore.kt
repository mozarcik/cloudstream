package com.lagradost.cloudstream3.tv.presentation.screens.home

import android.util.Log
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.focus.TvFocusGroup

internal object HomeFocusStore {
    private const val FocusDebugTag = "TvHomeFocus"
    const val ContinueWatchingResume = "home:continue_watching:resume"
    const val ContinueWatchingDetails = "home:continue_watching:details"
    const val ContinueWatchingRemove = "home:continue_watching:remove"
    const val Sources = "home:sources"

    private var lastFocusedTargetId: String? = null
    val lastTargetId: String? get() = lastFocusedTargetId

    fun onTargetFocused(targetId: String) {
        lastFocusedTargetId = targetId
        Log.d(FocusDebugTag, "onTargetFocused target=$targetId")
    }

    fun resetLastFocused() {
        lastFocusedTargetId = null
    }

    fun continueWatchingCard(item: MediaItemCompat): String {
        return "home:continue_watching:card:${mediaIdentity(item)}"
    }

    private val rowGroups = HashMap<String, TvFocusGroup>()

    /** Focus memory of a feed row: the poster index last focused (the "Show all" tile comes after the last poster). */
    fun rowGroup(rowKey: String): TvFocusGroup = rowGroups.getOrPut(rowKey) { TvFocusGroup() }

    /** Focus memory of the hero's Continue watching cards. */
    val cardsGroup = TvFocusGroup()

    fun feedPoster(feedId: String, item: MediaItemCompat): String {
        return "home:feed:$feedId:item:${mediaIdentity(item)}"
    }

    fun feedShowMore(feedId: String): String {
        return "home:feed:$feedId:show_more"
    }

    /** Resume / Details / Remove: the hero's own buttons, where Back opens the menu. */
    fun isHeroButtonTarget(targetId: String?): Boolean {
        return targetId == ContinueWatchingResume ||
            targetId == ContinueWatchingDetails ||
            targetId == ContinueWatchingRemove
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
