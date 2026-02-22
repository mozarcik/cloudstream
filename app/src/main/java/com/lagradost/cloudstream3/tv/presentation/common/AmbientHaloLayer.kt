package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.lagradost.cloudstream3.R
import kotlin.math.sqrt

private const val AMBIENT_HALO_FADE_IN_MS = 220
private const val AMBIENT_HALO_FADE_OUT_MS = 160
private const val AMBIENT_HALO_MOVE_MS = 220
private const val AMBIENT_HALO_SPRITE_SIZE_DP = 620f
private const val AMBIENT_HALO_SCALE_FACTOR = 2.0f
private const val AMBIENT_HALO_MIN_SCALE = 0.78f
private const val AMBIENT_HALO_MAX_SCALE = 2.35f
private const val AMBIENT_HALO_CENTER_Y_SHIFT_FACTOR = 0.18f
private const val AMBIENT_HALO_CENTER_Y_EXTRA_PX = 50f
private const val AMBIENT_HALO_TINT_LAYER_ALPHA = 0.96f
private const val AMBIENT_HALO_NEUTRAL_LAYER_ALPHA = 0.20f

private const val AMBIENT_HALO_MIN_LIGHTNESS = 0.38f
private const val AMBIENT_HALO_MAX_LIGHTNESS = 0.55f
private const val AMBIENT_HALO_MIN_SATURATION = 0.45f

private val AMBIENT_BASE_BACKGROUND_BRUSH = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF101824),
        Color(0xFF131B27),
    )
)

private class AmbientHaloGeometryState {
    var center: Offset = Offset.Zero
    var scale: Float = AMBIENT_HALO_MIN_SCALE
}

@Composable
fun AmbientHaloLayer(
    state: HaloState?,
    hostBoundsInRoot: Rect,
    modifier: Modifier = Modifier,
) {
    val spritePainter = painterResource(id = R.drawable.tv_halo_sprite)
    val density = LocalDensity.current
    val spriteSizeDp = AMBIENT_HALO_SPRITE_SIZE_DP.dp
    val spriteSizePx = with(density) { spriteSizeDp.toPx() }

    val normalizedColor = remember(state?.color) {
        state?.color?.let(::normalizeAmbientHaloColor) ?: Color.White
    }
    val targetCenter = remember(state?.rectInRoot, hostBoundsInRoot) {
        state?.rectInRoot?.let { rect ->
            Offset(
                x = rect.center.x - hostBoundsInRoot.left,
                y = rect.center.y - hostBoundsInRoot.top +
                    (rect.height * AMBIENT_HALO_CENTER_Y_SHIFT_FACTOR) +
                    AMBIENT_HALO_CENTER_Y_EXTRA_PX
            )
        }
    }
    val targetScale = remember(state?.rectInRoot, spriteSizePx) {
        state?.rectInRoot?.let { rect ->
            computeHaloScale(rect = rect, spriteSizePx = spriteSizePx)
        } ?: AMBIENT_HALO_MIN_SCALE
    }

    val retainedGeometry = remember { AmbientHaloGeometryState() }
    SideEffect {
        targetCenter?.let { center ->
            retainedGeometry.center = center
        }
        if (state != null) {
            retainedGeometry.scale = targetScale
        }
    }

    val resolvedCenter = targetCenter ?: retainedGeometry.center
    val resolvedScale = if (state == null) retainedGeometry.scale else targetScale

    val haloAlpha by animateFloatAsState(
        targetValue = if (state == null) 0f else 1f,
        animationSpec = tween(
            durationMillis = if (state == null) AMBIENT_HALO_FADE_OUT_MS else AMBIENT_HALO_FADE_IN_MS,
            easing = LinearEasing
        ),
        label = "ambient-halo-alpha"
    )
    val haloTranslationX = resolvedCenter.x - (spriteSizePx * 0.5f)
    val haloTranslationY = resolvedCenter.y - (spriteSizePx * 0.5f)
    val haloScale by animateFloatAsState(
        targetValue = resolvedScale,
        animationSpec = tween(durationMillis = AMBIENT_HALO_MOVE_MS, easing = FastOutSlowInEasing),
        label = "ambient-halo-scale"
    )
    val haloTint by animateColorAsState(
        targetValue = normalizedColor,
        animationSpec = tween(durationMillis = AMBIENT_HALO_MOVE_MS, easing = FastOutSlowInEasing),
        label = "ambient-halo-tint"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = AMBIENT_BASE_BACKGROUND_BRUSH)
    ) {
        Image(
            painter = spritePainter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(haloTint),
            modifier = Modifier
                .size(spriteSizeDp)
                .graphicsLayer {
                    translationX = haloTranslationX
                    translationY = haloTranslationY
                    scaleX = haloScale
                    scaleY = haloScale
                    alpha = haloAlpha * AMBIENT_HALO_TINT_LAYER_ALPHA
                }
        )

        Image(
            painter = spritePainter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier
                .size(spriteSizeDp)
                .graphicsLayer {
                    translationX = haloTranslationX
                    translationY = haloTranslationY
                    scaleX = haloScale * 1.08f
                    scaleY = haloScale * 1.08f
                    alpha = haloAlpha * AMBIENT_HALO_NEUTRAL_LAYER_ALPHA
                }
        )
    }
}

private fun computeHaloScale(
    rect: Rect,
    spriteSizePx: Float,
): Float {
    if (spriteSizePx <= 0f) {
        return AMBIENT_HALO_MIN_SCALE
    }

    val rectDiagonal = sqrt((rect.width * rect.width) + (rect.height * rect.height))
    val targetSpriteDiameter = rectDiagonal * AMBIENT_HALO_SCALE_FACTOR
    return (targetSpriteDiameter / spriteSizePx).coerceIn(
        minimumValue = AMBIENT_HALO_MIN_SCALE,
        maximumValue = AMBIENT_HALO_MAX_SCALE
    )
}

private fun normalizeAmbientHaloColor(color: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[1] = hsl[1].coerceAtLeast(AMBIENT_HALO_MIN_SATURATION)
    hsl[2] = hsl[2].coerceIn(AMBIENT_HALO_MIN_LIGHTNESS, AMBIENT_HALO_MAX_LIGHTNESS)
    return Color(ColorUtils.HSLToColor(hsl))
}
