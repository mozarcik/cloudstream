package com.lagradost.cloudstream3.tv.compat

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class DownloadMirrorSelectionUiState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val loadedSourcesCount: Int = 0,
    val selectionRequest: MovieDetailsCompatSelectionRequest? = null,
    val isLoadingUiSkipped: Boolean = false,
)

sealed interface DownloadMirrorSelectionEvent {
    data class Open(
        val compat: MovieDetailsEpisodeActionsCompat,
        val context: Context?,
        val preferredSeason: Int? = null,
        val preferredEpisode: Int? = null,
    ) : DownloadMirrorSelectionEvent

    data object SkipLoadingUi : DownloadMirrorSelectionEvent
    data object Close : DownloadMirrorSelectionEvent
}

sealed interface DownloadMirrorSelectionEffect {
    data class LoadingFinished(
        val outcome: MovieDetailsCompatActionOutcome,
    ) : DownloadMirrorSelectionEffect
}

class DownloadMirrorSelectionStateHolder(
    private val scope: CoroutineScope,
) {
    private companion object {
        const val DebugTag = "DownloadMirrorHolder"
    }

    private val _uiState = MutableStateFlow(DownloadMirrorSelectionUiState())
    val uiState: StateFlow<DownloadMirrorSelectionUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<DownloadMirrorSelectionEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<DownloadMirrorSelectionEffect> = _effects.asSharedFlow()

    private var loadingJob: Job? = null
    private var activeRequestVersion: Int = 0

    fun onEvent(event: DownloadMirrorSelectionEvent) {
        when (event) {
            is DownloadMirrorSelectionEvent.Open -> openPanel(
                compat = event.compat,
                context = event.context,
                preferredSeason = event.preferredSeason,
                preferredEpisode = event.preferredEpisode,
            )

            DownloadMirrorSelectionEvent.SkipLoadingUi -> skipLoadingUi()
            DownloadMirrorSelectionEvent.Close -> closePanel()
        }
    }

    fun updateSelectionRequest(request: MovieDetailsCompatSelectionRequest?) {
        _uiState.update { current ->
            current.copy(selectionRequest = request)
        }
        logState("selection_request_updated")
    }

    private fun openPanel(
        compat: MovieDetailsEpisodeActionsCompat,
        context: Context?,
        preferredSeason: Int?,
        preferredEpisode: Int?,
    ) {
        val requestVersion = activeRequestVersion + 1
        activeRequestVersion = requestVersion
        loadingJob?.cancel()
        _uiState.value = DownloadMirrorSelectionUiState(
            isVisible = true,
            isLoading = true,
        )
        Log.d(
            DebugTag,
            "open_panel requestVersion=$requestVersion season=${preferredSeason ?: "null"} episode=${preferredEpisode ?: "null"}"
        )
        logState("open_panel_initialized")

        loadingJob = scope.launch(Dispatchers.IO) {
            try {
                val outcome = compat.requestDownloadMirrorSelection(
                    context = context,
                    preferredSeason = preferredSeason,
                    preferredEpisode = preferredEpisode,
                    onSourcesProgress = { loadedSources ->
                        if (activeRequestVersion != requestVersion) return@requestDownloadMirrorSelection
                        _uiState.update { current ->
                            current.copy(loadedSourcesCount = loadedSources)
                        }
                        logState("sources_progress($loadedSources)")
                    },
                    onSelectionUpdated = { request ->
                        if (activeRequestVersion != requestVersion) return@requestDownloadMirrorSelection
                        _uiState.update { current ->
                            current.copy(selectionRequest = request)
                        }
                        Log.d(
                            DebugTag,
                            "selection_updated requestVersion=$requestVersion options=${request.options.size}"
                        )
                        logState("selection_updated")
                    },
                    shouldCancelLoading = {
                        activeRequestVersion != requestVersion
                    }
                )
                if (activeRequestVersion != requestVersion) return@launch

                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        isLoadingUiSkipped = false,
                        selectionRequest = when (outcome) {
                            is MovieDetailsCompatActionOutcome.OpenSelection -> outcome.request
                            MovieDetailsCompatActionOutcome.Completed -> null
                        },
                    )
                }
                Log.d(
                    DebugTag,
                    "loading_finished requestVersion=$requestVersion outcome=${outcome::class.java.simpleName}"
                )
                logState("loading_finished")
                _effects.tryEmit(
                    DownloadMirrorSelectionEffect.LoadingFinished(outcome)
                )
            } catch (_: CancellationException) {
                Log.d(DebugTag, "download mirror loading cancelled")
            } finally {
                if (activeRequestVersion == requestVersion) {
                    loadingJob = null
                }
            }
        }
    }

    private fun skipLoadingUi() {
        _uiState.update { current ->
            if (!current.isLoading || current.loadedSourcesCount <= 0) {
                current
            } else {
                current.copy(isLoadingUiSkipped = true)
            }
        }
        logState("skip_loading_ui")
    }

    private fun closePanel() {
        activeRequestVersion += 1
        loadingJob?.cancel()
        loadingJob = null
        logState("close_panel_requested")
        _uiState.value = DownloadMirrorSelectionUiState()
        logState("close_panel_cleared")
    }

    private fun logState(event: String) {
        val current = _uiState.value
        Log.d(
            DebugTag,
            "$event visible=${current.isVisible} loading=${current.isLoading} skipped=${current.isLoadingUiSkipped} " +
                "loadedSources=${current.loadedSourcesCount} options=${current.selectionRequest?.options?.size ?: 0}"
        )
    }
}
