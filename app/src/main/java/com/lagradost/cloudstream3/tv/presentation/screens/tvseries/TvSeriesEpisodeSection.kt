package com.lagradost.cloudstream3.tv.presentation.screens.tvseries

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason
import com.lagradost.cloudstream3.tv.presentation.screens.movies.TitleValueText
import com.lagradost.cloudstream3.tv.presentation.screens.movies.rememberChildPadding
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamBorderWidth
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape
import java.text.DateFormat
import java.util.Date

internal fun resolveInitialSeasonId(
    seasons: List<TvSeason>,
    preferredSeasonNumber: Int?
): String? {
    if (seasons.isEmpty()) return null
    if (preferredSeasonNumber == null) return seasons.first().id

    return seasons.firstOrNull { season ->
        season.displaySeasonNumber == preferredSeasonNumber ||
            season.seasonNumber == preferredSeasonNumber
    }?.id ?: seasons.first().id
}

@Composable
internal fun SeasonsSectionHeader(modifier: Modifier = Modifier) {
    SectionHeaderText(
        text = stringResource(R.string.tv_seasons),
        modifier = modifier
    )
}

@Composable
internal fun EpisodesSectionHeader(modifier: Modifier = Modifier) {
    SectionHeaderText(
        text = stringResource(R.string.episodes),
        modifier = modifier
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SeasonSelectorRow(
    seasons: List<TvSeason>,
    selectedSeasonId: String?,
    onSeasonSelected: (TvSeason) -> Unit,
    modifier: Modifier = Modifier
) {
    if (seasons.isEmpty()) return

    val childPadding = rememberChildPadding()
    val focusManager = LocalFocusManager.current
    val (tabRowFocusRequester, firstTabFocusRequester) = remember { FocusRequester.createRefs() }
    val selectedTabIndex = seasons.indexOfFirst { season -> season.id == selectedSeasonId }
        .takeIf { it >= 0 } ?: 0

    TabRow(
        modifier = modifier
            .focusRequester(tabRowFocusRequester)
            .focusRestorer(firstTabFocusRequester),
        selectedTabIndex = selectedTabIndex,
        separator = { Spacer(modifier = Modifier.width(12.dp)) }
    ) {
        seasons.forEachIndexed { index, season ->
            val tabModifier = if (index == 0) {
                Modifier.focusRequester(firstTabFocusRequester)
            } else {
                Modifier
            }

            Tab(
                selected = index == selectedTabIndex,
                onFocus = { onSeasonSelected(season) },
                onClick = { focusManager.moveFocus(FocusDirection.Down) },
                modifier = tabModifier
            ) {
                Text(
                    text = seasonChipLabel(season = season),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (index == selectedTabIndex) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Medium
                        }
                    ),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
internal fun EpisodeCard(
    episode: TvEpisode,
    fallbackDescription: String,
    onEpisodeSelected: (TvEpisode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { onEpisodeSelected(episode) },
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(CloudStreamCardShape),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(
                    width = CloudStreamBorderWidth,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                shape = CloudStreamCardShape
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(episode.posterUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(248.dp)
                    .aspectRatio(16f / 9f)
                    .clip(CloudStreamCardShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = episodeTitle(episode = episode),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val metadataLine = episodeMetadataLine(episode = episode)
                if (metadataLine != null) {
                    Text(
                        text = metadataLine,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val description = episode.description.ifBlank { fallbackDescription }
                Text(
                    text = description.ifBlank { stringResource(R.string.tv_no_episode_description) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun NoEpisodesRow(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.no_episodes_found),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
internal fun AdditionalInfoSection(
    tvSeriesDetails: MovieDetails,
    modifier: Modifier = Modifier
) {
    val childPadding = rememberChildPadding()
    val infoItems = listOf(
        stringResource(R.string.status) to tvSeriesDetails.status,
        stringResource(R.string.tv_original_language) to tvSeriesDetails.originalLanguage,
        stringResource(R.string.tv_budget) to tvSeriesDetails.budget,
        stringResource(R.string.tv_revenue) to tvSeriesDetails.revenue
    ).filter { (_, value) -> value.isNotBlank() }

    if (infoItems.isEmpty()) return

    Column(modifier = modifier) {
        SectionHeaderText(
            text = stringResource(R.string.tv_additional_information),
            modifier = Modifier.padding(start = childPadding.start)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = childPadding.start),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            infoItems.forEach { (title, value) ->
                TitleValueText(
                    modifier = Modifier.width(220.dp),
                    title = title,
                    value = value
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold
        ),
        modifier = modifier.padding(top = 12.dp, bottom = 8.dp)
    )
}

@Composable
private fun seasonChipLabel(season: TvSeason): String {
    val seasonLabel = stringResource(R.string.season)
    val seasonNumber = season.displaySeasonNumber ?: season.seasonNumber
    val customTitle = season.title.orEmpty().trim()

    return when {
        seasonNumber != null && customTitle.isNotBlank() -> "$seasonLabel $seasonNumber $customTitle"
        seasonNumber != null -> "$seasonLabel $seasonNumber"
        customTitle.isNotBlank() -> customTitle
        else -> seasonLabel
    }
}

@Composable
private fun episodeTitle(episode: TvEpisode): String {
    val fallbackTitle = stringResource(R.string.tv_episode_untitled)
    val title = episode.title.ifBlank { fallbackTitle }
    return title
}

@Composable
private fun episodeMetadataLine(episode: TvEpisode): String? {
    val texts = mutableListOf<String>()

    episode.episodeNumber?.let { episodeNumber ->
        texts.add(
            "${stringResource(R.string.episode)} ${episodeNumber.toString().padStart(2, '0')}"
        )
    }

    episode.durationMinutes?.let { durationMinutes ->
        texts.add(formatEpisodeDuration(durationMinutes))
    }

    episode.ratingText?.let { rating ->
        texts.add(stringResource(R.string.tv_episode_rating_format, rating))
    }

    episode.releaseDateMillis?.let { releaseDateMillis ->
        texts.add(
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(releaseDateMillis))
        )
    }

    if (texts.isEmpty()) return null
    return texts.joinToString(separator = " \u2022 ")
}

@Composable
private fun formatEpisodeDuration(durationMinutes: Int): String {
    if (durationMinutes < 60) {
        return stringResource(R.string.duration_format, durationMinutes)
    }

    val hours = durationMinutes / 60
    val minutes = durationMinutes % 60
    return stringResource(R.string.tv_episode_duration_hour_min_format, hours, minutes)
}
