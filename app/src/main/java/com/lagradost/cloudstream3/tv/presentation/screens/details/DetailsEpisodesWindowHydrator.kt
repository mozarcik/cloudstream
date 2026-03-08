package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.content.Context
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.util.tvTraceAsyncSection
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class DetailsEpisodeWindowState(
    val downloadState: DetailsDownloadButtonUiState,
    val isWatched: Boolean,
)

internal class DetailsEpisodesWindowHydrator(
    private val context: Context,
    private val actionsCompat: MovieDetailsEpisodeActionsCompat,
) {
    suspend fun load(
        requestedEpisodes: List<TvEpisode>,
        resolveEpisodeSeason: (TvEpisode) -> Int?,
        traceCookie: Int,
    ): Map<String, DetailsEpisodeWindowState> {
        return tvTraceAsyncSection(
            sectionName = "details_episode_batch_hydration",
            cookie = traceCookie,
        ) {
            withContext(Dispatchers.IO) {
                requestedEpisodes.associate { episode ->
                    val episodeSeason = resolveEpisodeSeason(episode)
                    val snapshot = actionsCompat.getDownloadSnapshotForEpisode(
                        context = context,
                        preferredSeason = episodeSeason,
                        preferredEpisode = episode.episodeNumber,
                    )
                    val normalizedStatus = snapshot?.let(::normalizeDownloadStatus)
                    val progressFraction = when {
                        snapshot == null -> 0f
                        normalizedStatus == VideoDownloadManager.DownloadType.IsDone -> 1f
                        else -> calculateProgressFraction(snapshot.downloadedBytes, snapshot.totalBytes)
                    }
                    val isWatched = actionsCompat.isEpisodeWatched(
                        preferredSeason = episodeSeason,
                        preferredEpisode = episode.episodeNumber,
                    ) == true

                    episode.id to DetailsEpisodeWindowState(
                        downloadState = DetailsDownloadButtonUiState(
                            episodeId = snapshot?.episodeId,
                            status = normalizedStatus,
                            progressFraction = progressFraction,
                        ),
                        isWatched = isWatched,
                    )
                }
            }
        }
    }
}
