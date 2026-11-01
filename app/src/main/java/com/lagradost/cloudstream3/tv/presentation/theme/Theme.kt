package com.lagradost.cloudstream3.tv.presentation.theme // ktlint-disable filename

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme

@Composable
fun CloudStreamTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = rememberCloudStreamColorScheme()
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = MaterialTheme.shapes,
        typography = Typography,
        content = content
    )
}
