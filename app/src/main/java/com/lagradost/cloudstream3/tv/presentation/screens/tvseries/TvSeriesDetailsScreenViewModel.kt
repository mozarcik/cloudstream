package com.lagradost.cloudstream3.tv.presentation.screens.tvseries

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TvSeriesDetailsScreenViewModel(
    savedStateHandle: SavedStateHandle,
    repository: MovieRepository,
) : ViewModel() {
    private companion object {
        const val DebugTag = "TvSeriesDetailsVM"
    }

    val uiState = savedStateHandle
        .getStateFlow<String?>(TvSeriesDetailsScreen.UrlBundleKey, null)
        .map { url ->
            val apiName = savedStateHandle.get<String>(TvSeriesDetailsScreen.ApiNameBundleKey)
            
            if (url == null || apiName == null) {
                Log.e(DebugTag, "missing navigation args url=$url apiName=$apiName")
                return@map TvSeriesDetailsScreenUiState.Error
            }
            
            try {
                val details = repository.getTvSeriesDetails(url = url, apiName = apiName)
                Log.d(
                    DebugTag,
                    "loaded details id=${details.id} name=${details.name} seasonCount=${details.seasonCount} episodeCount=${details.episodeCount} seasons=${details.seasons.size}"
                )
                TvSeriesDetailsScreenUiState.Done(tvSeriesDetails = details)
            } catch (e: Exception) {
                Log.e(DebugTag, "failed loading details for api=$apiName url=$url", e)
                TvSeriesDetailsScreenUiState.Error
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TvSeriesDetailsScreenUiState.Loading
        )
}

sealed class TvSeriesDetailsScreenUiState {
    data object Loading : TvSeriesDetailsScreenUiState()
    data object Error : TvSeriesDetailsScreenUiState()
    data class Done(val tvSeriesDetails: MovieDetails) : TvSeriesDetailsScreenUiState()
}
