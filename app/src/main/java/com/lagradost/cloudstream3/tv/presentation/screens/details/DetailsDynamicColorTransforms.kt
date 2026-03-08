package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

internal fun ColorScheme.toDetailsDynamicColorScheme(
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
