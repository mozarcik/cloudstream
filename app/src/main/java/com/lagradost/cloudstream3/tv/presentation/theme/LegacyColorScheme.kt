package com.lagradost.cloudstream3.tv.presentation.theme

import androidx.annotation.AttrRes
import androidx.appcompat.R as AppCompatR
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import com.google.android.material.R as MaterialR
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.utils.UIHelper.getResourceColor

/** Fixed palette from the TV redesign; values are owned by [TvColors], this name is kept for existing call sites. */
object CloudStreamTokens {
    val Ground = TvColors.Ground
    val Card = TvColors.Card
    val Panel = TvColors.Panel
    val Focus = TvColors.Focus
    val OnFocus = TvColors.OnFocus
    val Text = TvColors.Text
    val Text2 = TvColors.Text2
    val Label = TvColors.Label
    val Accent = Color(0xFFF4B97A)
    val OnAccent = Color(0xFF2A1A0C)
    val Selected = TvColors.Selected
    val Cyan = Color(0xFF8FD3E0)
    val Outline = TvColors.BorderChip
    val ButtonFill = TvColors.FillButton
    val ChipFill = TvColors.FillChip
}

@Composable
internal fun rememberCloudStreamColorScheme(): ColorScheme = remember {
    darkColorScheme(
        primary = CloudStreamTokens.Accent,
        onPrimary = CloudStreamTokens.OnAccent,
        primaryContainer = Color(0xFF3A2C22),
        onPrimaryContainer = CloudStreamTokens.Text,
        secondary = CloudStreamTokens.Selected,
        onSecondary = CloudStreamTokens.OnFocus,
        secondaryContainer = Color(0x1AFFFFFF),
        onSecondaryContainer = CloudStreamTokens.Text,
        tertiary = CloudStreamTokens.Cyan,
        onTertiary = CloudStreamTokens.OnFocus,
        tertiaryContainer = Color(0x248FD3E0),
        onTertiaryContainer = CloudStreamTokens.Text,
        background = CloudStreamTokens.Ground,
        onBackground = CloudStreamTokens.Text,
        surface = CloudStreamTokens.Ground,
        onSurface = CloudStreamTokens.Text,
        surfaceVariant = CloudStreamTokens.Card,
        onSurfaceVariant = CloudStreamTokens.Text2,
        inverseSurface = CloudStreamTokens.Focus,
        inverseOnSurface = CloudStreamTokens.OnFocus,
        border = CloudStreamTokens.Outline,
        borderVariant = CloudStreamTokens.Outline,
        scrim = Color(0xFF050609),
        inversePrimary = CloudStreamTokens.OnAccent,
    )
}

