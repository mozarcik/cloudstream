package com.lagradost.cloudstream3.tv.presentation.screens.grid

import com.lagradost.cloudstream3.tv.presentation.focus.TvFocusGroup

/**
 * Remembers which poster of the "Show more" grid had focus, so focus can come back to it after a
 * details screen or the side menu. One instance per grid source (home / library / search).
 */
class TvGridFocusStore {
    /** The grid's focus group: it enters on the poster that had focus last. */
    val group = TvFocusGroup()

    private var hasFocused = false

    /** A title was opened from the grid: the next grid screen is a return and restores focus. */
    var restorePending: Boolean = false
        private set

    /** Poster that had focus last, or null when none has yet. */
    val lastIndex: Int? get() = if (hasFocused) group.lastIndex else null

    fun onItemFocused(index: Int) {
        group.onItemFocused(index)
        hasFocused = true
    }

    fun scheduleRestoreToLastFocused() {
        restorePending = hasFocused
    }

    fun clearPendingRestore() {
        restorePending = false
    }

    /** A new list was opened: forget everything about the previous one. */
    fun reset() {
        restorePending = false
        hasFocused = false
        group.lastIndex = 0
    }
}
