package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

internal const val DetailsThemePaletteCacheMaxEntries = 48
internal const val DetailsThemeSampleSizePx = 96
internal const val DetailsThemePaletteColorCount = 24

@Immutable
internal data class DetailsArtworkPalette(
    val primarySeed: Color,
    val secondarySeed: Color,
    val tertiarySeed: Color,
    val backgroundSeed: Color,
    val surfaceSeed: Color,
)

internal object DetailsArtworkPaletteCache {
    private val cache = LruCache<String, DetailsArtworkPalette>(DetailsThemePaletteCacheMaxEntries)

    @Synchronized
    fun get(key: String): DetailsArtworkPalette? = cache.get(key)

    @Synchronized
    fun put(key: String, value: DetailsArtworkPalette) {
        cache.put(key, value)
    }
}

internal suspend fun Drawable.toDetailsArtworkPaletteOrNull(
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
