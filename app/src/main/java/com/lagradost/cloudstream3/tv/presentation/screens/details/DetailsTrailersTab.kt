package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.data.entities.MovieTrailer
import com.lagradost.cloudstream3.tv.presentation.focus.tvFocusFill
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

private const val FocusedAccentAlpha = 0.12f

/** "Trailers": up to three rows centered in the content area. Focus enters on the first row, Up leaves to the tabs. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun DetailsTrailersTab(
    trailers: List<MovieTrailer>,
    entryRequester: FocusRequester,
    upRequester: FocusRequester,
    onTrailerClick: (MovieTrailer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstItem = remember { FocusRequester() }
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = TvDimens.CastRingInset),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(TvDimens.TrailerRowGap),
            modifier = Modifier
                .width(TvDimens.TrailerRowWidth)
                .focusRequester(entryRequester)
                .focusRestorer(firstItem),
        ) {
            trailers.forEachIndexed { position, trailer ->
                key(trailer.index) {
                    TrailerRow(
                        trailer = trailer,
                        onClick = { onTrailerClick(trailer) },
                        modifier = if (position == 0) {
                            Modifier.focusRequester(firstItem).focusProperties { up = upRequester }
                        } else {
                            Modifier
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrailerRow(trailer: MovieTrailer, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val accentFill = TvColors.OnFocus.copy(alpha = FocusedAccentAlpha)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(TvDimens.TrailerRowHeight)
            .tvFocusFill(focused = focused, shape = TvShapes.TrailerRow, container = TvColors.FillRow)
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = TvDimens.TrailerRowPadding),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(TvDimens.TrailerCircleSize)
                .background(if (focused) accentFill else TvColors.BorderChip, TvShapes.Pill),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = if (focused) TvColors.OnFocus else colors.onSurface,
                modifier = Modifier.size(TvDimens.TrailerCircleSize / 2),
            )
        }
        Spacer(Modifier.width(TvDimens.TrailerRowPadding))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.tv_details_trailer_item, trailer.index + 1),
                style = TvTextStyles.PlayerEpisode,
                color = if (focused) TvColors.OnFocus else colors.onSurface,
                maxLines = 1,
            )
            Text(
                text = trailer.host,
                style = TvTextStyles.FeedFocusedMeta,
                color = if (focused) TvColors.OnFocusSecondary else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TvDimens.InfoChipGap)) {
            if (trailer.qualityLabel.isNotEmpty()) TrailerChip(trailer.qualityLabel, focused, accentFill)
            if (trailer.isHls) TrailerChip("HLS", focused, accentFill)
        }
    }
}

@Composable
private fun TrailerChip(label: String, focused: Boolean, accentFill: androidx.compose.ui.graphics.Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(TvDimens.TrailerChipHeight)
            .background(if (focused) accentFill else TvColors.FillIcon, TvShapes.Pill)
            .padding(horizontal = TvDimens.InfoChipHorizontalPadding),
    ) {
        Text(
            text = label,
            style = TvTextStyles.Chip,
            color = if (focused) TvColors.OnFocus else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
