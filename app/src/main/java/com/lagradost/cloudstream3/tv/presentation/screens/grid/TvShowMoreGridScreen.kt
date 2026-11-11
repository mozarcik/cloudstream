package com.lagradost.cloudstream3.tv.presentation.screens.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import com.lagradost.cloudstream3.tv.presentation.screens.dashboard.LocalDashboardContentEntry
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.SlidingSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.TvButton
import com.lagradost.cloudstream3.tv.presentation.common.TvEmptyState
import com.lagradost.cloudstream3.tv.presentation.common.TvErrorState
import com.lagradost.cloudstream3.tv.presentation.common.TvFocusedItemTitle
import com.lagradost.cloudstream3.tv.presentation.common.TvListRow
import com.lagradost.cloudstream3.tv.presentation.common.TvRowLeading
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.focus.TvInitialFocusEffect
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

private val HeaderShadowBrush = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))

@Immutable
data class TvGridSortOption(val id: String, val label: String)

/** Sorting offered by the list behind the grid; only methods the list supports are in [options]. */
@Immutable
data class TvGridSort(
    val options: List<TvGridSortOption>,
    val selectedId: String,
    val onSelected: (String) -> Unit,
)

/**
 * The one "Show more" screen (home feed, library list, search source): header with title,
 * subtitle and optional "Sort: ..." button, a 6-column paged poster grid, and the loading /
 * empty / error states. Callers only supply the data source and the focus store.
 *
 * The top bar is hidden meanwhile. D-pad: Up from the first row goes to the sort button (or stays in the grid); OK in the sort
 * panel selects, closes the panel and moves focus to the first poster; Back closes the panel,
 * otherwise it leaves the screen (handled by navigation) and Back from details restores focus to
 * the poster that was opened.
 */
