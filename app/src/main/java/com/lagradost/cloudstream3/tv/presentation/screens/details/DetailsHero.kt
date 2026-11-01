package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.ProviderType
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.ShowStatus
import com.lagradost.cloudstream3.VPNStatus
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.presentation.focus.TvFocusMemory
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsDownloadActionState
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsQuickAction
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

private const val MaxNotices = 2
private const val NoticeBackgroundAlpha = 0.14f
private const val NoticeTextWhiteMix = 0.55f
private const val MetaTextAlpha = 0.85f
private const val DescriptionTextAlpha = 0.9f
private const val EyebrowSeparator = " • "
private const val MetaSeparator = "•"

/** Focus targets of the hero: the actions row, tabs below and where focus starts. */
internal class DetailsHeroFocus(
    val play: FocusRequester = FocusRequester(),
    val watchlist: FocusRequester = FocusRequester(),
    val favorite: FocusRequester = FocusRequester(),
    val download: FocusRequester = FocusRequester(),
    val search: FocusRequester = FocusRequester(),
    val more: FocusRequester = FocusRequester(),
) {
    /** Where focus starts: Play, or the first visible action when Play is hidden ("Coming soon"). */
    fun initial(showPlay: Boolean, showWatchlist: Boolean): FocusRequester = when {
        showPlay -> play
        showWatchlist -> watchlist
        else -> favorite
    }
}

internal data class DetailsHeroActions(
    val showPlay: Boolean,
    val showWatchlist: Boolean,
    val showDownload: Boolean,
    val showMore: Boolean,
)

/**
 * Top state of the details page: eyebrow, title, meta line, genres line (not focusable),
 * a fixed notice bar and the action buttons. Fills [TvDimens.DetailsHeroHeight].
 */
@Composable
internal fun DetailsHero(
    details: MovieDetails,
    isSeries: Boolean,
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
    val notices = rememberDetailsNotices(details)
    val typeLabel = stringResource(if (isSeries) R.string.tv_series_singular else R.string.movies_singular)
    val eyebrow = remember(typeLabel, details.providerName) {
        listOf(typeLabel, details.providerName.trim())
            .filter { it.isNotBlank() }
            .joinToString(EyebrowSeparator)
            .uppercase()
    }
    val metaItems = rememberDetailsMetaItems(details, isSeries)
    val genres = remember(details.categories) { details.categories.joinToString(", ") }
    val descriptionLines = if (notices.isEmpty()) {
        TvDimens.DetailsDescriptionLines
    } else {
        TvDimens.DetailsDescriptionLinesWithNotice
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(TvDimens.DetailsHeroHeight)
            .padding(start = TvDimens.DetailsContentStart),
    ) {
        Spacer(Modifier.height(TvDimens.DetailsTextTop))
        Text(
            text = eyebrow,
            style = TvTextStyles.DetailsEyebrow,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = TvDimens.DetailsTextWidth),
        )
        // Compose adds the font's own leading to the line box; a fixed slot per line keeps the mockup's line-height 1.
        var titleLines by remember(details.name) { mutableIntStateOf(1) }
        Text(
            text = details.name,
            style = TvTextStyles.DetailsTitle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> titleLines = result.lineCount.coerceAtLeast(1) },
            modifier = Modifier
                .padding(top = TvDimens.DetailsEyebrowGap)
                .widthIn(max = TvDimens.DetailsTextWidth)
                .height(TvDimens.DetailsTitleLineHeight * titleLines)
                .wrapContentHeight(Alignment.CenterVertically, unbounded = true)
                .testTag("details_title_text"),
        )
        if (metaItems.isNotEmpty()) {
            DetailsMetaRow(
                items = metaItems,
                modifier = Modifier
                    .padding(top = TvDimens.DetailsMetaTop)
                    .widthIn(max = TvDimens.DetailsTextWidth),
            )
        }
        if (genres.isNotBlank()) {
            Text(
                text = genres,
                style = TvTextStyles.Body,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = DescriptionTextAlpha),
                maxLines = descriptionLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = TvDimens.DetailsDescriptionTop)
                    .widthIn(max = TvDimens.DetailsDescriptionWidth)
                    .testTag("details_genres"),
            )
        }

        Spacer(Modifier.weight(1f))

        notices.forEach { notice ->
            DetailsNoticeBar(
                notice = notice,
                modifier = Modifier.padding(bottom = TvDimens.DetailsNoticeGap),
            )
        }
        DetailsActionsRow(
            details = details,
            playLabel = playLabel,
            downloadState = downloadState,
            actions = actions,
            focus = focus,
            downRequester = downRequester,
            onPlayClick = onPlayClick,
            onQuickAction = onQuickAction,
            onFocused = onFocused,
            memory = memory,
        )
        Spacer(Modifier.height(TvDimens.DetailsActionsBottom))
    }
}

private sealed interface DetailsMetaItem {
    data class Label(val text: String, val flexible: Boolean = false) : DetailsMetaItem
    data class Rating(val text: String) : DetailsMetaItem
}

