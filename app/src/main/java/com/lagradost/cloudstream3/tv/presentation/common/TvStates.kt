package com.lagradost.cloudstream3.tv.presentation.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/** Placeholder block shown while content loads (no shimmer: static fill, cheap on weak GPUs). */
fun Modifier.tvSkeleton(shape: Shape = TvShapes.Card): Modifier = background(TvColors.Skeleton, shape)

@Composable
fun TvSkeletonBlock(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = TvShapes.Card,
) {
    Spacer(modifier = modifier.size(width, height).tvSkeleton(shape))
}

/** Skeleton row of poster-sized blocks, the loading state of feeds / grids. */
@Composable
fun TvPosterSkeletonRow(
    count: Int,
    modifier: Modifier = Modifier,
    posterWidth: Dp = TvDimens.PosterWidth,
    posterHeight: Dp = TvDimens.PosterHeight,
    gap: Dp = TvDimens.MiniGridGap,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = modifier) {
        repeat(count) {
            TvSkeletonBlock(width = posterWidth, height = posterHeight)
        }
    }
}

/** Centered icon + title + message (+ optional action): empty lists, no sources, no downloads. */
@Composable
fun TvEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    /** Penpot illustration (`R.drawable.tv_empty_*`); wins over [icon]. */
    @DrawableRes illustration: Int? = null,
    message: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        if (illustration != null) {
            Image(
                painter = painterResource(illustration),
                contentDescription = null,
                modifier = Modifier.size(TvDimens.StateIllustrationSize),
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TvColors.Disabled,
                modifier = Modifier.size(TvDimens.StateIconSize),
            )
        }
        Text(
            text = title,
            style = TvTextStyles.StateTitle,
            color = TvColors.Text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = TvDimens.StateTitleGap),
        )
        if (message != null) {
            Text(
                text = message,
                style = TvTextStyles.StateMessage,
                color = TvColors.Text2,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = TvDimens.StateMessageGap)
                    .widthIn(max = TvDimens.StateMaxWidth),
            )
        }
        if (action != null) {
            Column(modifier = Modifier.padding(top = TvDimens.StateActionGap)) { action() }
        }
    }
}

/** Error with message and "Try again" (and optional secondary action). */
@Composable
fun TvErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    retryText: String? = null,
    onRetry: (() -> Unit)? = null,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    retryModifier: Modifier = Modifier,
    secondaryModifier: Modifier = Modifier,
    messageStyle: TextStyle = TvTextStyles.DialogBody,
    messageMaxLines: Int = Int.MAX_VALUE,
    @DrawableRes illustration: Int? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        if (illustration != null) {
            Image(
                painter = painterResource(illustration),
                contentDescription = null,
                modifier = Modifier.size(TvDimens.StateIllustrationSize),
            )
        }
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = TvTextStyles.Level,
                color = TvColors.Text,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = TvDimens.StateMaxWidth),
            )
        }
        Text(
            text = message,
            style = messageStyle,
            color = TvColors.ErrorText,
            textAlign = TextAlign.Center,
            maxLines = messageMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = if (title.isNullOrBlank()) 0.dp else TvDimens.StateErrorTitleGap)
                .widthIn(max = TvDimens.StateMaxWidth),
        )
        if ((onRetry != null && retryText != null) || (onSecondary != null && secondaryText != null)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(TvDimens.StateButtonGap),
                modifier = Modifier.padding(top = TvDimens.StateActionGap),
            ) {
                if (onRetry != null && retryText != null) {
                    TvButton(
                        text = retryText,
                        onClick = onRetry,
                        height = TvDimens.ButtonHeightLarge,
                        modifier = retryModifier,
                    )
                }
                if (onSecondary != null && secondaryText != null) {
                    TvButton(
                        text = secondaryText,
                        onClick = onSecondary,
                        height = TvDimens.ButtonHeightLarge,
                        modifier = secondaryModifier,
                    )
                }
            }
        }
    }
}
