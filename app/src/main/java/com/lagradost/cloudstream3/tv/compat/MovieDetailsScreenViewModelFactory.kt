package com.lagradost.cloudstream3.tv.compat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsScreenViewModel

class MovieDetailsScreenViewModelFactory(
    private val savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MovieDetailsScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MovieDetailsScreenViewModel(savedStateHandle, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}