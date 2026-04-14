package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.net.Uri
import android.util.Log
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.tv.presentation.screens.player.core.resolvePlayerPlaybackTarget
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsUiState
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.ui.player.DownloadFileGenerator
import com.lagradost.cloudstream3.ui.player.ExtractorUri
import com.lagradost.cloudstream3.ui.player.LOADTYPE_INAPP
import com.lagradost.cloudstream3.ui.player.RepoLinkGenerator
import com.lagradost.cloudstream3.ui.player.toSubtitleFetchEpisodeMetadataLog
import com.lagradost.cloudstream3.ui.player.toSubtitleFetchLogPayload
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.buildResultEpisode
import com.lagradost.cloudstream3.utils.DOWNLOAD_EPISODE_CACHE
import com.lagradost.cloudstream3.utils.DOWNLOAD_HEADER_CACHE
import com.lagradost.cloudstream3.utils.DataStore.getKey
import com.lagradost.cloudstream3.utils.DataStore.getKeys
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.VideoDownloadHelper
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DebugTag = "TvPlayerVM"
private const val InitialFinalizeQuietWindowMs = 1_500L
private const val ReadyRefreshBatchSize = 20
private const val ReadyRefreshDebounceMs = 900L

private data class DownloadedPlaybackTarget(
    val metadata: TvPlayerMetadata,
    val episodes: List<ResultEpisode>,
    val selectedEpisodeIndex: Int,
    val extractorItems: List<ExtractorUri>,
)

private fun playerDiagnosticsLog(message: String) {
    Log.i(DebugTag, message)
}

internal fun retry(context: PlayerScreenCoordinatorContext) {
    context.catalog.loadingJob?.cancel()
    context.catalog.pendingInitialFinalizeJob?.cancel()
    context.catalog.pendingInitialFinalizeJob = null
    context.catalog.prefetchJob?.cancel()
    context.catalog.prefetchJob = null
    context.catalog.prefetchedFromEpisodeId = null
    context.catalog.prefetchingFromEpisodeId = null
    context.catalog.generator = null
    context.catalog.store.reset()
    context.catalog.hasFinalized = false
    context.core.baseMetadata = TvPlayerMetadata.Empty
    context.core.metadata = TvPlayerMetadata.Empty
    context.core.loadingUseBlackBackground = false
    context.core.currentLoadResponse = null
    context.catalog.pendingReadyRefreshChanges = 0
    context.catalog.pendingReadyRefreshJob?.cancel()
    context.catalog.pendingReadyRefreshJob = null
    context.core.currentEpisode = null
    context.core.episodeQueue = emptyList()
    context.core.currentEpisodeIndex = -1
    context.panels.subtitleEncodingController.resetNavigation()
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
    val rawPlaybackTarget = context.savedStateHandle
        .get<String>(PlayerScreenNavigation.PlaybackTargetBundleKey)
    val playbackTarget = PlayerScreenNavigation.fromNavigationArg(rawPlaybackTarget)
    val downloadedEpisodeId = (playbackTarget as? PlayerStartTarget.DownloadedEpisode)?.episodeId

    if (downloadedEpisodeId == null && (url.isBlank() || apiName.isBlank())) {
        Log.e(DebugTag, "missing args: url=$url apiName=$apiName")
        val uiError = playerUiErrorFromRes(R.string.error_loading_links_toast)
        context.core.uiState.value = TvPlayerUiState.Error(
            metadata = TvPlayerMetadata.Empty,
            error = uiError,
        )
        reportPlayerHandledError(
            metadata = TvPlayerMetadata.Empty,
            uiMessage = uiError.message,
            eventMessage = "TV player missing navigation args",
        )
        return
    }

    context.catalog.loadingJob = context.coroutineScope.launch {
        if (downloadedEpisodeId != null) {
            val downloadedTarget = withContext(Dispatchers.IO) {
                resolveDownloadedPlaybackTarget(
                    episodeId = downloadedEpisodeId,
                    fallbackApiName = apiName,
                )
            }
            if (downloadedTarget == null) {
                Log.e(DebugTag, "downloaded playback target unavailable episodeId=$downloadedEpisodeId")
                val metadata = TvPlayerMetadata(
                    title = apiName.ifBlank { "Downloads" },
                    subtitle = "",
                    backdropUri = null,
                    apiName = apiName,
                )
                val uiError = playerUiErrorFromRes(R.string.no_links_found_toast)
                context.core.uiState.value = TvPlayerUiState.Error(
                    metadata = metadata,
                    error = uiError,
                )
                reportPlayerHandledError(
                    metadata = metadata,
                    uiMessage = uiError.message,
                    eventMessage = "TV player downloaded target unavailable episodeId=$downloadedEpisodeId",
                )
                return@launch
            }

            applyDownloadedPlaybackTarget(
                context = context,
                target = downloadedTarget,
            )
            return@launch
        }

        val api = APIHolder.getApiFromNameNull(apiName)
        if (api == null) {
            Log.e(DebugTag, "provider not found api=$apiName")
            val metadata = TvPlayerMetadata(
                title = apiName,
                subtitle = "",
                backdropUri = null,
                apiName = apiName,
            )
            val uiError = playerUiErrorFromRes(R.string.error_loading_links_toast)
            context.core.uiState.value = TvPlayerUiState.Error(
                metadata = metadata,
                error = uiError,
            )
            reportPlayerHandledError(
                metadata = metadata,
                uiMessage = uiError.message,
                eventMessage = "TV player provider not found api=$apiName",
            )
            return@launch
        }

        val repository = APIRepository(api)
        val target = withContext(Dispatchers.IO) {
            resolvePlayerPlaybackTarget(
                repository = repository,
                url = url,
                apiName = apiName,
                startTarget = playbackTarget,
            )
        }

        if (target == null) {
            val metadata = TvPlayerMetadata(
                title = apiName,
                subtitle = "",
                backdropUri = null,
                apiName = apiName,
            )
            val uiError = playerUiErrorFromRes(R.string.no_links_found_toast)
            context.core.uiState.value = TvPlayerUiState.Error(
                metadata = metadata,
                error = uiError,
            )
            reportPlayerHandledError(
                metadata = metadata,
                uiMessage = uiError.message,
                eventMessage = "TV player resolved no playable links api=$apiName",
            )
            return@launch
        }

        context.core.baseMetadata = target.metadata.copy(
            season = null,
            episode = null,
            episodeTitle = null,
        )
        context.core.metadata = target.metadata
        context.core.currentEpisode = target.episode
        context.core.episodeQueue = target.episodes
        context.core.currentEpisodeIndex = target.selectedEpisodeIndex
        context.core.currentLoadResponse = target.page
        context.panels.subtitleEncodingController.resetNavigation()
        context.panels.onlineSubtitlesController.reset(
            query = defaultOnlineSubtitlesQuery(context),
        )
        context.core.playbackProgressState.onEpisodeChanged(
            episode = target.episode,
            nextEpisode = target.episodes.getOrNull(target.selectedEpisodeIndex + 1),
        )
        context.catalog.generator = RepoLinkGenerator(
            episodes = target.episodes,
            currentIndex = target.selectedEpisodeIndex,
            page = target.page,
        )
        context.core.loadingUseBlackBackground = false
        loadCurrentGeneratorEpisodeSources(
            context = context,
            showLoadingState = true,
            clearPublicUiState = true,
        )
    }
}