@Composable
internal fun rememberLegacyCloudStreamColorScheme(): ColorScheme {
    val context = LocalContext.current
    val background = context.resolveThemeColor(
        attr = R.attr.primaryBlackBackground,
        fallbackRes = R.color.primaryBlackBackground,
    )
    val surface = background
    val surfaceVariant = context.resolveThemeColor(
        attr = R.attr.primaryGrayBackground,
        fallbackRes = R.color.primaryGrayBackground,
    )
    val primary = context.resolveThemeColor(
        attr = R.attr.colorPrimary,
        fallbackRes = R.color.colorPrimary,
    )
    val primaryContainer = context.resolveThemeColor(
        attr = R.attr.colorPrimaryDark,
        fallbackRes = R.color.colorPrimaryDark,
    )
    val secondary = context.resolveThemeColor(
        attr = AppCompatR.attr.colorAccent,
        fallbackRes = R.color.colorAccent,
    )
    val onSurface = context.resolveThemeColor(
        attr = R.attr.textColor,
        fallbackRes = R.color.textColor,
    )
    val onSurfaceVariant = context.resolveThemeColor(
        attr = R.attr.grayTextColor,
        fallbackRes = R.color.grayTextColor,
    )
    val onPrimary = context.resolveThemeColor(
        attr = MaterialR.attr.colorOnPrimary,
        fallbackColor = bestContrastAgainst(
            background = primary,
            first = onSurface,
            second = surface,
        ),
    )
    val secondaryContainer = remember(surfaceVariant, primary) {
        lerp(surfaceVariant, primary, 0.18f)
    }
    val tertiaryContainer = remember(surfaceVariant, secondary) {
        lerp(surfaceVariant, secondary, 0.18f)
    }
    val scrim = remember(background, onSurface) {
        if (background.luminance() <= onSurface.luminance()) background else onSurface
    }
    val isLightScheme = background.luminance() > 0.5f

    return remember(
        background,
        surface,
        surfaceVariant,
        primary,
        primaryContainer,
        secondary,
        onSurface,
        onSurfaceVariant,
        onPrimary,
        secondaryContainer,
        tertiaryContainer,
        scrim,
        isLightScheme,
    ) {
        val schemeFactory: (
            Color,
            Color,
            Color,
            Color,
            Color,
            Color,
            Color,
            Color,
            Color,
            Color,
            Color,
            Color,
            Color,
            Color,
            Color,
        ) -> ColorScheme = if (isLightScheme) {
            { primaryColor,
              onPrimaryColor,
              primaryContainerColor,
              secondaryColor,
              secondaryContainerColor,
              tertiaryColor,
              tertiaryContainerColor,
              backgroundColor,
              onBackgroundColor,
              surfaceColor,
              onSurfaceColor,
              surfaceVariantColor,
              onSurfaceVariantColor,
              scrimColor,
              inversePrimaryColor ->
                lightColorScheme(
                    primary = primaryColor,
                    onPrimary = onPrimaryColor,
                    primaryContainer = primaryContainerColor,
                    onPrimaryContainer = bestContrastAgainst(primaryContainerColor, onSurfaceColor, surfaceColor),
                    secondary = secondaryColor,
                    onSecondary = bestContrastAgainst(secondaryColor, onSurfaceColor, surfaceColor),
                    secondaryContainer = secondaryContainerColor,
                    onSecondaryContainer = bestContrastAgainst(secondaryContainerColor, onSurfaceColor, surfaceColor),
                    tertiary = tertiaryColor,
                    onTertiary = bestContrastAgainst(tertiaryColor, onSurfaceColor, surfaceColor),
                    tertiaryContainer = tertiaryContainerColor,
                    onTertiaryContainer = bestContrastAgainst(tertiaryContainerColor, onSurfaceColor, surfaceColor),
                    background = backgroundColor,
                    onBackground = onBackgroundColor,
                    surface = surfaceColor,
                    onSurface = onSurfaceColor,
                    surfaceVariant = surfaceVariantColor,
                    onSurfaceVariant = onSurfaceVariantColor,
                    scrim = scrimColor,
                    inversePrimary = inversePrimaryColor,
                )
            }
        } else {
            { primaryColor,
              onPrimaryColor,
              primaryContainerColor,
              secondaryColor,
              secondaryContainerColor,
              tertiaryColor,
              tertiaryContainerColor,
              backgroundColor,
              onBackgroundColor,
              surfaceColor,
              onSurfaceColor,
              surfaceVariantColor,
              onSurfaceVariantColor,
              scrimColor,
              inversePrimaryColor ->
                darkColorScheme(
                    primary = primaryColor,
                    onPrimary = onPrimaryColor,
                    primaryContainer = primaryContainerColor,
                    onPrimaryContainer = bestContrastAgainst(primaryContainerColor, onSurfaceColor, surfaceColor),
                    secondary = secondaryColor,
                    onSecondary = bestContrastAgainst(secondaryColor, onSurfaceColor, surfaceColor),
                    secondaryContainer = secondaryContainerColor,
                    onSecondaryContainer = bestContrastAgainst(secondaryContainerColor, onSurfaceColor, surfaceColor),
                    tertiary = tertiaryColor,
                    onTertiary = bestContrastAgainst(tertiaryColor, onSurfaceColor, surfaceColor),
                    tertiaryContainer = tertiaryContainerColor,
                    onTertiaryContainer = bestContrastAgainst(tertiaryContainerColor, onSurfaceColor, surfaceColor),
                    background = backgroundColor,
                    onBackground = onBackgroundColor,
                    surface = surfaceColor,
                    onSurface = onSurfaceColor,
                    surfaceVariant = surfaceVariantColor,
                    onSurfaceVariant = onSurfaceVariantColor,
                    scrim = scrimColor,
                    inversePrimary = inversePrimaryColor,
                )
            }
        }

        schemeFactory(
            primary,
            onPrimary,
            primaryContainer,
            secondary,
            secondaryContainer,
            secondary,
            tertiaryContainer,
            background,
            onSurface,
            surface,
            onSurface,
            surfaceVariant,
            onSurfaceVariant,
            scrim,
            lerp(primary, surface, 0.3f),
        )
    }
}

private fun android.content.Context.resolveThemeColor(
    @AttrRes attr: Int,
    fallbackRes: Int,
): Color {
    val resolved = getResourceColor(attr)
    return if (resolved == 0) {
        Color(ContextCompat.getColor(this, fallbackRes))
    } else {
        Color(resolved)
    }
}

private fun android.content.Context.resolveThemeColor(
    @AttrRes attr: Int,
    fallbackColor: Color,
): Color {
    val resolved = getResourceColor(attr)
    return if (resolved == 0) fallbackColor else Color(resolved)
}

private fun bestContrastAgainst(
    background: Color,
    first: Color,
    second: Color,
): Color {
    return if (contrastRatio(first, background) >= contrastRatio(second, background)) {
        first
    } else {
        second
    }
}

private fun contrastRatio(first: Color, second: Color): Float {
    val lighter = maxOf(first.luminance(), second.luminance()) + 0.05f
    val darker = minOf(first.luminance(), second.luminance()) + 0.05f
    return lighter / darker
}
