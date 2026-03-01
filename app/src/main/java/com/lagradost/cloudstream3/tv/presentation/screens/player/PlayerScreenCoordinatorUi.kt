package com.lagradost.cloudstream3.tv.presentation.screens.player

import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.LoadResponse.Companion.getAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.getImdbId
import com.lagradost.cloudstream3.LoadResponse.Companion.getMalId
import com.lagradost.cloudstream3.LoadResponse.Companion.getTMDbId
import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities.SubtitleSearch
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSourceState
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSourceStatus
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.collections.immutable.toPersistentList

internal fun postReadyState(
    context: PlayerScreenCoordinatorContext,
    link: ExtractorLink,
    currentIndex: Int,
) {
    val sourceStatesSnapshot = context.catalog.store.snapshotSourceStates()
    val isCurrentSourceReady =
        sourceStatesSnapshot[link.url]?.status == TvPlayerSourceStatus.Success
    if (isCurrentSourceReady) {
        context.panels.stateHolder.applyPreferredSubtitleAutoSelection(
            context.catalog.store.orderedSubtitles,
        )
    }
    val panelSelection = context.panels.stateHolder.selection(
        currentLink = link,
        subtitles = context.catalog.store.orderedSubtitles,
    )
    // WHY: player powinien wystartować najpierw na samym źródle.
    // Dopiero po sukcesie odtwarzania udostępniamy/subskrybujemy napisy w UI i playerze.
    val subtitlesForUi = if (isCurrentSourceReady) context.catalog.store.orderedSubtitles else emptyList()
    val selectedSubtitleIndexForUi = if (isCurrentSourceReady) {
        panelSelection.selectedSubtitleIndex
    } else {
        -1
    }
    val currentPanelsUiState = context.panels.uiState.value
    val shouldRefreshPanelsUiState =
        panelSelection.activePanel != TvPlayerSidePanel.None ||
            currentPanelsUiState.activePanel != TvPlayerSidePanel.None
    if (shouldRefreshPanelsUiState) {
        context.panels.uiState.value = buildPanelsUiState(
            context = context,
            link = link,
            currentIndex = currentIndex,
            sourceStatesSnapshot = sourceStatesSnapshot,
            subtitlesForUi = subtitlesForUi,
        )
    }
    context.catalog.uiState.value = PlayerCatalogUiState(
        sourceCount = context.catalog.store.orderedLinks.size,
        sources = context.catalog.store.orderedLinks.toPersistentList(),
        currentSourceIndex = currentIndex,
        subtitles = subtitlesForUi.toPersistentList(),
        selectedSubtitleIndex = selectedSubtitleIndexForUi,
        selectedAudioTrackIndex = panelSelection.selectedAudioTrackIndex,
    )
    val episodeId = context.core.currentEpisode?.id ?: -1
    context.core.uiState.value = TvPlayerUiState.Ready(
        metadata = context.core.metadata,
        link = link,
        episodeId = episodeId,
        resumePositionMs = context.core.playbackProgressState.resumePositionMs,
    )
}

internal fun updateCatalogUiState(
    context: PlayerScreenCoordinatorContext,
    currentIndex: Int,
    link: ExtractorLink,
) {
    val sourceStatesSnapshot = context.catalog.store.snapshotSourceStates()
    val isCurrentSourceReady =
        sourceStatesSnapshot[link.url]?.status == TvPlayerSourceStatus.Success
    if (isCurrentSourceReady) {
        context.panels.stateHolder.applyPreferredSubtitleAutoSelection(
            context.catalog.store.orderedSubtitles,
        )
    }
    val panelSelection = context.panels.stateHolder.selection(
        currentLink = link,
        subtitles = context.catalog.store.orderedSubtitles,
    )
    val subtitlesForUi = if (isCurrentSourceReady) context.catalog.store.orderedSubtitles else emptyList()
    val selectedSubtitleIndexForUi = if (isCurrentSourceReady) {
        panelSelection.selectedSubtitleIndex
    } else {
        -1
    }
    context.catalog.uiState.value = PlayerCatalogUiState(
        sourceCount = context.catalog.store.orderedLinks.size,
        sources = context.catalog.store.orderedLinks.toPersistentList(),
        currentSourceIndex = currentIndex,
        subtitles = subtitlesForUi.toPersistentList(),
        selectedSubtitleIndex = selectedSubtitleIndexForUi,
        selectedAudioTrackIndex = panelSelection.selectedAudioTrackIndex,
    )
}

internal fun postReadyStateForCurrentLink(context: PlayerScreenCoordinatorContext) {
    if (!context.catalog.hasFinalized) return
    val link = context.catalog.store.currentLink() ?: return
    postReadyState(
        context = context,
        link = link,
        currentIndex = context.catalog.store.currentLinkIndex,
    )
}

