package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val DetailsThemePaletteCacheMaxEntries = 48
private const val DetailsThemeSampleSizePx = 96
private const val DetailsThemePaletteColorCount = 24
private const val DetailsThemeBackgroundBlendDark = 0.52f
private const val DetailsThemeBackgroundBlendLight = 0.36f
private const val DetailsThemeSurfaceBlendDark = 0.48f
private const val DetailsThemeSurfaceBlendLight = 0.32f
private const val DetailsThemeSurfaceVariantBlendDark = 0.58f
private const val DetailsThemeSurfaceVariantBlendLight = 0.44f
private const val DetailsThemePrimaryContainerBlendDark = 0.30f
private const val DetailsThemePrimaryContainerBlendLight = 0.22f
private const val DetailsThemeSecondaryContainerBlendDark = 0.26f
private const val DetailsThemeSecondaryContainerBlendLight = 0.20f
private const val DetailsThemeTertiaryContainerBlendDark = 0.24f
private const val DetailsThemeTertiaryContainerBlendLight = 0.18f
private const val DetailsThemePrimaryMinSaturation = 0.42f
private const val DetailsThemeCompanionMinSaturation = 0.24f
private const val DetailsThemeSurfaceMinSaturation = 0.08f
private const val DetailsThemePrimaryHueFallback = 26f
private const val DetailsThemeTertiaryHueFallback = -34f
private const val DetailsThemeDistinctHueMinDelta = 18f

private val DetailsThemeBlack = Color(0xFF000000)
private val DetailsThemeWhite = Color(0xFFFFFFFF)

@Immutable
private data class DetailsArtworkPalette(
    val primarySeed: Color,
    val secondarySeed: Color,
    val tertiarySeed: Color,
    val backgroundSeed: Color,
    val surfaceSeed: Color,
)

private object DetailsArtworkPaletteCache {
    private val cache = LruCache<String, DetailsArtworkPalette>(DetailsThemePaletteCacheMaxEntries)

    @Synchronized
    fun get(key: String): DetailsArtworkPalette? = cache.get(key)

    @Synchronized
    fun put(key: String, value: DetailsArtworkPalette) {
        cache.put(key, value)
    }
}

@Composable
internal fun rememberDetailsDynamicColorScheme(
    artworkKey: String,
    artworkUrl: String?,
    baseColorScheme: ColorScheme,
): ColorScheme {
    val context = LocalContext.current
    val cachedPalette = remember(artworkKey) {
        DetailsArtworkPaletteCache.get(artworkKey)
    }
    val artworkPalette by produceState<DetailsArtworkPalette?>(
        initialValue = cachedPalette,
        key1 = artworkKey,
        key2 = artworkUrl,
    ) {
        if (value != null || artworkUrl.isNullOrBlank()) {
            return@produceState
        }

        val resolvedPalette = withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(artworkUrl)
                .allowHardware(false)
                .build()
            val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult
                ?: return@withContext null
            val drawable = result.image.asDrawable(context.resources)
            drawable.toDetailsArtworkPaletteOrNull(
                fallbackPrimary = baseColorScheme.primary,
                fallbackSecondary = baseColorScheme.secondary,
                fallbackTertiary = baseColorScheme.tertiary,
                fallbackSurface = baseColorScheme.surface,
                fallbackBackground = baseColorScheme.background,
            )
        }

        if (resolvedPalette != null) {
            DetailsArtworkPaletteCache.put(artworkKey, resolvedPalette)
        }
        value = resolvedPalette
    }

    return remember(baseColorScheme, artworkPalette) {
        artworkPalette?.let { palette ->
            baseColorScheme.toDetailsDynamicColorScheme(palette)
        } ?: baseColorScheme
    }
}

