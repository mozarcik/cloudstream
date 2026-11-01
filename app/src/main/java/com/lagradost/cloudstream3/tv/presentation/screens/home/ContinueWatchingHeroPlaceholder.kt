package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors

private val SkeletonColor = TvColors.Skeleton

/** Loading skeleton for the Continue watching hero (Home states board, "Ładowanie"). */
@Composable
internal fun ContinueWatchingHeroPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        SkeletonBlock(width = 110.dp, height = 12.dp, left = HomeContentHorizontalPadding, top = 135.dp)
        SkeletonBlock(width = 319.dp, height = 44.dp, left = HomeContentHorizontalPadding, top = 159.dp)
        SkeletonBlock(width = 220.dp, height = 15.dp, left = HomeContentHorizontalPadding, top = 218.dp)
        SkeletonBlock(
            width = 110.dp, height = 36.dp, left = HomeContentHorizontalPadding, top = 260.dp,
            shape = CircleShape,
        )
        SkeletonBlock(
            width = 98.dp, height = 36.dp, left = HomeContentHorizontalPadding + 120.dp, top = 260.dp,
            shape = CircleShape,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(ContinueWatchingCardSpacing),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = HomeContentHorizontalPadding, top = ContinueWatchingCardsTop)
        ) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .width(ContinueWatchingCardWidth)
                        .height(ContinueWatchingCardHeight)
                        .clip(RoundedCornerShape(7.dp))
                        .background(SkeletonColor)
                )
            }
        }
    }
}

@Composable
private fun SkeletonBlock(
    width: Dp,
    height: Dp,
    left: Dp,
    top: Dp,
    shape: Shape = RoundedCornerShape(3.dp),
) {
    Box(
        modifier = Modifier
            .offset(x = left, y = top)
            .width(width)
            .height(height)
            .clip(shape)
            .background(SkeletonColor)
    )
}
