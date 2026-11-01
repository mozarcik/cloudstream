package com.lagradost.cloudstream3.tv.presentation.screens.home

import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.lagradost.cloudstream3.tv.presentation.common.TvProgressBar
import com.lagradost.cloudstream3.tv.presentation.focus.tvCardFocus
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.compat.home.preferredBackdropUriOrNull

@Composable
internal fun ContinueWatchingHeroCard(
    item: MediaItemCompat,
    colorTokens: ContinueWatchingHeroColorTokens,
    isSelected: Boolean,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    isInteractive: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val progress = (item.continueWatching?.progress ?: 0f).coerceIn(0f, 1f)
    val imageRequest = rememberContinueWatchingImageRequest(item.preferredBackdropUriOrNull())

    Box(
        modifier = modifier
            .width(ContinueWatchingCardWidth)
            .height(ContinueWatchingCardHeight)
            .testTag("continue_watching_card_${item.id}")
            .focusRequester(focusRequester)
            .focusProperties {
                canFocus = isInteractive
                up = upFocusRequester
                down = downFocusRequester
            }
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onFocused()
                }
            }
            // Ring is drawn outside the card, so it must come before the clip.
            .tvCardFocus(focused = isFocused)
            .clip(TvShapes.Card)
            .background(TvColors.PosterPlaceholder)
            .then(
                if (isSelected && !isFocused) {
                    Modifier.border(TvDimens.HeroSelectedBorder, Color.White.copy(alpha = 0.55f), TvShapes.Card)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        val seasonEpisode = item.continueWatchingSeasonEpisodeLabel()
        if (seasonEpisode != null) {
            Text(
                text = seasonEpisode,
                style = TvTextStyles.Badge,
                color = TvColors.Text,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = TvDimens.HeroBadgeStart, bottom = TvDimens.HeroBadgeBottom)
                    .clip(RoundedCornerShape(TvDimens.HeroBadgeRadius))
                    .background(Color(0xD10A0C10))
                    .padding(horizontal = TvDimens.HeroBadgeHorizontal, vertical = TvDimens.HeroBadgeVertical)
            )
        }

        TvProgressBar(
            progress = progress,
            accent = colorTokens.progressIndicatorColor,
            track = Color.White.copy(alpha = 0.25f),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
