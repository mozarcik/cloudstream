package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlin.math.abs

internal fun Color.normalizeAccent(
    isLightScheme: Boolean,
    fallback: Color,
): Color {
    return adjustHsl(
        fallback = fallback,
        minSaturation = DetailsThemePrimaryMinSaturation,
        lightnessRange = if (isLightScheme) 0.36f..0.50f else 0.58f..0.74f,
    )
}

internal fun Color.normalizeCompanion(
    isLightScheme: Boolean,
    fallback: Color,
): Color {
    return adjustHsl(
        fallback = fallback,
        minSaturation = DetailsThemeCompanionMinSaturation,
        lightnessRange = if (isLightScheme) 0.34f..0.52f else 0.52f..0.68f,
    )
}

internal fun Color.normalizeBackgroundSeed(
    isLightScheme: Boolean,
    fallback: Color,
): Color {
    return adjustHsl(
        fallback = fallback,
        minSaturation = DetailsThemeSurfaceMinSaturation,
        maxSaturation = 0.26f,
        lightnessRange = if (isLightScheme) 0.88f..0.95f else 0.08f..0.16f,
    )
}

internal fun Color.normalizeSurfaceSeed(
    isLightScheme: Boolean,
    fallback: Color,
): Color {
    return adjustHsl(
        fallback = fallback,
        minSaturation = DetailsThemeSurfaceMinSaturation,
        maxSaturation = 0.30f,
        lightnessRange = if (isLightScheme) 0.84f..0.93f else 0.12f..0.20f,
    )
}

internal fun Color.normalizeSurfaceVariantSeed(
    isLightScheme: Boolean,
    fallback: Color,
): Color {
    return adjustHsl(
        fallback = fallback,
        minSaturation = 0.12f,
        maxSaturation = 0.36f,
        lightnessRange = if (isLightScheme) 0.80f..0.90f else 0.18f..0.28f,
    )
}

internal fun Color.ensureDistinctFrom(
    reference: Color,
    fallbackHueShift: Float,
): Color {
    val hueDelta = hueDistanceTo(reference)
    return if (hueDelta >= DetailsThemeDistinctHueMinDelta) {
        this
    } else {
        shiftHue(fallbackHueShift)
    }
}

internal fun preferredContentColor(
    background: Color,
    preferred: Color,
): Color {
    val fallback = if (background.luminance() > 0.5f) {
        DetailsThemeBlack
    } else {
        DetailsThemeWhite
    }
    return if (contrastRatio(preferred, background) >= contrastRatio(fallback, background)) {
        preferred
    } else {
        fallback
    }
}

private fun Color.adjustHsl(
    fallback: Color,
    minSaturation: Float,
    lightnessRange: ClosedFloatingPointRange<Float>,
    maxSaturation: Float = 0.90f,
): Color {
    val argb = toArgb()
    if (argb == 0) {
        return fallback
    }
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    hsl[1] = hsl[1].coerceIn(minSaturation, maxSaturation)
    hsl[2] = hsl[2].coerceIn(lightnessRange.start, lightnessRange.endInclusive)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Color.shiftHue(
    degrees: Float,
): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[0] = (hsl[0] + degrees + 360f) % 360f
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Color.hueDistanceTo(
    other: Color,
): Float {
    val first = FloatArray(3)
    val second = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), first)
    ColorUtils.colorToHSL(other.toArgb(), second)
    val directDistance = abs(first[0] - second[0])
    return minOf(directDistance, 360f - directDistance)
}

private fun contrastRatio(
    first: Color,
    second: Color,
): Float {
    val lighter = maxOf(first.luminance(), second.luminance()) + 0.05f
    val darker = minOf(first.luminance(), second.luminance()) + 0.05f
    return lighter / darker
}
