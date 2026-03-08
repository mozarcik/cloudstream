package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason
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
    selectedSeasonId: String?,
    selectedEpisodes: List<TvEpisode>,
    detailsDescription: String,
    childPadding: Padding,
    seasonTabFocusRequesters: List<FocusRequester>,
    onSeasonSelected: (TvSeason) -> Unit,
    resolveEpisodeDownloadState: (TvEpisode) -> DetailsDownloadButtonUiState,
    resolveEpisodeWatchedState: (TvEpisode) -> Boolean,
    toDownloadUiState: (DetailsDownloadButtonUiState) -> MovieDetailsDownloadActionState,
    onEpisodeSelected: (TvEpisode) -> Unit,
    onEpisodeQuickActionClick: (TvEpisode, MovieDetailsQuickAction) -> Unit,
) {
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
                selectedSeasonId = selectedSeasonId,
                focusRequesters = seasonTabFocusRequesters,
                onSeasonSelected = onSeasonSelected,
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
        EpisodeCard(
            episode = episode,
            fallbackDescription = detailsDescription,
            onEpisodeSelected = onEpisodeSelected,
            isWatched = resolveEpisodeWatchedState(episode),
            downloadActionState = toDownloadUiState(episodeDownloadState),
            onEpisodeQuickActionClick = onEpisodeQuickActionClick,
            modifier = Modifier
                .padding(start = childPadding.start, end = childPadding.end)
                .padding(bottom = 12.dp)
        )
    }
}
