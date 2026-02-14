package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamSurfaceDefaults

private const val FEED_GRID_COLUMNS = 6
private const val FEED_GRID_ROWS = 2
private const val FEED_GRID_SIZE = FEED_GRID_COLUMNS * FEED_GRID_ROWS
private const val FEED_VISIBLE_ITEMS_BEFORE_SHOW_MORE = 11

private const val FEED_POSTER_ASPECT_RATIO = 2f / 3f
private val FeedGridColumnSpacing = 10.dp
private val FeedGridRowSpacing = 10.dp
private val FeedGridHorizontalPadding = 8.dp
private val FeedSectionShape = RoundedCornerShape(18.dp)
private val FeedSectionHeaderHeight = 58.dp
private const val FeedFocusedScale = 1.05f
private const val FeedCardAnimationDurationMs = 140
private const val FeedFocusedOverlayHeightFraction = 0.5f

private data class FeedMiniGridConfig(
    val displayItems: List<MediaItemCompat>,
    val showMore: Boolean,
    val rowCount: Int,
    val totalSlots: Int,
)

private fun resolveFeedMiniGridConfig(items: List<MediaItemCompat>): FeedMiniGridConfig {
    val showMore = items.size > FEED_GRID_SIZE
    val displayItems = if (showMore) {
        items.take(FEED_VISIBLE_ITEMS_BEFORE_SHOW_MORE)
    } else {
        items.take(FEED_GRID_SIZE)
    }
    val totalSlots = displayItems.size + if (showMore) 1 else 0
    val rowCount = when {
        totalSlots <= 0 -> 1
        else -> ((totalSlots + FEED_GRID_COLUMNS - 1) / FEED_GRID_COLUMNS)
            .coerceAtMost(FEED_GRID_ROWS)
    }

    return FeedMiniGridConfig(
        displayItems = displayItems,
        showMore = showMore,
        rowCount = rowCount,
        totalSlots = totalSlots
    )
}

private fun gridHeightForRows(
    cardHeight: androidx.compose.ui.unit.Dp,
    rowCount: Int,
): androidx.compose.ui.unit.Dp {
    val spacingRows = (rowCount - 1).coerceAtLeast(0)
    return (cardHeight * rowCount) + (FeedGridRowSpacing * spacingRows)
}

@Composable
fun FeedSection(
    title: String,
    state: HomeFeedLoadState,
    onMediaClick: (MediaItemCompat) -> Unit,
    onShowMoreClick: () -> Unit,
    isInteractive: Boolean,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cardWidth = (
            maxWidth -
                (FeedGridHorizontalPadding * 2) -
                (FeedGridColumnSpacing * (FEED_GRID_COLUMNS - 1))
            ) / FEED_GRID_COLUMNS
        val cardHeight = cardWidth / FEED_POSTER_ASPECT_RATIO
        val successGridConfig = (state as? HomeFeedLoadState.Success)?.let { success ->
            resolveFeedMiniGridConfig(success.items)
        }
        val rowCount = successGridConfig?.rowCount ?: FEED_GRID_ROWS
        val gridHeight = gridHeightForRows(cardHeight, rowCount)
        val sectionHeight = gridHeight + FeedSectionHeaderHeight

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sectionHeight)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            when (state) {
                HomeFeedLoadState.Loading -> {
                    FeedMiniGridPlaceholder(
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        gridHeight = gridHeight,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HomeFeedLoadState.Error -> {
                    FeedMiniGridEmptyOrError(
                        gridHeight = gridHeight,
                        isInteractive = isInteractive,
                        text = stringResource(R.string.tv_home_failed_to_load),
                        modifier = Modifier.fillMaxWidth(),
                        firstItemFocusRequester = firstItemFocusRequester,
                    )
                }

                is HomeFeedLoadState.Success -> {
                    FeedMiniGrid(
                        items = state.items,
                        onMediaClick = onMediaClick,
                        onShowMoreClick = onShowMoreClick,
                        isInteractive = isInteractive,
                        firstItemFocusRequester = firstItemFocusRequester,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun FeedMiniGrid(
    items: List<MediaItemCompat>,
    onMediaClick: (MediaItemCompat) -> Unit,
    onShowMoreClick: () -> Unit,
    isInteractive: Boolean,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
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
                                    .height(cardHeight)
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
        glow = CloudStreamSurfaceDefaults.glow(),
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
internal fun FeedPosterCard(
    item: MediaItemCompat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    val ratingLabel = remember(item.score) { item.ratingLabelOrNull() }
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = CloudStreamCardShape),
        border = CloudStreamSurfaceDefaults.border(shape = CloudStreamCardShape),
        glow = CloudStreamSurfaceDefaults.glow(),
        colors = CloudStreamSurfaceDefaults.colors(),
        scale = CloudStreamSurfaceDefaults.scale(),
        modifier = modifier.onFocusChanged { isFocused = it.isFocused }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.posterUri.takeIf { it.isNotBlank() })
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    FeedPosterSkeleton(modifier = Modifier.fillMaxSize())
                },
                error = {
                    FeedPosterSkeleton(modifier = Modifier.fillMaxSize())
                },
                success = {
                    SubcomposeAsyncImageContent()
                }
            )

            AnimatedVisibility(
                visible = isFocused,
                enter = fadeIn(animationSpec = tween(durationMillis = 170)),
                exit = fadeOut(animationSpec = tween(durationMillis = 140)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(FeedFocusedOverlayHeightFraction)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.56f),
                                    Color.Black.copy(alpha = 0.9f)
                                )
                            ),
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (ratingLabel != null) {
                        Text(
                            text = ratingLabel,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun MediaItemCompat.ratingLabelOrNull(): String? {
    val formattedScore = this.score
        ?.toString(maxScore = 10, decimals = 1, removeTrailingZeros = true)
        ?.takeIf { it.isNotBlank() }
    return formattedScore?.let { "⭐ $it" }
}

@Composable
private fun FeedPosterSkeleton(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = CloudStreamCardShape)
    )
}

@Composable
private fun FeedMiniGridEmptyOrError(
    gridHeight: androidx.compose.ui.unit.Dp,
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
        glow = CloudStreamSurfaceDefaults.glow(),
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
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    gridHeight: androidx.compose.ui.unit.Dp,
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
