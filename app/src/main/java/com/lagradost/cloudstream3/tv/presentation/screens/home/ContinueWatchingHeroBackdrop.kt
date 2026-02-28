package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    Box(
        modifier = modifier.background(Color(0xFF10151D))
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
                        colors = listOf(
                            Color(0xAD10151D),
                            Color(0x6B10151D),
                            Color(0xC610151D)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x5610151D),
                            Color(0xB010151D)
                        )
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
