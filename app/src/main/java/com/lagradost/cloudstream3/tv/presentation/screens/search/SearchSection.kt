package com.lagradost.cloudstream3.tv.presentation.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.Role
import com.lagradost.cloudstream3.tv.presentation.focus.tvFocusFill
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.TvFocusedItemTitle
import com.lagradost.cloudstream3.tv.presentation.common.TvPosterSkeletonRow
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import com.lagradost.cloudstream3.tv.presentation.screens.home.FeedPosterCard
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedLoadState
import com.lagradost.cloudstream3.tv.presentation.screens.home.ShowMoreCard
import com.lagradost.cloudstream3.tv.presentation.screens.home.resolveFeedMiniGridConfig
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

private val SearchHeaderBlock =
    TvDimens.FeedPinnedTop + TvDimens.FeedHeaderHeight +
        (TvDimens.FeedHeaderGridGap - TvDimens.SearchRowVerticalPadding)

/** Height of a source with posters or skeletons: header + row with ring headroom. */
internal val SearchSectionHeight =
    SearchHeaderBlock + TvDimens.PosterHeight + TvDimens.SearchRowVerticalPadding * 2

/** Height of a failed source (mockup: header, 42 dp row, next header 21 dp below). */
internal val SearchErrorSectionHeight =
    SearchHeaderBlock + TvDimens.SearchRowVerticalPadding + TvDimens.SearchErrorRowHeight

internal fun searchItemKey(item: MediaItemCompat): String = "${item.apiName}|${item.id}|${item.url}"

/**
 * Results of one source: header (name + result count, focused poster title on the right) and a
 * single row of posters ending with "Show more", or an error row, or skeletons while loading.
 */
@Composable
internal fun SearchSection(
    section: SearchSectionUiState,
    onMediaClick: (MediaItemCompat) -> Unit,
    onShowMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    restoreItemKey: String? = null,
    onRestoreConsumed: () -> Unit = {},
) {
    val focusedItem = remember { mutableStateOf<MediaItemCompat?>(null) }
    val state = section.state
    val inset = TvDimens.ScreenHorizontalPadding
    // A new search must not show the title of the poster focused in the previous results.
    LaunchedEffect(state is HomeFeedLoadState.Loading) {
        if (state is HomeFeedLoadState.Loading) focusedItem.value = null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(if (state is HomeFeedLoadState.Error) SearchErrorSectionHeight else SearchSectionHeight)
            .padding(top = TvDimens.FeedPinnedTop)
            .onFocusChanged { if (!it.hasFocus) focusedItem.value = null }
    ) {
        SearchSectionHeader(
            title = section.title,
            state = state,
            focusedItem = focusedItem,
            modifier = Modifier.padding(horizontal = inset),
        )
        // The row owns SearchRowVerticalPadding of ring headroom; take it out of the header gap.
        Spacer(Modifier.height(TvDimens.FeedHeaderGridGap - TvDimens.SearchRowVerticalPadding))

        when (state) {
            HomeFeedLoadState.Loading -> TvPosterSkeletonRow(
                count = TvDimens.SearchSkeletonCount,
                modifier = Modifier
                    .padding(start = inset)
                    .padding(vertical = TvDimens.SearchRowVerticalPadding),
            )

            HomeFeedLoadState.Error -> SearchErrorRow(
                focusRequester = firstItemFocusRequester,
                upFocusRequester = upFocusRequester,
                modifier = Modifier
                    .padding(start = inset)
                    .padding(top = TvDimens.SearchRowVerticalPadding),
            )

            is HomeFeedLoadState.Success -> SearchPosterRow(
                items = state.items,
                focusedItem = focusedItem,
                onMediaClick = onMediaClick,
                onShowMoreClick = onShowMoreClick,
                firstItemFocusRequester = firstItemFocusRequester,
                upFocusRequester = upFocusRequester,
                restoreItemKey = restoreItemKey,
                onRestoreConsumed = onRestoreConsumed,
            )
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    state: HomeFeedLoadState,
    focusedItem: State<MediaItemCompat?>,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(TvDimens.FeedHeaderHeight),
    ) {
        Text(
            text = title,
            style = TvTextStyles.Section,
            color = TvColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = TvDimens.FeedFocusedTitleMaxWidth),
        )
        val count = when (state) {
            HomeFeedLoadState.Loading -> null
            HomeFeedLoadState.Error -> stringResource(R.string.error)
            is HomeFeedLoadState.Success ->
                pluralStringResource(R.plurals.tv_search_results_count, state.items.size, state.items.size)
        }
        if (count != null) {
            Text(
                text = count,
                style = TvTextStyles.Meta,
                color = TvColors.Label,
                maxLines = 1,
                modifier = Modifier.padding(start = TvDimens.FeedHeaderTitleGap),
            )
        }
        Spacer(Modifier.weight(1f))
        TvFocusedItemTitle(focusedItem)
    }
}

