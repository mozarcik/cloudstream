package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.common.TvSkeletonBlock
import com.lagradost.cloudstream3.tv.presentation.common.tvSkeleton
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/**
 * Loading state of the details page (mockup "Ładowanie szczegółów"): the real title and backdrop when the
 * list item passed them along, skeleton blocks for the rest, in the positions of the final hero.
 */
@Composable
fun DetailsLoadingPlaceholder(
    title: String,
    backdropUri: String?,
    posterUri: String?,
    modifier: Modifier = Modifier,
) {
    val artwork = backdropUri?.takeIf { it.isNotBlank() } ?: posterUri?.takeIf { it.isNotBlank() }
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (artwork == null) {
            Spacer(
                modifier = Modifier
                    .offset(x = TvDimens.DetailsMovieBackdropStart)
                    .width(TvDimens.DetailsMovieBackdropWidth)
                    .height(TvDimens.DetailsMovieBackdropHeight)
                    .tvSkeleton(TvShapes.Row),
            )
        } else {
            DetailsBackdrop(uri = artwork, headers = emptyMap(), isSeries = false, dim = { 0f })
        }
        Column(
            modifier = Modifier
                .padding(start = TvDimens.DetailsContentStart, top = TvDimens.DetailsTextTop),
        ) {
            // Same line boxes as the final hero (an invisible text of the same style), so nothing shifts on swap.
            SkeletonLine(TvDimens.DetailsSkeletonEyebrowWidth, TvTextStyles.DetailsEyebrow)
            if (title.isNotBlank()) {
                var titleLines by remember(title) { mutableIntStateOf(1) }
                Text(
                    text = title,
                    style = TvTextStyles.DetailsTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result -> titleLines = result.lineCount.coerceAtLeast(1) },
                    modifier = Modifier
                        .padding(top = TvDimens.DetailsEyebrowGap)
                        .widthIn(max = TvDimens.DetailsTextWidth)
                        .testTag("details_loading_title")
                        .height(TvDimens.DetailsTitleLineHeight * titleLines)
                        .wrapContentHeight(Alignment.CenterVertically, unbounded = true),
                )
            } else {
                Spacer(Modifier.height(TvDimens.DetailsEyebrowGap))
                TvSkeletonBlock(TvDimens.DetailsSkeletonTitleWidth, TvDimens.DetailsTitleLineHeight)
            }
            Spacer(Modifier.height(TvDimens.DetailsMetaTop))
            SkeletonLine(TvDimens.DetailsSkeletonMetaWidth, TvTextStyles.DetailsMeta)
            Spacer(Modifier.height(TvDimens.DetailsDescriptionTop))
            Column(verticalArrangement = Arrangement.spacedBy(TvDimens.DetailsEyebrowGap)) {
                TvDimens.DetailsSkeletonDescriptionWidths.forEach { width -> TvSkeletonBlock(width, TvDimens.DetailsSkeletonLineHeight, shape = TvShapes.Card) }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(TvDimens.DetailsActionGap),
            modifier = Modifier.offset(
                x = TvDimens.DetailsContentStart,
                y = TvDimens.DetailsHeroHeight - TvDimens.DetailsActionsBottom - TvDimens.ButtonHeight,
            ),
        ) {
            TvDimens.DetailsSkeletonActionWidths.forEach { width -> TvSkeletonBlock(width, TvDimens.ButtonHeight, shape = TvShapes.Pill) }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(TvDimens.DetailsSkeletonTabGap),
            modifier = Modifier.offset(
                x = TvDimens.DetailsContentStart,
                y = TvDimens.DetailsHeroHeight + (TvDimens.DetailsTabHeight - TvDimens.DetailsSkeletonTabHeight) / 2,
            ),
        ) {
            TvDimens.DetailsSkeletonTabWidths.forEach { width -> TvSkeletonBlock(width, TvDimens.DetailsSkeletonTabHeight, shape = TvShapes.Card) }
        }
    }
}

@Composable
private fun SkeletonLine(width: Dp, style: TextStyle) {
    Box(Modifier.width(width).tvSkeleton(TvShapes.Card)) {
        Text(text = " ", style = style, modifier = Modifier.alpha(0f))
    }
}
