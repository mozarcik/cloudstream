@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.tv.material3.Carousel
import androidx.tv.material3.CarouselDefaults
import androidx.tv.material3.CarouselState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.tv.compat.home.FeaturedItemCompat
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.utils.handleDPadKeyEvents

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
                Box(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomEnd)
                        .clip(FeaturedCarouselShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        .padding(FeaturedCarouselIndicatorContentPadding)
                        .focusProperties { canFocus = false }
                ) {
                    CarouselDefaults.IndicatorRow(
                        itemCount = items.size,
                        activeItemIndex = carouselState.activeItemIndex,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = FeaturedCarouselIndicatorPadding)
                    )
                }
            }
        },
        contentTransformStartToEnd = fadeIn(
            animationSpec = tween(durationMillis = FeaturedCarouselTransitionDurationMs)
        ).togetherWith(
            fadeOut(animationSpec = tween(durationMillis = FeaturedCarouselTransitionDurationMs))
        ),
        contentTransformEndToStart = fadeIn(
            animationSpec = tween(durationMillis = FeaturedCarouselTransitionDurationMs)
        ).togetherWith(
            fadeOut(animationSpec = tween(durationMillis = FeaturedCarouselTransitionDurationMs))
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(FeaturedCarouselHeight)
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
            .border(
                width = FeaturedCarouselBorderWidth,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (isCarouselFocused) 1f else 0f
                ),
                shape = FeaturedCarouselShape
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
