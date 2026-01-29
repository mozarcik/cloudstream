package com.lagradost.cloudstream3.tv.presentation.screens.media

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MediaDetailsScreenViewModel(
    savedStateHandle: SavedStateHandle,
    repository: MovieRepository,
) : ViewModel() {
    private companion object {
        const val DebugTag = "TvMediaDetailsVM"
    }

    val uiState = savedStateHandle
        .getStateFlow<String?>(MediaDetailsScreen.UrlBundleKey, null)
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
                MediaDetailsScreenUiState.Done(mediaDetails = details)
            } catch (e: Exception) {
                Log.e(DebugTag, "failed loading details for api=$apiName url=$url", e)
                MediaDetailsScreenUiState.Error
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MediaDetailsScreenUiState.Loading
        )
}

sealed class MediaDetailsScreenUiState {
    data object Loading : MediaDetailsScreenUiState()
    data object Error : MediaDetailsScreenUiState()
    data class Done(val mediaDetails: MovieDetails) : MediaDetailsScreenUiState()
}
