package com.lagradost.cloudstream3.tv.compat

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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
                context = event.context
            )

            DownloadMirrorSelectionEvent.SkipLoadingUi -> skipLoadingUi()
            DownloadMirrorSelectionEvent.Close -> closePanel()
        }
    }

    fun updateSelectionRequest(request: MovieDetailsCompatSelectionRequest?) {
        _uiState.update { current ->
            current.copy(selectionRequest = request)
        }
    }

    private fun openPanel(
        compat: MovieDetailsEpisodeActionsCompat,
        context: Context?,
    ) {
        val requestVersion = activeRequestVersion + 1
        activeRequestVersion = requestVersion
        loadingJob?.cancel()
        _uiState.value = DownloadMirrorSelectionUiState(
            isVisible = true,
            isLoading = true,
        )

        loadingJob = scope.launch {
            try {
                val outcome = compat.requestDownloadMirrorSelection(
                    context = context,
                    onSourcesProgress = { loadedSources ->
                        if (activeRequestVersion != requestVersion) return@requestDownloadMirrorSelection
                        _uiState.update { current ->
                            current.copy(loadedSourcesCount = loadedSources)
                        }
                    },
                    onSelectionUpdated = { request ->
                        if (activeRequestVersion != requestVersion) return@requestDownloadMirrorSelection
                        _uiState.update { current ->
                            current.copy(selectionRequest = request)
                        }
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
                    )
                }
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
    }

    private fun closePanel() {
        activeRequestVersion += 1
        loadingJob?.cancel()
        loadingJob = null
        _uiState.value = DownloadMirrorSelectionUiState()
    }
}
