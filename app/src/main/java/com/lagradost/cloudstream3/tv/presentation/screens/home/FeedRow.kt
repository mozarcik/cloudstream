package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.focusedItemMeta
import com.lagradost.cloudstream3.tv.presentation.focus.isItemComposed
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
    rowFocusRequester: FocusRequester,
    onMediaClick: (MediaItemCompat) -> Unit,
    onShowAllClick: () -> Unit,
    isInteractive: Boolean,
    modifier: Modifier = Modifier,
    onRefreshClick: () -> Unit = {},
    sourceName: String? = null,
    upFocusRequester: FocusRequester? = null,
    onItemFocused: ((MediaItemCompat) -> Unit)? = null,
    onShowAllFocused: (() -> Unit)? = null,
    onSectionFocusChanged: ((Boolean) -> Unit)? = null,
    /** Up/Down pressed in the row; true consumes the key (a loading neighbour has nothing to focus). */
    shouldBlockVerticalMove: ((up: Boolean) -> Boolean)? = null,
) {
    val focusedItem = remember { mutableStateOf<MediaItemCompat?>(null) }
    val isShowAllFocused = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Never read during composition, so a plain holder avoids recomposition.
    val pendingUpdate = remember { arrayOfNulls<Job>(1) }
    // Recomposing the description text costs ~10 ms, so it must not share a frame with the focus
    // change. The coroutine starts during key handling: the first frame resumes inside the frame
    // that already renders the focus change, the second one is the following frame.
    val updateFocusDescription = { item: MediaItemCompat?, showAll: Boolean ->
        pendingUpdate[0]?.cancel()
        pendingUpdate[0] = scope.launch {
            withFrameNanos { }
            withFrameNanos { }
            focusedItem.value = item
            isShowAllFocused.value = showAll
        }
    }
    val isEmptyFeed = state is HomeFeedLoadState.Error ||
        (state is HomeFeedLoadState.Success && state.items.isEmpty())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isEmptyFeed) FeedRowEmptySectionHeight else FeedRowSectionHeight)
            .padding(top = TvDimens.FeedRowTop)
            .onFocusChanged { focusState ->
                if (!focusState.hasFocus) {
                    updateFocusDescription(null, false)
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
                shouldBlockVerticalMove = shouldBlockVerticalMove,
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
                        shouldBlockVerticalMove = shouldBlockVerticalMove,
                        onClick = onRefreshClick,
                    )
                } else {
                    FeedPosterRow(
                        posters = posters,
                        totalCount = state.items.size,
                        feedName = title,
                        rowKey = rowKey,
                        rowFocusRequester = rowFocusRequester,
                        upFocusRequester = upFocusRequester,
                        isInteractive = isInteractive,
                        onFocusDescriptionChange = updateFocusDescription,
                        onMediaClick = onMediaClick,
                        onShowAllClick = onShowAllClick,
                        onItemFocused = onItemFocused,
                        onShowAllFocused = onShowAllFocused,
                        shouldBlockVerticalMove = shouldBlockVerticalMove,
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
    rowFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    isInteractive: Boolean,
    onFocusDescriptionChange: (item: MediaItemCompat?, showAll: Boolean) -> Unit,
    onMediaClick: (MediaItemCompat) -> Unit,
    onShowAllClick: () -> Unit,
    onItemFocused: ((MediaItemCompat) -> Unit)?,
    onShowAllFocused: (() -> Unit)?,
    shouldBlockVerticalMove: ((up: Boolean) -> Boolean)?,
) {
    val showAllIndex = posters.size
    val group = remember(rowKey) { HomeFocusStore.rowGroup(rowKey) }
    // Test tag of the row's cards: "home:feed:<feedId>:slot:<index>" (rowKey is "<sourceId>|<feedId>").
    val slotTagPrefix = remember(rowKey) { "home:feed:${rowKey.substringAfterLast('|')}:slot" }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = group.lastIndex.coerceIn(0, showAllIndex),
    )

    // A new row composes its posters in two steps (the first few now, the rest two frames later),
    // so entering a feed does not stall one frame. A row that was left scrolled (or is restored) composes
    // everything at once: its remembered poster may be further along.
    val composeInSteps = group.lastIndex == 0
    var composedCount by remember(rowKey) {
        mutableIntStateOf(if (composeInSteps) FeedRowFirstStepPosters else Int.MAX_VALUE)
    }
    LaunchedEffect(rowKey) {
        if (composedCount != Int.MAX_VALUE) {
            withFrameNanos { }
            withFrameNanos { }
            composedCount = Int.MAX_VALUE
        }
    }
    val shownPosters = if (composedCount >= posters.size) posters else posters.subList(0, composedCount)

    fun verticalKeys(): Modifier = Modifier.onPreviewKeyEvent { event ->
        if (!isInteractive || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionUp -> shouldBlockVerticalMove?.invoke(true) ?: false
            Key.DirectionDown -> shouldBlockVerticalMove?.invoke(false) ?: false
            else -> false
        }
    }

    val density = LocalDensity.current
    val bringIntoViewSpec = remember(density) {
        horizontalBringIntoViewSpec(
            startInsetPx = with(density) { TvDimens.ContentStartWithRail.toPx() },
            endInsetPx = with(density) { TvDimens.ScreenHorizontalPadding.toPx() },
        )
    }

    val rowModifier = with(group) {
        Modifier
            // Up / Down from any poster (one handler for the row, not one per poster).
            .then(verticalKeys())
            .focusRequester(rowFocusRequester)
            // Entering the row from above / below lands on the position it was left at.
            .tvFocusGroup(isItemComposed = { index ->
                // With two-step composition the remembered index may not be there yet.
                val shown = if (index == showAllIndex) shownPosters.size == posters.size else index < shownPosters.size
                shown && listState.isItemComposed(index)
            })
    }

    // Posters scroll under the nav rail (like the hero cards); the first one sits at 88 dp.
    CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoViewSpec) {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(TvDimens.FeedRowGap),
            contentPadding = PaddingValues(
                start = TvDimens.ContentStartWithRail,
                end = TvDimens.ScreenHorizontalPadding,
            ),
            modifier = rowModifier
        ) {
            itemsIndexed(shownPosters, key = { index, item -> "$index:${item.url}" }) { index, item ->
                FeedPosterCard(
                    item = item,
                    focusableTestTag = "$slotTagPrefix:$index",
                    onClick = { onMediaClick(item) },
                    onFocused = {
                        group.onItemFocused(index)
                        onFocusDescriptionChange(item, false)
                        onItemFocused?.invoke(item)
                    },
                    modifier = Modifier
                        // Height comes from the poster's 2:3 aspect ratio.
                        .width(TvDimens.FeedRowPosterWidth)
                        .focusRequester(group.itemRequester(index))
                        .focusProperties {
                            canFocus = isInteractive
                            up = upFocusRequester ?: FocusRequester.Default
                        }
                )
            }
            if (shownPosters.size == posters.size) item(key = "show_all") {
                ShowAllTile(
                    subtitle = stringResource(R.string.tv_feed_show_all_subtitle, feedName, "$totalCount+"),
                    onClick = onShowAllClick,
                    onFocused = {
                        group.onItemFocused(showAllIndex)
                        onFocusDescriptionChange(null, true)
                        onShowAllFocused?.invoke()
                    },
                    modifier = Modifier
                        .width(TvDimens.FeedRowPosterWidth)
                        .height(TvDimens.FeedRowPosterHeight)
                        .focusRequester(group.itemRequester(showAllIndex))
                        .focusProperties {
                            canFocus = isInteractive
                            up = upFocusRequester ?: FocusRequester.Default
                            // End of the row: nothing to the right.
                            right = FocusRequester.Cancel
                        }
                )
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

/** Feed title + source: one text, two colours (set once per feed; the focus never touches it). */
private val FeedRowSourceSpan = TvTextStyles.Meta.toSpanStyle().copy(color = TvColors.Label)

/** Focused poster's "year • type": second colour of the description text. */
private val FeedRowDescriptionMetaSpan = TvTextStyles.FeedFocusedMeta.toSpanStyle().copy(color = TvColors.Text2)

/**
 * One box, two fixed-width texts (title + source on the left, the focused poster on the right),
 * so changing the description never re-measures a sibling or the header itself.
 */
@Composable
private fun FeedRowHeader(
    title: String,
    sourceName: String?,
    feedName: String,
    focusedItem: State<MediaItemCompat?>,
    isShowAllFocused: State<Boolean>,
) {
    val headline = remember(title, sourceName) {
        buildAnnotatedString {
            append(title)
            if (!sourceName.isNullOrBlank()) {
                append("  ")
                withStyle(FeedRowSourceSpan) { append(sourceName) }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TvDimens.FeedHeaderHeight)
            .padding(start = TvDimens.HomeContentPadding, end = TvDimens.ScreenHorizontalPadding)
    ) {
        Text(
            text = headline,
            style = TvTextStyles.Section,
            color = TvColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(end = TvDimens.FeedRowDescriptionWidth)
                .fillMaxWidth(),
        )
        FeedRowFocusDescription(
            feedName = feedName,
            focusedItem = focusedItem,
            isShowAllFocused = isShowAllFocused,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

/** Own scope: moving focus between posters recomposes only this, not the whole header. */
@Composable
private fun FeedRowFocusDescription(
    feedName: String,
    focusedItem: State<MediaItemCompat?>,
    isShowAllFocused: State<Boolean>,
    modifier: Modifier = Modifier,
) {
    val item = focusedItem.value
    val showAllHint = if (isShowAllFocused.value) {
        stringResource(R.string.tv_feed_show_all_hint, feedName)
    } else {
        null
    }
    val meta = if (showAllHint == null && item != null) focusedItemMeta(item) else null
    val line = remember(showAllHint, item, meta) {
        buildAnnotatedString {
            when {
                showAllHint != null -> append(showAllHint)
                item != null && meta != null -> {
                    append(item.name)
                    append("  ")
                    withStyle(FeedRowDescriptionMetaSpan) { append(meta) }
                }
            }
        }
    }
    Text(
        text = line,
        style = TvTextStyles.FeedFocusedTitle,
        color = TvColors.Text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.End,
        modifier = modifier.width(TvDimens.FeedRowDescriptionWidth),
    )
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

/** Empty / failed feed: dashed box that takes focus; OK reloads. */
@Composable
private fun FeedEmptyBox(
    title: String,
    isInteractive: Boolean,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    shouldBlockVerticalMove: ((up: Boolean) -> Boolean)?,
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
                    Key.DirectionUp -> shouldBlockVerticalMove?.invoke(true) ?: false
                    Key.DirectionDown -> shouldBlockVerticalMove?.invoke(false) ?: false
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

/** Posters composed in the first step of a new row (see [FeedPosterRow]). */
private const val FeedRowFirstStepPosters = 4