private fun ColorScheme.toDetailsDynamicColorScheme(
    palette: DetailsArtworkPalette,
): ColorScheme {
    val isLightScheme = background.luminance() > 0.5f
    val primary = palette.primarySeed.normalizeAccent(
        isLightScheme = isLightScheme,
        fallback = primary,
    )
    val secondary = palette.secondarySeed
        .ensureDistinctFrom(reference = primary, fallbackHueShift = DetailsThemePrimaryHueFallback)
        .normalizeCompanion(
            isLightScheme = isLightScheme,
            fallback = secondary,
        )
    val tertiary = palette.tertiarySeed
        .ensureDistinctFrom(reference = primary, fallbackHueShift = DetailsThemeTertiaryHueFallback)
        .ensureDistinctFrom(reference = secondary, fallbackHueShift = -DetailsThemePrimaryHueFallback)
        .normalizeCompanion(
            isLightScheme = isLightScheme,
            fallback = tertiary,
        )

    val backgroundTint = palette.backgroundSeed.normalizeBackgroundSeed(
        isLightScheme = isLightScheme,
        fallback = background,
    )
    val surfaceTint = palette.surfaceSeed.normalizeSurfaceSeed(
        isLightScheme = isLightScheme,
        fallback = surface,
    )
    val surfaceVariantTint = lerp(surfaceTint, secondary, 0.20f).normalizeSurfaceVariantSeed(
        isLightScheme = isLightScheme,
        fallback = surfaceVariant,
    )

    val background = lerp(
        background,
        backgroundTint,
        if (isLightScheme) DetailsThemeBackgroundBlendLight else DetailsThemeBackgroundBlendDark,
    )
    val surface = lerp(
        surface,
        surfaceTint,
        if (isLightScheme) DetailsThemeSurfaceBlendLight else DetailsThemeSurfaceBlendDark,
    )
    val surfaceVariant = lerp(
        surfaceVariant,
        surfaceVariantTint,
        if (isLightScheme) DetailsThemeSurfaceVariantBlendLight else DetailsThemeSurfaceVariantBlendDark,
    )

    val primaryContainer = lerp(
        background,
        primary,
        if (isLightScheme) DetailsThemePrimaryContainerBlendLight else DetailsThemePrimaryContainerBlendDark,
    )
    val secondaryContainer = lerp(
        surfaceVariant,
        secondary,
        if (isLightScheme) DetailsThemeSecondaryContainerBlendLight else DetailsThemeSecondaryContainerBlendDark,
    )
    val tertiaryContainer = lerp(
        surfaceVariant,
        tertiary,
        if (isLightScheme) DetailsThemeTertiaryContainerBlendLight else DetailsThemeTertiaryContainerBlendDark,
    )

    val onBackground = preferredContentColor(background, preferred = onSurface)
    val onSurface = preferredContentColor(surface, preferred = onSurface)
    val onSurfaceVariant = preferredContentColor(surfaceVariant, preferred = onSurfaceVariant)
    val onPrimary = preferredContentColor(primary, preferred = onPrimary)
    val onPrimaryContainer = preferredContentColor(primaryContainer, preferred = onSurface)
    val onSecondary = preferredContentColor(secondary, preferred = onSurface)
    val onSecondaryContainer = preferredContentColor(secondaryContainer, preferred = onSurfaceVariant)
    val onTertiary = preferredContentColor(tertiary, preferred = onSurface)
    val onTertiaryContainer = preferredContentColor(tertiaryContainer, preferred = onSurfaceVariant)
    val scrim = lerp(background, DetailsThemeBlack, if (isLightScheme) 0.74f else 0.42f)
    val inversePrimary = lerp(
        primary,
        if (isLightScheme) DetailsThemeBlack else DetailsThemeWhite,
        0.22f,
    )

    return if (isLightScheme) {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            scrim = scrim,
            inversePrimary = inversePrimary,
        )
    } else {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            scrim = scrim,
            inversePrimary = inversePrimary,
        )
    }
}

