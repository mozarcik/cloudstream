package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import kotlin.math.roundToInt

@Composable
internal fun PlayerGlobalTooltip(
    tooltipState: PlayerControlTooltipState,
    modifier: Modifier = Modifier,
) {
    val verticalOffsetPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        PlayerControlsTokens.TooltipVerticalOffset.toPx()
    }

    Layout(
        content = {
            Surface(
                shape = PlayerControlsTokens.TooltipShape,
                tonalElevation = PlayerControlsTokens.TooltipTonalElevation,
                colors = SurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = PlayerControlsTokens.TooltipAlpha),
                ),
                modifier = Modifier
                    .alpha(PlayerControlsTokens.TooltipAlpha)
                    .widthIn(max = PlayerControlsTokens.TooltipMaxWidth),
            ) {
                Text(
                    text = tooltipState.text,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        horizontal = PlayerControlsTokens.TooltipHorizontalPadding,
                        vertical = PlayerControlsTokens.TooltipVerticalPadding,
                    ),
                )
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val tooltipPlaceable = measurables.first().measure(
            constraints.copy(minWidth = 0, minHeight = 0),
        )

        val maxX = (constraints.maxWidth - tooltipPlaceable.width).coerceAtLeast(0)
        val targetX = (tooltipState.anchorCenterXPx - (tooltipPlaceable.width / 2f))
            .roundToInt()
            .coerceIn(0, maxX)
        val targetY = (tooltipState.anchorTopYPx - verticalOffsetPx - tooltipPlaceable.height).roundToInt()

        layout(constraints.maxWidth, constraints.maxHeight) {
            tooltipPlaceable.placeRelative(x = targetX, y = targetY)
        }
    }
}
