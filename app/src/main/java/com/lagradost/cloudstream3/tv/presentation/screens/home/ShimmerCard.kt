/*
 * CloudStream TV - Shimmer Card for Loading State
 */

package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.tv.material3.MaterialTheme

/**
 * Shimmer card placeholder shown while loading
 * Task 5.2: Loading states
 */
@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(shimmerBrush())
    )
}

@Composable
fun shimmerBrush(): Brush {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val shimmerColors = remember(surfaceColor, surfaceVariantColor) {
        listOf(
            surfaceColor,
            surfaceVariantColor.copy(alpha = 0.78f),
            surfaceColor,
        )
    }
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )
}
