package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason
import com.lagradost.cloudstream3.tv.presentation.focus.rememberFocusRequesters
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsDownloadActionState
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsQuickAction
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.EpisodeCard
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.EpisodesSectionHeader
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.NoEpisodesRow
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.SeasonSelectorRow
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.SeasonsSectionHeader
import com.lagradost.cloudstream3.tv.presentation.utils.Padding

internal fun LazyListScope.detailsSeriesItems(
    mode: DetailsScreenMode,
    seasons: List<TvSeason>,
    seasonRequesters: List<FocusRequester>,
    selectedSeasonId: String?,
    selectedEpisodes: List<TvEpisode>,
    detailsDescription: String,
    childPadding: Padding,
    upFocusRequester: FocusRequester,
    onSeasonSelected: (TvSeason) -> Unit,
    resolveEpisodeDownloadState: (TvEpisode) -> DetailsDownloadButtonUiState,
    resolveEpisodeWatchedState: (TvEpisode) -> Boolean,
    toDownloadUiState: (DetailsDownloadButtonUiState) -> MovieDetailsDownloadActionState,
    onEpisodeSelected: (TvEpisode) -> Unit,
    onEpisodeQuickActionClick: (TvEpisode, MovieDetailsQuickAction) -> Unit,
) {
    val selectedSeasonIndex = seasons.indexOfFirst { season -> season.id == selectedSeasonId }
        .takeIf { it >= 0 } ?: 0

    if (mode == DetailsScreenMode.Media && seasons.isNotEmpty()) {
        item {
            SeasonsSectionHeader(
                modifier = Modifier.padding(start = childPadding.start)
            )
        }
    }

    if (seasons.isNotEmpty()) {
        item {
            SeasonSelectorRow(
                seasons = seasons,
                seasonRequesters = seasonRequesters,
                selectedSeasonId = selectedSeasonId,
                onSeasonSelected = onSeasonSelected,
                upFocusRequester = upFocusRequester,
                modifier = Modifier
                    .padding(start = childPadding.start, end = childPadding.end)
                    .padding(bottom = 8.dp, top = 8.dp)
            )
        }
    }

    if (mode == DetailsScreenMode.Media) {
        item {
            EpisodesSectionHeader(
                modifier = Modifier.padding(start = childPadding.start)
            )
        }
    }

    if (selectedEpisodes.isEmpty()) {
        item {
            NoEpisodesRow(
                modifier = Modifier
                    .padding(start = childPadding.start, end = childPadding.end)
                    .padding(bottom = 8.dp)
            )
        }
        return
    }

    items(
        items = selectedEpisodes,
        key = { episode -> episode.id }
    ) { episode ->
        val episodeDownloadState = resolveEpisodeDownloadState(episode)
        val episodeUpFocusRequester = when {
            seasons.isEmpty() && selectedEpisodes.first() == episode -> upFocusRequester
            selectedEpisodes.first() == episode -> seasonRequesters[selectedSeasonIndex]
            else -> FocusRequester.Default
        }

        EpisodeCard(
            episode = episode,
            fallbackDescription = detailsDescription,
            onEpisodeSelected = onEpisodeSelected,
            isWatched = resolveEpisodeWatchedState(episode),
            downloadActionState = toDownloadUiState(episodeDownloadState),
            onEpisodeQuickActionClick = onEpisodeQuickActionClick,
            modifier = Modifier
                .focusProperties{up = episodeUpFocusRequester}
                .padding(start = childPadding.start, end = childPadding.end)
                .padding(bottom = 12.dp)
        )
    }
}