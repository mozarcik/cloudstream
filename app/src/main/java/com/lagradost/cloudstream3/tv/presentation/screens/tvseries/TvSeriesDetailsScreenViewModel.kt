package com.lagradost.cloudstream3.tv.presentation.screens.tvseries

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DetailsLoadingPreview
import com.lagradost.cloudstream3.ui.WatchType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TvSeriesDetailsScreenViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository,
) : ViewModel() {
    private companion object {
        const val DebugTag = "TvSeriesDetailsVM"
    }

    private val favoriteOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val bookmarkOverrides = MutableStateFlow<Map<String, WatchType>>(emptyMap())
    private val initialLoadingPreview = DetailsLoadingPreview(
        title = savedStateHandle.get<String>(TvSeriesDetailsScreen.LoadingTitleBundleKey)
            ?.takeIf { it.isNotBlank() },
        posterUri = savedStateHandle.get<String>(TvSeriesDetailsScreen.LoadingPosterBundleKey)
            ?.takeIf { it.isNotBlank() },
        backdropUri = savedStateHandle.get<String>(TvSeriesDetailsScreen.LoadingBackdropBundleKey)
            ?.takeIf { it.isNotBlank() },
    )

    private val baseUiState = savedStateHandle
        .getStateFlow<String?>(TvSeriesDetailsScreen.UrlBundleKey, null)
        .onEach {
            favoriteOverrides.value = emptyMap()
            bookmarkOverrides.value = emptyMap()
        }
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
                TvSeriesDetailsScreenUiState.Done(
                    tvSeriesDetails = details,
                    sourceUrl = url,
                    apiName = apiName
                )
            } catch (e: Exception) {
                Log.e(DebugTag, "failed loading details for api=$apiName url=$url", e)
                TvSeriesDetailsScreenUiState.Error
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TvSeriesDetailsScreenUiState.Loading(preview = initialLoadingPreview)
        )

    val uiState = combine(baseUiState, favoriteOverrides, bookmarkOverrides) { state, favoriteState, bookmarkState ->
        when (state) {
            is TvSeriesDetailsScreenUiState.Done -> {
                val seriesId = state.tvSeriesDetails.id
                val overrideFavorite = favoriteState[seriesId]
                val overrideBookmark = bookmarkState[seriesId]
                if (overrideFavorite == null && overrideBookmark == null) {
                    state
                } else {
                    state.copy(
                        tvSeriesDetails = state.tvSeriesDetails.withLibraryOverride(
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
        initialValue = TvSeriesDetailsScreenUiState.Loading(preview = initialLoadingPreview)
    )

    fun onFavoriteClick() {
        val currentState = uiState.value as? TvSeriesDetailsScreenUiState.Done ?: return

        val url = savedStateHandle.get<String>(TvSeriesDetailsScreen.UrlBundleKey)
        val apiName = savedStateHandle.get<String>(TvSeriesDetailsScreen.ApiNameBundleKey)

        if (url == null || apiName == null) {
            Log.e(DebugTag, "cannot toggle favorite due to missing args url=$url apiName=$apiName")
            return
        }

        val targetFavoriteState = !currentState.tvSeriesDetails.isFavorite
        val seriesId = currentState.tvSeriesDetails.id
        viewModelScope.launch {
            try {
                repository.setMediaFavorite(
                    url = url,
                    apiName = apiName,
                    isFavorite = targetFavoriteState
                )
                favoriteOverrides.update { currentOverrides ->
                    currentOverrides + (seriesId to targetFavoriteState)
                }
                Log.d(DebugTag, "favorite toggled seriesId=$seriesId isFavorite=$targetFavoriteState")
            } catch (e: Exception) {
                Log.e(DebugTag, "failed to toggle favorite for api=$apiName url=$url", e)
            }
        }
    }

    fun onBookmarkClick(status: WatchType) {
        val currentState = uiState.value as? TvSeriesDetailsScreenUiState.Done ?: return

        val url = savedStateHandle.get<String>(TvSeriesDetailsScreen.UrlBundleKey)
        val apiName = savedStateHandle.get<String>(TvSeriesDetailsScreen.ApiNameBundleKey)

        if (url == null || apiName == null) {
            Log.e(DebugTag, "cannot update bookmark due to missing args url=$url apiName=$apiName")
            return
        }

        val seriesId = currentState.tvSeriesDetails.id
        viewModelScope.launch {
            try {
                repository.setMediaBookmarkStatus(
                    url = url,
                    apiName = apiName,
                    status = status
                )
                bookmarkOverrides.update { currentOverrides ->
                    currentOverrides + (seriesId to status)
                }
                Log.d(DebugTag, "bookmark updated seriesId=$seriesId status=$status")
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

sealed class TvSeriesDetailsScreenUiState {
    data class Loading(
        val preview: DetailsLoadingPreview = DetailsLoadingPreview()
    ) : TvSeriesDetailsScreenUiState()
    data object Error : TvSeriesDetailsScreenUiState()
    data class Done(
        val tvSeriesDetails: MovieDetails,
        val sourceUrl: String,
        val apiName: String,
    ) : TvSeriesDetailsScreenUiState()
}
