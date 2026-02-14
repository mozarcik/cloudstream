package com.lagradost.cloudstream3.tv.presentation.screens.media

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MediaDetailsScreenViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository,
) : ViewModel() {
    private companion object {
        const val DebugTag = "TvMediaDetailsVM"
    }
    private val favoriteOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private val baseUiState = savedStateHandle
        .getStateFlow<String?>(MediaDetailsScreen.UrlBundleKey, null)
        .onEach {
            favoriteOverrides.value = emptyMap()
        }
        .map { url ->
            val apiName = savedStateHandle.get<String>(MediaDetailsScreen.ApiNameBundleKey)

            if (url == null || apiName == null) {
                Log.e(DebugTag, "missing navigation args url=$url apiName=$apiName")
                return@map MediaDetailsScreenUiState.Error
            }

            try {
                val details = repository.getMediaDetails(url = url, apiName = apiName)
                Log.d(
                    DebugTag,
                    "loaded details id=${details.id} name=${details.name} seasonCount=${details.seasonCount} episodeCount=${details.episodeCount} seasons=${details.seasons.size}"
                )
                MediaDetailsScreenUiState.Done(
                    mediaDetails = details,
                    sourceUrl = url,
                    apiName = apiName
                )
            } catch (e: Exception) {
                Log.e(DebugTag, "failed loading details for api=$apiName url=$url", e)
                MediaDetailsScreenUiState.Error
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MediaDetailsScreenUiState.Loading
        )

    val uiState = combine(baseUiState, favoriteOverrides) { state, overrides ->
        when (state) {
            is MediaDetailsScreenUiState.Done -> {
                val overrideFavorite = overrides[state.mediaDetails.id]
                if (overrideFavorite == null) {
                    state
                } else {
                    state.copy(
                        mediaDetails = state.mediaDetails.copy(isFavorite = overrideFavorite)
                    )
                }
            }

            else -> state
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MediaDetailsScreenUiState.Loading
    )

    fun onFavoriteClick() {
        val currentState = uiState.value as? MediaDetailsScreenUiState.Done ?: return

        val url = savedStateHandle.get<String>(MediaDetailsScreen.UrlBundleKey)
        val apiName = savedStateHandle.get<String>(MediaDetailsScreen.ApiNameBundleKey)

        if (url == null || apiName == null) {
            Log.e(DebugTag, "cannot toggle favorite due to missing args url=$url apiName=$apiName")
            return
        }

        val targetFavoriteState = !currentState.mediaDetails.isFavorite
        val mediaId = currentState.mediaDetails.id
        viewModelScope.launch {
            try {
                repository.setMediaFavorite(
                    url = url,
                    apiName = apiName,
                    isFavorite = targetFavoriteState
                )
                favoriteOverrides.update { currentOverrides ->
                    currentOverrides + (mediaId to targetFavoriteState)
                }
                Log.d(DebugTag, "favorite toggled mediaId=$mediaId isFavorite=$targetFavoriteState")
            } catch (e: Exception) {
                Log.e(DebugTag, "failed to toggle favorite for api=$apiName url=$url", e)
            }
        }
    }
}

sealed class MediaDetailsScreenUiState {
    data object Loading : MediaDetailsScreenUiState()
    data object Error : MediaDetailsScreenUiState()
    data class Done(
        val mediaDetails: MovieDetails,
        val sourceUrl: String,
        val apiName: String,
    ) : MediaDetailsScreenUiState()
}