@Composable
private fun SearchPosterRow(
    items: List<MediaItemCompat>,
    focusedItem: androidx.compose.runtime.MutableState<MediaItemCompat?>,
    onMediaClick: (MediaItemCompat) -> Unit,
    onShowMoreClick: () -> Unit,
    firstItemFocusRequester: FocusRequester?,
    upFocusRequester: FocusRequester?,
    restoreItemKey: String?,
    onRestoreConsumed: () -> Unit,
) {
    val config = remember(items) { resolveFeedMiniGridConfig(items) }
    val rowState = rememberLazyListState()
    val requesters = remember(config.totalSlots) { List(config.totalSlots) { FocusRequester() } }
    fun requesterFor(index: Int) =
        if (index == 0 && firstItemFocusRequester != null) firstItemFocusRequester else requesters[index]

    val restoreIndex = remember(restoreItemKey, config) {
        restoreItemKey?.let { key ->
            (0 until config.totalSlots).firstOrNull { slot ->
                if (config.showMore && slot == config.totalSlots - 1) {
                    key == SHOW_MORE_KEY
                } else {
                    searchItemKey(config.displayItems[slot]) == key
                }
            }
        }
    }
    LaunchedEffect(restoreIndex) {
        if (restoreIndex == null) return@LaunchedEffect
        rowState.scrollToItem(restoreIndex)
        requesterFor(restoreIndex).requestFocusWithRetry()
        onRestoreConsumed()
    }

    LazyRow(
        state = rowState,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.MiniGridGap),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = TvDimens.ScreenHorizontalPadding,
            end = TvDimens.ScreenHorizontalPadding,
            top = TvDimens.SearchRowVerticalPadding,
            bottom = TvDimens.SearchRowVerticalPadding,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        itemsIndexed(
            items = List(config.totalSlots) { it },
            key = { _, slot ->
                if (config.showMore && slot == config.totalSlots - 1) {
                    SHOW_MORE_KEY
                } else {
                    searchItemKey(config.displayItems[slot])
                }
            },
        ) { _, slot ->
            val focusModifier = Modifier
                .width(TvDimens.PosterWidth)
                .height(TvDimens.PosterHeight)
                .focusRequester(requesterFor(slot))
                .focusProperties { if (upFocusRequester != null) up = upFocusRequester }
            if (config.showMore && slot == config.totalSlots - 1) {
                ShowMoreCard(
                    onClick = onShowMoreClick,
                    onFocused = { focusedItem.value = null },
                    modifier = focusModifier,
                )
            } else {
                val item = config.displayItems[slot]
                FeedPosterCard(
                    item = item,
                    onClick = { onMediaClick(item) },
                    onFocused = { focusedItem.value = item },
                    modifier = focusModifier,
                )
            }
        }
    }
}

/** Source that failed: message row instead of posters. Focusable (white row fill, no scale); OK does nothing. */
@Composable
private fun SearchErrorRow(
    focusRequester: FocusRequester?,
    upFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.SearchErrorRowGap),
        modifier = modifier
            .width(TvDimens.SearchErrorRowWidth)
            .height(TvDimens.SearchErrorRowHeight)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusProperties { if (upFocusRequester != null) up = upFocusRequester }
            .tvFocusFill(focused = focused, shape = TvShapes.Row, container = TvColors.Card)
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button) {}
            .padding(horizontal = TvDimens.SearchErrorRowHorizontalPadding),
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = if (focused) TvColors.OnFocus else TvColors.ErrorText,
            modifier = Modifier.size(TvDimens.SearchErrorRowIconSize),
        )
        Text(
            text = stringResource(R.string.tv_search_failed_to_load),
            style = TvTextStyles.SearchErrorRow,
            color = if (focused) TvColors.OnFocus else TvColors.Meta,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
