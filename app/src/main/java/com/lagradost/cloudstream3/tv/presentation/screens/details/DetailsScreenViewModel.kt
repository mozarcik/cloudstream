package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.reporting.AppErrorReporter
import com.lagradost.cloudstream3.reporting.HandledAppErrorContext
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.compat.UnavailableDetailsCompat
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository
import com.lagradost.cloudstream3.tv.presentation.common.TvErrorUiModel
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
    private val tmdbSourceResolver = DetailsTmdbSourceResolver()
    private val libraryActionHandler = DetailsLibraryActionHandler(
        repository = repository,
        libraryOverridesStateHolder = libraryOverridesStateHolder,
    )
    private var activeSource: DetailsRouteSource? = routeArgs.source

    val unavailableDetails: UnavailableDetailsUiModel
        get() = routeArgs.unavailableDetails

    val canRemoveFromLibrary: Boolean by lazy {
        routeArgs.canRemoveFromLibrary()
    }

    val actionsCompat: MovieDetailsEpisodeActionsCompat?
        get() = routeStateHolder.actionsCompat

    internal val currentSource: DetailsRouteSource?
        get() = activeSource

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
        val currentSource = activeSource ?: return
        viewModelScope.launch {
            libraryActionHandler.toggleFavorite(currentState, currentSource)
        }
    }

    fun onBookmarkClick(status: WatchType) {
        if (!mode.allowsBookmark) return

        val currentState = uiState.value as? DetailsScreenUiState.Done ?: return
        val currentSource = activeSource ?: return
        viewModelScope.launch {
            libraryActionHandler.updateBookmark(currentState, currentSource, status)
        }
    }

    fun removeUnavailableItemFromLibrary(): Boolean {
        val source = routeArgs.source ?: return false
        return UnavailableDetailsCompat.removeFromLibrary(
            sourceUrl = source.url,
            apiName = source.apiName
        )
    }

    fun retry() {
        loadDetails()
    }

    private fun loadDetails() {
        val originSource = routeArgs.source
        if (originSource == null) {
            DetailsScreenLoadLogger.logMissingArgs()
            routeStateHolder.showError(
                error = createDetailsError(
                    throwable = null,
                    fallbackResId = R.string.error_invalid_data,
                )
            )
            reportHandledDetailsError(
                throwable = null,
                uiMessage = localizeString(R.string.error_invalid_data),
                eventMessage = "TV details missing navigation args",
                source = null,
            )
            return
        }

        libraryOverridesStateHolder.clear()
        routeStateHolder.showLoading()
        activeSource = originSource

        viewModelScope.launch {
            try {
                val source = routeArgs.tmdbResolveRequest?.let { request ->
                    DetailsScreenLoadLogger.logTmdbResolveStart(
                        request = request,
                        mode = mode,
                    )
                    tmdbSourceResolver.resolve(request)?.also { resolvedSource ->
                        DetailsScreenLoadLogger.logTmdbResolveSuccess(
                            request = request,
                            resolvedSource = resolvedSource,
                        )
                    } ?: run {
                        DetailsScreenLoadLogger.logTmdbResolveUnavailable(
                            request = request,
                            mode = mode,
                        )
                        routeStateHolder.showUnavailable()
                        return@launch
                    }
                } ?: originSource

                activeSource = source

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
                        reportHandledDetailsError(
                            throwable = outcome.error,
                            uiMessage = localizeString(R.string.tv_home_failed_to_load),
                            eventMessage = "TV details secondary load failed",
                            source = source,
                        )
                    }
                }
            } catch (e: Exception) {
                DetailsScreenLoadLogger.logPrimaryFailure(activeSource ?: originSource, mode, e)
                val uiMessage = createDetailsError(
                    throwable = e,
                    fallbackResId = R.string.tv_home_failed_to_load,
                )
                routeStateHolder.showError(uiMessage)
                reportHandledDetailsError(
                    throwable = e,
                    uiMessage = uiMessage.message,
                    eventMessage = "TV details failed to load",
                    source = activeSource ?: originSource,
                )
            }
        }
    }

    private fun reportHandledDetailsError(
        throwable: Throwable?,
        uiMessage: String,
        eventMessage: String,
        source: DetailsRouteSource?,
    ) {
        AppErrorReporter.reportHandled(
            context = HandledAppErrorContext(
                screen = "tv_details_${mode.name.lowercase()}",
                uiMessage = uiMessage,
                eventMessage = eventMessage,
                providerName = source?.apiName,
                itemTitle = routeArgs.loadingPreview.title,
                mediaType = mode.name,
            ),
            throwable = throwable,
        )
    }

    private fun createDetailsError(
        throwable: Throwable?,
        fallbackResId: Int,
    ): TvErrorUiModel {
        val message = throwable
            ?.localizedMessage
            ?.lineSequence()
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
            .takeIf { it.isNotBlank() }
            ?: localizeString(fallbackResId)

        return TvErrorUiModel(message = message)
    }

    private fun localizeString(resId: Int): String {
        return CloudStreamApp.context?.getString(resId)
            ?: when (resId) {
                R.string.error_invalid_data -> "Invalid data"
                R.string.tv_home_failed_to_load -> "Failed to load"
                else -> "Error"
            }
    }
}
