package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.TvFocusedItemTitle
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import com.lagradost.cloudstream3.tv.presentation.focus.tvCardFocus
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/** Height of one feed in the Home list: top gap, header, gap, posters (rows are 244 dp apart in the mockup). */
internal val FeedRowSectionHeight = TvDimens.FeedRowTop + TvDimens.FeedHeaderHeight +
    TvDimens.FeedRowHeaderGap + TvDimens.FeedRowPosterHeight

/** A feed with nothing to show is a shorter focusable box (board 16), so Up / Down still pass through it. */
internal val FeedRowEmptySectionHeight = TvDimens.FeedRowTop + TvDimens.FeedHeaderHeight +
    TvDimens.FeedRowHeaderGap + TvDimens.FeedRowEmptyHeight

/** Posters shown in a feed row; the "Show all" tile comes after them. */
internal fun feedRowItems(items: List<MediaItemCompat>): List<MediaItemCompat> = items.take(TvDimens.FeedRowMaxItems)

/**
 * One Home feed: header (title + source on the left, the focused poster on the right) and a
 * single row of up to 10 posters closed by a "Show all" tile. Fixed height, so focus never moves
 * the layout. The row remembers the focused position per feed ([rowKey]).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun FeedRow(
    title: String,
    state: HomeFeedLoadState,
    rowKey: String,
    feedId: String,
    rowFocusRequester: FocusRequester,
    onMediaClick: (MediaItemCompat) -> Unit,
    onShowAllClick: () -> Unit,
    isInteractive: Boolean,
    modifier: Modifier = Modifier,
    onRefreshClick: () -> Unit = {},
    sourceName: String? = null,
    upFocusRequester: FocusRequester? = null,
    pendingRestoreFocusTargetId: String? = null,
    restoreFocusToken: Int = 0,
    onItemFocused: ((MediaItemCompat) -> Unit)? = null,
    onShowAllFocused: (() -> Unit)? = null,
    onRestoreFocusConsumed: ((String) -> Unit)? = null,
    onSectionFocusChanged: ((Boolean) -> Unit)? = null,
    /** Up/Down pressed in the row; returns true when the Dashboard moved focus to another feed. */
    onVerticalExit: ((up: Boolean) -> Boolean)? = null,
) {
    val focusedItem = remember { mutableStateOf<MediaItemCompat?>(null) }
    val isShowAllFocused = remember { mutableStateOf(false) }
    val isEmptyFeed = state is HomeFeedLoadState.Error ||
        (state is HomeFeedLoadState.Success && state.items.isEmpty())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isEmptyFeed) FeedRowEmptySectionHeight else FeedRowSectionHeight)
            .padding(top = TvDimens.FeedRowTop)
            .onFocusChanged { focusState ->
                if (!focusState.hasFocus) {
                    focusedItem.value = null
                    isShowAllFocused.value = false
                }
                onSectionFocusChanged?.invoke(focusState.hasFocus)
            }
    ) {
        FeedRowHeader(
            title = title,
            sourceName = sourceName,
            feedName = title,
            focusedItem = focusedItem,
            isShowAllFocused = isShowAllFocused,
        )
        Spacer(Modifier.height(TvDimens.FeedRowHeaderGap))

        when (state) {
            HomeFeedLoadState.Loading -> FeedRowPlaceholder()

            HomeFeedLoadState.Error -> FeedEmptyBox(
                title = stringResource(R.string.tv_feed_error_title),
                isInteractive = isInteractive,
                focusRequester = rowFocusRequester,
                upFocusRequester = upFocusRequester,
                onVerticalExit = onVerticalExit,
                onClick = onRefreshClick,
            )

            is HomeFeedLoadState.Success -> {
                val posters = remember(state.items) { feedRowItems(state.items) }
                if (posters.isEmpty()) {
                    FeedEmptyBox(
                        title = stringResource(R.string.tv_feed_empty_title),
                        isInteractive = isInteractive,
                        focusRequester = rowFocusRequester,
                        upFocusRequester = upFocusRequester,
                        onVerticalExit = onVerticalExit,
                        onClick = onRefreshClick,
                    )
                } else {
                    FeedPosterRow(
                        posters = posters,
                        totalCount = state.items.size,
                        feedName = title,
                        rowKey = rowKey,
                        feedId = feedId,
                        rowFocusRequester = rowFocusRequester,
                        upFocusRequester = upFocusRequester,
                        isInteractive = isInteractive,
                        pendingRestoreFocusTargetId = pendingRestoreFocusTargetId,
                        restoreFocusToken = restoreFocusToken,
                        focusedItem = focusedItem,
                        isShowAllFocused = isShowAllFocused,
                        onMediaClick = onMediaClick,
                        onShowAllClick = onShowAllClick,
                        onItemFocused = onItemFocused,
                        onShowAllFocused = onShowAllFocused,
                        onRestoreFocusConsumed = onRestoreFocusConsumed,
                        onVerticalExit = onVerticalExit,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun FeedPosterRow(
    posters: List<MediaItemCompat>,
    totalCount: Int,
    feedName: String,
    rowKey: String,
    feedId: String,
    rowFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    isInteractive: Boolean,
    pendingRestoreFocusTargetId: String?,
    restoreFocusToken: Int,
    focusedItem: androidx.compose.runtime.MutableState<MediaItemCompat?>,
    isShowAllFocused: androidx.compose.runtime.MutableState<Boolean>,
    onMediaClick: (MediaItemCompat) -> Unit,
    onShowAllClick: () -> Unit,
    onItemFocused: ((MediaItemCompat) -> Unit)?,
    onShowAllFocused: (() -> Unit)?,
    onRestoreFocusConsumed: ((String) -> Unit)?,
    onVerticalExit: ((up: Boolean) -> Boolean)?,
) {
    val showAllIndex = posters.size
    val requesters = remember(posters.size) { List(posters.size + 1) { FocusRequester() } }
    // Test tag of the row's cards: "home:feed:<feedId>:slot:<index>" (rowKey is "<sourceId>|<feedId>").
    val slotTagPrefix = remember(rowKey) { "home:feed:${rowKey.substringAfterLast('|')}:slot" }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = HomeFocusStore.rowIndex(rowKey).coerceIn(0, showAllIndex),
    )

    // Back from details / the full grid: scroll to the remembered poster and focus it.
    LaunchedEffect(restoreFocusToken, pendingRestoreFocusTargetId, posters) {
        val target = pendingRestoreFocusTargetId ?: return@LaunchedEffect
        if (restoreFocusToken <= 0 || HomeFocusStore.feedIdFromTarget(target) != feedId) return@LaunchedEffect
        val index = if (target == HomeFocusStore.feedShowMore(feedId)) {
            showAllIndex
        } else {
            posters.indexOfFirst { HomeFocusStore.feedPoster(feedId, it) == target }
        }
        if (index < 0) return@LaunchedEffect
        listState.scrollToItem(index)
        if (requesters[index].requestFocusWithRetry(attempts = 18)) onRestoreFocusConsumed?.invoke(target)
    }

    fun verticalKeys(): Modifier = Modifier.onPreviewKeyEvent { event ->
        if (!isInteractive || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionUp -> onVerticalExit?.invoke(true) ?: false
            Key.DirectionDown -> onVerticalExit?.invoke(false) ?: false
            else -> false
        }
    }

    val density = LocalDensity.current
    val bringIntoViewSpec = remember(density) {
        horizontalBringIntoViewSpec(
            startInsetPx = with(density) { (TvDimens.ContentStartWithRail - TvDimens.ContentClipStart).toPx() },
            endInsetPx = with(density) { TvDimens.ScreenHorizontalPadding.toPx() },
        )
    }

    // Posters hide under the rail's edge (68 dp), not under its icons; the first one sits at 88 dp.
    CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoViewSpec) {
    Box(
        Modifier
            .padding(start = TvDimens.ContentClipStart)
            .horizontalClip()
    ) {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(TvDimens.FeedRowGap),
            contentPadding = PaddingValues(
                start = TvDimens.ContentStartWithRail - TvDimens.ContentClipStart,
                end = TvDimens.ScreenHorizontalPadding,
            ),
            modifier = Modifier
                .focusRequester(rowFocusRequester)
                // Entering the row from above / below lands on the position it was left at.
                .focusProperties {
                    enter = { requesters[HomeFocusStore.rowIndex(rowKey).coerceIn(0, showAllIndex)] }
                }
                .focusGroup()
        ) {
            itemsIndexed(posters, key = { index, item -> "$index:${item.url}" }) { index, item ->
                FeedPosterCard(
                    item = item,
                    focusableTestTag = "$slotTagPrefix:$index",
                    onClick = { onMediaClick(item) },
                    onFocused = {
                        HomeFocusStore.setRowIndex(rowKey, index)
                        focusedItem.value = item
                        isShowAllFocused.value = false
                        onItemFocused?.invoke(item)
                    },
                    modifier = Modifier
                        .width(TvDimens.FeedRowPosterWidth)
                        .height(TvDimens.FeedRowPosterHeight)
                        .focusRequester(requesters[index])
                        .focusProperties {
                            canFocus = isInteractive
                            up = upFocusRequester ?: FocusRequester.Default
                        }
                        .then(verticalKeys())
                )
            }
            item(key = "show_all") {
                ShowAllTile(
                    subtitle = stringResource(R.string.tv_feed_show_all_subtitle, feedName, "$totalCount+"),
                    onClick = onShowAllClick,
                    onFocused = {
                        HomeFocusStore.setRowIndex(rowKey, showAllIndex)
                        focusedItem.value = null
                        isShowAllFocused.value = true
                        onShowAllFocused?.invoke()
                    },
                    modifier = Modifier
                        .width(TvDimens.FeedRowPosterWidth)
                        .height(TvDimens.FeedRowPosterHeight)
                        .focusRequester(requesters[showAllIndex])
                        .focusProperties {
                            canFocus = isInteractive
                            up = upFocusRequester ?: FocusRequester.Default
                            // End of the row: nothing to the right.
                            right = FocusRequester.Cancel
                        }
                        .then(verticalKeys())
                )
            }
        }
    }
    }
}

/**
 * Home turns the default bring-into-view off (it scrolls the page itself), so a row scrolls
 * sideways here: a poster that is not fully visible is brought in line with the row's insets.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun horizontalBringIntoViewSpec(startInsetPx: Float, endInsetPx: Float) = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val end = offset + size
        return when {
            offset < startInsetPx -> offset - startInsetPx
            end > containerSize - endInsetPx -> end - (containerSize - endInsetPx)
            else -> 0f
        }
    }
}

/** Clips sideways only, so the focus ring and scale of a poster are not cut above / below the row. */
private fun Modifier.horizontalClip(): Modifier = this.drawWithContent {
    clipRect(left = 0f, top = -size.height, right = size.width, bottom = size.height * 2) {
        this@drawWithContent.drawContent()
    }
}

@Composable
private fun FeedRowHeader(
    title: String,
    sourceName: String?,
    feedName: String,
    focusedItem: State<MediaItemCompat?>,
    isShowAllFocused: State<Boolean>,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(TvDimens.FeedHeaderHeight)
            .padding(start = TvDimens.HomeContentPadding, end = TvDimens.ScreenHorizontalPadding)
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
        Spacer(Modifier.weight(1f))
        // Fixed width, so what the focus shows never moves the title.
        Box(Modifier.width(TvDimens.FeedRowDescriptionWidth), contentAlignment = Alignment.CenterEnd) {
            if (isShowAllFocused.value) {
                Text(
                    text = stringResource(R.string.tv_feed_show_all_hint, feedName),
                    style = TvTextStyles.FeedFocusedTitle,
                    color = TvColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                TvFocusedItemTitle(focusedItem)
            }
        }
    }
}

/** Closes a feed row: same size as a poster, ring + scale focus like the posters. */
@Composable
private fun ShowAllTile(
    subtitle: String,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Column(
        verticalArrangement = Arrangement.spacedBy(TvDimens.ShowMoreGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .focusProperties { canFocus = true }
            .onFocusChanged { if (it.isFocused) onFocused() }
            .tvCardFocus(focused = focused)
            .background(TvColors.Panel, TvShapes.Card)
            .border(TvDimens.ChipBorderWidth, TvColors.BorderChip, TvShapes.Card)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = TvDimens.ShowMoreHorizontalPadding)
    ) {
        Icon(
            imageVector = Icons.Default.GridView,
            contentDescription = null,
            tint = TvColors.Text,
            modifier = Modifier
                .size(TvDimens.FeedShowAllCircle)
                .background(TvColors.FillIcon, CircleShape)
                .padding(10.dp)
        )
        Text(
            text = stringResource(R.string.tv_feed_show_all),
            style = TvTextStyles.ShowMoreTitle,
            color = TvColors.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = TvTextStyles.Meta,
            color = TvColors.Label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FeedRowPlaceholder() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TvDimens.FeedRowGap),
        modifier = Modifier.padding(start = TvDimens.HomeContentPadding)
    ) {
        repeat(7) {
            FeedPosterSkeleton(
                modifier = Modifier
                    .width(TvDimens.FeedRowPosterWidth)
                    .height(TvDimens.FeedRowPosterHeight)
            )
        }
    }
}

/** Empty / failed feed: dashed box that takes focus; Up / Down move to the neighbouring feeds, OK reloads. */
@Composable
private fun FeedEmptyBox(
    title: String,
    isInteractive: Boolean,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    onVerticalExit: ((up: Boolean) -> Boolean)?,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val dashColor = TvColors.BorderChip
    val density = LocalDensity.current
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(start = TvDimens.HomeContentPadding, end = TvDimens.ScreenHorizontalPadding)
            .fillMaxWidth()
            .height(TvDimens.FeedRowEmptyHeight)
            .focusRequester(focusRequester)
            .focusProperties {
                canFocus = isInteractive
                up = upFocusRequester ?: FocusRequester.Default
            }
            .onPreviewKeyEvent { event ->
                if (!isInteractive || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> onVerticalExit?.invoke(true) ?: false
                    Key.DirectionDown -> onVerticalExit?.invoke(false) ?: false
                    else -> false
                }
            }
            .tvCardFocus(focused = focused, scale = 1f)
            .background(TvColors.Panel, TvShapes.Card)
            .drawBehind {
                val stroke = with(density) { 1.dp.toPx() }
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(with(density) { TvDimens.CardRadius.toPx() }),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(stroke * 8, stroke * 6)),
                    ),
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isInteractive,
                role = Role.Button,
                onClick = onClick,
            )
    ) {
        Icon(
            imageVector = Icons.Outlined.ViewAgenda,
            contentDescription = null,
            tint = TvColors.Label,
            modifier = Modifier.size(28.dp),
        )
        Text(text = title, style = TvTextStyles.ShowMoreTitle, color = TvColors.Text, maxLines = 1)
        Text(
            text = stringResource(R.string.tv_feed_empty_hint),
            style = TvTextStyles.Meta,
            color = TvColors.Label,
            maxLines = 1,
        )
    }
}