private suspend fun resolveDownloadedPlaybackTarget(
    episodeId: Int,
    fallbackApiName: String,
): DownloadedPlaybackTarget? {
    val appContext = CloudStreamApp.context ?: return null
    val cachedEpisodes = appContext.getKeys(DOWNLOAD_EPISODE_CACHE)
        .mapNotNull { key ->
            appContext.getKey<VideoDownloadHelper.DownloadEpisodeCached>(key)
        }
    val selectedEpisode = cachedEpisodes.firstOrNull { episode ->
        episode.id == episodeId
    } ?: return null
    val header = appContext.getKey<VideoDownloadHelper.DownloadHeaderCached>(
        DOWNLOAD_HEADER_CACHE,
        selectedEpisode.parentId.toString()
    )

    val sourceName = header?.apiName?.ifBlank { null }
        ?: fallbackApiName.ifBlank { "Downloaded" }
    val title = header?.name?.ifBlank { null }
        ?: selectedEpisode.name?.ifBlank { null }
        ?: appContext.getString(R.string.downloaded_file)
    val episodeTitle = selectedEpisode.name
        ?.takeIf { name -> name.isNotBlank() && !name.equals(title, ignoreCase = true) }
    val season = selectedEpisode.season?.takeIf { value -> value > 0 }
    val episodeNumber = selectedEpisode.episode.takeIf { value -> value > 0 }
    val isEpisodeBased = header?.type?.isEpisodeBased() == true ||
        (season != null && episodeNumber != null)
    val tvType = header?.type ?: if (isEpisodeBased) TvType.TvSeries else TvType.Movie

    val downloadedLabel = appContext.getString(R.string.downloaded)

    val metadata = TvPlayerMetadata(
        title = title,
        subtitle = downloadedLabel,
        backdropUri = header?.backdrop ?: header?.poster ?: selectedEpisode.poster,
        apiName = sourceName,
        season = season,
        episode = episodeNumber,
        episodeTitle = episodeTitle,
        isEpisodeBased = isEpisodeBased,
    )

    val orderedEpisodes = (cachedEpisodes + selectedEpisode)
        .asSequence()
        .filter { episode -> episode.parentId == selectedEpisode.parentId }
        .distinctBy { episode -> episode.id }
        .sortedWith(
            compareBy<VideoDownloadHelper.DownloadEpisodeCached> { episode -> episode.season ?: 0 }
                .thenBy { episode -> episode.episode }
        )
        .toList()

    val extractorItems = mutableListOf<ExtractorUri>()
    val resultEpisodes = orderedEpisodes.mapIndexedNotNull { index, episode ->
        val downloadInfo = appContext.getKey<VideoDownloadManager.DownloadedFileInfo>(
            VideoDownloadManager.KEY_DOWNLOAD_INFO,
            episode.id.toString()
        ) ?: return@mapIndexedNotNull null

        extractorItems += ExtractorUri(
            uri = Uri.EMPTY,
            id = episode.id,
            parentId = episode.parentId,
            name = episode.name ?: title,
            season = episode.season,
            episode = episode.episode,
            headerName = title,
            tvType = tvType,
            basePath = downloadInfo.basePath,
            displayName = downloadInfo.displayName,
            relativePath = downloadInfo.relativePath,
        )

        val resolvedSeason = episode.season?.takeIf { value -> value > 0 }
        buildResultEpisode(
            headerName = title,
            name = episode.name ?: title,
            poster = episode.poster ?: header?.poster,
            episode = episode.episode,
            seasonIndex = resolvedSeason,
            season = resolvedSeason,
            data = "download://${episode.id}",
            apiName = sourceName,
            id = episode.id,
            index = index,
            description = episode.description,
            rating = episode.score,
            tvType = tvType,
            parentId = episode.parentId,
        )
    }

    if (extractorItems.isEmpty() || resultEpisodes.isEmpty()) {
        val fallbackFile = VideoDownloadManager.getDownloadFileInfoAndUpdateSettings(
            appContext,
            selectedEpisode.id
        ) ?: return null
        extractorItems += ExtractorUri(
            uri = fallbackFile.path,
            id = selectedEpisode.id,
            parentId = selectedEpisode.parentId,
            name = selectedEpisode.name ?: title,
            season = selectedEpisode.season,
            episode = selectedEpisode.episode,
            headerName = title,
            tvType = tvType,
        )
    }

    val selectedEpisodeIndex = resultEpisodes.indexOfFirst { episode ->
        episode.id == selectedEpisode.id
    }.takeIf { index -> index >= 0 } ?: 0

    return DownloadedPlaybackTarget(
        metadata = metadata,
        episodes = if (resultEpisodes.isEmpty()) {
            listOf(
                buildResultEpisode(
                    headerName = title,
                    name = episodeTitle ?: title,
                    poster = selectedEpisode.poster ?: header?.poster,
                    episode = episodeNumber ?: 0,
                    seasonIndex = season,
                    season = season,
                    data = "download://${selectedEpisode.id}",
                    apiName = sourceName,
                    id = selectedEpisode.id,
                    index = 0,
                    description = selectedEpisode.description,
                    rating = selectedEpisode.score,
                    tvType = tvType,
                    parentId = selectedEpisode.parentId,
                )
            )
        } else {
            resultEpisodes
        },
        selectedEpisodeIndex = selectedEpisodeIndex,
        extractorItems = extractorItems,
    )
}

