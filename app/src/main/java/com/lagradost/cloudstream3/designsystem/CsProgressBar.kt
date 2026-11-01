package com.lagradost.cloudstream3.designsystem

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/** Rounded progress bar in the title's accent color; draws in a single node. */
@Composable
fun CsProgressBar(
    progress: Float,
    accent: Color,
    track: Color,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(color = track, cornerRadius = radius)
                val fraction = progress.coerceIn(0f, 1f)
                if (fraction > 0f) {
                    drawRoundRect(
                        color = accent,
                        size = Size(size.width * fraction, size.height),
                        cornerRadius = radius,
                    )
                }
            }
    )
}
