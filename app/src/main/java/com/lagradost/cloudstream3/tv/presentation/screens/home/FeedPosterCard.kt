package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.compat.home.ratingLabelOrNull
import com.lagradost.cloudstream3.tv.presentation.common.LocalHaloController
import com.lagradost.cloudstream3.tv.presentation.common.LocalHaloEnabled
import com.lagradost.cloudstream3.tv.presentation.common.PosterCard
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape

@Composable
internal fun FeedPosterCard(
    item: MediaItemCompat,
    onClick: () -> Unit,
    onFocused: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val haloController = LocalHaloController.current
    val isHaloEnabled = LocalHaloEnabled.current
    val haloKey = remember(item.apiName, item.id, item.url, item.posterUri) {
        Any()
    }

    PosterCard(
        model = item.posterUri.takeIf { uri -> uri.isNotBlank() } ?: item.url,
        title = item.name,
        ratingLabel = item.ratingLabelOrNull(),
        shape = CloudStreamCardShape,
        onClick = onClick,
        onFocus = { focusInfo ->
            if (focusInfo.isFocused) {
                onFocused?.invoke()
            }

            if (!isHaloEnabled) {
                return@PosterCard
            }

            if (focusInfo.isFocused) {
                haloController.onItemFocused(
                    key = haloKey,
                    rectInRoot = focusInfo.boundsInRoot,
                    color = focusInfo.accentColor
                )
            } else {
                haloController.onItemFocusCleared(haloKey)
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
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = CloudStreamCardShape)
    )
}
