package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.icons.CustomDownload
import com.lagradost.cloudstream3.tv.presentation.common.TvButton
import com.lagradost.cloudstream3.tv.presentation.common.TvButtonVariant
import com.lagradost.cloudstream3.tv.presentation.focus.TvFocusMemory
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsDownloadActionState
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsQuickAction
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import androidx.tv.material3.MaterialTheme
import kotlin.math.roundToInt

internal const val HeroKeyWatchlist = "hero:watchlist"
internal const val HeroKeyDownload = "hero:download"
internal const val HeroKeyMore = "hero:more"

private const val RingSweepFull = 360f
private const val RingStartAngle = -90f
private const val RingTrackAlpha = 0.25f

/**
 * Hero action buttons: Play, Watchlist, Favorite, Download, Search, More. Every button has a fixed width and a
 * label; a state change swaps icon and label but never the width. Down from any button goes to the tabs.
 */
@Composable
internal fun DetailsActionsRow(
    details: MovieDetails,
    playLabel: String,
    downloadState: MovieDetailsDownloadActionState,
    actions: DetailsHeroActions,
    focus: DetailsHeroFocus,
    downRequester: FocusRequester,
    onPlayClick: () -> Unit,
    onQuickAction: (MovieDetailsQuickAction) -> Unit,
    onFocused: () -> Unit,
    memory: TvFocusMemory,
    modifier: Modifier = Modifier,
) {
    val labelStyle = TvTextStyles.Button
    val watchlistLabel = details.bookmarkLabelRes
        ?.takeIf { details.isBookmarked }
        ?.let { stringResource(it) }
        ?: stringResource(R.string.watchlist_list_name)
    // Each width follows the label of the current state, so only a user action (or new data) changes it.
    val playWidth = rememberDetailsButtonWidth(listOf(playLabel), labelStyle)
    val watchlistWidth = rememberDetailsButtonWidth(listOf(watchlistLabel), labelStyle)
    val favoriteWidth = rememberDetailsButtonWidth(listOf(stringResource(R.string.favorite)), labelStyle)
    val downloadWidth = rememberDetailsButtonWidth(listOf(downloadWidthLabel(downloadState)), labelStyle)
    val searchWidth = rememberDetailsButtonWidth(listOf(stringResource(R.string.search)), labelStyle)
    val moreWidth = rememberDetailsButtonWidth(listOf(stringResource(R.string.tv_details_more)), labelStyle)

    fun Modifier.requester(target: FocusRequester) = this
        .focusRequester(target)
        .focusProperties { down = downRequester }

    // Buttons that open a panel are tracked so focus returns to them when the panel closes.
    fun Modifier.tracked(key: String) = with(memory) { track(key) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(TvDimens.DetailsActionGap),
        modifier = modifier
            .requiredHeight(TvDimens.ButtonHeight)
            .focusGroup()
            .onFocusChanged { state -> if (state.hasFocus) onFocused() },
    ) {
        if (actions.showPlay) {
            TvButton(
                text = playLabel,
                icon = Icons.Filled.PlayArrow,
                onClick = onPlayClick,
                variant = TvButtonVariant.Primary,
                width = playWidth,
                modifier = Modifier.requester(focus.play).testTag("details_play_button"),
            )
        }
        if (actions.showWatchlist) {
            TvButton(
                text = watchlistLabel,
                icon = if (details.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                onClick = { onQuickAction(MovieDetailsQuickAction.Bookmark) },
                width = watchlistWidth,
                modifier = Modifier.requester(focus.watchlist).tracked(HeroKeyWatchlist).testTag("action_bookmark"),
            )
        }
        TvButton(
            text = stringResource(R.string.favorite),
            icon = if (details.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            onClick = { onQuickAction(MovieDetailsQuickAction.Favorite) },
            width = favoriteWidth,
            modifier = Modifier.requester(focus.favorite).testTag("action_favorite"),
        )
        if (actions.showDownload) {
            DetailsDownloadButton(
                state = downloadState,
                width = downloadWidth,
                onClick = { onQuickAction(MovieDetailsQuickAction.Download) },
                modifier = Modifier.requester(focus.download).tracked(HeroKeyDownload).testTag("action_download"),
            )
        }
        TvButton(
            text = stringResource(R.string.search),
            icon = Icons.Filled.Search,
            onClick = { onQuickAction(MovieDetailsQuickAction.Search) },
            width = searchWidth,
            modifier = Modifier.requester(focus.search).testTag("action_search"),
        )
        if (actions.showMore) {
            TvButton(
                text = stringResource(R.string.tv_details_more),
                icon = Icons.Filled.MoreVert,
                onClick = { onQuickAction(MovieDetailsQuickAction.More) },
                width = moreWidth,
                modifier = Modifier.requester(focus.more).tracked(HeroKeyMore).testTag("action_more"),
            )
        }
    }
}

/**
 * Label used to size the Download button in the given state. Downloading uses the widest percentage, so the
 * width stays the same while the progress goes from 9% to 10% (and up to 100%).
 */
@Composable
private fun downloadWidthLabel(state: MovieDetailsDownloadActionState): String = when (state) {
    MovieDetailsDownloadActionState.Idle -> stringResource(R.string.download)
    is MovieDetailsDownloadActionState.Downloading -> "${stringResource(R.string.downloading)} 100%"
    MovieDetailsDownloadActionState.Downloaded -> stringResource(R.string.downloaded)
    MovieDetailsDownloadActionState.Failed -> stringResource(R.string.download_failed)
}

/**
 * Width of a hero button: icon + padding + the widest of the given labels (measured with the button style).
 * Callers pass only the label of the current state, so the width is stable within that state.
 */
@Composable
internal fun rememberDetailsButtonWidth(labels: List<String>, style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(labels, style, density) {
        val widest = labels.maxOf { label -> measurer.measure(label, style, maxLines = 1).size.width }
        TvDimens.ButtonPaddingStart + TvDimens.ButtonIconSize + TvDimens.ButtonIconGap +
            with(density) { widest.toDp() } + TvDimens.ButtonPaddingEnd
    }
}

/** Download button: Idle / Downloading n% (progress ring) / Downloaded (green) / Failed (red). */
@Composable
internal fun DetailsDownloadButton(
    state: MovieDetailsDownloadActionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp,
) {
    when (state) {
        MovieDetailsDownloadActionState.Idle -> TvButton(
            text = stringResource(R.string.download),
            icon = Icons.Filled.CustomDownload,
            onClick = onClick,
            width = width,
            modifier = modifier,
        )

        is MovieDetailsDownloadActionState.Downloading -> {
            val percent = (state.progress.coerceIn(0f, 1f) * 100f).roundToInt()
            val accent = MaterialTheme.colorScheme.primary
            TvButton(
                text = "${stringResource(R.string.downloading)} $percent%",
                onClick = onClick,
                width = width,
                iconContent = { content -> DownloadProgressRing(state.progress, accent, content) },
                modifier = modifier,
            )
        }

        MovieDetailsDownloadActionState.Downloaded -> TvButton(
            text = stringResource(R.string.downloaded),
            icon = Icons.Outlined.CheckCircle,
            onClick = onClick,
            width = width,
            tint = TvColors.Success,
            modifier = modifier,
        )

        MovieDetailsDownloadActionState.Failed -> TvButton(
            text = stringResource(R.string.download_failed),
            icon = Icons.Outlined.ErrorOutline,
            onClick = onClick,
            width = width,
            tint = TvColors.ErrorText,
            modifier = modifier,
        )
    }
}

/** Circular progress (mockup: 26 px ring, 3 px stroke, accent arc over a 25% white track). */
@Composable
internal fun DownloadProgressRing(progress: Float, accent: Color, track: Color, size: androidx.compose.ui.unit.Dp = TvDimens.ActionRingSize) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = TvDimens.ActionRingStroke.toPx()
        val inset = stroke / 2f
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(
            color = track.copy(alpha = RingTrackAlpha),
            startAngle = 0f,
            sweepAngle = RingSweepFull,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = accent,
            startAngle = RingStartAngle,
            sweepAngle = RingSweepFull * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
