package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
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
    loadingLabel: String,
    isMorePanelOpen: Boolean,
    firstFeedCardFocusRequester: FocusRequester,
    firstSectionUpFocusRequester: FocusRequester? = null,
    onMediaClick: (MediaItemCompat) -> Unit,
    onOpenFeedGrid: (FeedCategory) -> Unit,
    pendingRestoreFocusTargetId: String? = null,
    restoreFocusToken: Int = 0,
    onItemFocused: ((FeedCategory, MediaItemCompat) -> Unit)? = null,
    onShowMoreFocused: ((FeedCategory) -> Unit)? = null,
    onRestoreFocusConsumed: ((String) -> Unit)? = null,
    onSectionFocusChanged: ((feedId: String, hasFocus: Boolean) -> Unit)? = null,
    onVerticalExit: ((feedIndex: Int, up: Boolean, column: Int) -> Boolean)? = null,
    /** Focus target of a feed's first element (or its empty / error box), for feeds below the first. */
    feedFocusRequester: ((feedId: String) -> FocusRequester)? = null,
) {
    if (feedsUiState.feedSections.isEmpty() && feedsUiState.isFeedListLoading) {
        items(count = 2) {
            FeedSection(
                title = loadingLabel,
                state = HomeFeedLoadState.Loading,
                onMediaClick = {},
                onShowMoreClick = {},
                isInteractive = false,
                modifier = Modifier.padding(horizontal = HomeContentHorizontalPadding),
            )
        }
    } else if (feedsUiState.feedSections.isEmpty()) {
        item {
            TvEmptyState(
                icon = Icons.Default.GridView,
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
            FeedSection(
                title = section.feed.name,
                sourceName = sourceName,
                state = section.state,
                onMediaClick = onMediaClick,
                onShowMoreClick = {
                    onOpenFeedGrid(section.feed)
                },
                isInteractive = !isMorePanelOpen,
                modifier = Modifier.padding(horizontal = HomeContentHorizontalPadding),
                firstItemFocusRequester = if (index == 0) {
                    firstFeedCardFocusRequester
                } else {
                    feedFocusRequester?.invoke(section.feed.id)
                },
                upFocusRequester = if (index == 0) {
                    firstSectionUpFocusRequester
                } else {
                    null
                },
                sectionFocusKey = "home:feed:${section.feed.id}",
                pendingRestoreFocusTargetId = pendingRestoreFocusTargetId,
                restoreFocusToken = restoreFocusToken,
                onItemFocused = { item ->
                    onItemFocused?.invoke(section.feed, item)
                },
                onShowMoreFocused = {
                    onShowMoreFocused?.invoke(section.feed)
                },
                onRestoreFocusConsumed = onRestoreFocusConsumed,
                onSectionFocusChanged = { hasFocus ->
                    onSectionFocusChanged?.invoke(section.feed.id, hasFocus)
                },
                onVerticalExit = onVerticalExit?.let { exit ->
                    { up, column -> exit(index, up, column) }
                },
            )
        }
    }
}
