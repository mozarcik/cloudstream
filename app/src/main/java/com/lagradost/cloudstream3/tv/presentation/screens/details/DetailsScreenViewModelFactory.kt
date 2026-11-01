package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository

class DetailsScreenViewModelFactory(
    private val savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository,
    private val mode: DetailsScreenMode,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailsScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailsScreenViewModel(
                savedStateHandle = savedStateHandle,
                repository = repository,
                mode = mode,
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
