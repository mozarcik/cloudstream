package com.lagradost.cloudstream3.tv.presentation.screens.player

import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPlaybackErrorDetails
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSourceState
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSourceStatus

internal fun onPlaybackProgress(
    context: PlayerScreenCoordinatorContext,
    positionMs: Long,
    durationMs: Long,
) {
    context.core.playbackProgressState.onPlaybackProgress(
        positionMs = positionMs,
        durationMs = durationMs,
    )
}

internal fun onPlaybackStopped(
    context: PlayerScreenCoordinatorContext,
    positionMs: Long,
    durationMs: Long,
) {
    context.core.playbackProgressState.onPlaybackStopped(
        positionMs = positionMs,
        durationMs = durationMs,
    )
}

internal fun selectSource(
    context: PlayerScreenCoordinatorContext,
    index: Int,
    forceReloadCurrent: Boolean = false,
) {
    if (!context.catalog.hasFinalized) return
    val selectedLink = context.catalog.store.linkAt(index) ?: return

    if (!forceReloadCurrent && index == context.catalog.store.currentLinkIndex) {
        closePanel(context)
        return
    }

    context.catalog.store.updateSourceState(
        url = selectedLink.url,
        state = TvPlayerSourceState(status = TvPlayerSourceStatus.Loading),
    )
    context.panels.stateHolder.onSourceChanged(selectedLink)
    context.panels.onlineSubtitlesController.resetNavigation()
    context.catalog.store.setCurrentLinkIndex(index)
    postReadyState(
        context = context,
        link = selectedLink,
        currentIndex = index,
    )
}

internal fun retrySource(
    context: PlayerScreenCoordinatorContext,
    index: Int,
) {
    if (!context.catalog.hasFinalized) return
    selectSource(
        context = context,
        index = index,
        forceReloadCurrent = true,
    )
}

internal fun onPlaybackReady(context: PlayerScreenCoordinatorContext) {
    if (!context.catalog.hasFinalized) return
    val currentLink = context.catalog.store.currentLink() ?: return
    val stateUpdated = context.catalog.store.updateSourceState(
        url = currentLink.url,
        state = TvPlayerSourceState(status = TvPlayerSourceStatus.Success),
    )
    if (stateUpdated) {
        postReadyStateForCurrentLink(context)
    }
}

internal fun onPlaybackError(
    context: PlayerScreenCoordinatorContext,
    error: TvPlayerPlaybackErrorDetails?,
) {
    if (!context.catalog.hasFinalized) return

    val currentLink = context.catalog.store.currentLink()
    if (currentLink != null) {
        context.catalog.store.updateSourceState(
            url = currentLink.url,
            state = TvPlayerSourceState(
                status = TvPlayerSourceStatus.Error,
                httpCode = error?.httpCode,
            ),
        )
    }

    val nextIndex = context.catalog.store.currentLinkIndex + 1
    val nextLink = context.catalog.store.linkAt(nextIndex)
    if (nextLink != null) {
        notifySourceAutoFallback(
            context = context,
            failedLink = currentLink,
        )
        context.catalog.store.updateSourceState(
            url = nextLink.url,
            state = TvPlayerSourceState(status = TvPlayerSourceStatus.Loading),
        )
        context.panels.stateHolder.onSourceChanged(
            newLink = nextLink,
            preserveSourcesPanel = true,
        )
        context.panels.onlineSubtitlesController.resetNavigation()
        context.catalog.store.setCurrentLinkIndex(nextIndex)
        postReadyState(
            context = context,
            link = nextLink,
            currentIndex = nextIndex,
        )
        return
    }

    context.core.uiState.value = TvPlayerUiState.Error(
        metadata = context.core.metadata,
        messageResId = R.string.no_links_found_toast,
    )
}

private fun notifySourceAutoFallback(
    context: PlayerScreenCoordinatorContext,
    failedLink: com.lagradost.cloudstream3.utils.ExtractorLink?,
) {
    if (context.panels.uiState.value.activePanel == TvPlayerSidePanel.Sources) {
        return
    }

    val sourceLabel = failedLink?.name
        ?.takeIf { name -> name.isNotBlank() }
        ?: failedLink?.source
            ?.takeIf { source -> source.isNotBlank() }

    val template = stringFromAppContext(
        context = context,
        resId = R.string.tv_player_source_trying_next,
        fallback = "Source \"%1\$s\" failed. Trying the next source…",
    )
    val message = sourceLabel?.let { label ->
        runCatching {
            template.format(label)
        }.getOrElse {
            "Source \"$label\" failed. Trying the next source…"
        }
    } ?: stringFromAppContext(
        context = context,
        resId = R.string.tv_player_source_trying_next_generic,
        fallback = "This source failed. Trying the next one…",
    )

    context.emitPanelEffect(TvPlayerPanelEffect.ShowMessage(message))
}
