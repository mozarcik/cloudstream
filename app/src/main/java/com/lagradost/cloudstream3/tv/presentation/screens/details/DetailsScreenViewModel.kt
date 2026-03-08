package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.compat.UnavailableDetailsCompat
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository
import com.lagradost.cloudstream3.tv.presentation.screens.unavailable.UnavailableDetailsUiModel
import com.lagradost.cloudstream3.ui.WatchType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailsScreenViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: MovieRepository,
    private val mode: DetailsScreenMode,
) : ViewModel() {

    private val routeArgs = savedStateHandle.toDetailsScreenRouteArgs(mode)
    private val routeStateHolder = DetailsRouteStateHolder(routeArgs.loadingPreview)
    private val libraryOverridesStateHolder = DetailsLibraryOverridesStateHolder()
    private val loadCoordinator = DetailsScreenLoadCoordinator(repository)
    private val libraryActionHandler = DetailsLibraryActionHandler(
        repository = repository,
        libraryOverridesStateHolder = libraryOverridesStateHolder,
    )

    val unavailableDetails: UnavailableDetailsUiModel
        get() = routeArgs.unavailableDetails

    val shouldShowUnavailableState: Boolean
        get() = routeArgs.shouldShowUnavailableState

    val canRemoveFromLibrary: Boolean by lazy {
        routeArgs.canRemoveFromLibrary()
    }

    val actionsCompat: MovieDetailsEpisodeActionsCompat?
        get() = routeStateHolder.actionsCompat

    val uiState: StateFlow<DetailsScreenUiState> = combine(
        routeStateHolder.baseUiState,
        libraryOverridesStateHolder.favoriteOverrides,
        libraryOverridesStateHolder.bookmarkOverrides,
    ) { state, favoriteState, bookmarkState ->
        applyDetailsLibraryOverrides(
            state = state,
            favoriteOverrides = favoriteState,
            bookmarkOverrides = bookmarkState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = routeStateHolder.baseUiState.value,
    )

    init {
        loadDetails()
    }

    fun onFavoriteClick() {
        val currentState = uiState.value as? DetailsScreenUiState.Done ?: return
        viewModelScope.launch {
            libraryActionHandler.toggleFavorite(currentState, routeArgs.source)
        }
    }

    fun onBookmarkClick(status: WatchType) {
        if (!mode.allowsBookmark) return

        val currentState = uiState.value as? DetailsScreenUiState.Done ?: return
        viewModelScope.launch {
            libraryActionHandler.updateBookmark(currentState, routeArgs.source, status)
        }
    }

    fun removeUnavailableItemFromLibrary(): Boolean {
        val source = routeArgs.source ?: return false
        return UnavailableDetailsCompat.removeFromLibrary(
            sourceUrl = source.url,
            apiName = source.apiName
        )
    }

    private fun loadDetails() {
        val source = routeArgs.source
        if (source == null) {
            DetailsScreenLoadLogger.logMissingArgs()
            routeStateHolder.showError()
            return
        }

        libraryOverridesStateHolder.clear()
        routeStateHolder.showLoading()

        viewModelScope.launch {
            try {
                when (val outcome = loadCoordinator.load(
                    url = source.url,
                    apiName = source.apiName,
                    onPrimaryLoaded = { primary ->
                        routeStateHolder.applyPrimary(primary)
                        DetailsScreenLoadLogger.logPrimaryLoaded(primary, mode)
                    }
                )) {
                    is DetailsScreenLoadOutcome.Success -> {
                        routeStateHolder.applySecondary(outcome.secondary)
                        DetailsScreenLoadLogger.logSecondaryLoaded(outcome.secondary)
                    }

                    is DetailsScreenLoadOutcome.SecondaryFailure -> {
                        DetailsScreenLoadLogger.logSecondaryFailure(source, mode, outcome.error)
                        routeStateHolder.finishSecondaryLoading()
                    }
                }
            } catch (e: Exception) {
                DetailsScreenLoadLogger.logPrimaryFailure(source, mode, e)
                routeStateHolder.showError()
            }
        }
    }
}
