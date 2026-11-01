package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.compat.home.ratingLabelOrNull
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/**
 * Title + "year • type" of the focused poster (feed header, "Show more" header). Not focusable.
 * Reads [focusedItem] here, so only this composable recomposes when focus moves.
 */
@Composable
fun TvFocusedItemTitle(
    focusedItem: State<MediaItemCompat?>,
    modifier: Modifier = Modifier,
    /** Library: "year • ★ rating" instead of "year • type". */
    showRating: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(TvDimens.FeedFocusedTitleGap),
) {
    val item = focusedItem.value ?: return
    val typeLabel = if (item is MediaItemCompat.TvSeries || item.type?.isEpisodeBased() == true) {
        stringResource(R.string.tv_series_singular)
    } else {
        stringResource(R.string.movies_singular)
    }
    val year = when (item) {
        is MediaItemCompat.Movie -> item.year
        is MediaItemCompat.TvSeries -> item.year
        else -> null // MediaItemCompat.Other carries no year
    }
    val meta = listOfNotNull(
        year?.toString(),
        if (showRating) item.ratingLabelOrNull()?.let { "★ $it" } else typeLabel,
    ).joinToString(separator = " • ")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
        modifier = modifier,
    ) {
        Text(
            text = item.name,
            style = TvTextStyles.FeedFocusedTitle,
            color = TvColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = TvDimens.FeedFocusedTitleMaxWidth),
        )
        Text(
            text = meta,
            style = TvTextStyles.FeedFocusedMeta,
            color = TvColors.Text2,
            maxLines = 1,
        )
    }
}
