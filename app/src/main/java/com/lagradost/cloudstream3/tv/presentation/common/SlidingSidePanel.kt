package com.lagradost.cloudstream3.tv.presentation.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults

enum class SidePanelEdge {
    Left,
    Right,
}

@Composable
fun SlidingSidePanel(
    visible: Boolean,
    onCloseRequested: () -> Unit,
    edge: SidePanelEdge = SidePanelEdge.Right,
    panelWidth: Dp = 340.dp,
    widthFraction: Float? = null,
    overlayColor: Color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
    panelShape: Shape? = null,
    panelBackgroundColor: Color = MaterialTheme.colorScheme.surface,
    panelOuterPadding: PaddingValues = PaddingValues(12.dp),
    panelPadding: PaddingValues = PaddingValues(12.dp),
    modifier: Modifier = Modifier,
    panelTestTag: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    BackHandler(enabled = visible) {
        onCloseRequested()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor)
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth ->
                    if (edge == SidePanelEdge.Right) fullWidth else -fullWidth
                }
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth ->
                    if (edge == SidePanelEdge.Right) fullWidth else -fullWidth
                }
            ),
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
        ) {
            if (panelShape == null) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    colors = SurfaceDefaults.colors(
                        containerColor = panelBackgroundColor
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(panelPadding)
                    ) {
                        content()
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = panelShape,
                    colors = SurfaceDefaults.colors(
                        containerColor = panelBackgroundColor
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(panelPadding)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
