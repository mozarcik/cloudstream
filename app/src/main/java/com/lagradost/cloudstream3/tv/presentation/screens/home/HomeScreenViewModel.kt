package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.tv.data.entities.MovieList
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HomeScreenViewModel(private val movieRepository: MovieRepository) : ViewModel() {

    val uiState: StateFlow<HomeScreenUiState> = combine(
        movieRepository.getTrendingMovies(),
        movieRepository.getTop10Movies(),
        movieRepository.getNowPlayingMovies(),
    ) { trendingMovieList, top10MovieList, nowPlayingMovieList ->
        HomeScreenUiState.Ready(
            trendingMovieList,
            top10MovieList,
            nowPlayingMovieList
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeScreenUiState.Loading
    )
}

sealed interface HomeScreenUiState {
    data object Loading : HomeScreenUiState
    data object Error : HomeScreenUiState
    data class Ready(
        val trendingMovieList: MovieList,
        val top10MovieList: MovieList,
        val nowPlayingMovieList: MovieList
    ) : HomeScreenUiState
}
