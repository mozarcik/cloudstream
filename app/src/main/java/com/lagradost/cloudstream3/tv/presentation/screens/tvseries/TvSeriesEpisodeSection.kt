package com.lagradost.cloudstream3.tv.presentation.screens.tvseries

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason
import com.lagradost.cloudstream3.tv.icons.CustomDownload
import com.lagradost.cloudstream3.tv.presentation.common.ActionIconSpec
import com.lagradost.cloudstream3.tv.presentation.common.ActionIconsPill
import com.lagradost.cloudstream3.tv.presentation.common.ActionIconsPillDefaults
import com.lagradost.cloudstream3.tv.presentation.focus.rememberFocusRequesters
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsDownloadActionState
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsQuickAction
import com.lagradost.cloudstream3.tv.presentation.screens.movies.TitleValueText
import com.lagradost.cloudstream3.tv.presentation.screens.movies.rememberChildPadding
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamBorderWidth
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

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

private val CompactActionMinHeight = 40.dp
private val CompactPlayButtonWithIconPadding = PaddingValues(start = 10.dp, top = 6.dp, end = 12.dp, bottom = 6.dp)
private val CompactPlayIconSize = 18.dp
private val CompactPlayIconSpacing = 6.dp

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
    focusRequesters: List<FocusRequester>,
    onSeasonSelected: (TvSeason) -> Unit,
    modifier: Modifier = Modifier
) {
    if (seasons.isEmpty()) return

    val (tabRowFocusRequester, fallbackTabFocusRequester) = remember { FocusRequester.createRefs() }
    val internalTabFocusRequesters = rememberFocusRequesters(count = seasons.size)
    val tabFocusRequesters = if (focusRequesters.size == seasons.size) {
        focusRequesters
    } else {
        internalTabFocusRequesters
    }
    val selectedTabIndex = seasons.indexOfFirst { season -> season.id == selectedSeasonId }
        .takeIf { it >= 0 } ?: 0
    val selectedTabFocusRequester = tabFocusRequesters.getOrElse(selectedTabIndex) {
        fallbackTabFocusRequester
    }

    TabRow(
        modifier = modifier
            .focusRequester(tabRowFocusRequester)
            .focusRestorer(selectedTabFocusRequester),
        selectedTabIndex = selectedTabIndex,
        separator = { Spacer(modifier = Modifier.width(12.dp)) },
    ) {
        seasons.forEachIndexed { index, season ->
            Tab(
                selected = index == selectedTabIndex,
                onFocus = { onSeasonSelected(season) },
                onClick = { onSeasonSelected(season) },
                modifier = Modifier.focusRequester(
                    tabFocusRequesters.getOrElse(index) { fallbackTabFocusRequester }
                ),
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
    onEpisodeQuickActionClick: (TvEpisode, MovieDetailsQuickAction) -> Unit = { _, _ -> },
    isWatched: Boolean = false,
    downloadActionState: MovieDetailsDownloadActionState = MovieDetailsDownloadActionState.Idle,
    modifier: Modifier = Modifier
) {
    val actionFocusRequester = remember { FocusRequester() }

    var hasCardOrChildFocus by remember { mutableStateOf(false) }

    Surface(
        onClick = { onEpisodeSelected(episode) },
        modifier = modifier
            .focusProperties { right = actionFocusRequester }
            .onFocusChanged { focusState ->
                hasCardOrChildFocus = focusState.hasFocus
            },
        shape = ClickableSurfaceDefaults.shape(CloudStreamCardShape),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(
                    width = CloudStreamBorderWidth,
                    color = MaterialTheme.colorScheme.primary
                ),
                shape = CloudStreamCardShape
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
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

                AnimatedVisibility(visible = hasCardOrChildFocus) {
                    DetailsActionsRow(
                        onPlayClick = { onEpisodeSelected(episode) },
                        playButtonLabel = stringResource(R.string.home_play),
                        onPlayLongClick = { },
                        isWatched = isWatched,
                        downloadActionState = downloadActionState,
                        onQuickActionClick = { action ->
                            onEpisodeQuickActionClick(episode, action)
                        },
                        playButtonModifier = Modifier.focusRequester(actionFocusRequester)
                    )
                }
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

@Composable
private fun DetailsActionsRow(
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    playButtonModifier: Modifier = Modifier,
    playButtonLabel: String? = null,
    onPlayLongClick: (() -> Unit)? = null,
    isWatched: Boolean = false,
    downloadActionState: MovieDetailsDownloadActionState = MovieDetailsDownloadActionState.Idle,
    onQuickActionClick: (MovieDetailsQuickAction) -> Unit = {},
) {
    val watchedLabel = if (isWatched) {
        stringResource(R.string.action_remove_from_watched)
    } else {
        stringResource(R.string.action_mark_as_watched)
    }
    val watchedUpToLabel = if (isWatched) {
        stringResource(R.string.action_remove_mark_watched_up_to_this_episode)
    } else {
        stringResource(R.string.action_mark_watched_up_to_this_episode)
    }
    val watchedIcon = if (isWatched) Icons.Default.Close else Icons.Default.Done
    val watchedUpToIcon = if (isWatched) Icons.Default.RemoveDone else Icons.Default.DoneAll
    val watchedAction = if (isWatched) {
        MovieDetailsQuickAction.RemoveFromWatched
    } else {
        MovieDetailsQuickAction.MarkAsWatched
    }
    val watchedUpToAction = if (isWatched) {
        MovieDetailsQuickAction.RemoveWatchedUpToThisEpisode
    } else {
        MovieDetailsQuickAction.MarkWatchedUpToThisEpisode
    }
    val downloadIdleLabel = stringResource(R.string.download)
    val downloadingLabel = stringResource(R.string.downloading)
    val downloadedLabel = stringResource(R.string.downloaded)
    val downloadProgressFraction = when (downloadActionState) {
        MovieDetailsDownloadActionState.Idle -> 0f
        is MovieDetailsDownloadActionState.Downloading ->
            downloadActionState.progress.coerceIn(0f, 1f)

        MovieDetailsDownloadActionState.Downloaded -> 1f
        MovieDetailsDownloadActionState.Failed -> 0f
    }
    val downloadLabel = when (downloadActionState) {
        MovieDetailsDownloadActionState.Idle -> downloadIdleLabel
        is MovieDetailsDownloadActionState.Downloading ->
            "$downloadingLabel (${(downloadProgressFraction * 100f).roundToInt()}%)"

        MovieDetailsDownloadActionState.Downloaded -> downloadedLabel
        MovieDetailsDownloadActionState.Failed -> stringResource(R.string.download_failed)
    }
    val downloadIcon = when (downloadActionState) {
        MovieDetailsDownloadActionState.Idle -> Icons.Filled.CustomDownload
        is MovieDetailsDownloadActionState.Downloading -> Icons.Default.Downloading
        MovieDetailsDownloadActionState.Downloaded -> Icons.Default.DownloadDone
        MovieDetailsDownloadActionState.Failed -> Icons.Outlined.ErrorOutline
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PrimaryPlayButton(
            goToMoviePlayer = onPlayClick,
            playButtonLabel = playButtonLabel,
            onLongClick = onPlayLongClick,
            modifier = playButtonModifier,
        )

        val actions =
            listOf(
                ActionIconSpec(
                    icon = watchedIcon,
                    label = watchedLabel,
                    testTag = "action_watched",
                    action = watchedAction
                ),
                ActionIconSpec(
                    icon = watchedUpToIcon,
                    label = watchedUpToLabel,
                    testTag = "action_watched_all",
                    action = watchedUpToAction
                ),
                ActionIconSpec(
                    icon = downloadIcon,
                    label = downloadLabel,
                    testTag = "action_download",
                    action = MovieDetailsQuickAction.Download,
                    progressFraction = downloadProgressFraction
                ),
            )

        ActionIconsPill(
            actions = actions,
            style = ActionIconsPillDefaults.compact(),
            onActionClick = onQuickActionClick,
        )
    }
}


@Composable
private fun PrimaryPlayButton(
    goToMoviePlayer: () -> Unit,
    playButtonLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val label = playButtonLabel ?: stringResource(R.string.movies_singular)

    Button(
        onClick = goToMoviePlayer,
        onLongClick = onLongClick,
        contentPadding = CompactPlayButtonWithIconPadding,
        colors = ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            focusedContentColor = MaterialTheme.colorScheme.onPrimary,
            pressedContainerColor = MaterialTheme.colorScheme.primary,
            pressedContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = modifier.heightIn(min = CompactActionMinHeight)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(CompactPlayIconSize)
        )
        Spacer(Modifier.size(CompactPlayIconSpacing))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
