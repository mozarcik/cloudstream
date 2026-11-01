package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/**
 * Title block shared by the player and the loading screen (mockups PlayerControls / PlayerLoading):
 * title, episode line for series, and "source • quality" (or "year • provider" while loading).
 * Always at the top left; the caller decides whether it is visible.
 */
@Composable
internal fun PlayerTitleBlock(
    title: String,
    episodeLine: String?,
    infoLine: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(top = TvDimens.PlayerTitleTopPadding)
            .widthIn(max = TvDimens.PlayerTitleMaxWidth),
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = TvTextStyles.HeroPlayer,
                color = TvColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!episodeLine.isNullOrBlank()) {
            Text(
                text = episodeLine,
                style = TvTextStyles.PlayerEpisode,
                color = TvColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (!infoLine.isNullOrBlank()) {
            Text(
                text = infoLine,
                style = TvTextStyles.PlayerInfo,
                color = TvColors.PlayerValue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/** Top mask behind the title block (mockup: black 78% at the top fading to 0 over 340 px). */
@Composable
internal fun PlayerTopMask(modifier: Modifier = Modifier) {
    val gradient = remember {
        Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.78f),
            1f to Color.Transparent,
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(TvDimens.PlayerTopGradientHeight)
            .background(gradient),
    ) {}
}