private suspend fun applyDownloadedPlaybackTarget(
    context: PlayerScreenCoordinatorContext,
    target: DownloadedPlaybackTarget,
) {
    val selectedEpisode = target.episodes.getOrNull(target.selectedEpisodeIndex) ?: return
    context.core.baseMetadata = target.metadata.copy(
        season = null,
        episode = null,
        episodeTitle = null,
    )
    context.core.loadingUseBlackBackground = false
    context.core.metadata = resolveEpisodeMetadata(
        baseMetadata = context.core.baseMetadata,
        episode = selectedEpisode,
    )
    context.core.currentEpisode = selectedEpisode
    context.core.episodeQueue = target.episodes
    context.core.currentEpisodeIndex = target.selectedEpisodeIndex
    context.core.currentLoadResponse = null
    context.panels.subtitleEncodingController.resetNavigation()
    context.panels.onlineSubtitlesController.reset(
        query = defaultOnlineSubtitlesQuery(context),
    )
    context.core.playbackProgressState.onEpisodeChanged(
        episode = selectedEpisode,
        nextEpisode = target.episodes.getOrNull(target.selectedEpisodeIndex + 1),
    )
    context.catalog.generator = DownloadFileGenerator(
        episodes = target.extractorItems,
        currentIndex = target.selectedEpisodeIndex,
    )
    playerDiagnosticsLog(
        "offline playback prepared episodeId=${selectedEpisode.id} items=${target.extractorItems.size}" +
            " selectedIndex=${target.selectedEpisodeIndex}"
    )
    loadCurrentGeneratorEpisodeSources(
        context = context,
        showLoadingState = true,
        clearPublicUiState = true,
    )
}

