package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.compose.ui.graphics.RectangleShape
import com.lagradost.cloudstream3.tv.presentation.focus.TvBackHandler
import com.lagradost.cloudstream3.tv.presentation.focus.tvFocusTrap
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens

private const val PanelAnimationMs = TvDimens.MaxAnimationMs

/**
 * Panel background: the fixed redesign panel color on the static theme, the scheme surface on
 * screens with a dynamic (backdrop-derived) color scheme so they keep their tint.
 */
@Composable
fun tvPanelColor(): Color {
    val surface = MaterialTheme.colorScheme.surface
    return if (surface == TvColors.Ground) TvColors.Panel else surface
}

enum class SidePanelEdge {
    Left,
    Right,
}

@Composable
fun SlidingSidePanel(
    visible: Boolean,
    onCloseRequested: () -> Unit,
    edge: SidePanelEdge = SidePanelEdge.Right,
    panelWidth: Dp = TvDimens.PanelWidth,
    widthFraction: Float? = null,
    overlayColor: Color = TvColors.Scrim,
    panelShape: Shape? = null,
    panelBackgroundColor: Color = tvPanelColor(),
    panelOuterPadding: PaddingValues = PaddingValues(0.dp),
    panelPadding: PaddingValues = PaddingValues(
        start = TvDimens.PanelHorizontalPadding,
        end = TvDimens.PanelHorizontalPadding,
        top = TvDimens.PanelTopPadding,
    ),
    modifier: Modifier = Modifier,
    panelTestTag: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    TvBackHandler(enabled = visible, onBack = onCloseRequested)

    // Scrim and slide are driven by one progress value read only in draw/graphics lambdas,
    // so the panel content is not recomposed while animating.
    val progress = remember { Animatable(0f) }
    var composed by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        if (visible) composed = true
        progress.animateTo(if (visible) 1f else 0f, tween(PanelAnimationMs))
        if (!visible) composed = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(color = overlayColor, alpha = progress.value) }
    ) {
        if (visible || composed) {
            val direction = if (edge == SidePanelEdge.Right) 1f else -1f
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .then(
                        if (widthFraction == null) {
                            Modifier.width(panelWidth)
                        } else {
                            Modifier.fillMaxWidth(widthFraction)
                        }
                    )
                    .padding(panelOuterPadding)
                    .align(if (edge == SidePanelEdge.Right) Alignment.CenterEnd else Alignment.CenterStart)
                    .then(
                        if (panelTestTag.isNullOrBlank()) {
                            Modifier
                        } else {
                            Modifier.testTag(panelTestTag)
                        }
                    )
                    .graphicsLayer { translationX = direction * (1f - progress.value) * size.width }
                    .tvFocusTrap()
                    .background(color = panelBackgroundColor, shape = panelShape ?: RectangleShape)
                    .padding(panelPadding),
                content = content,
            )
        }
    }
}
