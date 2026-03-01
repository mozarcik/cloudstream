package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.util.Log
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.tv.presentation.screens.player.core.resolvePlayerPlaybackTarget
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsUiState
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.ui.player.LOADTYPE_INAPP
import com.lagradost.cloudstream3.ui.player.RepoLinkGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DebugTag = "TvPlayerVM"
private const val ReadyRefreshBatchSize = 20
private const val ReadyRefreshDebounceMs = 900L

internal fun retry(context: PlayerScreenCoordinatorContext) {
    context.catalog.loadingJob?.cancel()
    context.catalog.store.reset()
    context.catalog.hasFinalized = false
    context.core.metadata = TvPlayerMetadata.Empty
    context.core.currentLoadResponse = null
    context.catalog.pendingReadyRefreshChanges = 0
    context.catalog.pendingReadyRefreshJob?.cancel()
    context.catalog.pendingReadyRefreshJob = null
    context.core.currentEpisode = null
    context.panels.onlineSubtitlesController.reset()
    context.core.playbackProgressState.reset()
    context.panels.stateHolder.reset()
    context.catalog.uiState.value = PlayerCatalogUiState()
    context.panels.uiState.value = TvPlayerPanelsUiState()
    loadSources(context)
}

internal fun skipLoading(context: PlayerScreenCoordinatorContext) {
    if (!context.catalog.store.hasLoadedSources()) {
        return
    }
    finalizeLoading(
        context = context,
        forceError = true,
    )
}

