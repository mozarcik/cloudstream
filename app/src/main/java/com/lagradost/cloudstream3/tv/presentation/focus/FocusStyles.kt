package com.lagradost.cloudstream3.tv.presentation.focus

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens

/**
 * The one focus rule of the app.
 *
 * - Buttons, tabs, rows: white fill + dark content, no scale ([tvFocusFill]).
 * - Cards: 2 dp white ring with offset + scale 1.05 ([tvCardFocus]).
 * - No shadows, glow or blur. Animations are transform/alpha only and <= 150 ms.
 */

/** "White fill" variant: swaps the background between [container] and the white focus fill. */
fun Modifier.tvFocusFill(
    focused: Boolean,
    shape: Shape,
    container: Color = Color.Transparent,
    focusedContainer: Color = TvColors.Focus,
): Modifier = background(color = if (focused) focusedContainer else container, shape = shape)

/**
 * "Card" variant: white ring around the card (drawn outside its bounds, so parents must not
 * clip it) and a scale-up. Apply it to the element the ring should hug (the poster, not the title).
 */
@Composable
fun Modifier.tvCardFocus(
    focused: Boolean,
    cornerRadius: Dp = TvDimens.CardRadius,
    ringColor: Color = TvColors.Focus,
    scale: Float = TvDimens.CardFocusScale,
): Modifier {
    // A card that was never focused (most cards in a list) gets no animation state, layer or
    // draw node at all; they are added when the card first gains focus.
    val everFocused = remember { BooleanArray(1) }
    if (focused) everFocused[0] = true
    if (!everFocused[0]) return this

    val scaleState = animateFloatAsState(
        targetValue = if (focused) scale else 1f,
        animationSpec = if (focused) {
            tween(durationMillis = TvDimens.FocusAnimationMs, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "tv-card-focus-scale",
    )
    return this
        .graphicsLayer {
            val value = scaleState.value
            scaleX = value
            scaleY = value
        }
        .drawWithContent {
            if (focused) {
                val stroke = TvDimens.FocusRingWidth.toPx()
                val outset = TvDimens.FocusRingOffset.toPx() + stroke / 2f
                val radius = cornerRadius.toPx() + outset
                drawRoundRect(
                    color = ringColor,
                    topLeft = Offset(-outset, -outset),
                    size = Size(size.width + outset * 2f, size.height + outset * 2f),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = stroke),
                )
            }
            drawContent()
        }
}