@Composable
private fun rememberDetailsMetaItems(details: MovieDetails, isSeries: Boolean): List<DetailsMetaItem> {
    val originalTitle = details.originalTitle
        ?.takeIf { it.isNotBlank() }
        ?.let { stringResource(R.string.details_original_title_format, it) }
    val rating = detailsRatingText(details.score)
    val runtime = rememberDetailsRuntimeText(details)
    val seasons = if (isSeries) {
        details.seasonCount?.takeIf { it > 0 }?.let { count ->
            pluralStringResource(R.plurals.tv_details_seasons_count, count, count)
        }
    } else {
        null
    }
    val status = if (isSeries) {
        when (details.showStatus) {
            ShowStatus.Ongoing -> stringResource(R.string.status_ongoing)
            ShowStatus.Completed -> stringResource(R.string.status_completed)
            null -> null
        }
    } else {
        null
    }
    return remember(originalTitle, details.releaseDate, rating, runtime, details.pgRating, seasons, status) {
        buildList {
            originalTitle?.let { add(DetailsMetaItem.Label(it, flexible = true)) }
            details.releaseDate.takeIf { it.isNotBlank() }?.let { add(DetailsMetaItem.Label(it)) }
            rating?.let { add(DetailsMetaItem.Rating(it)) }
            runtime.takeIf { it.isNotBlank() }?.let { add(DetailsMetaItem.Label(it)) }
            details.pgRating.takeIf { it.isNotBlank() }?.let { add(DetailsMetaItem.Label(it)) }
            seasons?.let { add(DetailsMetaItem.Label(it)) }
            status?.let { add(DetailsMetaItem.Label(it)) }
        }
    }
}

@Composable
private fun DetailsMetaRow(items: List<DetailsMetaItem>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = MetaTextAlpha)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.DetailsMetaGap),
        modifier = modifier,
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Text(text = MetaSeparator, style = TvTextStyles.DetailsMeta, color = TvColors.DotSeparator)
            }
            when (item) {
                is DetailsMetaItem.Label -> Text(
                    text = item.text,
                    style = TvTextStyles.DetailsMeta,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (item.flexible) Modifier.weight(1f, fill = false) else Modifier,
                )

                is DetailsMetaItem.Rating -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.DetailsStarSize / 2),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(TvDimens.DetailsStarSize),
                    )
                    Text(text = item.text, style = TvTextStyles.DetailsMeta, color = color, maxLines = 1)
                }
            }
        }
    }
}

internal fun detailsRatingText(score: Score?): String? = score?.toStringNull(
    minScore = 0.1,
    maxScore = 10,
    decimals = 1,
    removeTrailingZeros = false,
    decimalChar = '.',
)

internal data class DetailsNotice(val icon: ImageVector, val text: String)

/** Provider messages (VPN, no metadata) and "Coming soon" that sit permanently above the buttons. */
@Composable
internal fun rememberDetailsNotices(details: MovieDetails): List<DetailsNotice> {
    val comingSoon = stringResource(R.string.coming_soon)
    val vpnMightBeNeeded = stringResource(R.string.vpn_might_be_needed)
    val vpnTorrent = stringResource(R.string.vpn_torrent)
    val noMetadata = stringResource(R.string.provider_info_meta)
    return remember(details.comingSoon, details.vpnStatus, details.providerType, comingSoon, vpnMightBeNeeded, vpnTorrent, noMetadata) {
        buildList {
            if (details.comingSoon) add(DetailsNotice(Icons.Outlined.Schedule, comingSoon))
            when (details.vpnStatus) {
                VPNStatus.None -> Unit
                VPNStatus.MightBeNeeded -> add(DetailsNotice(Icons.Outlined.Warning, vpnMightBeNeeded))
                VPNStatus.Torrent -> add(DetailsNotice(Icons.Outlined.Warning, vpnTorrent))
            }
            if (details.providerType == ProviderType.MetaProvider) {
                add(DetailsNotice(Icons.Outlined.Info, noMetadata))
            }
        }.take(MaxNotices)
    }
}

@Composable
internal fun DetailsNoticeBar(notice: DetailsNotice, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val textColor = lerp(accent, Color.White, NoticeTextWhiteMix)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.DetailsNoticeIconGap),
        modifier = modifier
            .widthIn(max = TvDimens.DetailsNoticeMaxWidth)
            .heightIn(min = TvDimens.DetailsNoticeHeight)
            .background(accent.copy(alpha = NoticeBackgroundAlpha), androidx.compose.foundation.shape.RoundedCornerShape(TvDimens.DetailsNoticeRadius))
            .padding(horizontal = TvDimens.DetailsNoticeHorizontalPadding, vertical = 4.dp),
    ) {
        Icon(
            imageVector = notice.icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(TvDimens.DetailsNoticeIconSize),
        )
        Text(
            text = notice.text,
            style = TvTextStyles.DetailsNotice,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Compact one-line header shown instead of the hero while the page shows a tab's content. */
@Composable
internal fun DetailsCompactHeader(details: MovieDetails, isSeries: Boolean, modifier: Modifier = Modifier) {
    val rating = detailsRatingText(details.score)
    val summary = remember(details.releaseDate, rating, details.providerName, isSeries, details.seasonCount) {
        listOfNotNull(
            details.releaseDate.takeIf { it.isNotBlank() },
            rating?.let { "★ $it" },
            details.providerName.takeIf { it.isNotBlank() && !isSeries },
        ).joinToString(" $MetaSeparator ")
    }
    val seasons = if (isSeries) {
        details.seasonCount?.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.tv_details_seasons_count, it, it) }
    } else {
        null
    }
    val status = if (isSeries) {
        when (details.showStatus) {
            ShowStatus.Ongoing -> stringResource(R.string.status_ongoing)
            ShowStatus.Completed -> stringResource(R.string.status_completed)
            null -> null
        }
    } else {
        null
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.DetailsCollapsedHeaderGap),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = TvDimens.DetailsContentStart, end = TvDimens.DetailsContentEnd, top = TvDimens.DetailsCollapsedHeaderTop),
    ) {
        Text(
            text = details.name,
            style = TvTextStyles.HeroSeries,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = listOfNotNull(summary.takeIf { it.isNotBlank() }, seasons, status).joinToString(" $MetaSeparator "),
            style = TvTextStyles.MetaLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
