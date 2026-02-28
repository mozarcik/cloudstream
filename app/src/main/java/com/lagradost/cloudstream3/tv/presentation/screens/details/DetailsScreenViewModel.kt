package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.compat.UnavailableDetailsCompat
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DetailsLoadingPreview
import com.lagradost.cloudstream3.tv.presentation.screens.unavailable.UnavailableDetailsUiModel
import com.lagradost.cloudstream3.ui.WatchType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailsScreenViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository,
    private val mode: DetailsScreenMode,
) : ViewModel() {

    private val favoriteOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val bookmarkOverrides = MutableStateFlow<Map<String, WatchType>>(emptyMap())

    private val sourceUrl: String?
        get() = savedStateHandle.get(DetailsScreenNavigation.UrlBundleKey)
    private val sourceApiName: String?
        get() = savedStateHandle.get(DetailsScreenNavigation.ApiNameBundleKey)

    private val initialLoadingPreview = DetailsLoadingPreview(
        title = savedStateHandle.get<String>(DetailsScreenNavigation.LoadingTitleBundleKey)
            ?.takeIf { it.isNotBlank() },
        posterUri = savedStateHandle.get<String>(DetailsScreenNavigation.LoadingPosterBundleKey)
            ?.takeIf { it.isNotBlank() },
        backdropUri = savedStateHandle.get<String>(DetailsScreenNavigation.LoadingBackdropBundleKey)
            ?.takeIf { it.isNotBlank() },
    )

    val unavailableDetails: UnavailableDetailsUiModel
        get() {
            val title = savedStateHandle
                .get<String>(DetailsScreenNavigation.LoadingTitleBundleKey)
                ?.takeIf { it.isNotBlank() }
                .orEmpty()
            val posterUrl = savedStateHandle
                .get<String>(DetailsScreenNavigation.LoadingPosterBundleKey)
                ?.takeIf { it.isNotBlank() }
            val backdropUrl = savedStateHandle
                .get<String>(DetailsScreenNavigation.LoadingBackdropBundleKey)
                ?.takeIf { it.isNotBlank() }
            val description = savedStateHandle
                .get<String>(DetailsScreenNavigation.LoadingDescriptionBundleKey)
                ?.takeIf { it.isNotBlank() }
            val type = savedStateHandle
                .get<String>(DetailsScreenNavigation.LoadingTypeBundleKey)
                .toTvTypeOrNull()
                ?: mode.unavailableFallbackType
            val year = savedStateHandle.get<Int>(DetailsScreenNavigation.LoadingYearBundleKey)
            val providerName = savedStateHandle
                .get<String>(DetailsScreenNavigation.LoadingProviderBundleKey)
                ?.takeIf { it.isNotBlank() }
                ?: sourceApiName

            return UnavailableDetailsUiModel(
                title = title,
                posterUrl = posterUrl,
                backdropUrl = backdropUrl,
                description = description,
                type = type,
                year = year,
                providerName = providerName
            )
        }

    val shouldShowUnavailableState: Boolean
        get() = !sourceUrl.isNullOrBlank() && !sourceApiName.isNullOrBlank()

    val canRemoveFromLibrary: Boolean by lazy {
        val url = sourceUrl ?: return@lazy false
        val apiName = sourceApiName ?: return@lazy false
        UnavailableDetailsCompat.isInLocalLibrary(
            sourceUrl = url,
            apiName = apiName
        )
    }

    private val baseUiState = savedStateHandle
        .getStateFlow<String?>(DetailsScreenNavigation.UrlBundleKey, null)
        .onEach {
            favoriteOverrides.value = emptyMap()
            bookmarkOverrides.value = emptyMap()
        }
        .map { url ->
            val apiName = savedStateHandle.get<String>(DetailsScreenNavigation.ApiNameBundleKey)

            if (url == null || apiName == null) {
                Log.e(TAG, "missing navigation args url=$url apiName=$apiName")
                return@map DetailsScreenUiState.Error
            }

            try {
                val details = repository.getDetails(url = url, apiName = apiName)
                Log.d(
                    TAG,
                    "loaded details id=${details.id} name=${details.name} mode=$mode seasonCount=${details.seasonCount} episodeCount=${details.episodeCount} seasons=${details.seasons.size}"
                )
                DetailsScreenUiState.Done(
                    details = details,
                    sourceUrl = url,
                    apiName = apiName
                )
            } catch (e: Exception) {
                Log.e(TAG, "failed loading details for api=$apiName url=$url mode=$mode", e)
                DetailsScreenUiState.Error
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailsScreenUiState.Loading(preview = initialLoadingPreview)
        )

    val uiState = combine(baseUiState, favoriteOverrides, bookmarkOverrides) { state, favoriteState, bookmarkState ->
        when (state) {
            is DetailsScreenUiState.Done -> {
                val detailsId = state.details.id
                val overrideFavorite = favoriteState[detailsId]
                val overrideBookmark = bookmarkState[detailsId]
                if (overrideFavorite == null && overrideBookmark == null) {
                    state
                } else {
                    state.copy(
                        details = state.details.withLibraryOverride(
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
        initialValue = DetailsScreenUiState.Loading(preview = initialLoadingPreview)
    )

    fun onFavoriteClick() {
        val currentState = uiState.value as? DetailsScreenUiState.Done ?: return

        val url = savedStateHandle.get<String>(DetailsScreenNavigation.UrlBundleKey)
        val apiName = savedStateHandle.get<String>(DetailsScreenNavigation.ApiNameBundleKey)

        if (url == null || apiName == null) {
            Log.e(TAG, "cannot toggle favorite due to missing args url=$url apiName=$apiName")
            return
        }

        val targetFavoriteState = !currentState.details.isFavorite
        val detailsId = currentState.details.id
        viewModelScope.launch {
            try {
                repository.setMediaFavorite(
                    url = url,
                    apiName = apiName,
                    isFavorite = targetFavoriteState
                )
                favoriteOverrides.update { currentOverrides ->
                    currentOverrides + (detailsId to targetFavoriteState)
                }
                Log.d(TAG, "favorite toggled detailsId=$detailsId isFavorite=$targetFavoriteState")
            } catch (e: Exception) {
                Log.e(TAG, "failed to toggle favorite for api=$apiName url=$url", e)
            }
        }
    }

    fun onBookmarkClick(status: WatchType) {
        if (!mode.allowsBookmark) return

        val currentState = uiState.value as? DetailsScreenUiState.Done ?: return

        val url = savedStateHandle.get<String>(DetailsScreenNavigation.UrlBundleKey)
        val apiName = savedStateHandle.get<String>(DetailsScreenNavigation.ApiNameBundleKey)

        if (url == null || apiName == null) {
            Log.e(TAG, "cannot update bookmark due to missing args url=$url apiName=$apiName")
            return
        }

        val detailsId = currentState.details.id
        viewModelScope.launch {
            try {
                repository.setMediaBookmarkStatus(
                    url = url,
                    apiName = apiName,
                    status = status
                )
                bookmarkOverrides.update { currentOverrides ->
                    currentOverrides + (detailsId to status)
                }
                Log.d(TAG, "bookmark updated detailsId=$detailsId status=$status")
            } catch (e: Exception) {
                Log.e(TAG, "failed to update bookmark for api=$apiName url=$url", e)
            }
        }
    }

    fun removeUnavailableItemFromLibrary(): Boolean {
        val url = sourceUrl ?: return false
        val apiName = sourceApiName ?: return false
        return UnavailableDetailsCompat.removeFromLibrary(
            sourceUrl = url,
            apiName = apiName
        )
    }

    private companion object {
        private const val TAG = "TvDetailsVM"
    }
}

@Immutable
sealed interface DetailsScreenUiState {
    @Immutable
    data class Loading(
        val preview: DetailsLoadingPreview = DetailsLoadingPreview()
    ) : DetailsScreenUiState

    data object Error : DetailsScreenUiState

    @Immutable
    data class Done(
        val details: MovieDetails,
        val sourceUrl: String,
        val apiName: String,
    ) : DetailsScreenUiState
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

private fun String?.toTvTypeOrNull(): TvType? {
    if (this.isNullOrBlank()) return null
    return TvType.entries.firstOrNull { type ->
        type.name == this
    }
}
