package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

internal const val FEED_GRID_COLUMNS = 6
internal const val FEED_GRID_ROWS = 2
internal const val FEED_GRID_SIZE = FEED_GRID_COLUMNS * FEED_GRID_ROWS
internal const val FEED_VISIBLE_ITEMS_BEFORE_SHOW_MORE = 11

internal const val FEED_POSTER_ASPECT_RATIO = 2f / 3f
internal val FeedGridColumnSpacing = 10.dp
internal val FeedGridRowSpacing = 4.dp
internal val FeedGridHorizontalPadding = 4.dp
internal val FeedSectionShape = RoundedCornerShape(18.dp)
internal val FeedSectionHeaderHeight = 58.dp
internal val FeedCardTitleHeight = 40.dp
internal val FeedCardTitleHorizontalPadding = 4.dp
internal val FeedCardTitleTopPadding = 4.dp

internal data class FeedMiniGridConfig(
    val displayItems: List<MediaItemCompat>,
    val showMore: Boolean,
    val rowCount: Int,
    val totalSlots: Int,
)

internal fun resolveFeedMiniGridConfig(items: List<MediaItemCompat>): FeedMiniGridConfig {
    val showMore = items.size > FEED_GRID_SIZE
    val displayItems = if (showMore) {
        items.take(FEED_VISIBLE_ITEMS_BEFORE_SHOW_MORE)
    } else {
        items.take(FEED_GRID_SIZE)
    }
    val totalSlots = displayItems.size + if (showMore) 1 else 0
    val rowCount = when {
        totalSlots <= 0 -> 1
        else -> ((totalSlots + FEED_GRID_COLUMNS - 1) / FEED_GRID_COLUMNS)
            .coerceAtMost(FEED_GRID_ROWS)
    }

    return FeedMiniGridConfig(
        displayItems = displayItems,
        showMore = showMore,
        rowCount = rowCount,
        totalSlots = totalSlots
    )
}

internal fun gridHeightForRows(
    cardHeight: Dp,
    rowCount: Int,
): Dp {
    val spacingRows = (rowCount - 1).coerceAtLeast(0)
    return (cardHeight * rowCount) + (FeedGridRowSpacing * spacingRows)
}
