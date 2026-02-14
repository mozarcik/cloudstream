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
    primary = Color(0xFFE3E2E6),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF00468A),
    onPrimaryContainer = Color(0xFFE3E2E6),
    secondary = Color(0xFFBDC7DC),
    onSecondary = Color(0xFF273141),
    secondaryContainer = Color(0xFF3E4758),
    onSecondaryContainer = Color(0xFFE3E2E6),
    tertiary = Color(0xFFDCBCE1),
    onTertiary = Color(0xFF3E2845),
    tertiaryContainer = Color(0xFF563E5C),
    onTertiaryContainer = Color(0xFFF9D8FE),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC4C6CF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFB4AB),
    border = Color(0xFF8E9099),
)

@Composable
fun CloudStreamTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme,
        shapes = MaterialTheme.shapes,
        typography = Typography,
    ) {
        // Set default content color and text style for all Text components
        CompositionLocalProvider(
            LocalContentColor provides darkColorScheme.onBackground
        ) {
            ProvideTextStyle(
                value = TextStyle(color = darkColorScheme.onBackground)
            ) {
                content()
            }
        }
    }
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
