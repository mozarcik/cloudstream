package com.lagradost.cloudstream3.tv.presentation.screens.player

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
import com.lagradost.cloudstream3.ui.player.LOADTYPE_INAPP
import com.lagradost.cloudstream3.ui.player.RepoLinkGenerator
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
import com.lagradost.cloudstream3.utils.newExtractorLink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DebugTag = "TvPlayerVM"
private const val ReadyRefreshBatchSize = 20
private const val ReadyRefreshDebounceMs = 900L

private data class DownloadedPlaybackTarget(
    val metadata: TvPlayerMetadata,
    val episode: ResultEpisode,
    val links: List<ExtractorLink>,
    val selectedUrl: String?,
)

private fun playerDiagnosticsLog(message: String) {
    Log.i(DebugTag, message)
}

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
    val rawEpisodeData = context.savedStateHandle
        .get<String>(PlayerScreenNavigation.EpisodeDataBundleKey)
    val downloadEpisodeId = PlayerScreenNavigation.parseDownloadedEpisodeId(rawEpisodeData)
    val episodeData = rawEpisodeData
        ?.takeIf { value -> value.isNotBlank() && downloadEpisodeId == null }

    if (downloadEpisodeId == null && (url.isBlank() || apiName.isBlank())) {
        Log.e(DebugTag, "missing args: url=$url apiName=$apiName")
        context.core.uiState.value = TvPlayerUiState.Error(
            metadata = TvPlayerMetadata.Empty,
            messageResId = R.string.error_loading_links_toast,
        )
        return
    }

    context.catalog.loadingJob = context.coroutineScope.launch {
        if (downloadEpisodeId != null) {
            val downloadedTarget = withContext(Dispatchers.IO) {
                resolveDownloadedPlaybackTarget(
                    episodeId = downloadEpisodeId,
                    fallbackApiName = apiName,
                )
            }
            if (downloadedTarget == null) {
                Log.e(DebugTag, "downloaded playback target unavailable episodeId=$downloadEpisodeId")
                context.core.uiState.value = TvPlayerUiState.Error(
                    metadata = TvPlayerMetadata(
                        title = apiName.ifBlank { "Downloads" },
                        subtitle = "",
                        backdropUri = null,
                        apiName = apiName,
                    ),
                    messageResId = R.string.no_links_found_toast,
                )
                return@launch
            }

            applyDownloadedPlaybackTarget(
                context = context,
                target = downloadedTarget,
                episodeId = downloadEpisodeId,
            )
            return@launch
        }

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
                            playerDiagnosticsLog(
                                "subtitle inserted: finalized=${context.catalog.hasFinalized}" +
                                    " id=${subtitle.getId()}" +
                                    " name=${subtitle.name}" +
                                    " language=${subtitle.languageCode ?: "null"}",
                            )
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

    val seasonLabel = appContext.getString(R.string.season)
    val episodeLabel = appContext.getString(R.string.episode)
    val downloadedLabel = appContext.getString(R.string.downloaded)
    val seasonEpisodeLabel = when {
        season != null && episodeNumber != null -> "$seasonLabel $season $episodeLabel $episodeNumber"
        season != null -> "$seasonLabel $season"
        episodeNumber != null -> "$episodeLabel $episodeNumber"
        else -> null
    }

    val metadata = TvPlayerMetadata(
        title = title,
        subtitle = listOfNotNull(seasonEpisodeLabel, downloadedLabel).joinToString(" . "),
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

    val links = mutableListOf<ExtractorLink>()
    var selectedUrl: String? = null
    orderedEpisodes.forEach { episode ->
        val fileInfo = VideoDownloadManager.getDownloadFileInfoAndUpdateSettings(
            appContext,
            episode.id
        ) ?: return@forEach

        val link = newExtractorLink(
            source = sourceName,
            name = episode.name?.ifBlank { title } ?: title,
            url = fileInfo.path.toString(),
            type = INFER_TYPE,
        ) {
            this.quality = 0
            this.referer = ""
        }
        links += link
        if (episode.id == selectedEpisode.id) {
            selectedUrl = link.url
        }
    }

    if (links.isEmpty()) {
        return null
    }

    val episodeIndex = orderedEpisodes.indexOfFirst { episode ->
        episode.id == selectedEpisode.id
    }.coerceAtLeast(0)
    val resultEpisode = buildResultEpisode(
        headerName = title,
        name = episodeTitle ?: title,
        poster = selectedEpisode.poster ?: header?.poster,
        episode = episodeNumber ?: 0,
        seasonIndex = season,
        season = season,
        data = "download://${selectedEpisode.id}",
        apiName = sourceName,
        id = selectedEpisode.id,
        index = episodeIndex,
        description = selectedEpisode.description,
        tvType = tvType,
        parentId = selectedEpisode.parentId,
    )

    return DownloadedPlaybackTarget(
        metadata = metadata,
        episode = resultEpisode,
        links = links,
        selectedUrl = selectedUrl,
    )
}

private fun applyDownloadedPlaybackTarget(
    context: PlayerScreenCoordinatorContext,
    target: DownloadedPlaybackTarget,
    episodeId: Int,
) {
    context.catalog.store.reset()
    target.links.forEach { link ->
        context.catalog.store.insertLink(link)
    }

    context.core.metadata = target.metadata
    context.core.currentEpisode = target.episode
    context.core.currentLoadResponse = null
    context.panels.onlineSubtitlesController.reset(
        query = defaultOnlineSubtitlesQuery(context),
    )
    context.core.playbackProgressState.onEpisodeChanged(target.episode)

    val rebuilt = context.catalog.store.rebuildOrderedData(
        currentUrl = target.selectedUrl
    )
    if (!rebuilt) {
        context.catalog.hasFinalized = true
        context.core.uiState.value = TvPlayerUiState.Error(
            metadata = target.metadata,
            messageResId = R.string.no_links_found_toast,
        )
        return
    }

    val selectedIndex = target.selectedUrl
        ?.let(context.catalog.store::indexOfUrl)
        ?.takeIf { index -> index >= 0 }
        ?: 0
    context.catalog.store.setCurrentLinkIndex(selectedIndex)
    val selectedLink = context.catalog.store.linkAt(selectedIndex)
        ?: context.catalog.store.orderedLinks.firstOrNull()
        ?: run {
            context.catalog.hasFinalized = true
            context.core.uiState.value = TvPlayerUiState.Error(
                metadata = target.metadata,
                messageResId = R.string.no_links_found_toast,
            )
            return
        }

    context.panels.stateHolder.onSourceChanged(selectedLink)
    context.catalog.hasFinalized = true
    playerDiagnosticsLog(
        "offline playback prepared episodeId=$episodeId links=${target.links.size} selectedIndex=$selectedIndex"
    )
    postReadyState(
        context = context,
        link = selectedLink,
        currentIndex = selectedIndex,
    )
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
