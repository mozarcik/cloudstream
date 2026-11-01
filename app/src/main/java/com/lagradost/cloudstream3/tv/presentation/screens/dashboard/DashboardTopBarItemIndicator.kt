package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamTokens
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.compose.ui.zIndex
import androidx.tv.material3.MaterialTheme

@Composable
fun DashboardTopBarItemIndicator(
    currentTabPosition: DpRect,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.onSurface,
    anyTabFocused: Boolean,
    shape: Shape
) {
    val width by animateDpAsState(targetValue = currentTabPosition.width, animationSpec = tween(TvDimens.MaxAnimationMs), label = "width")
    val height = currentTabPosition.height
    val leftOffset by animateDpAsState(targetValue = currentTabPosition.left, animationSpec = tween(TvDimens.MaxAnimationMs), label = "leftOffset")
    val topOffset = currentTabPosition.top

    // Active tab = translucent pill (mockup rgba(255,255,255,.10)); focused tab = white pill.
    val pillColor by animateColorAsState(
        targetValue = if (anyTabFocused) activeColor else TvColors.FillTabCurrent,
        animationSpec = tween(TvDimens.MaxAnimationMs),
        label = "pillColor"
    )

    Box(
        modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset(x = leftOffset, y = topOffset)
            .width(width)
            .height(height)
            .background(color = pillColor, shape = shape)
            .zIndex(-1f)
    )
}
