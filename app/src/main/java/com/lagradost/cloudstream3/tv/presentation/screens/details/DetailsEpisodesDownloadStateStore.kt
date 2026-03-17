package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.utils.VideoDownloadManager

internal data class DetailsEpisodeLocator(
    val seasonNumber: Int?,
    val episodeNumber: Int?,
)

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

internal fun updateDetailsEpisodeDownloadStateByKey(
    currentStates: Map<String, DetailsDownloadButtonUiState>,
    episodeKey: String,
    fallbackEpisodeId: Int? = null,
    transform: (DetailsDownloadButtonUiState) -> DetailsDownloadButtonUiState,
): Map<String, DetailsDownloadButtonUiState> {
    val currentState = currentStates[episodeKey]
    val baseState = when {
        currentState == null -> DetailsDownloadButtonUiState(episodeId = fallbackEpisodeId)
        currentState.episodeId == null && fallbackEpisodeId != null ->
            currentState.copy(episodeId = fallbackEpisodeId)
        else -> currentState
    }
    return currentStates + (episodeKey to transform(baseState))
}

internal fun mergeHydratedEpisodeDownloadStates(
    currentStates: Map<String, DetailsDownloadButtonUiState>,
    loadedStates: Map<String, DetailsDownloadButtonUiState>,
): Map<String, DetailsDownloadButtonUiState> {
    return loadedStates.entries.fold(currentStates) { states, (episodeKey, loadedState) ->
        val currentState = states[episodeKey]
        val nextState = if (currentState != null && shouldKeepOptimisticPendingEpisodeState(currentState, loadedState)) {
            currentState
        } else {
            loadedState
        }
        states + (episodeKey to nextState)
    }
}

internal fun shouldKeepOptimisticPendingEpisodeState(
    currentState: DetailsDownloadButtonUiState,
    loadedState: DetailsDownloadButtonUiState,
): Boolean {
    return currentState.status == VideoDownloadManager.DownloadType.IsPending &&
        loadedState.status == null &&
        loadedState.progressFraction <= 0f &&
        loadedState.episodeId == null
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