@Composable
fun TvShowMoreGridScreen(
    title: String,
    pagingItems: LazyPagingItems<MediaItemCompat>,
    focusStore: TvGridFocusStore,
    onMediaClick: (MediaItemCompat) -> Unit,
    screenKey: Any,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    sort: TvGridSort? = null,
    restoreFocusToken: Int = 0,
) {
    val gridState = rememberLazyGridState()
    val firstItemFocusRequester = remember { FocusRequester() }
    val sortButtonFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }

    var isSortPanelOpen by rememberSaveable(screenKey) { mutableStateOf(false) }
    var focusFirstItemToken by rememberSaveable(screenKey) { mutableIntStateOf(0) }
    var initialFocusHandled by rememberSaveable(screenKey) { mutableStateOf(false) }
    // Focus coming back into the grid (from the menu, or Down from Sort) goes to the poster that had it.
    var reenterToken by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    fun reenterGrid(): Boolean {
        if (pagingItems.itemCount == 0) return false
        if (focusStore.lastIndex != null) {
            focusStore.scheduleRestoreToLastFocused()
            reenterToken += 1
        } else {
            coroutineScope.launch {
                gridState.scrollToItem(0)
                firstItemFocusRequester.requestFocusWithRetry(attempts = 18)
            }
        }
        return true
    }
    val contentEntry = LocalDashboardContentEntry.current
    DisposableEffect(contentEntry, focusStore) {
        val restore: () -> Boolean = { focusStore.lastIndex != null && reenterGrid() }
        contentEntry.restoreFocus = restore
        onDispose { if (contentEntry.restoreFocus === restore) contentEntry.restoreFocus = null }
    }

    val focusedItem = remember { mutableStateOf<MediaItemCompat?>(null) }
    val subtitleStyle = SpanStyle(
        fontSize = TvTextStyles.Meta.fontSize,
        fontWeight = TvTextStyles.Meta.fontWeight,
        color = TvColors.Label,
    )
    val headerText = remember(title, subtitle) {
        buildAnnotatedString {
            append(title)
            if (!subtitle.isNullOrBlank()) {
                append("  ")
                withStyle(subtitleStyle) { append(subtitle) }
            }
        }
    }

    val itemCount = pagingItems.itemCount
    val refresh = pagingItems.loadState.refresh
    val isRefreshError = itemCount == 0 && refresh is LoadState.Error
    val isEmpty = itemCount == 0 && refresh is LoadState.NotLoading
    val hasRestoreTarget = restoreFocusToken > 0 && focusStore.pendingRestoreTargetId != null


    // The sort order changed: start again from the top.
    LaunchedEffect(sort?.selectedId) {
        if (initialFocusHandled) gridState.scrollToItem(0)
    }

    TvInitialFocusEffect(
        requester = firstItemFocusRequester,
        key = screenKey,
        enabled = itemCount > 0 && !hasRestoreTarget && !initialFocusHandled && !isSortPanelOpen,
        onFocused = { initialFocusHandled = true },
    )
    FocusRequestEffect(
        requester = firstItemFocusRequester,
        requestKey = focusFirstItemToken,
        enabled = focusFirstItemToken > 0 && !isSortPanelOpen && itemCount > 0,
    )
    TvInitialFocusEffect(
        requester = retryFocusRequester,
        key = isRefreshError,
        enabled = isRefreshError,
    )

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TvDimens.ContentStartWithRail,
                    end = TvDimens.ScreenHorizontalPadding,
                    top = TvDimens.GridHeaderTopPadding,
                )
                .height(TvDimens.GridHeaderHeight),
        ) {
            // One Text: title and subtitle share a baseline and ellipsize together.
            Text(
                text = headerText,
                style = TvTextStyles.Section,
                color = TvColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .width(TvDimens.GridFocusedTitleSlotWidth)
                    .padding(end = if (sort != null) TvDimens.GridFocusedTitleSortGap else 0.dp),
            ) {
                TvFocusedItemTitle(focusedItem)
            }
            if (sort != null) {
                val currentLabel = sort.options.firstOrNull { it.id == sort.selectedId }?.label.orEmpty()
                TvButton(
                    text = "${stringResource(R.string.sort)}: $currentLabel",
                    icon = Icons.AutoMirrored.Outlined.Sort,
                    height = TvDimens.GridSortButtonHeight,
                    onClick = { isSortPanelOpen = true },
                    modifier = Modifier
                        .focusRequester(sortButtonFocusRequester)
                        .focusProperties { up = FocusRequester.Cancel }
                        // The poster below may be scrolled out of the grid: bring it back first.
                        .onPreviewKeyEvent { event ->
                            event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown && reenterGrid()
                        },
                )
            }
        }

        Spacer(Modifier.height(TvDimens.GridHeaderBottomPadding))

        Box(modifier = Modifier.weight(1f)) {
        when {
            isRefreshError -> TvErrorState(
                illustration = R.drawable.tv_empty_error,
                message = (refresh as LoadState.Error).error.message
                    ?: stringResource(R.string.error),
                retryText = stringResource(R.string.tv_player_retry),
                onRetry = { pagingItems.retry() },
                retryModifier = Modifier.focusRequester(retryFocusRequester),
            )

            isEmpty -> TvEmptyState(
                illustration = R.drawable.tv_empty_empty_list,
                title = stringResource(R.string.tv_feed_empty),
            )

            else -> TvMediaGrid(
                pagingItems = pagingItems,
                gridState = gridState,
                onMediaClick = { item ->
                    focusStore.scheduleRestoreToLastFocused()
                    onMediaClick(item)
                },
                onItemFocused = { item, index ->
                    focusedItem.value = item
                    focusStore.onItemFocused(item, index)
                },
                targetIdOf = focusStore::targetId,
                firstItemFocusRequester = firstItemFocusRequester,
                upFocusRequester = if (sort != null) sortButtonFocusRequester else FocusRequester.Cancel,
                restore = TvGridRestore(
                    targetId = focusStore.pendingRestoreTargetId,
                    index = focusStore.pendingRestoreIndex,
                    token = restoreFocusToken + reenterToken,
                    onConsumed = { targetId ->
                        initialFocusHandled = true
                        focusStore.clearPendingRestore(targetId)
                    },
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged { state -> if (!state.hasFocus) focusedItem.value = null },
            )
        }
        // The header sits "above" the posters: once they scroll under it, a soft shadow separates
        // them. Hidden at the top (nothing under the header) and not drawn over the nav rail.
        val isScrolled by remember(gridState) {
            derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0 }
        }
        val shadowAlpha by animateFloatAsState(
            targetValue = if (isScrolled) 1f else 0f,
            animationSpec = tween(TvDimens.MaxAnimationMs),
            label = "gridHeaderShadow",
        )
        Box(
            modifier = Modifier
                .padding(start = TvDimens.ContentStartWithRail)
                .fillMaxWidth()
                .height(TvDimens.GridHeaderShadowHeight)
                .graphicsLayer { alpha = shadowAlpha }
                .background(HeaderShadowBrush),
        )
        }
    }

    if (sort != null) {
        TvGridSortPanel(
            visible = isSortPanelOpen,
            sort = sort,
            onSelected = { id ->
                sort.onSelected(id)
                isSortPanelOpen = false
                focusFirstItemToken += 1
            },
            onCloseRequested = {
                isSortPanelOpen = false
                sortButtonFocusRequester.requestFocus()
            },
        )
    }
}

@Composable
private fun TvGridSortPanel(
    visible: Boolean,
    sort: TvGridSort,
    onSelected: (String) -> Unit,
    onCloseRequested: () -> Unit,
) {
    SlidingSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        panelWidth = TvDimens.SortPanelWidth,
        panelTestTag = "grid_sort_panel",
    ) {
        val selectedRequester = remember { FocusRequester() }
        TvInitialFocusEffect(requester = selectedRequester)

        Column {
            Text(
                text = stringResource(R.string.sort_by),
                style = TvTextStyles.Section,
                color = TvColors.Text,
                modifier = Modifier.padding(
                    start = TvDimens.PanelTitleHorizontalPadding,
                    bottom = TvDimens.PanelTitleBottomPadding,
                ),
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(TvDimens.RowSpacing)) {
                items(sort.options.size, key = { index -> sort.options[index].id }) { index ->
                    val option = sort.options[index]
                    val selected = option.id == sort.selectedId
                    TvListRow(
                        title = option.label,
                        onClick = { onSelected(option.id) },
                        leadingIndicator = TvRowLeading.Radio(selected = selected),
                        height = TvDimens.SortPanelRowHeight,
                        shape = TvShapes.PanelRow,
                        titleStyle = TvTextStyles.RowPanel,
                        modifier = if (selected) Modifier.focusRequester(selectedRequester) else Modifier,
                    )
                }
            }
        }
    }
}
