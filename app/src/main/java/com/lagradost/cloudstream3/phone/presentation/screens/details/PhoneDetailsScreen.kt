package com.lagradost.cloudstream3.phone.presentation.screens.details

import com.lagradost.cloudstream3.phone.presentation.common.navBarBottom
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ExpandMore
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager.DownloadType
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.designsystem.CsColors
import com.lagradost.cloudstream3.phone.presentation.common.CsButton
import com.lagradost.cloudstream3.phone.presentation.common.CsIconButton
import com.lagradost.cloudstream3.phone.presentation.common.CsPosterCard
import com.lagradost.cloudstream3.phone.presentation.common.CsPosterImage
import com.lagradost.cloudstream3.phone.presentation.common.CsSectionHeader
import com.lagradost.cloudstream3.phone.presentation.theme.LocalPhoneAccent
import com.lagradost.cloudstream3.phone.presentation.theme.LocalPhoneOnAccent
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneDimens
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneShapes
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsScreenMode
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsScreenUiState
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsScreenViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.details.detailsRatingText
import com.lagradost.cloudstream3.tv.presentation.screens.details.isSeriesContent
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerStartTarget
import com.lagradost.cloudstream3.ui.WatchType

/** One scrolling screen for movies and series (mockups "Film" / "Serial"); no tabs. */
@Composable
fun PhoneDetailsScreen(
    mode: DetailsScreenMode,
    viewModel: DetailsScreenViewModel,
    onBack: () -> Unit,
    onPlay: (PlayerStartTarget) -> Unit,
    onSimilarClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Box(modifier.fillMaxSize().background(PhoneColors.Ground)) {
        when (val s = state) {
            is DetailsScreenUiState.Done -> DetailsContent(
                mode = mode,
                details = s.details,
                compat = viewModel.actionsCompat,
                onBack = onBack,
                onPlay = onPlay,
                onFavorite = viewModel::onFavoriteClick,
                onBookmark = viewModel::onBookmarkClick,
                onSimilarClick = onSimilarClick,
            )
            is DetailsScreenUiState.Loading -> LoadingContent(s.preview.title.orEmpty(), s.preview.posterUri, s.preview.backdropUri, onBack)
            is DetailsScreenUiState.Error -> CenteredMessage(s.error.message, viewModel::retry, onBack)
            DetailsScreenUiState.Unavailable -> CenteredMessage(viewModel.unavailableDetails.title, null, onBack)
        }
    }
}

/** What is already known (from the list the user tapped) plus placeholders, so the screen does not jump when data arrives. */
@Composable
private fun LoadingContent(title: String, posterUri: String?, backdropUri: String?, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box {
            Box(Modifier.fillMaxWidth().height(250.dp).background(PhoneColors.Card)) {
                (backdropUri?.takeIf { it.isNotBlank() } ?: posterUri?.takeIf { it.isNotBlank() })?.let {
                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(170.dp).background(Brush.verticalGradient(listOf(Color.Transparent, PhoneColors.Ground))))
            }
            CsIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.phone_back), onBack, Modifier.statusBarsPadding().padding(12.dp), container = Color(0xB30E1015), size = 44.dp)
        }
        Row(Modifier.padding(horizontal = 16.dp).pullUp(84.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
            CsPosterImage(posterUri.orEmpty(), title, Modifier.size(108.dp, 162.dp))
            Text(title, style = PhoneText.Level.copy(fontSize = 28.sp), color = PhoneColors.Text, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().height(52.dp).clip(PhoneShapes.Pill).background(PhoneColors.Card))
            Box(Modifier.fillMaxWidth(0.9f).height(12.dp).clip(PhoneShapes.Pill).background(PhoneColors.Card))
            Box(Modifier.fillMaxWidth(0.7f).height(12.dp).clip(PhoneShapes.Pill).background(PhoneColors.Card))
        }
    }
}

