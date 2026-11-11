package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.TvEmptyState

/** Height of the "no feeds for this source" state (mockup: centred below the source chips). */
private val HomeNoFeedsStateHeight = 260.dp

internal fun LazyListScope.homeFeedSections(
    feedsUiState: HomeFeedsUiState,
    sourceName: String?,
    sourceId: String?,
    isMorePanelOpen: Boolean,
    firstFeedRowFocusRequester: FocusRequester,
    firstSectionUpFocusRequester: FocusRequester? = null,
    onMediaClick: (MediaItemCompat) -> Unit,
    onOpenFeedGrid: (FeedCategory) -> Unit,
    onRefreshFeed: (FeedCategory) -> Unit,
    pendingRestoreFocusTargetId: String? = null,
    restoreFocusToken: Int = 0,
    onItemFocused: ((FeedCategory, MediaItemCompat) -> Unit)? = null,
    onShowAllFocused: ((FeedCategory) -> Unit)? = null,
    onRestoreFocusConsumed: ((String) -> Unit)? = null,
    onSectionFocusChanged: ((feedId: String, hasFocus: Boolean) -> Unit)? = null,
    shouldBlockVerticalMove: ((feedIndex: Int, up: Boolean) -> Boolean)? = null,
) {
    if (feedsUiState.feedSections.isEmpty() && feedsUiState.isFeedListLoading) {
        items(count = 2) {
            FeedRowPlaceholderSection()
        }
    } else if (feedsUiState.feedSections.isEmpty()) {
        item {
            TvEmptyState(
                illustration = R.drawable.tv_empty_empty_list,
                title = stringResource(R.string.tv_home_no_feeds),
                message = sourceName?.let { stringResource(R.string.tv_home_no_feeds_message, it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HomeNoFeedsStateHeight),
            )
        }
    } else {
        itemsIndexed(
            items = feedsUiState.feedSections,
            key = { _, section -> section.feed.id }
        ) { index, section ->
            val rowRequester = remember { FocusRequester() }
            FeedRow(
                title = section.feed.name,
                sourceName = sourceName,
                state = section.state,
                rowKey = "$sourceId|${section.feed.id}",
                feedId = section.feed.id,
                rowFocusRequester = if (index == 0) firstFeedRowFocusRequester else rowRequester,
                onMediaClick = onMediaClick,
                onShowAllClick = { onOpenFeedGrid(section.feed) },
                onRefreshClick = { onRefreshFeed(section.feed) },
                isInteractive = !isMorePanelOpen,
                upFocusRequester = if (index == 0) firstSectionUpFocusRequester else null,
                pendingRestoreFocusTargetId = pendingRestoreFocusTargetId,
                restoreFocusToken = restoreFocusToken,
                onItemFocused = { item -> onItemFocused?.invoke(section.feed, item) },
                onShowAllFocused = { onShowAllFocused?.invoke(section.feed) },
                onRestoreFocusConsumed = onRestoreFocusConsumed,
                onSectionFocusChanged = { hasFocus -> onSectionFocusChanged?.invoke(section.feed.id, hasFocus) },
                shouldBlockVerticalMove = shouldBlockVerticalMove?.let { block -> { up -> block(index, up) } },
            )
        }
    }
}

@Composable
private fun FeedRowPlaceholderSection() {
    FeedRow(
        title = stringResource(R.string.loading),
        state = HomeFeedLoadState.Loading,
        rowKey = "loading",
        feedId = "loading",
        rowFocusRequester = FocusRequester(),
        onMediaClick = {},
        onShowAllClick = {},
        isInteractive = false,
    )
}
