package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

internal fun LazyListScope.homeFeedSections(
    feedsUiState: HomeFeedsUiState,
    loadingLabel: String,
    noFeedsLabel: String,
    isMorePanelOpen: Boolean,
    firstFeedCardFocusRequester: FocusRequester,
    onMediaClick: (MediaItemCompat) -> Unit,
    onOpenFeedGrid: (FeedCategory) -> Unit,
) {
    if (feedsUiState.feedSections.isEmpty() && feedsUiState.isFeedListLoading) {
        items(count = 4) {
            FeedSection(
                title = loadingLabel,
                state = HomeFeedLoadState.Loading,
                onMediaClick = {},
                onShowMoreClick = {},
                isInteractive = false,
            )
        }
    } else if (feedsUiState.feedSections.isEmpty()) {
        item {
            Text(
                text = noFeedsLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    } else {
        itemsIndexed(
            items = feedsUiState.feedSections,
            key = { _, section -> section.feed.id }
        ) { index, section ->
            FeedSection(
                title = section.feed.name,
                state = section.state,
                onMediaClick = onMediaClick,
                onShowMoreClick = {
                    onOpenFeedGrid(section.feed)
                },
                isInteractive = !isMorePanelOpen,
                firstItemFocusRequester = if (index == 0) {
                    firstFeedCardFocusRequester
                } else {
                    null
                }
            )
        }
    }
}
