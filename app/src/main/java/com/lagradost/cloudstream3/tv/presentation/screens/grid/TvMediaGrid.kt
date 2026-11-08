package com.lagradost.cloudstream3.tv.presentation.screens.grid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.TvButton
import com.lagradost.cloudstream3.tv.presentation.common.tvSkeleton
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.screens.home.FeedPosterCard
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes

private const val PlaceholderKeyBase = Long.MIN_VALUE
private val GridPosterShape = TvShapes.Card

/** Where focus should return to after Back from a details screen. */
internal class TvGridRestore(
    val targetId: String?,
    val index: Int?,
    val token: Int,
    val onConsumed: (String) -> Unit,
)

/**
 * The 6-column poster grid shared by every "Show more" screen. Content always comes from
 * [LazyPagingItems] (a real pager for home feeds, an in-memory [androidx.paging.PagingData] for
 * library lists and search results), so loading, append and error states are handled in one place.
 *
 * The grid itself is clipped to its bounds, so [contentPadding] must stay larger than the focus
 * ring plus the 1.05 scale (about 8 dp) or the ring of the first row/column is cut off.
 */
@Composable
internal fun TvMediaGrid(
    pagingItems: LazyPagingItems<MediaItemCompat>,
    gridState: LazyGridState,
    onMediaClick: (MediaItemCompat) -> Unit,
    onItemFocused: (MediaItemCompat, Int) -> Unit,
    targetIdOf: (MediaItemCompat) -> String,
    firstItemFocusRequester: FocusRequester,
    restore: TvGridRestore,
    modifier: Modifier = Modifier,
    upFocusRequester: FocusRequester? = null,
) {
    val itemCount = pagingItems.itemCount
    val restoreRequester = remember { FocusRequester() }

    val restoreIndex = remember(itemCount, restore.targetId, restore.index, restore.token) {
        val target = restore.targetId
        if (restore.token <= 0 || target == null) {
            null
        } else {
            restore.index?.takeIf { index ->
                index < itemCount && pagingItems.peek(index)?.let(targetIdOf) == target
            } ?: (0 until itemCount).firstOrNull { index ->
                pagingItems.peek(index)?.let(targetIdOf) == target
            }
        }
    }

    LaunchedEffect(restoreIndex, restore.token) {
        restoreIndex?.let { index -> gridState.scrollToItem(index) }
    }

    FocusRequestEffect(
        requester = if (restoreIndex == 0) firstItemFocusRequester else restoreRequester,
        requestKey = Triple(restore.token, restore.targetId, restoreIndex),
        enabled = restoreIndex != null,
        attempts = TvDimens.FocusRestoreAttempts,
        retryDelayMs = TvDimens.FocusRestoreRetryMs,
        onFocused = { restore.targetId?.let(restore.onConsumed) },
    )

    val showSkeleton = itemCount == 0 && pagingItems.loadState.refresh is LoadState.Loading
    val append = pagingItems.loadState.append

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(TvDimens.GridColumns),
        contentPadding = PaddingValues(
            start = TvDimens.ContentStartWithRail,
            end = TvDimens.ScreenHorizontalPadding,
            top = TvDimens.GridContentTop - TvDimens.GridHeaderTopPadding - TvDimens.GridHeaderHeight -
                TvDimens.GridHeaderBottomPadding,
            bottom = TvDimens.GridContentBottom,
        ),
        horizontalArrangement = Arrangement.spacedBy(TvDimens.GridColumnGap),
        verticalArrangement = Arrangement.spacedBy(TvDimens.GridRowGap),
        modifier = modifier,
    ) {
        if (showSkeleton) {
            items(count = TvDimens.GridSkeletonCount, contentType = { "skeleton" }) {
                PosterSkeleton(Modifier.fillMaxWidth())
            }
        }

        items(
            count = itemCount,
            key = { index ->
                pagingItems.peek(index)?.let { item ->
                    // Paging may deliver the same title twice; the index keeps keys unique.
                    31L * item.gridKeySeed + index
                } ?: (PlaceholderKeyBase + index)
            },
            contentType = { "poster" },
        ) { index ->
            val item = pagingItems[index]
            if (item == null) {
                PosterSkeleton(Modifier.fillMaxWidth())
            } else {
                val requester = when (index) {
                    0 -> firstItemFocusRequester
                    restoreIndex -> restoreRequester
                    else -> null
                }
                FeedPosterCard(
                    item = item,
                    onClick = { onMediaClick(item) },
                    onFocused = { onItemFocused(item, index) },
                    shape = GridPosterShape,
                    ringCornerRadius = TvDimens.CardRadius,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties {
                            if (index < TvDimens.GridColumns && upFocusRequester != null) {
                                up = upFocusRequester
                            }
                        }
                        .then(if (requester != null) Modifier.focusRequester(requester) else Modifier),
                )
            }
        }

        if (append is LoadState.Loading) {
            item(span = { GridItemSpan(TvDimens.GridColumns) }, contentType = "append_loading") {
                Row(horizontalArrangement = Arrangement.spacedBy(TvDimens.GridColumnGap)) {
                    repeat(TvDimens.GridColumns) {
                        PosterSkeleton(Modifier.weight(1f))
                    }
                }
            }
        }

        if (append is LoadState.Error) {
            item(span = { GridItemSpan(TvDimens.GridColumns) }, contentType = "append_error") {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TvButton(
                        text = stringResource(R.string.tv_player_retry),
                        onClick = { pagingItems.retry() },
                        height = TvDimens.GridSortButtonHeight,
                    )
                }
            }
        }
    }
}

@Composable
private fun PosterSkeleton(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .aspectRatio(TvDimens.PosterAspectRatio)
            .tvSkeleton(GridPosterShape)
    )
}
