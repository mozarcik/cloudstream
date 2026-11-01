package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.TvFocusedItemTitle
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/**
 * One Home feed: header (title + source on the left, focused poster title on the right) and a
 * 2x6 mini-grid. Fixed height, so focusing posters never moves the layout.
 */
@Composable
fun FeedSection(
    title: String,
    state: HomeFeedLoadState,
    onMediaClick: (MediaItemCompat) -> Unit,
    onShowMoreClick: () -> Unit,
    isInteractive: Boolean,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    sourceName: String? = null,
    showMoreSubtitle: String? = null,
    /** Space above the header; Library trims it for its first list. */
    pinnedTop: Dp = TvDimens.FeedPinnedTop,
    showFocusedRating: Boolean = false,
    firstItemFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    sectionFocusKey: String? = null,
    pendingRestoreFocusTargetId: String? = null,
    restoreFocusToken: Int = 0,
    onItemFocused: ((MediaItemCompat) -> Unit)? = null,
    onShowMoreFocused: (() -> Unit)? = null,
    onRestoreFocusConsumed: ((String) -> Unit)? = null,
    onSectionFocusChanged: ((Boolean) -> Unit)? = null,
    onVerticalExit: ((up: Boolean, column: Int) -> Boolean)? = null,
) {
    val focusedItem = remember { mutableStateOf<MediaItemCompat?>(null) }
    val gridConfig = (state as? HomeFeedLoadState.Success)?.let { success ->
        remember(success.items) { resolveFeedMiniGridConfig(success.items) }
    }
    val rowCount = when (state) {
        HomeFeedLoadState.Error -> 1
        else -> gridConfig?.rowCount ?: FEED_GRID_ROWS
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(feedSectionHeight(rowCount) - (TvDimens.FeedPinnedTop - pinnedTop))
            .padding(top = pinnedTop)
            .onFocusChanged { focusState ->
                if (!focusState.hasFocus) focusedItem.value = null
                onSectionFocusChanged?.invoke(focusState.hasFocus)
            }
    ) {
        FeedHeader(title = title, sourceName = sourceName, focusedItem = focusedItem, showRating = showFocusedRating)
        Spacer(modifier = Modifier.height(TvDimens.FeedHeaderGridGap))

        when (state) {
            HomeFeedLoadState.Loading -> {
                FeedMiniGridPlaceholder(modifier = Modifier.fillMaxWidth())
            }

            HomeFeedLoadState.Error -> {
                FeedMiniGridEmptyOrError(
                    gridHeight = gridHeightForRows(rowCount),
                    isInteractive = isInteractive,
                    text = errorMessage ?: stringResource(R.string.tv_home_failed_to_load),
                    modifier = Modifier.fillMaxWidth(),
                    firstItemFocusRequester = firstItemFocusRequester,
                    upFocusRequester = upFocusRequester,
                    onVerticalExit = onVerticalExit,
                )
            }

            is HomeFeedLoadState.Success -> {
                FeedMiniGrid(
                    items = state.items,
                    onMediaClick = onMediaClick,
                    onShowMoreClick = onShowMoreClick,
                    isInteractive = isInteractive,
                    feedName = title,
                    showMoreSubtitle = showMoreSubtitle,
                    firstItemFocusRequester = firstItemFocusRequester,
                    upFocusRequester = upFocusRequester,
                    pendingRestoreFocusTargetId = pendingRestoreFocusTargetId,
                    restoreFocusToken = restoreFocusToken,
                    sectionFocusKey = sectionFocusKey,
                    onItemFocused = { item ->
                        focusedItem.value = item
                        onItemFocused?.invoke(item)
                    },
                    onShowMoreFocused = {
                        focusedItem.value = null
                        onShowMoreFocused?.invoke()
                    },
                    onRestoreFocusConsumed = onRestoreFocusConsumed,
                    onVerticalExit = onVerticalExit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FeedHeader(
    title: String,
    sourceName: String?,
    focusedItem: State<MediaItemCompat?>,
    showRating: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(TvDimens.FeedHeaderHeight)
    ) {
        Text(
            text = title,
            style = TvTextStyles.Section,
            color = TvColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = TvDimens.FeedFocusedTitleMaxWidth),
        )
        if (!sourceName.isNullOrBlank()) {
            Text(
                text = sourceName,
                style = TvTextStyles.Meta,
                color = TvColors.Label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = TvDimens.FeedHeaderTitleGap)
                    .widthIn(max = TvDimens.FeedSourceMaxWidth),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        TvFocusedItemTitle(focusedItem, showRating = showRating)
    }
}
