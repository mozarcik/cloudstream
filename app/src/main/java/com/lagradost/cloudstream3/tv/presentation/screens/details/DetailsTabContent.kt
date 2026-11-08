package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.data.entities.MovieCast
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.util.StringConstants
import com.lagradost.cloudstream3.tv.presentation.common.PosterCard
import com.lagradost.cloudstream3.tv.presentation.focus.TvFocusMemory
import com.lagradost.cloudstream3.tv.presentation.focus.tvCardFocus
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

private const val GridColumnCount = 6
private const val InitialsMaxLength = 2

/** "Cast & crew": one row of 92x120 dp cards (initials tile when there is no photo). */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun DetailsCastTab(
    cast: List<MovieCast>,
    entryRequester: FocusRequester,
    upRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val firstItem = remember { FocusRequester() }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(TvDimens.CastGap),
        contentPadding = PaddingValues(
            start = TvDimens.DetailsContentStart,
            end = TvDimens.DetailsContentEnd,
            top = TvDimens.CastRingInset,
            bottom = TvDimens.CastRingInset,
        ),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(entryRequester)
            .focusRestorer(firstItem),
    ) {
        itemsIndexed(cast, key = { _, member -> member.id }) { index, member ->
            CastCard(
                member = member,
                modifier = Modifier
                    .then(if (index == 0) Modifier.focusRequester(firstItem) else Modifier)
                    .focusProperties { up = upRequester },
            )
        }
    }
}

@Composable
private fun CastCard(member: MovieCast, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val context = LocalContext.current
    val request = remember(context, member.avatarUrl) {
        ImageRequest.Builder(context).data(member.avatarUrl).crossfade(false).build()
    }
    Column(modifier = Modifier.width(TvDimens.CastCardWidth)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(TvDimens.CastCardWidth, TvDimens.CastCardHeight)
                .tvCardFocus(focused = focused)
                .clip(TvShapes.Card)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(interactionSource = interactionSource, indication = null, onClick = {}),
        ) {
            if (member.avatarUrl.isBlank()) {
                Text(
                    text = initialsOf(member.realName),
                    style = TvTextStyles.Title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = CastInitialsAlpha),
                )
            } else {
                AsyncImage(
                    model = request,
                    contentDescription = StringConstants.Composable.ContentDescription.image(member.realName),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = member.realName,
            style = TvTextStyles.CastName,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = TvDimens.CastNameTop),
        )
        if (member.characterName.isNotBlank()) {
            Text(
                text = member.characterName,
                style = TvTextStyles.CastRole,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun initialsOf(name: String): String = name
    .split(' ')
    .filter { it.isNotBlank() }
    .take(InitialsMaxLength)
    .joinToString("") { it.first().uppercase() }

private const val CastInitialsAlpha = 0.7f

internal fun similarKey(movieId: String): String = "similar:$movieId"

/** "More like this": header with the focused title, then every recommendation in a 6-column grid. */
@Composable
internal fun DetailsSimilarTab(
    title: String,
    movies: List<Movie>,
    entryRequester: FocusRequester,
    upRequester: FocusRequester,
    onMovieSelected: (Movie) -> Unit,
    memory: TvFocusMemory,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    var focusedName by remember { mutableStateOf<String?>(null) }
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(TvDimens.FeedFocusedTitleGap),
            modifier = Modifier.padding(
                start = TvDimens.DetailsContentStart,
                end = TvDimens.DetailsContentEnd,
                bottom = TvDimens.FeedHeaderTitleGap,
            ),
        ) {
            Text(
                text = StringConstants.Composable.movieDetailsScreenSimilarTo(title),
                style = TvTextStyles.HeroSectionTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            focusedName?.let { name ->
                Text(
                    text = name,
                    style = TvTextStyles.FeedFocusedMeta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = TvDimens.FeedFocusedTitleMaxWidth),
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(GridColumnCount),
            horizontalArrangement = Arrangement.spacedBy(TvDimens.GridColumnGap),
            verticalArrangement = Arrangement.spacedBy(TvDimens.GridRowGap),
            contentPadding = PaddingValues(
                start = TvDimens.DetailsContentStart,
                end = TvDimens.DetailsContentEnd,
                top = TvDimens.CastRingInset,
                bottom = TvDimens.SimilarGridBottomInset,
            ),
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { state -> if (!state.hasFocus) focusedName = null }
                .focusRequester(entryRequester)
                .focusRestorer(),
        ) {
            items(movies.size, key = { index -> movies[index].id }) { index ->
                val movie = movies[index]
                PosterCard(
                    model = movie.posterUri.takeIf { it.isNotBlank() } ?: movie.id,
                    title = movie.name,
                    ratingLabel = null,
                    shape = TvShapes.Card,
                    onClick = { onMovieSelected(movie) },
                    onFocusChange = { focused -> if (focused) focusedName = movie.name },
                    modifier = (
                        if (index < GridColumnCount) {
                            Modifier.focusProperties { up = upRequester }
                        } else {
                            Modifier
                        }
                        ).then(with(memory) { Modifier.track(similarKey(movie.id)) }),
                )
            }
        }
        // Posters scroll under the header: fade them out instead of cutting them in half.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TvDimens.SimilarTopFadeHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.background, Color.Transparent),
                    ),
                ),
        )
        }
    }
}

