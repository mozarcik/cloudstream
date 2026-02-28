package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape

@Composable
internal fun ContinueWatchingHeroPlaceholder(
    message: String? = null,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val placeholderGradient = remember(surfaceVariantColor, surfaceColor) {
        listOf(surfaceVariantColor, surfaceColor)
    }
    Box(
        modifier = modifier
            .clip(ContinueWatchingHeroShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = placeholderGradient
                )
            )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.44f)
                .padding(
                    start = 32.dp,
                    end = 16.dp,
                    bottom = ContinueWatchingCardHeight + ContinueWatchingInfoBottomGap + ContinueWatchingCardsBottomInset
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(onSurfaceColor.copy(alpha = 0.12f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(onSurfaceColor.copy(alpha = 0.1f))
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(onSurfaceColor.copy(alpha = 0.2f))
                )
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(onSurfaceColor.copy(alpha = 0.12f))
                )
            }
            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = onSurfaceColor.copy(alpha = 0.84f)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = ContinueWatchingCardsBottomInset
                )
        ) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .width(ContinueWatchingCardWidth)
                        .height(ContinueWatchingCardHeight)
                        .clip(CloudStreamCardShape)
                        .background(onSurfaceColor.copy(alpha = 0.14f))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}
