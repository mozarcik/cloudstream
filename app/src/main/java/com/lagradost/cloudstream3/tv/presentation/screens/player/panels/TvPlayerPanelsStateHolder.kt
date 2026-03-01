package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesFragment.Companion.getAutoSelectLanguageTagIETF
import com.lagradost.cloudstream3.utils.ExtractorLink
import java.util.Locale

internal data class TvPlayerPanelsSelection(
    val activePanel: TvPlayerSidePanel,
    val selectedSubtitleIndex: Int,
    val selectedAudioTrackIndex: Int,
)

internal data class TvPlayerPanelActionOutcome(
    val selectedSourceIndex: Int? = null,
    val stateChanged: Boolean = false,
)

internal class TvPlayerPanelsStateHolder {
    private var activePanel: TvPlayerSidePanel = TvPlayerSidePanel.None
    private var selectedSubtitleId: String? = null
    private var selectedSubtitleIndex: Int = -1
    private var selectedAudioTrackIndex: Int = -1
    private var subtitleSelectionOverriddenByUser: Boolean = false
    private val preferredSubtitleLanguageTag: String = getAutoSelectLanguageTagIETF().trim()
    private val preferredSubtitleLanguageKey: String = preferredSubtitleLanguageTag.lowercase(Locale.ROOT)
    private val preferredSubtitleBaseLanguageKey: String = preferredSubtitleLanguageKey.substringBefore('-')

    fun reset() {
        activePanel = TvPlayerSidePanel.None
        selectedSubtitleId = null
        selectedSubtitleIndex = -1
        selectedAudioTrackIndex = -1
        subtitleSelectionOverriddenByUser = false
    }

    fun onSourceChanged(
        newLink: ExtractorLink?,
        preserveSourcesPanel: Boolean = false,
    ) {
        if (!preserveSourcesPanel || activePanel != TvPlayerSidePanel.Sources) {
            activePanel = TvPlayerSidePanel.None
        }
        selectedSubtitleId = null
        selectedSubtitleIndex = -1
        selectedAudioTrackIndex = defaultAudioTrackIndex(newLink)
        subtitleSelectionOverriddenByUser = false
    }

    fun openPanel(panel: TvPlayerSidePanel): Boolean {
        if (panel == TvPlayerSidePanel.None) return false
        if (activePanel == panel) return false

        activePanel = panel
        return true
    }

    fun closePanel(): Boolean {
        if (activePanel == TvPlayerSidePanel.None) return false
        activePanel = TvPlayerSidePanel.None
        return true
    }

    fun disableSubtitlesFromPlaybackError(): Boolean {
        if (selectedSubtitleId == null && selectedSubtitleIndex < 0) return false
        selectedSubtitleId = null
        selectedSubtitleIndex = -1
        subtitleSelectionOverriddenByUser = true
        return true
    }

    fun selectSubtitleById(subtitleId: String?): Boolean {
        if (subtitleId == null) {
            return disableSubtitlesFromPlaybackError()
        }

        val changed = selectedSubtitleId != subtitleId || activePanel != TvPlayerSidePanel.None
        selectedSubtitleId = subtitleId
        selectedSubtitleIndex = -1
        subtitleSelectionOverriddenByUser = true
        activePanel = TvPlayerSidePanel.None
        return changed
    }

    fun applyPreferredSubtitleAutoSelection(subtitles: List<SubtitleData>) {
        if (subtitleSelectionOverriddenByUser) return
        if (preferredSubtitleLanguageTag.isBlank()) return

        normalizeSelectedSubtitleIndex(subtitles)

        val currentSubtitle = subtitles.getOrNull(selectedSubtitleIndex)
        if (currentSubtitle != null && currentSubtitle.matchesLanguageCode(preferredSubtitleLanguageTag)) {
            return
        }

        val targetIndex = subtitles.indexOfFirst { subtitle ->
            subtitle.matchesLanguageCode(preferredSubtitleLanguageTag)
        }
        if (targetIndex < 0) return

        selectedSubtitleIndex = targetIndex
        selectedSubtitleId = subtitles[targetIndex].getId()
    }

    fun selection(
        currentLink: ExtractorLink,
        subtitles: List<SubtitleData>,
    ): TvPlayerPanelsSelection {
        normalizeSelectionIndexes(
            currentLink = currentLink,
            subtitles = subtitles,
        )
        return TvPlayerPanelsSelection(
            activePanel = activePanel,
            selectedSubtitleIndex = selectedSubtitleIndex,
            selectedAudioTrackIndex = selectedAudioTrackIndex,
        )
    }