internal fun loadSources(context: PlayerScreenCoordinatorContext) {
    val url = context.savedStateHandle.get<String>(PlayerScreenNavigation.UrlBundleKey).orEmpty()
    val apiName = context.savedStateHandle.get<String>(PlayerScreenNavigation.ApiNameBundleKey).orEmpty()
    val episodeData = context.savedStateHandle.get<String>(PlayerScreenNavigation.EpisodeDataBundleKey)
        ?.takeIf { it.isNotBlank() }

    if (url.isBlank() || apiName.isBlank()) {
        Log.e(DebugTag, "missing args: url=$url apiName=$apiName")
        context.core.uiState.value = TvPlayerUiState.Error(
            metadata = TvPlayerMetadata.Empty,
            messageResId = R.string.error_loading_links_toast,
        )
        return
    }

    context.catalog.loadingJob = context.coroutineScope.launch {
        val api = APIHolder.getApiFromNameNull(apiName)
        if (api == null) {
            Log.e(DebugTag, "provider not found api=$apiName")
            context.core.uiState.value = TvPlayerUiState.Error(
                metadata = TvPlayerMetadata(
                    title = apiName,
                    subtitle = "",
                    backdropUri = null,
                ),
                messageResId = R.string.error_loading_links_toast,
            )
            return@launch
        }

        val repository = APIRepository(api)
        val target = withContext(Dispatchers.IO) {
            resolvePlayerPlaybackTarget(
                repository = repository,
                url = url,
                apiName = apiName,
                directEpisodeData = episodeData,
            )
        }

        if (target == null) {
            context.core.uiState.value = TvPlayerUiState.Error(
                metadata = TvPlayerMetadata(
                    title = apiName,
                    subtitle = "",
                    backdropUri = null,
                ),
                messageResId = R.string.no_links_found_toast,
            )
            return@launch
        }

        context.core.metadata = target.metadata
        context.core.currentEpisode = target.episode
        context.core.currentLoadResponse = target.page
        context.panels.onlineSubtitlesController.reset(
            query = defaultOnlineSubtitlesQuery(context),
        )
        context.core.playbackProgressState.onEpisodeChanged(target.episode)
        postLoadingState(context)

        val generator = RepoLinkGenerator(
            episodes = listOf(target.episode),
            page = target.page,
        )

        try {
            withContext(Dispatchers.IO) {
                val loadingScope = this
                generator.generateLinks(
                    clearCache = false,
                    sourceTypes = LOADTYPE_INAPP,
                    callback = { (link, _) ->
                        if (link == null || link.url.isBlank()) return@generateLinks
                        val inserted = context.catalog.store.insertLink(link)
                        if (!inserted) return@generateLinks
                        loadingScope.launch(Dispatchers.Main.immediate) {
                            if (context.catalog.hasFinalized) {
                                onBackgroundDataInsertedAfterFinalize(context)
                            } else {
                                postLoadingState(context)
                            }
                        }
                    },
                    subtitleCallback = { subtitle ->
                        val inserted = context.catalog.store.insertSubtitle(subtitle)
                        if (!inserted) return@generateLinks
                        loadingScope.launch(Dispatchers.Main.immediate) {
                            if (context.catalog.hasFinalized) {
                                onBackgroundDataInsertedAfterFinalize(context)
                            }
                        }
                    },
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logError(throwable)
        }

        flushPendingReadyRefresh(
            context = context,
            force = true,
        )
        finalizeLoading(
            context = context,
            forceError = true,
        )
    }
}

internal fun postLoadingState(context: PlayerScreenCoordinatorContext) {
    if (context.catalog.hasFinalized) return
    context.core.uiState.value = TvPlayerUiState.LoadingSources(
        metadata = context.core.metadata,
        loadedSources = context.catalog.store.loadedSourcesCount(),
        canSkip = context.catalog.store.hasLoadedSources(),
    )
}

internal fun onBackgroundDataInsertedAfterFinalize(context: PlayerScreenCoordinatorContext) {
    if (!context.catalog.hasFinalized) return

    context.catalog.pendingReadyRefreshChanges += 1

    if (isSelectionPanelOpen(context)) {
        return
    }

    if (context.catalog.pendingReadyRefreshChanges >= ReadyRefreshBatchSize) {
        flushPendingReadyRefresh(
            context = context,
            force = false,
        )
        return
    }

    schedulePendingReadyRefresh(context)
}

internal fun flushPendingReadyRefresh(
    context: PlayerScreenCoordinatorContext,
    force: Boolean,
) {
    if (!context.catalog.hasFinalized) return
    if (!force && context.catalog.pendingReadyRefreshChanges <= 0) return

    context.catalog.pendingReadyRefreshJob?.cancel()
    context.catalog.pendingReadyRefreshJob = null
    context.catalog.pendingReadyRefreshChanges = 0
    refreshReadyStateFromLoadedData(context)
}

internal fun schedulePendingReadyRefresh(context: PlayerScreenCoordinatorContext) {
    if (context.catalog.pendingReadyRefreshJob?.isActive == true) {
        return
    }

    context.catalog.pendingReadyRefreshJob = context.coroutineScope.launch {
        delay(ReadyRefreshDebounceMs)
        context.catalog.pendingReadyRefreshJob = null
        if (!context.catalog.hasFinalized || isSelectionPanelOpen(context)) {
            return@launch
        }
        flushPendingReadyRefresh(
            context = context,
            force = false,
        )
    }
}

internal fun refreshReadyStateFromLoadedData(context: PlayerScreenCoordinatorContext) {
    if (!context.catalog.hasFinalized) return
    val rebuilt = context.catalog.store.rebuildOrderedData()
    if (!rebuilt) return
    val resolvedCurrentIndex = context.catalog.store.currentLinkIndex
    updateCatalogUiState(
        context = context,
        currentIndex = resolvedCurrentIndex,
        link = context.catalog.store.orderedLinks[resolvedCurrentIndex],
    )
    refreshPanelsUiStateForCurrentLink(context)
}

internal fun finalizeLoading(
    context: PlayerScreenCoordinatorContext,
    forceError: Boolean,
) {
    if (context.catalog.hasFinalized) return

    val rebuilt = context.catalog.store.rebuildOrderedData(currentUrl = null)
    if (rebuilt) {
        val firstLink = context.catalog.store.markFirstLinkLoading() ?: return
        context.panels.stateHolder.onSourceChanged(firstLink)
        context.catalog.hasFinalized = true
        postReadyState(
            context = context,
            link = firstLink,
            currentIndex = 0,
        )
        return
    }

    if (forceError) {
        context.catalog.hasFinalized = true
        context.core.uiState.value = TvPlayerUiState.Error(
            metadata = context.core.metadata,
            messageResId = R.string.no_links_found_toast,
        )
    }
}
