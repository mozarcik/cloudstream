package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.tv.compat.home.FeaturedItemCompat

@Composable
internal fun FeaturedCarouselItem(
    item: FeaturedItemCompat,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
) {
    val backdropGradient = remember {
        val panel = TvColors.FeaturedPanel
        Brush.horizontalGradient(
            0.0f to panel,
            0.30f to panel,
            0.45f to panel.copy(alpha = 0.70f),
            0.70f to panel.copy(alpha = 0f),
            1.0f to panel.copy(alpha = 0f),
        )
    }

    Box(modifier = modifier.background(TvColors.Card)) {
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
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = TvDimens.FeaturedContentTop)
                .widthIn(max = TvDimens.FeaturedTextMaxWidth + TvDimens.FeaturedContentPadding * 2)
                .padding(horizontal = TvDimens.FeaturedContentPadding)
        ) {
            Text(
                text = "FEATURED • ${item.navigationTarget.apiName}".uppercase(),
                style = TvTextStyles.Eyebrow,
                color = TvColors.FeaturedEyebrow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = TvDimens.FeaturedEyebrowGap)
            )
            Text(
                text = item.name,
                style = TvTextStyles.HeroFeatured,
                color = TvColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .height(TvDimens.FeaturedTitleHeight)
                    .wrapContentHeight(Alignment.CenterVertically, unbounded = true),
            )

            FeaturedCarouselMetadata(item = item)

            item.description?.let { description ->
                Text(
                    text = description,
                    style = TvTextStyles.FeaturedBody,
                    color = TvColors.BodyBright,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = FeaturedCarouselDescriptionTopPadding)
                )
            }

            item.supportingLabel?.let { supportingLabel ->
                Text(
                    text = supportingLabel,
                    style = TvTextStyles.FeaturedSupporting,
                    color = TvColors.RowSubtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = FeaturedCarouselSupportingTopPadding)
                )
            }

            if (isFocused) {
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
private fun FeaturedCarouselMetadata(
    item: FeaturedItemCompat,
) {
    val typeLabel = item.navigationTarget.type?.let { type ->
        if (type.isEpisodeBased()) stringResource(R.string.tv_series_singular)
        else stringResource(R.string.movies_singular)
    }
    val scoreLabel = item.scoreLabel?.takeIf { it.isNotBlank() }?.let { "★ $it" }
    val metadataLabel = remember(item.year, typeLabel, scoreLabel) {
        listOfNotNull(
            item.year?.toString(),
            typeLabel,
            scoreLabel
        ).joinToString(separator = " • ").takeIf { metadata -> metadata.isNotBlank() }
    }

    if (metadataLabel != null) {
        Text(
            text = metadataLabel,
            style = TvTextStyles.MetaLarge,
            color = TvColors.Meta,
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
            .height(FeaturedCarouselCtaHeight)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(TvColors.Focus)
            .padding(start = TvDimens.ButtonPaddingStart, end = TvDimens.ButtonPaddingEnd)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_outline_info_24),
            contentDescription = null,
            tint = TvColors.OnFocus,
            modifier = Modifier.size(TvDimens.ButtonIconSize)
        )
        Spacer(modifier = Modifier.size(TvDimens.ButtonIconGap))
        Text(
            text = stringResource(R.string.details),
            style = TvTextStyles.Button,
            color = TvColors.OnFocus
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
