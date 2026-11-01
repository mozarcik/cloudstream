package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.MaterialTheme
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens

private const val MovieImageBiasY = -0.6f // mockup object-position "center 20%"
private const val SeriesImageBiasX = -0.4f // mockup object-position "30% top"
private const val SeriesImageBiasY = -1f
private const val SideFadeSolidStop = 0.25f
private const val SideFadeSolidAlpha = 0.82f
private const val SideFadeEndStop = 0.62f
private const val BottomFadeMidStop = 0.55f
private const val BottomFadeMidAlpha = 0.92f

/**
 * Details backdrop: artwork on the right, a fade into the page color on the left and bottom and, while the
 * page is scrolled into a tab's content, a dim layer. Colors come from the (dynamic) color scheme.
 * [dim] is read in the draw phase only, so animating it does not recompose.
 */
@Composable
internal fun DetailsBackdrop(
    uri: String,
    headers: Map<String, String>,
    isSeries: Boolean,
    dim: () -> Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val background = MaterialTheme.colorScheme.background
    val request = remember(context, uri, headers) {
        ImageRequest.Builder(context)
            .data(uri.takeIf { it.isNotBlank() })
            .crossfade(false)
            .apply {
                if (headers.isNotEmpty()) {
                    httpHeaders(
                        NetworkHeaders.Builder().apply {
                            this["User-Agent"] = USER_AGENT
                            headers.forEach { (key, value) -> this[key] = value }
                        }.build()
                    )
                }
            }
            .build()
    }
    val start: Dp
    val width: Dp
    val height: Dp
    val fadeTop: Dp
    val alignment: Alignment
    if (isSeries) {
        start = TvDimens.DetailsSeriesBackdropStart
        width = TvDimens.DetailsSeriesBackdropWidth
        height = TvDimens.DetailsSeriesBackdropHeight
        fadeTop = TvDimens.DetailsSeriesFadeTop
        alignment = BiasAlignment(SeriesImageBiasX, SeriesImageBiasY)
    } else {
        start = TvDimens.DetailsMovieBackdropStart
        width = TvDimens.DetailsMovieBackdropWidth
        height = TvDimens.DetailsMovieBackdropHeight
        fadeTop = TvDimens.DetailsMovieFadeTop
        alignment = BiasAlignment(0f, MovieImageBiasY)
    }

    Box(modifier = modifier.fillMaxSize().background(background)) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = alignment,
            modifier = Modifier
                .offset(x = start)
                .width(width)
                .height(height),
        )
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val imageLeft = start.toPx()
                    val imageWidth = width.toPx()
                    val imageHeight = height.toPx()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to background,
                            SideFadeSolidStop to background.copy(alpha = SideFadeSolidAlpha),
                            SideFadeEndStop to Color.Transparent,
                            startX = imageLeft,
                            endX = imageLeft + imageWidth,
                        ),
                        topLeft = Offset(imageLeft, 0f),
                        size = Size(imageWidth, imageHeight),
                    )
                    val fadeStart = fadeTop.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            BottomFadeMidStop to background.copy(alpha = BottomFadeMidAlpha),
                            1f to background,
                            startY = fadeStart,
                            endY = imageHeight,
                        ),
                        topLeft = Offset(0f, fadeStart),
                        size = Size(size.width, size.height - fadeStart),
                    )
                    val dimAmount = dim()
                    if (dimAmount > 0f) {
                        drawRect(color = background.copy(alpha = TvDimens.DetailsDimAlpha * dimAmount))
                    }
                },
        )
    }
}