    fun onPanelItemAction(
        action: TvPlayerPanelItemAction,
        currentLink: ExtractorLink?,
        subtitles: List<SubtitleData>,
    ): TvPlayerPanelActionOutcome {
        return when (action) {
            TvPlayerPanelItemAction.None -> TvPlayerPanelActionOutcome()
            is TvPlayerPanelItemAction.SelectSource -> {
                TvPlayerPanelActionOutcome(
                    selectedSourceIndex = action.index,
                    stateChanged = false,
                )
            }
            is TvPlayerPanelItemAction.InspectSourceError -> {
                TvPlayerPanelActionOutcome()
            }
            TvPlayerPanelItemAction.DisableSubtitles -> {
                val changed = selectedSubtitleId != null || selectedSubtitleIndex != -1 || activePanel != TvPlayerSidePanel.None
                selectedSubtitleId = null
                selectedSubtitleIndex = -1
                subtitleSelectionOverriddenByUser = true
                activePanel = TvPlayerSidePanel.None
                TvPlayerPanelActionOutcome(stateChanged = changed)
            }
            is TvPlayerPanelItemAction.SelectSubtitle -> {
                val targetSubtitle = subtitles.getOrNull(action.index)
                val targetSubtitleId = targetSubtitle?.getId()
                val changed = targetSubtitleId != selectedSubtitleId || activePanel != TvPlayerSidePanel.None
                selectedSubtitleId = targetSubtitleId
                selectedSubtitleIndex = if (targetSubtitle == null) -1 else action.index
                subtitleSelectionOverriddenByUser = true
                activePanel = TvPlayerSidePanel.None
                TvPlayerPanelActionOutcome(stateChanged = changed)
            }
            TvPlayerPanelItemAction.SelectDefaultTrack -> {
                val changed = selectedAudioTrackIndex != -1 || activePanel != TvPlayerSidePanel.None
                selectedAudioTrackIndex = -1
                activePanel = TvPlayerSidePanel.None
                TvPlayerPanelActionOutcome(stateChanged = changed)
            }
            is TvPlayerPanelItemAction.SelectTrack -> {
                val audioTracks = currentLink?.audioTracks.orEmpty()
                val targetIndex = if (action.index in audioTracks.indices) {
                    action.index
                } else {
                    -1
                }
                val changed = targetIndex != selectedAudioTrackIndex || activePanel != TvPlayerSidePanel.None
                selectedAudioTrackIndex = targetIndex
                activePanel = TvPlayerSidePanel.None
                TvPlayerPanelActionOutcome(stateChanged = changed)
            }
            is TvPlayerPanelItemAction.ToggleSubtitleGroup -> {
                TvPlayerPanelActionOutcome()
            }
            is TvPlayerPanelItemAction.ExpandSubtitleGroup -> {
                TvPlayerPanelActionOutcome()
            }
            is TvPlayerPanelItemAction.CollapseSubtitleGroup -> {
                TvPlayerPanelActionOutcome()
            }
            TvPlayerPanelItemAction.LoadSubtitleFromFile,
            TvPlayerPanelItemAction.OpenOnlineSubtitles,
            TvPlayerPanelItemAction.LoadFirstAvailableSubtitle,
            TvPlayerPanelItemAction.BackFromOnlineSubtitles,
            TvPlayerPanelItemAction.EditOnlineSubtitlesQuery,
            is TvPlayerPanelItemAction.UpdateOnlineSubtitlesQuery,
            TvPlayerPanelItemAction.SelectOnlineSubtitlesLanguage,
            is TvPlayerPanelItemAction.SelectOnlineSubtitlesLanguageOption,
            TvPlayerPanelItemAction.RetryOnlineSubtitlesSearch,
            is TvPlayerPanelItemAction.SelectOnlineSubtitleResult -> {
                TvPlayerPanelActionOutcome()
            }
        }
    }

    fun buildPanelsUiState(
        orderedLinks: List<ExtractorLink>,
        currentSourceIndex: Int,
        sourceStates: Map<String, TvPlayerSourceState>,
        currentLink: ExtractorLink,
        subtitles: List<SubtitleData>,
        showOnlineSubtitleActions: Boolean,
        showFirstAvailableSubtitleAction: Boolean,
    ): TvPlayerPanelsUiState {
        normalizeSelectionIndexes(
            currentLink = currentLink,
            subtitles = subtitles,
        )
        return buildPlayerPanelsUiState(
            activePanel = activePanel,
            orderedLinks = orderedLinks,
            currentSourceIndex = currentSourceIndex,
            sourceStates = sourceStates,
            currentLink = currentLink,
            subtitles = subtitles,
            selectedSubtitleIndex = selectedSubtitleIndex,
            selectedAudioTrackIndex = selectedAudioTrackIndex,
            preferredSubtitleLanguageKey = preferredSubtitleLanguageKey,
            preferredSubtitleBaseLanguageKey = preferredSubtitleBaseLanguageKey,
            showOnlineSubtitleActions = showOnlineSubtitleActions,
            showFirstAvailableSubtitleAction = showFirstAvailableSubtitleAction,
        )
    }

    private fun normalizeSelectionIndexes(
        currentLink: ExtractorLink,
        subtitles: List<SubtitleData>,
    ) {
        normalizeSelectedSubtitleIndex(subtitles)
        if (selectedAudioTrackIndex !in currentLink.audioTracks.indices) {
            selectedAudioTrackIndex = -1
        }
    }

    private fun normalizeSelectedSubtitleIndex(subtitles: List<SubtitleData>) {
        selectedSubtitleIndex = selectedSubtitleId?.let { subtitleId ->
            subtitles.indexOfFirst { subtitle ->
                subtitle.getId() == subtitleId
            }.takeIf { index -> index >= 0 }
        } ?: -1
        if (selectedSubtitleIndex == -1) {
            selectedSubtitleId = null
        }
        if (selectedSubtitleIndex !in subtitles.indices) {
            selectedSubtitleIndex = -1
        }
    }

    private fun defaultAudioTrackIndex(link: ExtractorLink?): Int {
        if (link == null) return -1
        return if (link.audioTracks.isNotEmpty()) 0 else -1
    }
}
