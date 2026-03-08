package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.resolveInitialSeasonId
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val DetailsEpisodesDebugTag = "TvDetailsEpisodes"

@Stable
internal class DetailsEpisodesStateHolder(
    private val scope: CoroutineScope,
    private val hydrator: DetailsEpisodesWindowHydrator,
    initialSeasonId: String?,
) {
    var selectedSeasonId by mutableStateOf(initialSeasonId)
        private set
    var hasUserSelectedSeason by mutableStateOf(false)
        private set
    private var generation by mutableIntStateOf(0)
    private var episodeDownloadStates by mutableStateOf<Map<String, DetailsDownloadButtonUiState>>(emptyMap())
    private var episodeWatchedStates by mutableStateOf<Map<String, Boolean>>(emptyMap())
    private var loadingEpisodeStateIds by mutableStateOf<Set<String>>(emptySet())

    fun syncSelectedSeason(
        seasons: List<TvSeason>,
        currentSeason: Int?,
    ) {
        val resolvedSeasonId = resolveInitialSeasonId(seasons, currentSeason)
        if (seasons.none { season -> season.id == selectedSeasonId }) {
            selectedSeasonId = resolvedSeasonId
            clearEpisodeStates()
            logSeasonState(reason = "sync_missing_selection", seasons = seasons, currentSeason = currentSeason)
            return
        }
        if (!hasUserSelectedSeason && resolvedSeasonId != null && resolvedSeasonId != selectedSeasonId) {
            selectedSeasonId = resolvedSeasonId
            clearEpisodeStates()
            logSeasonState(reason = "sync_auto_select", seasons = seasons, currentSeason = currentSeason)
        }
    }

    fun onSeasonSelected(seasonId: String) {
        if (selectedSeasonId == seasonId && hasUserSelectedSeason) return
        hasUserSelectedSeason = true
        selectedSeasonId = seasonId
        clearEpisodeStates()
        Log.d(DetailsEpisodesDebugTag, "user selected seasonId=$seasonId")
    }

    fun resolveEpisodeDownloadState(episode: TvEpisode): DetailsDownloadButtonUiState =
        episodeDownloadStates[episode.id] ?: DetailsDownloadButtonUiState()

    fun resolveEpisodeWatchedState(episode: TvEpisode): Boolean = episodeWatchedStates[episode.id] == true

    fun hydrateVisibleWindow(episodes: List<TvEpisode>, visibleItemKeys: List<Any>, resolveEpisodeSeason: (TvEpisode) -> Int?) {
        if (episodes.isEmpty()) {
            clearEpisodeStates()
            return
        }

        val requestedEpisodes = resolveRequestedEpisodes(episodes, visibleItemKeys)
        if (requestedEpisodes.isEmpty()) {
            return
        }

        val pendingIds = requestedEpisodes.map(TvEpisode::id).toSet()
        val requestGeneration = generation
        loadingEpisodeStateIds = loadingEpisodeStateIds + pendingIds

        scope.launch {
            try {
                val loadedStates = hydrator.load(
                    requestedEpisodes = requestedEpisodes,
                    resolveEpisodeSeason = resolveEpisodeSeason,
                    traceCookie = pendingIds.hashCode(),
                )
                if (requestGeneration != generation) return@launch

                episodeDownloadStates = episodeDownloadStates + loadedStates.mapValues { (_, state) ->
                    state.downloadState
                }
                episodeWatchedStates = episodeWatchedStates + loadedStates.mapValues { (_, state) ->
                    state.isWatched
                }
            } catch (error: Throwable) {
                Log.e(
                    DetailsEpisodesDebugTag,
                    "visible episode hydration failed pending=${pendingIds.size}",
                    error,
                )
            } finally {
                if (requestGeneration == generation) {
                    loadingEpisodeStateIds = loadingEpisodeStateIds - pendingIds
                }
            }
        }
    }

    fun invalidateEpisodeStates() = clearEpisodeStates()

    fun onDownloadStatusChanged(episodeId: Int, status: VideoDownloadManager.DownloadType) {
        episodeDownloadStates = updateDetailsEpisodeDownloadStatesByEpisodeId(
            currentStates = episodeDownloadStates,
            episodeId = episodeId,
        ) { current ->
            current.withStatus(status)
        }
    }

    fun onDownloadProgressChanged(episodeId: Int, downloadedBytes: Long, totalBytes: Long) {
        val progress = calculateProgressFraction(downloadedBytes, totalBytes)
        episodeDownloadStates = updateDetailsEpisodeDownloadStatesByEpisodeId(
            currentStates = episodeDownloadStates,
            episodeId = episodeId,
        ) { current ->
            current.withProgress(progress)
        }
    }

    private fun resolveRequestedEpisodes(episodes: List<TvEpisode>, visibleItemKeys: List<Any>): List<TvEpisode> =
        filterRequestedDetailEpisodes(
            episodes = episodes,
            visibleItemKeys = visibleItemKeys,
            episodeDownloadStates = episodeDownloadStates,
            episodeWatchedStates = episodeWatchedStates,
            loadingEpisodeStateIds = loadingEpisodeStateIds,
        )

    private fun clearEpisodeStates() {
        generation += 1
        episodeDownloadStates = emptyMap()
        episodeWatchedStates = emptyMap()
        loadingEpisodeStateIds = emptySet()
    }

    private fun logSeasonState(
        reason: String,
        seasons: List<TvSeason>,
        currentSeason: Int?,
    ) {
        val hasUnseasonedBucket = seasons.any { season ->
            season.seasonNumber == 0 || season.displaySeasonNumber == 0 || season.episodes.any { episode -> episode.seasonNumber == null }
        }
        if (!hasUnseasonedBucket) return

        val seasonSummary = seasons.joinToString(separator = ",") { season ->
            "id=${season.id} season=${season.seasonNumber ?: "null"} display=${season.displaySeasonNumber ?: "null"} count=${season.episodes.size}"
        }
        Log.d(
            DetailsEpisodesDebugTag,
            "season sync reason=$reason currentSeason=$currentSeason selectedSeasonId=$selectedSeasonId seasons=[$seasonSummary]"
        )
    }
}
