package com.lagradost.cloudstream3.tv.compat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.TvSeriesDetailsScreenViewModel

class TvSeriesDetailsScreenViewModelFactory(
    private val savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TvSeriesDetailsScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TvSeriesDetailsScreenViewModel(savedStateHandle, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
