package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.utils.ExtractorLink

internal fun buildPlayerPanelsUiState(
    activePanel: TvPlayerSidePanel,
    orderedLinks: List<ExtractorLink>,
    currentSourceIndex: Int,
    sourceStates: Map<String, TvPlayerSourceState>,
    currentLink: ExtractorLink,
    subtitles: List<SubtitleData>,
    selectedSubtitleIndex: Int,
    selectedAudioTrackIndex: Int,
    preferredSubtitleLanguageKey: String,
    preferredSubtitleBaseLanguageKey: String,
    showOnlineSubtitleActions: Boolean,
    showFirstAvailableSubtitleAction: Boolean,
): TvPlayerPanelsUiState {
    val sourcePanel = buildSourcePanelItems(
        orderedLinks = orderedLinks,
        currentSourceIndex = currentSourceIndex,
        sourceStates = sourceStates,
    )
    val subtitlePanel = buildSubtitlePanelItems(
        subtitles = subtitles,
        selectedSubtitleIndex = selectedSubtitleIndex,
        preferredSubtitleLanguageKey = preferredSubtitleLanguageKey,
        preferredSubtitleBaseLanguageKey = preferredSubtitleBaseLanguageKey,
        showOnlineSubtitleActions = showOnlineSubtitleActions,
        showFirstAvailableSubtitleAction = showFirstAvailableSubtitleAction,
    )
    val trackPanel = buildTrackPanelItems(
        currentLink = currentLink,
        selectedAudioTrackIndex = selectedAudioTrackIndex,
    )
    return TvPlayerPanelsUiState(
        activePanel = activePanel,
        sourceItems = sourcePanel.items,
        subtitleItems = subtitlePanel.items,
        trackItems = trackPanel.items,
        sourceInitialFocusedItemId = sourcePanel.initialFocusedItemId,
        subtitleInitialFocusedItemId = subtitlePanel.initialFocusedItemId,
        trackInitialFocusedItemId = trackPanel.initialFocusedItemId,
    )
}
