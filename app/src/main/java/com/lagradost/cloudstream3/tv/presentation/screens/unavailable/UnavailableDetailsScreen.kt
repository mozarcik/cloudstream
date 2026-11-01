package com.lagradost.cloudstream3.tv.presentation.screens.unavailable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.tv.presentation.common.TvButton
import com.lagradost.cloudstream3.tv.presentation.common.TvButtonVariant
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

@Immutable
data class UnavailableDetailsUiModel(
    val title: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val description: String? = null,
    val type: TvType? = null,
    val year: Int? = null,
    val providerName: String? = null,
)

/**
 * "This title is currently unavailable" (source removed): poster tile, eyebrow, title, explanation and two
 * actions. Search manually is focused first, Remove from library (when allowed) is the destructive one.
 */
@Composable
fun UnavailableDetailsScreen(
    state: UnavailableDetailsUiModel,
    showRemoveFromLibraryAction: Boolean,
    onRemoveFromLibrary: () -> Unit,
    onManualSearch: (String) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val manualSearchFocusRequester = remember { FocusRequester() }
    val typeLabel = rememberTypeLabel(type = state.type)
    val provider = state.providerName?.trim().orEmpty()
    val safeTitle = state.title.ifBlank { stringResource(R.string.details) }
    val manualSearchQuery = remember(state.title) { state.title.trim() }

    FocusRequestEffect(requester = manualSearchFocusRequester, requestKey = state.title)
    BackHandler(onBack = onBackPressed)

    Row(
        horizontalArrangement = Arrangement.spacedBy(TvDimens.UnavailablePosterGap),
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Ground)
            .padding(start = TvDimens.ScreenHorizontalPadding, top = TvDimens.UnavailableTop),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(TvDimens.UnavailablePosterWidth, TvDimens.UnavailablePosterHeight)
                .clip(TvShapes.Row)
                .background(TvColors.PosterPlaceholder),
        ) {
            if (state.posterUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Outlined.BrokenImage,
                    contentDescription = null,
                    tint = TvColors.Disabled,
                    modifier = Modifier.size(TvDimens.StateIconSize / 2),
                )
            } else {
                AsyncImage(
                    model = state.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(modifier = Modifier.widthIn(max = TvDimens.UnavailableTextWidth)) {
            if (typeLabel != null) {
                Text(
                    text = typeLabel.uppercase(),
                    style = TvTextStyles.Eyebrow,
                    color = TvColors.LabelPanel,
                )
            }
            Text(
                text = safeTitle,
                style = TvTextStyles.Title,
                color = TvColors.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = TvDimens.UnavailableTitleTop),
            )
            Text(
                text = stringResource(R.string.tv_unavailable_details_title),
                style = TvTextStyles.StateTitle,
                color = TvColors.Text,
                modifier = Modifier.padding(top = TvDimens.UnavailableHeadingTop),
            )
            Text(
                text = if (provider.isNotBlank()) {
                    stringResource(R.string.tv_unavailable_details_message_with_provider, provider)
                } else {
                    stringResource(R.string.tv_unavailable_details_message)
                },
                style = TvTextStyles.StateMessage,
                color = TvColors.Text2,
                modifier = Modifier.padding(top = TvDimens.StateMessageGap),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(TvDimens.StateButtonGap),
                modifier = Modifier.padding(top = TvDimens.UnavailableActionsTop),
            ) {
                TvButton(
                    text = stringResource(R.string.tv_unavailable_details_search_button),
                    icon = Icons.Filled.Search,
                    height = TvDimens.ButtonHeightLarge,
                    onClick = { if (manualSearchQuery.isNotBlank()) onManualSearch(manualSearchQuery) },
                    modifier = Modifier.focusRequester(manualSearchFocusRequester),
                )
                if (showRemoveFromLibraryAction) {
                    TvButton(
                        text = stringResource(R.string.tv_unavailable_details_remove_button),
                        icon = Icons.Filled.DeleteOutline,
                        variant = TvButtonVariant.Destructive,
                        height = TvDimens.ButtonHeightLarge,
                        onClick = onRemoveFromLibrary,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberTypeLabel(type: TvType?): String? {
    val tvLabel = stringResource(R.string.tv_series_singular)
    val movieLabel = stringResource(R.string.movies_singular)
    val resolvedType = type ?: return null

    return if (resolvedType.isEpisodeBased()) {
        tvLabel
    } else {
        movieLabel
    }
}
