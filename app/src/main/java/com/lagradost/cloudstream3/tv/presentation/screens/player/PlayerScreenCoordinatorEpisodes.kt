package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.player.LOADTYPE_INAPP
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun onPlaybackEnded(
    context: PlayerScreenCoordinatorContext,
    positionMs: Long,
    durationMs: Long,
) {
    if (!context.catalog.hasFinalized) return
    if (!isAutoplayNextEpisodeEnabled()) return

    playNextEpisode(
        context = context,
        positionMs = positionMs,
        durationMs = durationMs,
    )
}

internal fun playNextEpisode(
    context: PlayerScreenCoordinatorContext,
    positionMs: Long,
    durationMs: Long,
) {
    if (!context.catalog.hasFinalized) return
    val nextEpisodeIndex = resolveNextEpisodeIndex(
        currentEpisodeIndex = context.core.currentEpisodeIndex,
        totalEpisodes = context.core.episodeQueue.size,
    ) ?: return
    val nextEpisode = context.core.episodeQueue.getOrNull(nextEpisodeIndex) ?: return
    val generator = context.catalog.generator ?: return

    context.core.playbackProgressState.onPlaybackStopped(
        positionMs = positionMs,
        durationMs = durationMs,
    )
    context.core.playbackProgressState.setPersistenceEnabled(false)
    context.core.currentEpisodeIndex = nextEpisodeIndex
    context.core.currentEpisode = nextEpisode
    context.core.metadata = resolveEpisodeLoadingMetadata(
        context = context,
        episode = nextEpisode,
    )
    context.core.loadingUseBlackBackground = true
    context.panels.subtitleEncodingController.resetNavigation()
    context.panels.onlineSubtitlesController.reset(
        query = defaultOnlineSubtitlesQuery(context),
    )
    context.core.playbackProgressState.onEpisodeChanged(
        episode = nextEpisode,
        nextEpisode = context.core.episodeQueue.getOrNull(nextEpisodeIndex + 1),
    )
    context.core.uiState.value = TvPlayerUiState.LoadingSources(
        metadata = context.core.metadata,
        loadedSources = 0,
        canSkip = false,
        useBlackBackground = true,
    )
    context.catalog.hasFinalized = false
    context.catalog.pendingInitialFinalizeJob?.cancel()
    context.catalog.pendingInitialFinalizeJob = null
    context.catalog.pendingReadyRefreshJob?.cancel()
    context.catalog.pendingReadyRefreshJob = null
    context.catalog.pendingReadyRefreshChanges = 0

    context.catalog.loadingJob?.cancel()
    context.catalog.prefetchJob?.cancel()
    context.catalog.prefetchJob = null
    context.catalog.prefetchingFromEpisodeId = null

    context.catalog.loadingJob = context.coroutineScope.launch {
        generator.goto(nextEpisodeIndex)
        loadCurrentGeneratorEpisodeSources(
            context = context,
            showLoadingState = true,
            clearPublicUiState = true,
        )
    }
}

private fun resolveEpisodeLoadingMetadata(
    context: PlayerScreenCoordinatorContext,
    episode: com.lagradost.cloudstream3.ui.result.ResultEpisode,
): TvPlayerMetadata {
    val fallbackTitle = stringFromAppContext(
        context = context,
        resId = R.string.loading,
        fallback = "Loading...",
    )
    val resolvedMetadata = resolveEpisodeMetadata(
        baseMetadata = context.core.baseMetadata,
        episode = episode,
    )
    val resolvedTitle = resolvedMetadata.title.takeIf { title ->
        title.isNotBlank()
    } ?: episode.headerName.takeIf { headerName ->
        headerName.isNotBlank()
    } ?: episode.name?.takeIf { name ->
        name.isNotBlank()
    } ?: fallbackTitle

    return resolvedMetadata.copy(title = resolvedTitle)
}

internal fun maybePrefetchNextEpisode(
    context: PlayerScreenCoordinatorContext,
    positionMs: Long,
    durationMs: Long,
) {
    if (!context.catalog.hasFinalized) return
    if (!shouldPrefetchNextEpisode(positionMs = positionMs, durationMs = durationMs)) return

    val generator = context.catalog.generator ?: return
    if (!generator.hasCache || !generator.hasNext()) return

    val currentEpisodeId = context.core.currentEpisode?.id ?: return
    if (context.catalog.prefetchedFromEpisodeId == currentEpisodeId ||
        context.catalog.prefetchingFromEpisodeId == currentEpisodeId
    ) {
        return
    }

    context.catalog.prefetchJob?.cancel()
    context.catalog.prefetchingFromEpisodeId = currentEpisodeId
    context.catalog.prefetchJob = context.coroutineScope.launch {
        try {
            withContext(Dispatchers.IO) {
                generator.generateLinks(
                    clearCache = false,
                    sourceTypes = LOADTYPE_INAPP,
                    callback = {},
                    subtitleCallback = {},
                    offset = 1,
                )
            }
            context.catalog.prefetchedFromEpisodeId = currentEpisodeId
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logError(throwable)
        } finally {
            if (context.catalog.prefetchingFromEpisodeId == currentEpisodeId) {
                context.catalog.prefetchingFromEpisodeId = null
            }
            context.catalog.prefetchJob = null
        }
    }
}

private fun isAutoplayNextEpisodeEnabled(): Boolean {
    val appContext = CloudStreamApp.context ?: return true
    val settingsManager = PreferenceManager.getDefaultSharedPreferences(appContext) ?: return true
    return settingsManager.getBoolean(
        appContext.getString(R.string.autoplay_next_key),
        true,
    )
}
