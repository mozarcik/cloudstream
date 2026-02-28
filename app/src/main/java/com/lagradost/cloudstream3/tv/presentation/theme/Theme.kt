package com.lagradost.cloudstream3.tv.presentation.theme // ktlint-disable filename

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.darkColorScheme

private val darkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
)

@Composable
fun CloudStreamTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme,
        shapes = MaterialTheme.shapes,
        typography = Typography,
        content = content
    )
//    {
////         Set default content color and text style for all Text components
//        CompositionLocalProvider(
//            LocalContentColor provides darkColorScheme.onBackground
//        ) {
//            ProvideTextStyle(
//                value = TextStyle(color = darkColorScheme.onBackground)
//            ) {
//                content()
//            }
//        }
//    }
}

object CloudStreamSurfaceDefaults {

    @Composable
    fun border(
        shape: Shape = CloudStreamCardShape,
        width: Dp = 1.dp,
        color: Color = MaterialTheme.colorScheme.border,
    ) = ClickableSurfaceDefaults.border(
        focusedBorder = Border(
            border = BorderStroke(width = width, color = color),
            shape = shape
        ),
        pressedBorder = Border(
            border = BorderStroke(width = width, color = color),
            shape = shape
        )
    )

    @Composable
    fun glow(
        elevation: Dp = 5.dp,
        elevationColor: Color = MaterialTheme.colorScheme.primary,
    ) = ClickableSurfaceDefaults.glow(
        focusedGlow = Glow(
            elevation = elevation,
            elevationColor = elevationColor
        ),
        pressedGlow = Glow(
            elevation = elevation,
            elevationColor = elevationColor
        )
    )

    @Composable
    fun colors(
        containerColor: Color = Color.Transparent,
        focusedContainerColor: Color = Color.Transparent,
    ) = ClickableSurfaceDefaults.colors(
        containerColor = containerColor,
        focusedContainerColor = focusedContainerColor
    )

    fun scale(
        focusedScale: Float = 1.06f,
    ) = ClickableSurfaceDefaults.scale(focusedScale = focusedScale)
}
