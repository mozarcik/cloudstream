package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListPrefetchScope
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.NestedPrefetchScope
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens

/**
 * Brings list item [index] to the top edge with a short slide, so moving between feeds (Up/Down)
 * is readable instead of a jump. An item that is not composed yet cannot be measured, so the
 * list's own animated scroll brings it in (the feed above a pinned one is scrolled out).
 */
internal suspend fun LazyListState.slideToItem(index: Int) {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        animateScrollToItem(index)
        return
    }
    val delta = (item.offset - layoutInfo.viewportStartOffset).toFloat()
    if (delta != 0f) animateScrollBy(delta, tween(durationMillis = TvDimens.MaxAnimationMs))
}

/**
 * Home moves focus (and the list) one feed at a time, so the next scroll target is always a
 * neighbour of the visible rows. The default strategy prefetches only while the list is moving,
 * which composes a whole new feed (all its posters) inside the scroll animation frames. This one
 * also composes the neighbouring feeds ahead of time, in idle frames.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class NeighbourRowsPrefetchStrategy(
    private val delegate: LazyListPrefetchStrategy = LazyListPrefetchStrategy(),
) : LazyListPrefetchStrategy {
    private var lastNext = -1
    private var lastPrevious = -1
    private var handles = arrayOfNulls<LazyLayoutPrefetchState.PrefetchHandle>(2)

    override fun LazyListPrefetchScope.onScroll(delta: Float, layoutInfo: LazyListLayoutInfo) {
        with(delegate) { onScroll(delta, layoutInfo) }
    }

    override fun LazyListPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyListLayoutInfo) {
        with(delegate) { onVisibleItemsUpdated(layoutInfo) }
        val visible = layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) return
        val next = visible.last().index + 1
        val previous = visible.first().index - 1
        if (next < layoutInfo.totalItemsCount && next != lastNext) {
            lastNext = next
            handles[0] = schedulePrefetch(next)
        }
        if (previous >= 0 && previous != lastPrevious) {
            lastPrevious = previous
            handles[1] = schedulePrefetch(previous)
        }
    }

    override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) {
        with(delegate) { onNestedPrefetch(firstVisibleItemIndex) }
    }
}
