package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.net.Uri
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelItemAction
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSourceErrorDialog
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSourceStatus
import com.lagradost.cloudstream3.ui.player.PlayerSubtitleHelper.Companion.toSubtitleMimeType
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.player.SubtitleOrigin
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.safefile.SafeFile

internal fun openPanel(
    context: PlayerScreenCoordinatorContext,
    panel: TvPlayerSidePanel,
) {
    if (!context.catalog.hasFinalized) return
    if (context.panels.stateHolder.openPanel(panel)) {
        if (panel == TvPlayerSidePanel.Subtitles) {
            context.panels.onlineSubtitlesController.resetNavigation()
            postReadyStateForCurrentLink(context)
        } else {
            refreshPanelsUiStateForCurrentLink(context)
        }
    }
}

internal fun closePanel(context: PlayerScreenCoordinatorContext) {
    if (!context.catalog.hasFinalized) return
    val wasSubtitlesPanelOpen =
        context.panels.uiState.value.activePanel == TvPlayerSidePanel.Subtitles
    if (context.panels.stateHolder.closePanel()) {
        context.panels.onlineSubtitlesController.resetNavigation()
        if (wasSubtitlesPanelOpen) {
            postReadyStateForCurrentLink(context)
        } else {
            updateReadyStateActivePanel(
                context = context,
                panel = TvPlayerSidePanel.None,
            )
        }
        if (context.catalog.pendingReadyRefreshChanges > 0) {
            flushPendingReadyRefresh(
                context = context,
                force = false,
            )
        }
        return
    }

    if (context.panels.uiState.value.activePanel != TvPlayerSidePanel.None) {
        updateReadyStateActivePanel(
            context = context,
            panel = TvPlayerSidePanel.None,
        )
    }
}

internal fun disableSubtitlesFromPlaybackError(context: PlayerScreenCoordinatorContext) {
    if (!context.catalog.hasFinalized) return
    if (context.panels.stateHolder.disableSubtitlesFromPlaybackError()) {
        context.panels.onlineSubtitlesController.resetNavigation()
        postReadyStateForCurrentLink(context)
    }
}

internal fun onPanelItemAction(
    context: PlayerScreenCoordinatorContext,
    action: TvPlayerPanelItemAction,
) {
    if (!context.catalog.hasFinalized) return

    when (action) {
        TvPlayerPanelItemAction.LoadSubtitleFromFile -> {
            context.emitPanelEffect(TvPlayerPanelEffect.OpenSubtitleFilePicker)
            return
        }
        TvPlayerPanelItemAction.OpenOnlineSubtitles -> {
            context.panels.onlineSubtitlesController.openOnlineSubtitlesPanel()
            postReadyStateForCurrentLink(context)
            return
        }
        TvPlayerPanelItemAction.LoadFirstAvailableSubtitle -> {
            closePanel(context)
            context.panels.onlineSubtitlesController.loadFirstAvailableSubtitle()
            return
        }
        TvPlayerPanelItemAction.BackFromOnlineSubtitles -> {
            if (context.panels.onlineSubtitlesController.navigateBack()) {
                postReadyStateForCurrentLink(context)
            }
            return
        }
        TvPlayerPanelItemAction.EditOnlineSubtitlesQuery -> return
        is TvPlayerPanelItemAction.UpdateOnlineSubtitlesQuery -> {
            context.panels.onlineSubtitlesController.onQueryUpdated(action.query)
            return
        }
        TvPlayerPanelItemAction.SelectOnlineSubtitlesLanguage -> {
            if (context.panels.onlineSubtitlesController.openOnlineSubtitlesLanguagePanel()) {
                postReadyStateForCurrentLink(context)
            }
            return
        }
        is TvPlayerPanelItemAction.SelectOnlineSubtitlesLanguageOption -> {
            val navigationChanged = context.panels.onlineSubtitlesController
                .selectLanguageAndReturnToSearch(action.languageTag)
            if (navigationChanged) {
                postReadyStateForCurrentLink(context)
            }
            return
        }
        TvPlayerPanelItemAction.RetryOnlineSubtitlesSearch -> {
            context.panels.onlineSubtitlesController.retrySearch()
            return
        }
        is TvPlayerPanelItemAction.SelectOnlineSubtitleResult -> {
            context.panels.onlineSubtitlesController.selectOnlineSubtitleResult(action.resultId)
            return
        }
        is TvPlayerPanelItemAction.InspectSourceError -> {
            openSourceErrorDialog(
                context = context,
                index = action.index,
            )
            return
        }
        else -> Unit
    }

    val outcome = context.panels.stateHolder.onPanelItemAction(
        action = action,
        currentLink = context.catalog.store.currentLink(),
        subtitles = context.catalog.store.orderedSubtitles,
    )
    val selectedSourceIndex = outcome.selectedSourceIndex
    if (selectedSourceIndex != null) {
        selectSource(
            context = context,
            index = selectedSourceIndex,
        )
        return
    }

    when (action) {
        TvPlayerPanelItemAction.DisableSubtitles,
        is TvPlayerPanelItemAction.SelectSubtitle,
        TvPlayerPanelItemAction.SelectDefaultTrack,
        is TvPlayerPanelItemAction.SelectTrack -> {
            context.panels.onlineSubtitlesController.resetNavigation()
        }
        else -> Unit
    }

    if (outcome.stateChanged) {
        postReadyStateForCurrentLink(context)
    }
}

