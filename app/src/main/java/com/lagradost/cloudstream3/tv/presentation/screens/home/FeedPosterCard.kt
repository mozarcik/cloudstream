package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.compat.home.ratingLabelOrNull
import com.lagradost.cloudstream3.tv.presentation.common.tvSkeleton
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.common.PosterCard
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens

@Composable
internal fun FeedPosterCard(
    item: MediaItemCompat,
    onClick: () -> Unit,
    onFocused: (() -> Unit)? = null,
    focusableTestTag: String? = null,
    shape: Shape = CloudStreamCardShape,
    ringCornerRadius: Dp = TvDimens.CardRadius,
    modifier: Modifier = Modifier,
) {
    val wasFocused = remember { BooleanArray(1) }
    PosterCard(
        model = item.posterUri.takeIf { uri -> uri.isNotBlank() } ?: item.url,
        title = item.name,
        ratingLabel = item.ratingLabelOrNull(),
        shape = shape,
        ringCornerRadius = ringCornerRadius,
        onClick = onClick,
        focusableTestTag = focusableTestTag,
        onFocus = { focusInfo ->
            // PosterCard reports on every layout change while focused; notify only on focus gain.
            if (focusInfo.isFocused != wasFocused[0]) {
                wasFocused[0] = focusInfo.isFocused
                if (focusInfo.isFocused) onFocused?.invoke()
            }
        },
        modifier = modifier,
    )
}

@Composable
internal fun FeedPosterSkeleton(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .tvSkeleton(TvShapes.Card)
    )
}