private fun resetGeneratorLoadState(
    context: PlayerScreenCoordinatorContext,
    clearPublicUiState: Boolean,
) {
    context.catalog.store.reset()
    context.catalog.hasFinalized = false
    context.catalog.pendingInitialFinalizeJob?.cancel()
    context.catalog.pendingInitialFinalizeJob = null
    context.catalog.pendingReadyRefreshChanges = 0
    context.catalog.pendingReadyRefreshJob?.cancel()
    context.catalog.pendingReadyRefreshJob = null
    context.panels.stateHolder.onSourceChanged(newLink = null)
    context.panels.uiState.value = TvPlayerPanelsUiState()
    if (clearPublicUiState) {
        context.catalog.uiState.value = PlayerCatalogUiState()
    }
}

internal suspend fun loadCurrentGeneratorEpisodeSources(
    context: PlayerScreenCoordinatorContext,
    showLoadingState: Boolean,
    clearPublicUiState: Boolean,
    beforeFinalize: (() -> Unit)? = null,
) {
    val generator = context.catalog.generator ?: return
    var beforeFinalizeApplied = false

    fun applyBeforeFinalizeIfNeeded() {
        if (beforeFinalizeApplied) return
        beforeFinalize?.invoke()
        beforeFinalizeApplied = true
    }

    resetGeneratorLoadState(
        context = context,
        clearPublicUiState = clearPublicUiState,
    )

    if (showLoadingState) {
        postLoadingState(context)
    }

    try {
        withContext(Dispatchers.IO) {
            val loadingScope = this
            val subtitleEpisodeMetadata = context.core.currentEpisode.toSubtitleFetchEpisodeMetadataLog()
            generator.generateLinks(
                clearCache = false,
                sourceTypes = LOADTYPE_INAPP,
                callback = { (link, uri) ->
                    val playableLink = playableLinkFromGenerator(
                        extractorLink = link,
                        extractorUri = uri,
                    ) ?: return@generateLinks
                    if (playableLink.url.isBlank()) return@generateLinks
                    val inserted = context.catalog.store.insertLink(playableLink)
                    if (!inserted) return@generateLinks
                    loadingScope.launch(Dispatchers.Main.immediate) {
                        if (context.catalog.hasFinalized) {
                            onBackgroundDataInsertedAfterFinalize(context)
                        } else {
                            applyBeforeFinalizeIfNeeded()
                            schedulePendingInitialFinalize(context)
                            if (showLoadingState) {
                                postLoadingState(context)
                            }
                        }
                    }
                },
                subtitleCallback = { subtitle ->
                    val inserted = context.catalog.store.insertSubtitle(subtitle)
                    playerDiagnosticsLog(
                        "subtitle fetched [tv-generator]: inserted=$inserted " +
                            subtitle.toSubtitleFetchLogPayload() +
                            " $subtitleEpisodeMetadata",
                    )
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

    applyBeforeFinalizeIfNeeded()
    flushPendingReadyRefresh(
        context = context,
        force = true,
    )
    finalizeLoading(
        context = context,
        forceError = true,
    )
}

@Suppress("DEPRECATION_ERROR")
private fun playableLinkFromGenerator(
    extractorLink: ExtractorLink?,
    extractorUri: ExtractorUri?,
): ExtractorLink? {
    if (extractorLink != null) {
        return extractorLink
    }

    val resolvedUri = extractorUri?.uri?.takeIf { uri -> uri != Uri.EMPTY } ?: return null
    val resolvedSource = extractorUri.headerName
        ?.takeIf { source -> source.isNotBlank() }
        ?: extractorUri.name.takeIf { name -> name.isNotBlank() }
        ?: "Downloaded"
    val resolvedName = extractorUri.name.takeIf { name -> name.isNotBlank() } ?: resolvedSource

    return ExtractorLink(
        source = resolvedSource,
        name = resolvedName,
        url = resolvedUri.toString(),
        referer = "",
        quality = 0,
        type = INFER_TYPE,
    )
}

internal fun postLoadingState(context: PlayerScreenCoordinatorContext) {
    if (context.catalog.hasFinalized) return
    context.core.uiState.value = TvPlayerUiState.LoadingSources(
        metadata = resolveLoadingStateMetadata(context),
        loadedSources = context.catalog.store.loadedSourcesCount(),
        canSkip = context.catalog.store.hasLoadedSources(),
        useBlackBackground = context.core.loadingUseBlackBackground,
    )
}

private fun resolveLoadingStateMetadata(context: PlayerScreenCoordinatorContext): TvPlayerMetadata {
    val fallbackTitle = stringFromAppContext(
        context = context,
        resId = R.string.loading,
        fallback = "Loading...",
    )
    val currentMetadata = context.core.metadata
    val resolvedTitle = currentMetadata.title.takeIf { title ->
        title.isNotBlank()
    } ?: context.core.currentEpisode?.name?.takeIf { episodeName ->
        episodeName.isNotBlank()
    } ?: context.core.currentEpisode?.headerName?.takeIf { headerName ->
        headerName.isNotBlank()
    } ?: fallbackTitle

    return currentMetadata.copy(title = resolvedTitle)
}

internal fun schedulePendingInitialFinalize(context: PlayerScreenCoordinatorContext) {
    if (context.catalog.hasFinalized || !context.catalog.store.hasLoadedSources()) {
        return
    }

    context.catalog.pendingInitialFinalizeJob?.cancel()
    context.catalog.pendingInitialFinalizeJob = context.coroutineScope.launch {
        delay(InitialFinalizeQuietWindowMs)
        context.catalog.pendingInitialFinalizeJob = null
        if (context.catalog.hasFinalized || !context.catalog.store.hasLoadedSources()) {
            return@launch
        }

        playerDiagnosticsLog(
            "initial finalize quiet window elapsed:" +
                " loadedSources=${context.catalog.store.loadedSourcesCount()}",
        )
        finalizeLoading(
            context = context,
            forceError = false,
        )
    }
}

internal fun onBackgroundDataInsertedAfterFinalize(context: PlayerScreenCoordinatorContext) {
    if (!context.catalog.hasFinalized) return

    context.catalog.pendingReadyRefreshChanges += 1
    playerDiagnosticsLog(
        "background player data inserted after finalize:" +
            " pendingChanges=${context.catalog.pendingReadyRefreshChanges}" +
            " selectionPanelOpen=${isSelectionPanelOpen(context)}",
    )

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

    playerDiagnosticsLog(
        "flushPendingReadyRefresh: force=$force pendingChanges=${context.catalog.pendingReadyRefreshChanges}",
    )
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
    playerDiagnosticsLog(
        "refreshReadyStateFromLoadedData: subtitles=${context.catalog.store.orderedSubtitles.size}" +
            " links=${context.catalog.store.orderedLinks.size}" +
            " currentIndex=${context.catalog.store.currentLinkIndex}",
    )
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
    context.catalog.pendingInitialFinalizeJob?.cancel()
    context.catalog.pendingInitialFinalizeJob = null

    val rebuilt = context.catalog.store.rebuildOrderedData(currentUrl = null)
    if (rebuilt) {
        val firstLink = context.catalog.store.markFirstLinkLoading() ?: return
        context.panels.stateHolder.onSourceChanged(firstLink)
        context.catalog.hasFinalized = true
        context.core.loadingUseBlackBackground = false
        postReadyState(
            context = context,
            link = firstLink,
            currentIndex = 0,
        )
        return
    }

    if (forceError) {
        context.catalog.hasFinalized = true
        context.core.loadingUseBlackBackground = false
        val uiError = playerUiErrorFromRes(R.string.no_links_found_toast)
        context.core.uiState.value = TvPlayerUiState.Error(
            metadata = context.core.metadata,
            error = uiError,
        )
        reportPlayerHandledError(
            metadata = context.core.metadata,
            uiMessage = uiError.message,
            eventMessage = "TV player exhausted all links during finalizeLoading",
        )
    }
}