private suspend fun Drawable.toDetailsArtworkPaletteOrNull(
    fallbackPrimary: Color,
    fallbackSecondary: Color,
    fallbackTertiary: Color,
    fallbackSurface: Color,
    fallbackBackground: Color,
): DetailsArtworkPalette? {
    val sampledBitmap = toDetailsThemeSampleOrNull(DetailsThemeSampleSizePx) ?: return null
    val palette = Palette.from(sampledBitmap)
        .clearFilters()
        .maximumColorCount(DetailsThemePaletteColorCount)
        .generate()

    val primary = palette.firstAvailableColor(
        palette.vibrantSwatch,
        palette.lightVibrantSwatch,
        palette.dominantSwatch,
        palette.mutedSwatch,
        palette.darkVibrantSwatch,
        palette.darkMutedSwatch,
    ) ?: fallbackPrimary
    val secondary = palette.firstAvailableColor(
        palette.mutedSwatch,
        palette.darkMutedSwatch,
        palette.lightMutedSwatch,
        palette.dominantSwatch,
        palette.vibrantSwatch,
    ) ?: fallbackSecondary
    val tertiary = palette.firstAvailableColor(
        palette.lightVibrantSwatch,
        palette.lightMutedSwatch,
        palette.vibrantSwatch,
        palette.dominantSwatch,
        palette.mutedSwatch,
    ) ?: fallbackTertiary
    val background = palette.firstAvailableColor(
        palette.darkMutedSwatch,
        palette.mutedSwatch,
        palette.dominantSwatch,
        palette.darkVibrantSwatch,
        palette.lightMutedSwatch,
    ) ?: fallbackBackground
    val surface = palette.firstAvailableColor(
        palette.dominantSwatch,
        palette.mutedSwatch,
        palette.darkMutedSwatch,
        palette.lightMutedSwatch,
        palette.vibrantSwatch,
    ) ?: fallbackSurface

    return DetailsArtworkPalette(
        primarySeed = primary,
        secondarySeed = secondary,
        tertiarySeed = tertiary,
        backgroundSeed = background,
        surfaceSeed = surface,
    )
}

private fun Palette.firstAvailableColor(
    vararg swatches: Palette.Swatch?,
): Color? {
    return swatches.firstNotNullOfOrNull { swatch ->
        swatch?.rgb?.let(::Color)
    }
}

private suspend fun Drawable.toDetailsThemeSampleOrNull(
    sampleSizePx: Int,
): Bitmap? {
    if (sampleSizePx <= 0) {
        return null
    }

    if (this is BitmapDrawable) {
        val sourceBitmap = bitmap ?: return null
        if (sourceBitmap.width <= 0 || sourceBitmap.height <= 0) {
            return null
        }
        val softwareBitmap = sourceBitmap.toArgb8888BitmapOrNull() ?: return null
        return runCatching {
            if (softwareBitmap.width == sampleSizePx && softwareBitmap.height == sampleSizePx) {
                softwareBitmap
            } else {
                Bitmap.createScaledBitmap(
                    softwareBitmap,
                    sampleSizePx,
                    sampleSizePx,
                    true,
                ).toArgb8888BitmapOrNull()
            }
        }.getOrNull()
    }

    val sampledBitmap = withContext(Dispatchers.Main.immediate) {
        runCatching {
            toBitmap(
                width = sampleSizePx,
                height = sampleSizePx,
                config = Bitmap.Config.ARGB_8888,
            )
        }.getOrNull()
    }
    return sampledBitmap?.toArgb8888BitmapOrNull()
}

private fun Bitmap.toArgb8888BitmapOrNull(): Bitmap? {
    if (isRecycled) {
        return null
    }
    if (config == Bitmap.Config.ARGB_8888) {
        return this
    }
    return runCatching {
        copy(Bitmap.Config.ARGB_8888, false)
    }.getOrNull()
}

private fun Color.normalizeAccent(
    isLightScheme: Boolean,
    fallback: Color,
): Color {
    return adjustHsl(
        fallback = fallback,
        minSaturation = DetailsThemePrimaryMinSaturation,
        lightnessRange = if (isLightScheme) 0.36f..0.50f else 0.58f..0.74f,
    )
}

private fun Color.normalizeCompanion(
    isLightScheme: Boolean,
    fallback: Color,
): Color {
    return adjustHsl(
        fallback = fallback,
        minSaturation = DetailsThemeCompanionMinSaturation,
        lightnessRange = if (isLightScheme) 0.34f..0.52f else 0.52f..0.68f,
    )
}

private fun Color.normalizeBackgroundSeed(
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

private fun Color.normalizeSurfaceSeed(
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

private fun Color.normalizeSurfaceVariantSeed(
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

private fun Color.ensureDistinctFrom(
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

private fun preferredContentColor(
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

private fun contrastRatio(
    first: Color,
    second: Color,
): Float {
    val lighter = maxOf(first.luminance(), second.luminance()) + 0.05f
    val darker = minOf(first.luminance(), second.luminance()) + 0.05f
    return lighter / darker
}
