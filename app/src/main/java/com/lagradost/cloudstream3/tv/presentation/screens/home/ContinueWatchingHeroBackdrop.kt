package com.lagradost.cloudstream3.tv.presentation.screens.home

import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val imageRequest = rememberContinueWatchingImageRequest(imageUrl)
    val ground = CloudStreamTokens.Ground
    Box(modifier = modifier.background(ground)) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0f, -0.4f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(BackdropWidth)
                    .height(BackdropHeight),
            )
        }

        // Left fade so the text stays readable over the artwork.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(BackdropWidth)
                .height(BackdropHeight)
                .background(
                    Brush.horizontalGradient(
                        0f to ground,
                        0.22f to ground.copy(alpha = 0.75f),
                        0.55f to Color.Transparent,
                    )
                )
        )
        // Bottom fade into the rows below.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = BackdropBottomFadeTop)
                .fillMaxWidth()
                .height(BackdropBottomFadeHeight)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.92f to ground,
                    )
                )
        )
    }
}

private val BackdropWidth = TvDimens.HeroBackdropWidth
private val BackdropHeight = TvDimens.HeroBackdropHeight
private val BackdropBottomFadeTop = TvDimens.HeroBackdropFadeTop
private val BackdropBottomFadeHeight = TvDimens.HeroBackdropFadeHeight

@Composable
internal fun rememberContinueWatchingImageRequest(imageUrl: String?): ImageRequest? {
    val context = LocalContext.current

    return remember(context, imageUrl) {
        imageUrl?.takeIf { url ->
            url.isNotBlank()
        }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(false)
                .build()
        }
    }
}
