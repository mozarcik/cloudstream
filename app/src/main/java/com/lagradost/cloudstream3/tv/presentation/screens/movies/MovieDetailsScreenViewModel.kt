package com.lagradost.cloudstream3.tv.presentation.screens.movies

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MovieDetailsScreenViewModel(
    savedStateHandle: SavedStateHandle,
    repository: MovieRepository,
) : ViewModel() {
    private companion object {
        const val DebugTag = "TvMovieDetailsVM"
    }

    val uiState = savedStateHandle
        .getStateFlow<String?>(MovieDetailsScreen.UrlBundleKey, null)
        .map { url ->
            val apiName = savedStateHandle.get<String>(MovieDetailsScreen.ApiNameBundleKey)
            
            // Early return for missing parameters
            if (url == null || apiName == null) {
                Log.e(DebugTag, "missing navigation args url=$url apiName=$apiName")
                return@map MovieDetailsScreenUiState.Error
            }
            
            // Try to load movie details
            try {
                val details = repository.getMovieDetails(url = url, apiName = apiName)
                Log.d(
                    DebugTag,
                    "loaded details id=${details.id} name=${details.name} seasonCount=${details.seasonCount} episodeCount=${details.episodeCount} seasons=${details.seasons.size}"
                )
                MovieDetailsScreenUiState.Done(movieDetails = details)
            } catch (e: Exception) {
                Log.e(DebugTag, "failed loading details for api=$apiName url=$url", e)
                MovieDetailsScreenUiState.Error
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MovieDetailsScreenUiState.Loading
        )
}

sealed class MovieDetailsScreenUiState {
    data object Loading : MovieDetailsScreenUiState()
    data object Error : MovieDetailsScreenUiState()
    data class Done(val movieDetails: MovieDetails) : MovieDetailsScreenUiState()
}