@Composable
private fun CenteredMessage(text: String, onRetry: (() -> Unit)?, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        CsIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.phone_back), onBack, Modifier.padding(12.dp))
        Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text, style = PhoneText.Row, color = PhoneColors.Text2)
            if (onRetry != null) CsButton(stringResource(R.string.phone_retry), onRetry)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsContent(
    mode: DetailsScreenMode,
    details: MovieDetails,
    compat: com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat?,
    onBack: () -> Unit,
    onPlay: (PlayerStartTarget) -> Unit,
    onFavorite: () -> Unit,
    onBookmark: (WatchType) -> Unit,
    onSimilarClick: (Movie) -> Unit,
) {
    val isSeries = mode.isSeriesContent(details)
    var showBookmarkSheet by remember { mutableStateOf(false) }
    var showSeasonSheet by remember { mutableStateOf(false) }
    var seasonIndex by remember(details.id) {
        mutableIntStateOf(details.seasons.indexOfFirst { it.seasonNumber == details.currentSeason }.coerceAtLeast(0))
    }
    // Accent from the title's artwork (same extraction as TV); the series / movie colors are the fallback until it is ready.
    val artwork = com.lagradost.cloudstream3.tv.util.rememberArtworkColorRoles(details.backdropUri.ifBlank { details.posterUri }, isDarkScheme = true)
    val accent = artwork?.primary ?: if (isSeries) CsColors.Info else CsColors.AccentDefault
    val onAccent = artwork?.onPrimary ?: CsColors.OnAccentDefault
    val twoColumns = isSeries && androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentCompat by androidx.compose.runtime.rememberUpdatedState(compat)
    val actions = rememberPhoneDetailsActions({ currentCompat }, onPlay, context)

    androidx.compose.runtime.CompositionLocalProvider(LocalPhoneAccent provides accent, LocalPhoneOnAccent provides onAccent) {
        val top: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {
        item(key = "hero") { Hero(details, isSeries, onBack) }
        item(key = "tags") {
            if (details.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = PhoneDimens.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 14.dp),
                ) { items(details.categories) { Tag(it) } }
            }
        }
        item(key = "actions") {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                // Same rule as TV: "Play S3:E2" for the current episode (resume target or next unwatched).
                val resume = details.currentSeason?.let { s -> details.currentEpisode?.let { e -> stringResource(R.string.phone_play_episode, s, e) } }
                CsButton(
                    label = resume ?: stringResource(R.string.home_play),
                    onClick = { onPlay(PlayerStartTarget.Default) },
                    icon = Icons.Default.PlayArrow,
                    primary = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (mode.allowsBookmark) {
                        ActionTile(
                            icon = if (details.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            label = details.bookmarkLabelRes?.let { stringResource(it) } ?: stringResource(R.string.phone_watchlist),
                            modifier = Modifier.weight(1f),
                            onClick = { showBookmarkSheet = true },
                        )
                    }
                    if (!isSeries) {
                        val snapshot = rememberDownloadSnapshot(actions, compat, context, null, null)
                        DownloadTile(snapshot, Modifier.weight(1f)) { actions.openDownload() }
                    }
                    ActionTile(
                        icon = if (details.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        label = stringResource(R.string.phone_favorite),
                        modifier = Modifier.weight(1f),
                        onClick = onFavorite,
                    )
                    if (mode.allowsExtendedActions) {
                        ActionTile(
                            icon = Icons.Default.MoreHoriz,
                            label = stringResource(R.string.phone_more_options),
                            modifier = Modifier.weight(1f),
                            onClick = { actions.openMore() },
                        )
                    }
                }
            }
        }
        if (details.description.isNotBlank()) {
            item(key = "description") { Description(details.description) }
        }
        }
        val episodes: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {
        if (isSeries && details.seasons.isNotEmpty()) {
            val season = details.seasons[seasonIndex.coerceIn(0, details.seasons.lastIndex)]
            item(key = "episodes-header") {
                Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.episodes), style = PhoneText.Section, color = PhoneColors.Text, modifier = Modifier.weight(1f))
                    SeasonDropdown(seasonLabel(season), onClick = { showSeasonSheet = true })
                }
            }
            itemsIndexed(season.episodes, key = { index, ep -> "${ep.id}#$index" }) { _, ep ->
                val season = ep.seasonNumber ?: season.seasonNumber
                EpisodeRow(
                    ep = ep,
                    watched = rememberWatched(actions, compat, season, ep.episodeNumber),
                    snapshot = rememberDownloadSnapshot(actions, compat, context, season, ep.episodeNumber),
                    onClick = { onPlay(PlayerStartTarget.DirectEpisodeData(ep.data)) },
                    onDownload = { actions.openDownload(season, ep.episodeNumber) },
                    onMore = { actions.openMore(season, ep.episodeNumber) },
                )
            }
        }
        }
        val bottom: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {
        if (details.cast.isNotEmpty()) {
            item(key = "cast") {
                CsSectionHeader(stringResource(R.string.phone_cast_crew), Modifier.padding(top = 26.dp, bottom = 10.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(details.cast, key = { index, c -> "${c.id}#$index" }) { _, c ->
                        Column(Modifier.width(84.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(84.dp).clip(CircleShape).background(PhoneColors.Placeholder)) {
                                if (c.avatarUrl.isNotBlank()) AsyncImage(c.avatarUrl, c.realName, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Text(c.realName, style = PhoneText.PosterTitle, color = PhoneColors.Text, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                            if (c.characterName.isNotBlank()) Text(c.characterName, style = PhoneText.Meta.copy(fontSize = 12.sp), color = PhoneColors.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
        item(key = "info") { InfoCard(details, isSeries) }
        if (details.similarMovies.isNotEmpty()) {
            item(key = "similar") {
                CsSectionHeader(stringResource(R.string.phone_more_like_this), Modifier.padding(top = 26.dp, bottom = 10.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(PhoneDimens.PosterGap)) {
                    itemsIndexed(details.similarMovies, key = { index, m -> "${m.id}#$index" }) { _, m -> CsPosterCard(m.name, m.posterUri, { onSimilarClick(m) }) }
                }
            }
        }
        }
        // Tablet: info on the left, episodes on the right (mockup "Tablet — Serial"); phone: one column.
        if (twoColumns) {
            Row(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(bottom = 40.dp + navBarBottom())) { top(); bottom() }
                LazyColumn(Modifier.weight(1.15f).fillMaxHeight().statusBarsPadding(), contentPadding = PaddingValues(end = 12.dp, bottom = 40.dp + navBarBottom())) { episodes() }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp + navBarBottom())) { top(); episodes(); bottom() }
        }

        DetailsActionSheet(actions)
        if (showBookmarkSheet) {
            ModalBottomSheet(onDismissRequest = { showBookmarkSheet = false }, containerColor = PhoneColors.Sheet) {
                SheetTitle(stringResource(R.string.phone_set_watch_status), details.name)
                val currentRes = details.bookmarkLabelRes ?: WatchType.NONE.stringRes
                WatchType.entries.forEach { type ->
                    SheetRow(stringResource(type.stringRes), selected = type.stringRes == currentRes) { onBookmark(type); showBookmarkSheet = false }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
            }
        }
        if (showSeasonSheet) {
            ModalBottomSheet(onDismissRequest = { showSeasonSheet = false }, containerColor = PhoneColors.Sheet) {
                SheetTitle(stringResource(R.string.season), details.name)
                details.seasons.forEachIndexed { i, s ->
                    SheetRow(seasonLabel(s), selected = i == seasonIndex, subtitle = stringResource(R.string.phone_episodes_count, s.episodes.size)) { seasonIndex = i; showSeasonSheet = false }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun seasonLabel(season: TvSeason): String =
    season.title?.takeIf { it.isNotBlank() }
        ?: season.displaySeasonNumber?.let { "${stringResource(R.string.season)} $it" }
        ?: stringResource(R.string.phone_extras)

@Composable
private fun Hero(details: MovieDetails, isSeries: Boolean, onBack: () -> Unit) {
    Box {
        Box(Modifier.fillMaxWidth().height(250.dp).background(PhoneColors.Card)) {
            val image = details.backdropUri.ifBlank { details.posterUri }
            if (image.isNotBlank()) AsyncImage(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(170.dp).background(Brush.verticalGradient(listOf(Color.Transparent, PhoneColors.Ground))))
        }
        CsIconButton(
            Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.phone_back), onBack,
            Modifier.statusBarsPadding().padding(12.dp), container = Color(0xB30E1015), size = 44.dp,
        )
    }
    Row(Modifier.pullUp(84.dp).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
        CsPosterImage(details.posterUri, details.name, Modifier.size(108.dp, 162.dp).border(1.dp, Color(0x14FFFFFF), PhoneShapes.Poster))
        Column(Modifier.weight(1f).padding(bottom = 2.dp)) {
            val kind = if (isSeries) stringResource(R.string.tv_series_singular) else stringResource(R.string.movies_singular)
            Text(listOf(kind, details.providerName).filter { it.isNotBlank() }.joinToString(" • "), style = PhoneText.Meta, color = LocalPhoneAccent.current, maxLines = 1)
            Text(details.name, style = PhoneText.Level.copy(fontSize = androidx.compose.ui.unit.TextUnit(28f, androidx.compose.ui.unit.TextUnitType.Sp)), color = PhoneColors.Text, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            val rating = details.score?.toDouble(10)?.takeIf { it >= 1.0 }?.let { "★ " + "%.1f".format(it) }
            val meta = listOfNotNull(rating, details.releaseDate.takeIf { it.isNotBlank() }?.take(4), details.duration.takeIf { it.isNotBlank() }).joinToString(" • ")
            if (meta.isNotEmpty()) Text(meta, style = PhoneText.Meta, color = PhoneColors.Body, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** Overlaps the previous item (poster over the backdrop) and gives the layout the space back, unlike `offset`. */
private fun Modifier.pullUp(amount: androidx.compose.ui.unit.Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val px = amount.roundToPx()
    layout(placeable.width, placeable.height - px) { placeable.placeRelative(0, -px) }
}

@Composable
private fun Tag(text: String) {
    Box(Modifier.height(30.dp).clip(PhoneShapes.Pill).background(PhoneColors.Card).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, style = PhoneText.Meta, color = PhoneColors.Body, maxLines = 1)
    }
}

@Composable
private fun ActionTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.height(64.dp).clip(RoundedCorner16).background(PhoneColors.Card).clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, null, tint = PhoneColors.Text, modifier = Modifier.size(22.dp))
        Text(label, style = PhoneText.Meta, color = PhoneColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
    }
}

private val RoundedCorner16 = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

@Composable
private fun Description(text: String) {
    var expanded by remember(text) { mutableStateOf(false) }
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
        Text(text, style = PhoneText.Body, color = PhoneColors.Body, maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
        Text(
            stringResource(if (expanded) R.string.phone_description_less else R.string.phone_description_more),
            style = PhoneText.Button, color = LocalPhoneAccent.current,
            modifier = Modifier.height(44.dp).clickable(role = Role.Button) { expanded = !expanded }.wrapCenter(),
        )
    }
}

private fun Modifier.wrapCenter(): Modifier = this.then(Modifier.padding(top = 12.dp))

@Composable
private fun SeasonDropdown(label: String, onClick: () -> Unit) {
    Row(
        Modifier.height(40.dp).clip(PhoneShapes.Pill).border(1.dp, PhoneColors.Outline, PhoneShapes.Pill).clickable(role = Role.DropdownList, onClick = onClick).padding(start = 16.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = PhoneText.Button.copy(fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)), color = PhoneColors.Text, maxLines = 1)
        Icon(Icons.Default.ExpandMore, null, tint = PhoneColors.Text, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EpisodeRow(
    ep: TvEpisode,
    watched: Boolean,
    snapshot: com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatDownloadSnapshot?,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(112.dp, 63.dp).clip(PhoneShapes.Poster).background(PhoneColors.Placeholder)) {
            if (ep.posterUri.isNotBlank()) AsyncImage(ep.posterUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (watched) Box(Modifier.padding(6.dp).size(22.dp).clip(CircleShape).background(Color(0xC70E1015)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, stringResource(R.string.phone_watched), tint = CsColors.Info, modifier = Modifier.size(14.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            val title = ep.title.ifBlank { stringResource(R.string.phone_episode_label, ep.episodeNumber ?: 0) }
            Text(listOfNotNull(ep.episodeNumber?.toString(), title).joinToString(". "), style = PhoneText.Row.copy(fontSize = 14.sp), color = PhoneColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(ep.durationMinutes?.let { "$it min" }, ep.ratingText).joinToString(" • ")
            if (meta.isNotEmpty()) Text(meta, style = PhoneText.Meta, color = PhoneColors.Text2, modifier = Modifier.padding(top = 3.dp))
            if (ep.description.isNotBlank()) Text(ep.description, style = PhoneText.Meta, color = PhoneColors.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DownloadIconButton(snapshot, onDownload)
        CsIconButton(Icons.Default.MoreVert, stringResource(R.string.phone_more_options), onMore, container = Color.Transparent, size = 44.dp)
    }
}

@Composable
private fun downloadLabel(snapshot: com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatDownloadSnapshot?): String {
    val percent = snapshot.percent()
    return when {
        snapshot?.status == DownloadType.IsDone -> stringResource(R.string.phone_download_done)
        snapshot?.status == DownloadType.IsFailed -> stringResource(R.string.phone_download_failed)
        snapshot != null && (snapshot.hasPendingRequest || snapshot.status == DownloadType.IsDownloading || snapshot.status == DownloadType.IsPending) ->
            stringResource(R.string.phone_download_downloading, percent ?: 0)
        else -> stringResource(R.string.phone_download)
    }
}

@Composable
private fun DownloadIconButton(snapshot: com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatDownloadSnapshot?, onClick: () -> Unit) {
    val percent = snapshot.percent()
    val busy = snapshot != null && (snapshot.hasPendingRequest || snapshot.status == DownloadType.IsDownloading || snapshot.status == DownloadType.IsPending)
    val done = snapshot?.status == DownloadType.IsDone
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Accessibility label carries the state; the glyph shows it visually.
        when {
            busy -> androidx.compose.material3.CircularProgressIndicator(
                progress = { (percent ?: 0) / 100f }, modifier = Modifier.size(28.dp), strokeWidth = 3.dp,
                color = LocalPhoneAccent.current, trackColor = Color(0x2EFFFFFF),
            )
            done -> Icon(Icons.Default.CheckCircle, stringResource(R.string.phone_download_state, downloadLabel(snapshot)), tint = CsColors.Info)
            else -> Icon(Icons.Default.Download, stringResource(R.string.phone_download_state, downloadLabel(snapshot)), tint = PhoneColors.Body)
        }
    }
}

@Composable
private fun DownloadTile(snapshot: com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatDownloadSnapshot?, modifier: Modifier, onClick: () -> Unit) {
    val done = snapshot?.status == DownloadType.IsDone
    ActionTile(
        icon = if (done) Icons.Default.CheckCircle else Icons.Default.Download,
        label = downloadLabel(snapshot).replaceFirstChar { it.uppercase() },
        modifier = modifier,
        onClick = onClick,
    )
}

/** Everything the app knows about the title (same fields as the TV "Details" tab, plus type, seasons and next episode). */
@Composable
private fun InfoCard(d: MovieDetails, isSeries: Boolean) {
    val status = d.status.takeIf { it.isNotBlank() } ?: when (d.showStatus) {
        com.lagradost.cloudstream3.ShowStatus.Ongoing -> stringResource(R.string.status_ongoing)
        com.lagradost.cloudstream3.ShowStatus.Completed -> stringResource(R.string.status_completed)
        null -> ""
    }
    val score = detailsRatingText(d.score)?.let { stringResource(R.string.tv_details_score_format, it) }.orEmpty()
    val type = d.type?.name.orEmpty()
    val seasons = if (isSeries && d.seasonCount != null) stringResource(R.string.phone_seasons_episodes, d.seasonCount ?: 0, d.episodeCount ?: 0) else ""
    val next = d.nextAiring?.let {
        val date = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(it.unixTime * 1000L))
        stringResource(R.string.phone_next_episode_value, it.season ?: 1, it.episode, date)
    }.orEmpty()
    val rows = listOf(
        stringResource(R.string.phone_label_original_title) to d.originalTitle?.takeIf { it != d.name }.orEmpty(),
        stringResource(R.string.phone_label_type) to type,
        stringResource(R.string.status) to status,
        stringResource(R.string.phone_label_next_episode) to next,
        stringResource(R.string.tv_details_release_date) to d.releaseDate,
        stringResource(R.string.tv_details_runtime) to d.duration,
        stringResource(R.string.phone_label_seasons) to seasons,
        stringResource(R.string.tv_details_age_rating) to d.pgRating,
        stringResource(R.string.tv_original_language) to d.originalLanguage,
        stringResource(R.string.tv_details_director) to d.director,
        stringResource(R.string.tv_details_screenplay) to d.screenplay,
        stringResource(R.string.tv_details_music) to d.music,
        stringResource(R.string.tv_budget) to d.budget,
        stringResource(R.string.tv_revenue) to d.revenue,
        stringResource(R.string.tv_details_score) to score,
        stringResource(R.string.tv_details_source) to d.providerName,
    ).filter { it.second.isNotBlank() }
    if (rows.isEmpty()) return
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 26.dp)) {
        Text(stringResource(R.string.phone_details_section), style = PhoneText.Section, color = PhoneColors.Text, modifier = Modifier.padding(bottom = 10.dp))
        Column(Modifier.fillMaxWidth().clip(PhoneShapes.Card).background(PhoneColors.Card)) {
            rows.forEachIndexed { i, (k, v) ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(PhoneColors.Divider))
                Row(Modifier.padding(16.dp, 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(k, style = PhoneText.Meta, color = PhoneColors.Text2, modifier = Modifier.width(112.dp))
                    Text(v, style = PhoneText.Meta, color = PhoneColors.Text, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String, subtitle: String? = null) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)) {
        Text(text, style = PhoneText.Section, color = PhoneColors.Text)
        if (!subtitle.isNullOrBlank()) Text(subtitle, style = PhoneText.Meta, color = PhoneColors.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SheetRow(text: String, selected: Boolean, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).background(if (selected) Color(0x14FFFFFF) else Color.Transparent).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text, style = PhoneText.Row, color = if (selected) CsColors.Info else PhoneColors.Text)
            if (subtitle != null) Text(subtitle, style = PhoneText.Meta, color = PhoneColors.Text2)
        }
        if (selected) Icon(Icons.Default.Check, null, tint = CsColors.Info)
    }
}
