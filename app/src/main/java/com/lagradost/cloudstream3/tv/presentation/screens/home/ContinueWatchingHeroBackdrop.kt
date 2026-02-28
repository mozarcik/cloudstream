package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
internal fun ContinueWatchingHeroBackdrop(
    posterUrl: String,
    applyBlur: Boolean,
    modifier: Modifier = Modifier,
) {
    val imageRequest = rememberContinueWatchingImageRequest(posterUrl)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val scrimColor = MaterialTheme.colorScheme.scrim
    val overlayBase = remember(surfaceColor, scrimColor) {
        if (surfaceColor.luminance() > 0.5f) surfaceColor else scrimColor
    }
    val horizontalGradientColors = remember(overlayBase, surfaceColor) {
        listOf(
            overlayBase.copy(alpha = 0.88f),
            overlayBase.copy(alpha = 0.56f),
            surfaceColor.copy(alpha = 0.82f),
        )
    }
    val verticalGradientColors = remember(overlayBase) {
        listOf(
            Color.Transparent,
            overlayBase.copy(alpha = 0.32f),
            overlayBase.copy(alpha = 0.64f),
        )
    }

    Box(
        modifier = modifier.background(surfaceColor)
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .then(if (applyBlur) Modifier.blur(8.dp) else Modifier),
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = horizontalGradientColors
                    )
                )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = verticalGradientColors
                    )
                )
        )
    }
}

@Composable
internal fun rememberContinueWatchingImageRequest(imageUrl: String): ImageRequest? {
    val context = LocalContext.current

    return remember(context, imageUrl) {
        imageUrl.takeIf { url ->
            url.isNotBlank()
        }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(false)
                .build()
        }
    }
}
