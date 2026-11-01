@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.focus.tvCardFocus
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.tv.material3.Carousel
import androidx.tv.material3.CarouselState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.tv.compat.home.FeaturedItemCompat
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.utils.handleDPadKeyEvents

private val FeaturedCarouselTransition = fadeIn(
    animationSpec = tween(durationMillis = TvDimens.FeaturedTransitionMs)
).togetherWith(
    fadeOut(animationSpec = tween(durationMillis = TvDimens.FeaturedTransitionMs))
)

private val FeaturedCarouselStateSaver = Saver<CarouselState, Int>(
    save = { carouselState -> carouselState.activeItemIndex },
    restore = { activeItemIndex -> CarouselState(activeItemIndex) }
)

@Composable
internal fun FeaturedCarousel(
    items: List<FeaturedItemCompat>,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester? = null,
    isInteractive: Boolean,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    onItemClick: (MediaItemCompat) -> Unit,
) {
    val carouselState = rememberSaveable(items.firstOrNull()?.id, saver = FeaturedCarouselStateSaver) {
        CarouselState(0)
    }
    var isCarouselFocused by remember { mutableStateOf(false) }

    Carousel(
        itemCount = items.size,
        carouselState = carouselState,
        carouselIndicator = {
            Box(modifier = Modifier.fillMaxSize()) {
                FeaturedCarouselIndicator(
                    itemCount = items.size,
                    activeIndex = carouselState.activeItemIndex,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = TvDimens.FeaturedIndicatorEnd,
                            bottom = TvDimens.FeaturedIndicatorBottom,
                        )
                        .focusProperties { canFocus = false }
                )
                if (items.size > 1) {
                    FeaturedCarouselChevron(
                        icon = Icons.Default.ChevronLeft,
                        alpha = if (carouselState.activeItemIndex > 0) 0.8f else 0.5f,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    FeaturedCarouselChevron(
                        icon = Icons.Default.ChevronRight,
                        alpha = if (carouselState.activeItemIndex < items.lastIndex) 0.8f else 0.5f,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        },
        contentTransformStartToEnd = FeaturedCarouselTransition,
        contentTransformEndToStart = FeaturedCarouselTransition,
        modifier = modifier
            .fillMaxWidth()
            .height(TvDimens.FeaturedHeight)
            .testTag("home_featured_carousel")
            .focusRequester(focusRequester)
            .focusProperties {
                canFocus = isInteractive
                up = upFocusRequester
                down = downFocusRequester ?: FocusRequester.Default
            }
            .handleDPadKeyEvents(
                onEnter = {
                    items.getOrNull(carouselState.activeItemIndex)
                        ?.navigationTarget
                        ?.let(onItemClick)
                }
            )
            .onFocusChanged { focusState ->
                isCarouselFocused = focusState.hasFocus
                if (focusState.hasFocus) {
                    onFocused()
                }
            }
            // Card focus rule (ring 4 px + offset 5 px) but WITHOUT the 1.05 scale: the card spans
            // the full content width, a scale would push the ring over the screen edges.
            // The ring is drawn outside the bounds, so it must come before the clip.
            .tvCardFocus(
                focused = isCarouselFocused,
                cornerRadius = TvDimens.FeaturedRadius,
                scale = 1f,
            )
            .clip(FeaturedCarouselShape),
    ) { index ->
        FeaturedCarouselItem(
            item = items[index],
            isFocused = isCarouselFocused,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun FeaturedCarouselIndicator(
    itemCount: Int,
    activeIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.FeaturedDotGap),
        modifier = modifier
            .clip(CircleShape)
            .background(TvColors.FeaturedIndicator)
            .padding(horizontal = TvDimens.FeaturedIndicatorHorizontal, vertical = TvDimens.FeaturedIndicatorVertical)
    ) {
        repeat(itemCount) { index ->
            Box(
                modifier = Modifier
                    .width(if (index == activeIndex) TvDimens.FeaturedDotActiveWidth else TvDimens.FeaturedDotWidth)
                    .height(TvDimens.FeaturedDotHeight)
                    .clip(CircleShape)
                    .background(
                        if (index == activeIndex) TvColors.Focus else Color.White.copy(alpha = 0.4f)
                    )
            )
        }
        Text(
            text = "${activeIndex + 1} / $itemCount",
            style = TvTextStyles.Caption,
            color = TvColors.Meta,
            modifier = Modifier.padding(start = TvDimens.FeaturedIndicatorCounterGap)
        )
    }
}

@Composable
private fun FeaturedCarouselChevron(
    icon: ImageVector,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(TvDimens.FeaturedChevronWidth)
            .height(TvDimens.FeaturedChevronHeight)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = alpha),
            modifier = Modifier.size(TvDimens.FeaturedChevronIcon)
        )
    }
}
