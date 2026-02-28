package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.tv.compat.home.FeaturedItemCompat

@Composable
internal fun FeaturedCarouselItem(
    item: FeaturedItemCompat,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val scrimColor = MaterialTheme.colorScheme.scrim
    val backdropGradient = remember(scrimColor, surfaceColor) {
        Brush.horizontalGradient(
            colors = listOf(
                scrimColor.copy(alpha = FeaturedCarouselImageOverlayAlpha),
                scrimColor.copy(alpha = FeaturedCarouselMidOverlayAlpha),
                surfaceColor.copy(alpha = FeaturedCarouselSurfaceOverlayAlpha)
            )
        )
    }
    val shadowColor = MaterialTheme.colorScheme.scrim.copy(alpha = FeaturedCarouselShadowAlpha)

    Box(modifier = modifier.background(surfaceColor)) {
        FeaturedCarouselBackdrop(
            item = item,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropGradient)
        )

        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .fillMaxSize()
                .padding(FeaturedCarouselContentPadding)
        ) {
            FeaturedCarouselTitle(
                item = item,
                shadowColor = shadowColor
            )

            FeaturedCarouselMetadata(item = item)

            item.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = FeaturedCarouselDescriptionAlpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = FeaturedCarouselDescriptionTopPadding)
                )
            }

            item.supportingLabel?.let { supportingLabel ->
                Text(
                    text = supportingLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = FeaturedCarouselSupportingTopPadding)
                )
            }

            AnimatedVisibility(visible = isFocused) {
                FeaturedCarouselCtaBadge(
                    modifier = Modifier.padding(top = FeaturedCarouselCtaTopPadding)
                )
            }
        }
    }
}

@Composable
private fun FeaturedCarouselBackdrop(
    item: FeaturedItemCompat,
    modifier: Modifier = Modifier,
) {
    val backdropRequest = rememberFeaturedImageRequest(
        imageUrl = item.backdropUri.ifBlank { item.posterUri },
        headers = item.imageHeaders
    )

    if (backdropRequest != null) {
        AsyncImage(
            model = backdropRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
private fun FeaturedCarouselTitle(
    item: FeaturedItemCompat,
    shadowColor: androidx.compose.ui.graphics.Color,
) {
    var shouldShowTextTitle by remember(item.id, item.logoUri) {
        mutableStateOf(item.logoUri.isNullOrBlank())
    }
    val logoRequest = rememberFeaturedImageRequest(
        imageUrl = item.logoUri.orEmpty(),
        headers = item.imageHeaders
    )
    val titleTypography = MaterialTheme.typography.displayMedium
    val titleStyle = remember(titleTypography, shadowColor) {
        titleTypography.copy(
            shadow = Shadow(
                color = shadowColor,
                offset = Offset(
                    x = FeaturedCarouselShadowOffsetX,
                    y = FeaturedCarouselShadowOffsetY
                ),
                blurRadius = FeaturedCarouselShadowBlurRadius
            )
        )
    }

    if (!shouldShowTextTitle && logoRequest != null) {
        AsyncImage(
            model = logoRequest,
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            onError = {
                shouldShowTextTitle = true
            },
            modifier = Modifier
                .fillMaxWidth(FeaturedCarouselTitleMaxWidthFraction)
                .height(FeaturedCarouselLogoHeight)
                .clip(FeaturedCarouselShape)
        )
    } else {
        Text(
            text = item.name,
            style = titleStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(FeaturedCarouselTitleMaxWidthFraction)
        )
    }
}

@Composable
private fun FeaturedCarouselMetadata(
    item: FeaturedItemCompat,
) {
    val durationLabel = item.durationMinutes?.let { minutes ->
        stringResource(id = R.string.duration_format, minutes)
    }
    val metadataLabel = remember(item.year, item.scoreLabel, durationLabel) {
        listOfNotNull(
            item.year?.toString(),
            item.scoreLabel,
            durationLabel
        ).joinToString(separator = " • ").takeIf { metadata -> metadata.isNotBlank() }
    }

    if (metadataLabel != null) {
        Text(
            text = metadataLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = FeaturedCarouselMetadataTopPadding)
        )
    }
}

@Composable
private fun FeaturedCarouselCtaBadge(
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(FeaturedCarouselShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(
                horizontal = FeaturedCarouselContentPadding,
                vertical = FeaturedCarouselIndicatorContentPadding
            )
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.size(FeaturedCarouselIndicatorContentPadding))
        Text(
            text = stringResource(R.string.details),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun rememberFeaturedImageRequest(
    imageUrl: String,
    headers: kotlinx.collections.immutable.PersistentMap<String, String>,
): ImageRequest? {
    val context = LocalContext.current

    return remember(context, imageUrl, headers) {
        imageUrl.takeIf { url -> url.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(false)
                .apply {
                    if (headers.isNotEmpty()) {
                        httpHeaders(
                            NetworkHeaders.Builder().apply {
                                this["User-Agent"] = USER_AGENT
                                headers.forEach { (key, value) ->
                                    this[key] = value
                                }
                            }.build()
                        )
                    }
                }
                .build()
        }
    }
}