private data class InfoEntry(val label: String, val value: String)

@OptIn(ExperimentalLayoutApi::class)
/** "Details": full story + genres on the left, a two-column grid of facts on the right. Not focusable. */
@Composable
internal fun DetailsInfoTab(details: MovieDetails, modifier: Modifier = Modifier) {
    val story = details.description.ifBlank { stringResource(R.string.normal_no_plot) }
    val statusLabel = details.status.takeIf { it.isNotBlank() } ?: when (details.showStatus) {
        com.lagradost.cloudstream3.ShowStatus.Ongoing -> stringResource(R.string.status_ongoing)
        com.lagradost.cloudstream3.ShowStatus.Completed -> stringResource(R.string.status_completed)
        null -> ""
    }
    val score = detailsRatingText(details.score)?.let { stringResource(R.string.tv_details_score_format, it) }.orEmpty()
    val runtimeText = rememberDetailsRuntimeText(details)
    val labels = listOf(
        stringResource(R.string.tv_details_director),
        stringResource(R.string.tv_details_screenplay),
        stringResource(R.string.tv_details_music),
        stringResource(R.string.tv_details_release_date),
        stringResource(R.string.tv_details_runtime),
        stringResource(R.string.tv_details_age_rating),
        stringResource(R.string.tv_original_language),
        stringResource(R.string.status),
        stringResource(R.string.tv_budget),
        stringResource(R.string.tv_revenue),
        stringResource(R.string.tv_details_score),
        stringResource(R.string.tv_details_source),
    )
    val values = listOf(
        details.director,
        details.screenplay,
        details.music,
        details.releaseDate,
        runtimeText,
        details.pgRating,
        details.originalLanguage,
        statusLabel,
        details.budget,
        details.revenue,
        score,
        details.providerName,
    )
    val entries = remember(labels, values) {
        labels.zip(values) { label, value -> InfoEntry(label.uppercase(), value) }
            .filter { it.value.isNotBlank() }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(start = TvDimens.DetailsContentStart, top = TvDimens.InfoContentTop),
    ) {
        Column(modifier = Modifier.width(TvDimens.InfoColumnWidth)) {
            Text(
                text = stringResource(R.string.tv_details_story).uppercase(),
                style = TvTextStyles.InfoLabel,
                color = TvColors.LabelPanel,
            )
            Text(
                text = story,
                style = TvTextStyles.InfoStory,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = InfoStoryMaxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = TvDimens.InfoLabelValueGap * 2),
            )
            if (details.categories.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.result_tags).uppercase(),
                    style = TvTextStyles.InfoLabel,
                    color = TvColors.LabelPanel,
                    modifier = Modifier.padding(top = TvDimens.InfoSectionGap),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.InfoChipGap),
                    verticalArrangement = Arrangement.spacedBy(TvDimens.InfoChipGap),
                    modifier = Modifier.padding(top = TvDimens.InfoChipGap + TvDimens.InfoLabelValueGap),
                ) {
                    details.categories.take(InfoMaxGenres).forEach { genre ->
                        Text(
                            text = genre,
                            style = TvTextStyles.Chip,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier
                                .background(TvColors.FillChip, TvShapes.Pill)
                                .padding(
                                    horizontal = TvDimens.InfoChipHorizontalPadding,
                                    vertical = TvDimens.InfoChipVerticalPadding,
                                ),
                        )
                    }
                }
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(TvDimens.InfoGridRowGap),
            modifier = Modifier
                .padding(start = TvDimens.InfoGridStart - TvDimens.InfoColumnWidth - TvDimens.DetailsContentStart)
                .width(TvDimens.InfoGridWidth),
        ) {
            entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(TvDimens.InfoGridColumnGap)) {
                    pair.forEach { entry ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = entry.label, style = TvTextStyles.InfoLabel, color = TvColors.LabelPanel, maxLines = 1)
                            Text(
                                text = entry.value,
                                style = TvTextStyles.InfoValue,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.padding(top = TvDimens.InfoLabelValueGap),
                            )
                        }
                    }
                    if (pair.size == 1) Box(modifier = Modifier.weight(1f).height(0.dp))
                }
            }
        }
    }
}

private const val InfoStoryMaxLines = 9
private const val InfoMaxGenres = 6
