package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
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
