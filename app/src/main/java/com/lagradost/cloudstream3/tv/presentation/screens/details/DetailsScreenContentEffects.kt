package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.LazyListState
import android.content.Context
import androidx.compose.runtime.rememberCoroutineScope
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionEffect
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionEvent
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionStateHolder
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatActionOutcome
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
internal fun DetailsScreenContentEffects(
    detailsId: String,
    detailsCurrentSeason: Int?,
    seasons: List<com.lagradost.cloudstream3.tv.data.entities.TvSeason>,
    selectedSeasonId: String?,
    selectedEpisodes: List<TvEpisode>,
    context: Context,
    listState: LazyListState,
    defaultActionsCompat: MovieDetailsEpisodeActionsCompat,
    downloadButtonViewModel: DetailsDownloadButtonViewModel,
    episodesStateHolder: DetailsEpisodesStateHolder,
    downloadMirrorStateHolder: DownloadMirrorSelectionStateHolder,
    onResolveEpisodeSeason: (TvEpisode) -> Int?,
    onHandleDownloadActionOutcome: (MovieDetailsCompatActionOutcome) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(seasons, detailsCurrentSeason) {
        episodesStateHolder.syncSelectedSeason(
            seasons = seasons,
            currentSeason = detailsCurrentSeason,
        )
    }

    LaunchedEffect(detailsId, selectedSeasonId, selectedEpisodes) {
        val hasUnseasonedEpisodes = selectedEpisodes.any { episode -> episode.seasonNumber == null }
        val hasSpecialSeason = seasons.any { season ->
            season.seasonNumber == 0 || season.displaySeasonNumber == 0
        }
        if (!hasUnseasonedEpisodes && !hasSpecialSeason) {
            return@LaunchedEffect
        }

        val episodePreview = selectedEpisodes
            .take(12)
            .joinToString(separator = " | ") { episode ->
                "${episode.seasonNumber ?: "null"}:${episode.episodeNumber ?: "null"}:${episode.title.take(24)}"
            }
        Log.d(
            DetailsDebugTag,
            "selected season detailsId=$detailsId selectedSeasonId=$selectedSeasonId episodes=${selectedEpisodes.size} preview=[$episodePreview]"
        )
    }

    LaunchedEffect(listState, selectedEpisodes, selectedSeasonId, defaultActionsCompat) {
        androidx.compose.runtime.snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.key } }
            .collect { visibleItemKeys ->
                episodesStateHolder.hydrateVisibleWindow(
                    episodes = selectedEpisodes,
                    visibleItemKeys = visibleItemKeys,
                    resolveEpisodeSeason = onResolveEpisodeSeason,
                )
            }
    }

    LaunchedEffect(defaultActionsCompat, context, detailsId) {
        downloadButtonViewModel.setDefaultCompat(defaultActionsCompat)
        delay(DetailsInitialDownloadSnapshotDelayMs)
        downloadButtonViewModel.refreshDefaultSnapshot(
            context = context,
            reason = "post_primary_ready"
        )
    }

    LaunchedEffect(downloadMirrorStateHolder) {
        downloadMirrorStateHolder.effects.collect { effect ->
            when (effect) {
                is DownloadMirrorSelectionEffect.LoadingFinished -> {
                    onHandleDownloadActionOutcome(effect.outcome)
                }
            }
        }
    }

    DisposableEffect(selectedSeasonId, coroutineScope) {
        val statusObserver: (Pair<Int, VideoDownloadManager.DownloadType>) -> Unit = { (episodeId, status) ->
            coroutineScope.launch {
                episodesStateHolder.onDownloadStatusChanged(episodeId = episodeId, status = status)
            }
        }
        val progressObserver: (Triple<Int, Long, Long>) -> Unit = { (episodeId, downloadedBytes, totalBytes) ->
            coroutineScope.launch {
                episodesStateHolder.onDownloadProgressChanged(
                    episodeId = episodeId,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
                )
            }
        }

        VideoDownloadManager.downloadStatusEvent += statusObserver
        VideoDownloadManager.downloadProgressEvent += progressObserver

        onDispose {
            VideoDownloadManager.downloadStatusEvent -= statusObserver
            VideoDownloadManager.downloadProgressEvent -= progressObserver
        }
    }

    DisposableEffect(downloadMirrorStateHolder) {
        onDispose {
            downloadMirrorStateHolder.onEvent(DownloadMirrorSelectionEvent.Close)
            downloadButtonViewModel.clearPendingCompat()
        }
    }
}
