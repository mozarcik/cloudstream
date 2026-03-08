package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.utils.VideoDownloadManager

internal fun updateDetailsEpisodeDownloadStatesByEpisodeId(
    currentStates: Map<String, DetailsDownloadButtonUiState>,
    episodeId: Int,
    transform: (DetailsDownloadButtonUiState) -> DetailsDownloadButtonUiState,
): Map<String, DetailsDownloadButtonUiState> {
    return currentStates.mapValues { (_, state) ->
        if (state.episodeId == episodeId) {
            transform(state)
        } else {
            state
        }
    }
}

internal fun DetailsDownloadButtonUiState.withStatus(status: VideoDownloadManager.DownloadType): DetailsDownloadButtonUiState {
    val nextProgress = when (status) {
        VideoDownloadManager.DownloadType.IsDone -> 1f
        VideoDownloadManager.DownloadType.IsStopped,
        VideoDownloadManager.DownloadType.IsFailed -> 0f
        else -> progressFraction
    }
    return copy(
        status = status,
        progressFraction = nextProgress,
    )
}

internal fun DetailsDownloadButtonUiState.withProgress(progress: Float): DetailsDownloadButtonUiState {
    val nextProgress = when {
        status == VideoDownloadManager.DownloadType.IsDone -> 1f
        progress > 0f -> progress
        else -> progressFraction
    }
    return copy(progressFraction = nextProgress)
}