internal fun onSubtitleFileSelected(
    context: PlayerScreenCoordinatorContext,
    uri: Uri?,
) {
    if (!context.catalog.hasFinalized) return
    val subtitleUri = uri ?: return

    val appContext = CloudStreamApp.context ?: return
    val subtitleName = runCatching {
        SafeFile.fromUri(appContext, subtitleUri)?.name()
    }.getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: subtitleUri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
        ?: subtitleUri.toString()

    val subtitle = SubtitleData(
        originalName = subtitleName,
        nameSuffix = "",
        url = subtitleUri.toString(),
        origin = SubtitleOrigin.DOWNLOADED_FILE,
        mimeType = subtitleName.toSubtitleMimeType(),
        headers = emptyMap(),
        languageCode = null,
    )

    context.catalog.store.insertSubtitle(subtitle)
    context.catalog.store.refreshOrderedSubtitles()
    context.panels.stateHolder.selectSubtitleById(subtitle.getId())
    context.panels.onlineSubtitlesController.resetNavigation()
    postReadyStateForCurrentLink(context)
}

internal fun onSubtitlesSidePanelBackPressed(context: PlayerScreenCoordinatorContext): Boolean {
    if (!context.catalog.hasFinalized) return false
    if (context.panels.uiState.value.activePanel != TvPlayerSidePanel.Subtitles) {
        return false
    }
    val navigatedBack = context.panels.onlineSubtitlesController.navigateBack()
    if (navigatedBack) {
        postReadyStateForCurrentLink(context)
    }
    return navigatedBack
}

internal fun openSourceErrorDialog(
    context: PlayerScreenCoordinatorContext,
    index: Int,
) {
    val link = context.catalog.store.linkAt(index) ?: return
    val sourceState = context.catalog.store.snapshotSourceStates()[link.url]
    if (sourceState?.status != TvPlayerSourceStatus.Error) {
        selectSource(
            context = context,
            index = index,
        )
        return
    }

    context.emitPanelEffect(
        TvPlayerPanelEffect.OpenSourceErrorDialog(
            dialog = TvPlayerSourceErrorDialog(
                sourceIndex = index,
                sourceLabel = sourceDisplayLabel(
                    link = link,
                    index = index,
                ),
                message = buildSourceErrorMessage(
                    context = context,
                    httpCode = sourceState.httpCode,
                ),
            ),
        )
    )
}

internal fun buildSourceErrorMessage(
    context: PlayerScreenCoordinatorContext,
    httpCode: Int?,
): String {
    return if (httpCode != null) {
        val template = stringFromAppContext(
            context = context,
            resId = R.string.tv_player_source_error_dialog_http_code,
            fallback = "This source couldn't be loaded (HTTP %1\$d).",
        )
        runCatching {
            template.format(httpCode)
        }.getOrElse {
            "This source couldn't be loaded (HTTP $httpCode)."
        }
    } else {
        stringFromAppContext(
            context = context,
            resId = R.string.tv_player_source_error_dialog_unavailable,
            fallback = "This source is currently unavailable.",
        )
    }
}

internal fun sourceDisplayLabel(
    link: ExtractorLink,
    index: Int,
): String {
    return link.name
        .takeIf { name -> name.isNotBlank() }
        ?: link.source
            .takeIf { source -> source.isNotBlank() }
        ?: "Source ${index + 1}"
}