internal fun createSubtitleSearchRequest(
    context: PlayerScreenCoordinatorContext,
    query: String,
    languageTag: String?,
): SubtitleSearch {
    return SubtitleSearch(
        query = query,
        lang = languageTag?.ifBlank { null },
        imdbId = context.core.currentLoadResponse?.getImdbId(),
        tmdbId = context.core.currentLoadResponse?.getTMDbId()?.toIntOrNull(),
        malId = context.core.currentLoadResponse?.getMalId()?.toIntOrNull(),
        aniListId = context.core.currentLoadResponse?.getAniListId()?.toIntOrNull(),
        epNumber = context.core.currentEpisode?.episode,
        seasonNumber = context.core.currentEpisode?.season,
        year = context.core.metadata.year,
    )
}

internal fun defaultOnlineSubtitlesQuery(context: PlayerScreenCoordinatorContext): String {
    return context.core.metadata.title.takeIf { title ->
        title.isNotBlank()
    } ?: context.core.currentEpisode?.headerName?.takeIf { headerName ->
        headerName.isNotBlank()
    } ?: ""
}

internal fun applyOnlineSubtitlesSelection(
    context: PlayerScreenCoordinatorContext,
    downloadedSubtitles: List<SubtitleData>,
) {
    if (downloadedSubtitles.isEmpty()) {
        return
    }

    downloadedSubtitles.forEach { subtitle ->
        context.catalog.store.insertSubtitle(subtitle)
    }
    context.catalog.store.refreshOrderedSubtitles()

    val selectedSubtitleId = downloadedSubtitles.firstNotNullOfOrNull { subtitle ->
        context.catalog.store.orderedSubtitles.firstOrNull { existing ->
            existing.getId() == subtitle.getId()
        }?.getId()
    } ?: downloadedSubtitles.first().getId()
    context.panels.stateHolder.selectSubtitleById(selectedSubtitleId)
    context.panels.onlineSubtitlesController.resetNavigation()
    postReadyStateForCurrentLink(context)
}

internal fun refreshReadyStateIfSubtitlesPanelVisible(context: PlayerScreenCoordinatorContext) {
    if (context.panels.uiState.value.activePanel != TvPlayerSidePanel.Subtitles) {
        return
    }
    postReadyStateForCurrentLink(context)
}

internal fun stringFromAppContext(
    context: PlayerScreenCoordinatorContext,
    resId: Int,
    fallback: String,
): String {
    return CloudStreamApp.context?.getString(resId) ?: fallback
}

internal fun updateReadyStateActivePanel(
    context: PlayerScreenCoordinatorContext,
    panel: TvPlayerSidePanel,
) {
    val currentState = context.panels.uiState.value
    if (currentState.activePanel == panel) return
    context.panels.uiState.value = currentState.copy(activePanel = panel)
}

internal fun isSelectionPanelOpen(context: PlayerScreenCoordinatorContext): Boolean {
    return context.panels.uiState.value.activePanel != TvPlayerSidePanel.None
}

internal fun buildPanelsUiState(
    context: PlayerScreenCoordinatorContext,
    link: ExtractorLink,
    currentIndex: Int,
    sourceStatesSnapshot: Map<String, TvPlayerSourceState>,
    subtitlesForUi: List<SubtitleData>,
): TvPlayerPanelsUiState {
    val onlinePanelContent = context.panels.onlineSubtitlesController.buildPanelContent()
    val hasOnlineSubtitleProviders = context.panels.onlineSubtitlesController.hasOnlineSubtitleProviders()
    val canLoadFirstAvailableSubtitle = context.panels.onlineSubtitlesController.canLoadFirstAvailableSubtitle()
    val basePanelsState = context.panels.stateHolder.buildPanelsUiState(
        orderedLinks = context.catalog.store.orderedLinks,
        currentSourceIndex = currentIndex,
        sourceStates = sourceStatesSnapshot,
        currentLink = link,
        subtitles = subtitlesForUi,
        showOnlineSubtitleActions = hasOnlineSubtitleProviders,
        showFirstAvailableSubtitleAction = canLoadFirstAvailableSubtitle,
    )
    return basePanelsState.copy(
        subtitlePanelScreen = onlinePanelContent.screen,
        subtitlePanelNavigationDirection = onlinePanelContent.direction,
        subtitleOnlineItems = onlinePanelContent.items,
        subtitleInitialFocusedItemId = onlinePanelContent.overrideMainInitialFocusedItemId
            ?: basePanelsState.subtitleInitialFocusedItemId,
        subtitleOnlineInitialFocusedItemId = onlinePanelContent.initialFocusedItemId,
    )
}

internal fun refreshPanelsUiStateForCurrentLink(context: PlayerScreenCoordinatorContext) {
    if (!context.catalog.hasFinalized) return
    val link = context.catalog.store.currentLink() ?: return
    val sourceStatesSnapshot = context.catalog.store.snapshotSourceStates()
    val isCurrentSourceReady =
        sourceStatesSnapshot[link.url]?.status == TvPlayerSourceStatus.Success
    val subtitlesForUi = if (isCurrentSourceReady) context.catalog.store.orderedSubtitles else emptyList()
    context.panels.uiState.value = buildPanelsUiState(
        context = context,
        link = link,
        currentIndex = context.catalog.store.currentLinkIndex,
        sourceStatesSnapshot = sourceStatesSnapshot,
        subtitlesForUi = subtitlesForUi,
    )
}
