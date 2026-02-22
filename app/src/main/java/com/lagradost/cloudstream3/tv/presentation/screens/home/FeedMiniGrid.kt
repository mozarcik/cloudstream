package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamSurfaceDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz

@Composable
fun FeedMiniGrid(
    items: List<MediaItemCompat>,
    onMediaClick: (MediaItemCompat) -> Unit,
    onShowMoreClick: () -> Unit,
    isInteractive: Boolean,
    cardWidth: Dp,
    cardHeight: Dp,
    cardPosterHeight: Dp,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
) {
    val gridConfig = remember(items) { resolveFeedMiniGridConfig(items) }
    val gridHeight = gridHeightForRows(cardHeight, gridConfig.rowCount)

    if (gridConfig.totalSlots == 0) {
        FeedMiniGridEmptyOrError(
            gridHeight = gridHeight,
            isInteractive = isInteractive,
            text = stringResource(R.string.tv_feed_empty),
            modifier = modifier.fillMaxWidth(),
            firstItemFocusRequester = firstItemFocusRequester
        )
        return
    }

    val internalRequesters = remember(gridConfig.totalSlots) {
        List(gridConfig.totalSlots) { FocusRequester() }
    }

    fun requesterFor(index: Int): FocusRequester {
        if (index == 0 && firstItemFocusRequester != null) {
            return firstItemFocusRequester
        }
        return internalRequesters[index]
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(FeedGridRowSpacing),
        modifier = modifier.height(gridHeight)
    ) {
        repeat(gridConfig.rowCount) { row ->
            val rowStart = row * FEED_GRID_COLUMNS
            val rowEndExclusive = minOf(rowStart + FEED_GRID_COLUMNS, gridConfig.totalSlots)

            Row(
                horizontalArrangement = Arrangement.spacedBy(space = FeedGridColumnSpacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FeedGridHorizontalPadding)
            ) {
                for (index in rowStart until rowEndExclusive) {
                    val leftIndex = (index - 1).takeIf { index > rowStart }
                    val rightIndex = (index + 1).takeIf { (index + 1) < rowEndExclusive }
                    val upIndex = (index - FEED_GRID_COLUMNS).takeIf { it >= 0 }
                    val downIndex = (index + FEED_GRID_COLUMNS).takeIf { it < gridConfig.totalSlots }

                    val focusModifier = Modifier
                        .focusRequester(requesterFor(index))
                        .focusProperties {
                            canFocus = isInteractive
                            left = leftIndex?.let(::requesterFor) ?: FocusRequester.Default
                            right = rightIndex?.let(::requesterFor) ?: FocusRequester.Default
                            up = upIndex?.let(::requesterFor) ?: FocusRequester.Default
                            down = downIndex?.let(::requesterFor) ?: FocusRequester.Default
                        }

                    when {
                        gridConfig.showMore && index == gridConfig.totalSlots - 1 -> {
                            ShowMoreCard(
                                onClick = onShowMoreClick,
                                modifier = Modifier
                                    .width(cardWidth)
                                    .height(cardPosterHeight)
                                    .then(focusModifier)
                            )
                        }

                        else -> {
                            val item = gridConfig.displayItems[index]
                            FeedPosterCard(
                                item = item,
                                onClick = { onMediaClick(item) },
                                modifier = Modifier
                                    .width(cardWidth)
                                    .height(cardHeight)
                                    .then(focusModifier)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShowMoreCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = true,
        shape = ClickableSurfaceDefaults.shape(shape = CloudStreamCardShape),
        border = CloudStreamSurfaceDefaults.border(shape = CloudStreamCardShape),
        colors = CloudStreamSurfaceDefaults.colors(),
        scale = CloudStreamSurfaceDefaults.scale(focusedScale = 1.04f),
        modifier = modifier.focusProperties { canFocus = true }
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.tv_home_show_more),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
internal fun FeedMiniGridEmptyOrError(
    gridHeight: Dp,
    isInteractive: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
) {
    Surface(
        onClick = { },
        enabled = isInteractive,
        shape = ClickableSurfaceDefaults.shape(shape = FeedSectionShape),
        border = CloudStreamSurfaceDefaults.border(shape = FeedSectionShape),
        glow = ClickableSurfaceDefaults.glow(
            glow = Glow.None,
            focusedGlow = Glow.None,
            pressedGlow = Glow.None
        ),
        colors = CloudStreamSurfaceDefaults.colors(),
        scale = ClickableSurfaceScale.None,
        modifier = modifier
            .height(gridHeight)
            .then(
                if (firstItemFocusRequester != null) {
                    Modifier.focusRequester(firstItemFocusRequester)
                } else {
                    Modifier
                }
            )
            .focusProperties { canFocus = isInteractive }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun FeedMiniGridPlaceholder(
    cardWidth: Dp,
    cardHeight: Dp,
    gridHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(FeedGridRowSpacing),
        modifier = modifier.height(gridHeight)
    ) {
        repeat(FEED_GRID_ROWS) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = FeedGridColumnSpacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FeedGridHorizontalPadding)
            ) {
                repeat(FEED_GRID_COLUMNS) {
                    FeedPosterSkeleton(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(cardHeight)
                    )
                }
            }
        }
    }
}
