package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.ui.unit.Dp
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens

// Dimensions live in TvDimens (poster 130.5x196 dp, gap 16 dp, header 28 dp); only grid math is here.
internal const val FEED_GRID_COLUMNS = 6
internal const val FEED_GRID_ROWS = 2
internal const val FEED_GRID_SIZE = FEED_GRID_COLUMNS * FEED_GRID_ROWS
internal const val FEED_VISIBLE_ITEMS_BEFORE_SHOW_MORE = 11

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

internal fun gridHeightForRows(rowCount: Int): Dp {
    val spacingRows = (rowCount - 1).coerceAtLeast(0)
    return (TvDimens.PosterHeight * rowCount) + (TvDimens.MiniGridGap * spacingRows)
}

/** Whole feed item in the Home list: pinned padding + header + gap + grid + bottom padding. */
internal fun feedSectionHeight(rowCount: Int): Dp {
    return TvDimens.FeedPinnedTop +
        TvDimens.FeedHeaderHeight +
        TvDimens.FeedHeaderGridGap +
        gridHeightForRows(rowCount) +
        TvDimens.FeedSectionBottomPadding
}

/** Focus-restore id of a mini-grid slot; matches [HomeFocusStore.feedPoster]/[HomeFocusStore.feedShowMore]. */
internal fun feedSlotTargetId(sectionKey: String, config: FeedMiniGridConfig, slot: Int): String {
    return if (config.showMore && slot == config.totalSlots - 1) {
        "$sectionKey:show_more"
    } else {
        val item = config.displayItems[slot]
        "$sectionKey:item:${item.apiName}|${item.id}|${item.url}"
    }
}

/** Slot reached when entering a grid from above (first row) or below (last row) in [column]. */
internal fun feedEntrySlot(config: FeedMiniGridConfig, fromAbove: Boolean, column: Int): Int {
    val row = if (fromAbove) 0 else config.rowCount - 1
    val rowStart = row * FEED_GRID_COLUMNS
    val rowEnd = minOf(rowStart + FEED_GRID_COLUMNS, config.totalSlots)
    return (rowStart + column).coerceIn(rowStart, (rowEnd - 1).coerceAtLeast(rowStart))
}
