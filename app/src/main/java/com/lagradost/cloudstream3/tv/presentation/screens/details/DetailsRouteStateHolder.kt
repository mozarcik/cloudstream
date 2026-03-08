package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DetailsLoadingPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class DetailsRouteStateHolder(
    private val loadingPreview: DetailsLoadingPreview,
) {

    private val _baseUiState = MutableStateFlow<DetailsScreenUiState>(
        DetailsScreenUiState.Loading(preview = loadingPreview)
    )
    val baseUiState: StateFlow<DetailsScreenUiState> = _baseUiState.asStateFlow()

    private var currentActionsCompat: MovieDetailsEpisodeActionsCompat? = null

    val actionsCompat: MovieDetailsEpisodeActionsCompat?
        get() = currentActionsCompat

    fun showLoading() {
        currentActionsCompat = null
        _baseUiState.value = DetailsScreenUiState.Loading(preview = loadingPreview)
    }

    fun showError() {
        currentActionsCompat = null
        _baseUiState.value = DetailsScreenUiState.Error
    }

    fun applyPrimary(primary: DetailsPrimaryStageResult) {
        currentActionsCompat = primary.actionsCompat
        _baseUiState.value = DetailsScreenUiState.Done(
            details = primary.details,
            isSecondaryContentLoading = true,
        )
    }

    fun applySecondary(secondary: DetailsSecondaryStageResult) {
        currentActionsCompat = secondary.actionsCompat
        updateDoneState { currentState ->
            currentState.copy(
                details = secondary.details,
                isSecondaryContentLoading = false,
            )
        }
    }

    fun finishSecondaryLoading() {
        updateDoneState { currentState ->
            currentState.copy(isSecondaryContentLoading = false)
        }
    }

    private fun updateDoneState(
        transform: (DetailsScreenUiState.Done) -> DetailsScreenUiState.Done,
    ) {
        _baseUiState.update { state ->
            val currentState = state as? DetailsScreenUiState.Done ?: return@update state
            transform(currentState)
        }
    }
}
