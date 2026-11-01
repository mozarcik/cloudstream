package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.Role
import com.lagradost.cloudstream3.tv.presentation.focus.tvCardFocus
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape

@Composable
fun FeedMiniGrid(
    items: List<MediaItemCompat>,
    onMediaClick: (MediaItemCompat) -> Unit,
    onShowMoreClick: () -> Unit,
    isInteractive: Boolean,
    feedName: String? = null,
    /** Overrides the default "All of <feedName>" caption of the "Show more" tile. */
    showMoreSubtitle: String? = null,
    pendingRestoreFocusTargetId: String? = null,
    restoreFocusToken: Int = 0,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    sectionFocusKey: String? = null,
    onItemFocused: ((MediaItemCompat) -> Unit)? = null,
    onShowMoreFocused: (() -> Unit)? = null,
    onRestoreFocusConsumed: ((String) -> Unit)? = null,
    /** Called when Up/Down leaves the grid vertically (up = true) from the given column. */
    onVerticalExit: ((up: Boolean, column: Int) -> Boolean)? = null,
) {
    val gridConfig = remember(items) { resolveFeedMiniGridConfig(items) }
    val gridHeight = gridHeightForRows(gridConfig.rowCount)

    if (gridConfig.totalSlots == 0) {
        FeedMiniGridEmptyOrError(
            gridHeight = gridHeight,
            isInteractive = isInteractive,
            text = stringResource(R.string.tv_feed_empty),
            modifier = modifier.fillMaxWidth(),
            firstItemFocusRequester = firstItemFocusRequester,
            upFocusRequester = upFocusRequester,
            onVerticalExit = onVerticalExit,
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

    fun focusTargetIdFor(index: Int): String? {
        val key = sectionFocusKey ?: return null
        return feedSlotTargetId(key, gridConfig, index)
    }

    val restoreFocusIndex = remember(
        pendingRestoreFocusTargetId,
        restoreFocusToken,
        sectionFocusKey,
        gridConfig.totalSlots,
        items
    ) {
        if (restoreFocusToken <= 0 || pendingRestoreFocusTargetId == null) {
            null
        } else {
            (0 until gridConfig.totalSlots).firstOrNull { index ->
                focusTargetIdFor(index) == pendingRestoreFocusTargetId
            }
        }
    }

    FocusRequestEffect(
        requester = restoreFocusIndex?.let(::requesterFor),
        requestKey = restoreFocusToken to pendingRestoreFocusTargetId,
        enabled = restoreFocusIndex != null && restoreFocusToken > 0,
        onFocused = {
            restoreFocusIndex
                ?.let(::focusTargetIdFor)
                ?.let { targetId ->
                    onRestoreFocusConsumed?.invoke(targetId)
                }
        }
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(TvDimens.MiniGridGap),
        modifier = modifier.height(gridHeight)
    ) {
        val cardWidth = TvDimens.PosterWidth
        val cardHeight = TvDimens.PosterHeight
        repeat(gridConfig.rowCount) { row ->
            val rowStart = row * FEED_GRID_COLUMNS
            val rowEndExclusive = minOf(rowStart + FEED_GRID_COLUMNS, gridConfig.totalSlots)

            Row(
                horizontalArrangement = Arrangement.spacedBy(space = TvDimens.MiniGridGap),
                modifier = Modifier.fillMaxWidth()
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
                            up = upIndex?.let(::requesterFor)
                                ?: upFocusRequester
                                ?: FocusRequester.Default
                            down = downIndex?.let(::requesterFor) ?: FocusRequester.Default
                        }
                        .then(
                            if (onVerticalExit != null && (upIndex == null || downIndex == null)) {
                                Modifier.onPreviewKeyEvent { event ->
                                    if (!isInteractive || event.type != KeyEventType.KeyDown) {
                                        return@onPreviewKeyEvent false
                                    }
                                    val column = index - rowStart
                                    when {
                                        event.key == Key.DirectionUp && upIndex == null -> {
                                            onVerticalExit(true, column)
                                        }
                                        event.key == Key.DirectionDown && downIndex == null -> {
                                            onVerticalExit(false, column)
                                        }
                                        else -> false
                                    }
                                }
                            } else {
                                Modifier
                            }
                        )

                    when {
                        gridConfig.showMore && index == gridConfig.totalSlots - 1 -> {
                            ShowMoreCard(
                                onClick = onShowMoreClick,
                                onFocused = onShowMoreFocused,
                                subtitle = showMoreSubtitle
                                    ?: feedName?.let { stringResource(R.string.tv_home_show_more_all_of, it) },
                                modifier = Modifier
                                    .width(cardWidth)
                                    .height(cardHeight)
                                    .then(
                                        sectionFocusKey?.let { key ->
                                            Modifier.testTag("$key:slot:$index")
                                        } ?: Modifier
                                    )
                                    .then(focusModifier)
                            )
                        }

                        else -> {
                            val item = gridConfig.displayItems[index]
                            FeedPosterCard(
                                item = item,
                                onClick = { onMediaClick(item) },
                                onFocused = {
                                    onItemFocused?.invoke(item)
                                },
                                focusableTestTag = sectionFocusKey?.let { key ->
                                    "$key:slot:$index"
                                },
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

/** "Show more" tile that closes a mini-grid: same size as a poster, ring + scale focus like cards. */
@Composable
fun ShowMoreCard(
    onClick: () -> Unit,
    onFocused: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Column(
        verticalArrangement = Arrangement.spacedBy(TvDimens.ShowMoreGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(TvDimens.PosterAspectRatio)
            .focusProperties { canFocus = true }
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocused?.invoke()
                }
            }
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
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TvColors.Text,
            modifier = Modifier
                .size(TvDimens.ShowMoreCircleSize)
                .background(TvColors.FillIcon, CircleShape)
                .padding(TvDimens.ShowMoreCirclePadding)
        )
        Text(
            text = stringResource(R.string.tv_home_show_more),
            style = TvTextStyles.ShowMoreTitle,
            color = TvColors.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = TvTextStyles.Meta,
                color = TvColors.Label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
    upFocusRequester: FocusRequester? = null,
    onVerticalExit: ((up: Boolean, column: Int) -> Boolean)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(gridHeight)
            // Up/Down are routed like in a populated grid, so the neighbouring feed is scrolled to.
            .onPreviewKeyEvent { event ->
                val up = event.key == Key.DirectionUp
                if (onVerticalExit != null && event.type == KeyEventType.KeyDown &&
                    (up || event.key == Key.DirectionDown)
                ) {
                    onVerticalExit(up, 0)
                } else {
                    false
                }
            }
            .then(
                if (firstItemFocusRequester != null) {
                    Modifier.focusRequester(firstItemFocusRequester)
                } else {
                    Modifier
                }
            )
            .focusProperties {
                canFocus = isInteractive
                up = upFocusRequester ?: FocusRequester.Default
            }
            // Focus rule for cards without the scale (the block spans the whole row).
            .tvCardFocus(focused = focused, scale = 1f)
            .background(TvColors.Card, TvShapes.Card)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isInteractive,
                onClick = {},
            )
    ) {
        Text(
            text = text,
            style = TvTextStyles.Body,
            color = TvColors.Text2,
        )
    }
}

@Composable
fun FeedMiniGridPlaceholder(
    modifier: Modifier = Modifier,
    rowCount: Int = FEED_GRID_ROWS,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TvDimens.MiniGridGap),
        modifier = modifier.height(gridHeightForRows(rowCount))
    ) {
        repeat(rowCount) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = TvDimens.MiniGridGap),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(FEED_GRID_COLUMNS) {
                    FeedPosterSkeleton(
                        modifier = Modifier
                            .width(TvDimens.PosterWidth)
                            .height(TvDimens.PosterHeight)
                    )
                }
            }
        }
    }
}
