package com.lagradost.cloudstream3.tv.presentation.screens.movies

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository
import com.lagradost.cloudstream3.ui.WatchType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MovieDetailsScreenViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository,
) : ViewModel() {
    private companion object {
        const val DebugTag = "TvMovieDetailsVM"
    }

    private val favoriteOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val bookmarkOverrides = MutableStateFlow<Map<String, WatchType>>(emptyMap())

    private val initialLoadingPreview = DetailsLoadingPreview(
        title = savedStateHandle.get<String>(MovieDetailsScreen.LoadingTitleBundleKey)
            ?.takeIf { it.isNotBlank() },
        posterUri = savedStateHandle.get<String>(MovieDetailsScreen.LoadingPosterBundleKey)
            ?.takeIf { it.isNotBlank() },
        backdropUri = savedStateHandle.get<String>(MovieDetailsScreen.LoadingBackdropBundleKey)
            ?.takeIf { it.isNotBlank() },
    )

    private val baseUiState = savedStateHandle
        .getStateFlow<String?>(MovieDetailsScreen.UrlBundleKey, null)
        .onEach {
            favoriteOverrides.value = emptyMap()
            bookmarkOverrides.value = emptyMap()
        }
        .map { url ->
            val apiName = savedStateHandle.get<String>(MovieDetailsScreen.ApiNameBundleKey)

            if (url == null || apiName == null) {
                Log.e(DebugTag, "missing navigation args url=$url apiName=$apiName")
                return@map MovieDetailsScreenUiState.Error
            }

            try {
                val details = repository.getMovieDetails(url = url, apiName = apiName)
                Log.d(
                    DebugTag,
                    "loaded details id=${details.id} name=${details.name} seasonCount=${details.seasonCount} episodeCount=${details.episodeCount} seasons=${details.seasons.size}"
                )
                MovieDetailsScreenUiState.Done(
                    movieDetails = details,
                    sourceUrl = url,
                    apiName = apiName
                )
            } catch (e: Exception) {
                Log.e(DebugTag, "failed loading details for api=$apiName url=$url", e)
                MovieDetailsScreenUiState.Error
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MovieDetailsScreenUiState.Loading(preview = initialLoadingPreview)
        )

    val uiState = combine(baseUiState, favoriteOverrides, bookmarkOverrides) { state, favoriteState, bookmarkState ->
        when (state) {
            is MovieDetailsScreenUiState.Done -> {
                val movieId = state.movieDetails.id
                val overrideFavorite = favoriteState[movieId]
                val overrideBookmark = bookmarkState[movieId]
                if (overrideFavorite == null && overrideBookmark == null) {
                    state
                } else {
                    state.copy(
                        movieDetails = state.movieDetails.withLibraryOverride(
                            favorite = overrideFavorite,
                            bookmark = overrideBookmark
                        )
                    )
                }
            }

            else -> state
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MovieDetailsScreenUiState.Loading(preview = initialLoadingPreview)
    )

    fun onFavoriteClick() {
        val currentState = uiState.value as? MovieDetailsScreenUiState.Done ?: return

        val url = savedStateHandle.get<String>(MovieDetailsScreen.UrlBundleKey)
        val apiName = savedStateHandle.get<String>(MovieDetailsScreen.ApiNameBundleKey)

        if (url == null || apiName == null) {
            Log.e(DebugTag, "cannot toggle favorite due to missing args url=$url apiName=$apiName")
            return
        }

        val targetFavoriteState = !currentState.movieDetails.isFavorite
        val movieId = currentState.movieDetails.id
        viewModelScope.launch {
            try {
                repository.setMediaFavorite(
                    url = url,
                    apiName = apiName,
                    isFavorite = targetFavoriteState
                )
                favoriteOverrides.update { currentOverrides ->
                    currentOverrides + (movieId to targetFavoriteState)
                }
                Log.d(DebugTag, "favorite toggled movieId=$movieId isFavorite=$targetFavoriteState")
            } catch (e: Exception) {
                Log.e(DebugTag, "failed to toggle favorite for api=$apiName url=$url", e)
            }
        }
    }

    fun onBookmarkClick(status: WatchType) {
        val currentState = uiState.value as? MovieDetailsScreenUiState.Done ?: return

        val url = savedStateHandle.get<String>(MovieDetailsScreen.UrlBundleKey)
        val apiName = savedStateHandle.get<String>(MovieDetailsScreen.ApiNameBundleKey)

        if (url == null || apiName == null) {
            Log.e(DebugTag, "cannot update bookmark due to missing args url=$url apiName=$apiName")
            return
        }

        val movieId = currentState.movieDetails.id
        viewModelScope.launch {
            try {
                repository.setMediaBookmarkStatus(
                    url = url,
                    apiName = apiName,
                    status = status
                )
                bookmarkOverrides.update { currentOverrides ->
                    currentOverrides + (movieId to status)
                }
                Log.d(DebugTag, "bookmark updated movieId=$movieId status=$status")
            } catch (e: Exception) {
                Log.e(DebugTag, "failed to update bookmark for api=$apiName url=$url", e)
            }
        }
    }
}

private fun MovieDetails.withLibraryOverride(
    favorite: Boolean?,
    bookmark: WatchType?,
): MovieDetails {
    var updatedDetails = this

    if (favorite != null) {
        updatedDetails = updatedDetails.copy(isFavorite = favorite)
    }

    if (bookmark != null) {
        updatedDetails = updatedDetails.copy(
            isBookmarked = bookmark != WatchType.NONE,
            bookmarkLabelRes = bookmark.stringRes.takeIf { bookmark != WatchType.NONE }
        )
    }

    return updatedDetails
}

sealed class MovieDetailsScreenUiState {
    data class Loading(
        val preview: DetailsLoadingPreview = DetailsLoadingPreview()
    ) : MovieDetailsScreenUiState()
    data object Error : MovieDetailsScreenUiState()
    data class Done(
        val movieDetails: MovieDetails,
        val sourceUrl: String,
        val apiName: String,
    ) : MovieDetailsScreenUiState()
}
