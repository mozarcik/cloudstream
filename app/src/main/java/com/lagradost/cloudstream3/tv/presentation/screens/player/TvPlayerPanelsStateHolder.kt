package com.lagradost.cloudstream3.tv.presentation.screens.player

import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.utils.DrmExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLink
import java.util.Locale

private const val UnknownQualityLabel = "Unknown"

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
    private var selectedSubtitleIndex: Int = -1
    private var selectedAudioTrackIndex: Int = -1
    private val expandedSubtitleGroups = linkedMapOf<String, Boolean>()

    fun reset() {
        activePanel = TvPlayerSidePanel.None
        selectedSubtitleIndex = -1
        selectedAudioTrackIndex = -1
        expandedSubtitleGroups.clear()
    }

    fun onSourceChanged(newLink: ExtractorLink?) {
        activePanel = TvPlayerSidePanel.None
        selectedSubtitleIndex = -1
        selectedAudioTrackIndex = defaultAudioTrackIndex(newLink)
        expandedSubtitleGroups.clear()
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
        if (selectedSubtitleIndex < 0) return false

        selectedSubtitleIndex = -1
        expandedSubtitleGroups.clear()
        return true
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
            TvPlayerPanelItemAction.DisableSubtitles -> {
                val changed = selectedSubtitleIndex != -1 || activePanel != TvPlayerSidePanel.None
                selectedSubtitleIndex = -1
                expandedSubtitleGroups.clear()
                activePanel = TvPlayerSidePanel.None
                TvPlayerPanelActionOutcome(stateChanged = changed)
            }
            is TvPlayerPanelItemAction.SelectSubtitle -> {
                val targetIndex = if (action.index in subtitles.indices) {
                    action.index
                } else {
                    -1
                }
                val changed = targetIndex != selectedSubtitleIndex || activePanel != TvPlayerSidePanel.None
                selectedSubtitleIndex = targetIndex
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
                val currentValue = expandedSubtitleGroups[action.groupKey] ?: false
                expandedSubtitleGroups[action.groupKey] = !currentValue
                TvPlayerPanelActionOutcome(stateChanged = true)
            }
            is TvPlayerPanelItemAction.ExpandSubtitleGroup -> {
                if (expandedSubtitleGroups[action.groupKey] == true) {
                    TvPlayerPanelActionOutcome()
                } else {
                    expandedSubtitleGroups[action.groupKey] = true
                    TvPlayerPanelActionOutcome(stateChanged = true)
                }
            }
            is TvPlayerPanelItemAction.CollapseSubtitleGroup -> {
                if (expandedSubtitleGroups[action.groupKey] == false) {
                    TvPlayerPanelActionOutcome()
                } else {
                    expandedSubtitleGroups[action.groupKey] = false
                    TvPlayerPanelActionOutcome(stateChanged = true)
                }
            }
        }
    }

    fun buildPanelsUiState(
        orderedLinks: List<ExtractorLink>,
        currentSourceIndex: Int,
        currentLink: ExtractorLink,
        subtitles: List<SubtitleData>,
    ): TvPlayerPanelsUiState {
        normalizeSelectionIndexes(
            currentLink = currentLink,
            subtitles = subtitles,
        )

        return when (activePanel) {
            TvPlayerSidePanel.None -> TvPlayerPanelsUiState(activePanel = TvPlayerSidePanel.None)
            TvPlayerSidePanel.Sources -> {
                val sourceItems = buildList {
                    orderedLinks
                        .withIndex()
                        .groupBy { indexedLink ->
                            sourceQualityHeaderLabel(
                                link = indexedLink.value,
                                unknownQualityLabel = UnknownQualityLabel,
                            )
                        }
                        .forEach { (qualityHeader, indexedLinks) ->
                            add(
                                TvPlayerPanelItemUi(
                                    id = "source_header_${qualityHeader.replace(" ", "_")}",
                                    title = qualityHeader,
                                    isSectionHeader = true,
                                    enabled = false,
                                )
                            )

                            indexedLinks.forEach { indexedLink ->
                                val index = indexedLink.index
                                val link = indexedLink.value
                                add(
                                    TvPlayerPanelItemUi(
                                        id = "source_$index",
                                        title = link.name.ifBlank { formatSourceMenuLabel(link) },
                                        titleMaxLines = 2,
                                        selected = index == currentSourceIndex,
                                        detailTexts = formatSourceDetailsTexts(
                                            link = link,
                                            unknownQualityLabel = UnknownQualityLabel,
                                        ),
                                        style = TvPlayerPanelItemStyle.SourceOption,
                                        action = TvPlayerPanelItemAction.SelectSource(index = index),
                                    )
                                )
                            }
                        }
                }
                TvPlayerPanelsUiState(
                    activePanel = TvPlayerSidePanel.Sources,
                    sourceItems = sourceItems,
                    sourceInitialFocusedItemId = sourceItems.firstOrNull { item ->
                        item.id == "source_${currentSourceIndex}" && !item.isSectionHeader
                    }?.id ?: sourceItems.firstOrNull { !it.isSectionHeader }?.id,
                )
            }
            TvPlayerSidePanel.Subtitles -> {
                val selectedSubtitleGroupKey = subtitles.getOrNull(selectedSubtitleIndex)?.let(::subtitleLanguageKey)
                val subtitleLanguageGroups = buildSubtitleLanguageGroups(
                    subtitles = subtitles,
                    selectedSubtitleIndex = selectedSubtitleIndex,
                )
                val subtitleItems = buildList {
                    add(
                        TvPlayerPanelItemUi(
                            id = "subtitle_none",
                            titleResId = R.string.no_subtitles,
                            selected = selectedSubtitleIndex == -1,
                            action = TvPlayerPanelItemAction.DisableSubtitles,
                        )
                    )

                    subtitleLanguageGroups.forEach { group ->
                        val groupItemId = subtitleGroupItemId(group.key)
                        val headerId = "subtitle_lang_$groupItemId"
                        val isExpanded = expandedSubtitleGroups[group.key] ?: (group.key == selectedSubtitleGroupKey)

                        add(
                            TvPlayerPanelItemUi(
                                id = headerId,
                                title = group.displayName,
                                selected = !isExpanded && group.items.any { indexedSubtitle ->
                                    indexedSubtitle.index == selectedSubtitleIndex
                                },
                                style = TvPlayerPanelItemStyle.SubtitleGroupHeader,
                                showChevron = true,
                                chevronExpanded = isExpanded,
                                action = TvPlayerPanelItemAction.ToggleSubtitleGroup(group.key),
                                onRightAction = if (isExpanded) {
                                    null
                                } else {
                                    TvPlayerPanelItemAction.ExpandSubtitleGroup(group.key)
                                },
                                onLeftAction = if (isExpanded) {
                                    TvPlayerPanelItemAction.CollapseSubtitleGroup(group.key)
                                } else {
                                    null
                                },
                            )
                        )

                        group.items.forEachIndexed { itemIndex, indexedSubtitle ->
                            val index = indexedSubtitle.index
                            val subtitle = indexedSubtitle.value
                            add(
                                TvPlayerPanelItemUi(
                                    id = "subtitle_$index",
                                    title = subtitle.name,
                                    titleMaxLines = 2,
                                    selected = selectedSubtitleIndex == index,
                                    isVisible = isExpanded,
                                    detailTexts = formatSubtitleDetailsTexts(subtitle),
                                    style = subtitleItemStyle(itemIndex = itemIndex, groupSize = group.items.size),
                                    showTrailingRadio = true,
                                    action = TvPlayerPanelItemAction.SelectSubtitle(index),
                                )
                            )
                        }
                    }
                }
                val initialSubtitleFocusedItemId = when {
                    selectedSubtitleIndex == -1 -> "subtitle_none"
                    selectedSubtitleGroupKey == null -> subtitleItems.firstOrNull { it.isVisible && !it.isSectionHeader }?.id
                    else -> {
                        val selectedGroupExpanded = expandedSubtitleGroups[selectedSubtitleGroupKey] ?: true
                        if (selectedGroupExpanded) {
                            "subtitle_$selectedSubtitleIndex"
                        } else {
                            "subtitle_lang_${subtitleGroupItemId(selectedSubtitleGroupKey)}"
                        }
                    }
                }
                TvPlayerPanelsUiState(
                    activePanel = TvPlayerSidePanel.Subtitles,
                    subtitleItems = subtitleItems,
                    subtitleInitialFocusedItemId = initialSubtitleFocusedItemId
                        ?: subtitleItems.firstOrNull { it.isVisible && !it.isSectionHeader }?.id,
                )
            }
            TvPlayerSidePanel.Tracks -> {
                val trackItems = buildList {
                    add(
                        TvPlayerPanelItemUi(
                            id = "track_default",
                            titleResId = R.string.action_default,
                            selected = selectedAudioTrackIndex == -1,
                            action = TvPlayerPanelItemAction.SelectDefaultTrack,
                        )
                    )
                    currentLink.audioTracks.forEachIndexed { index, audioTrack ->
                        add(
                            TvPlayerPanelItemUi(
                                id = "track_$index",
                                title = formatAudioTrackLabel(index = index, track = audioTrack),
                                selected = selectedAudioTrackIndex == index,
                                action = TvPlayerPanelItemAction.SelectTrack(index),
                            )
                        )
                    }
                }
                TvPlayerPanelsUiState(
                    activePanel = TvPlayerSidePanel.Tracks,
                    trackItems = trackItems,
                    trackInitialFocusedItemId = trackItems.firstOrNull { it.selected }?.id
                        ?: trackItems.firstOrNull()?.id,
                )
            }
        }
    }

    private fun normalizeSelectionIndexes(
        currentLink: ExtractorLink,
        subtitles: List<SubtitleData>,
    ) {
        if (selectedSubtitleIndex !in subtitles.indices) {
            selectedSubtitleIndex = -1
        }
        if (selectedAudioTrackIndex !in currentLink.audioTracks.indices) {
            selectedAudioTrackIndex = -1
        }
    }

    private fun defaultAudioTrackIndex(link: ExtractorLink?): Int {
        if (link == null) return -1
        return if (link.audioTracks.isNotEmpty()) 0 else -1
    }

    private fun subtitleItemStyle(itemIndex: Int, groupSize: Int): TvPlayerPanelItemStyle {
        return when {
            groupSize <= 1 -> TvPlayerPanelItemStyle.SubtitleItemSingle
            itemIndex == 0 -> TvPlayerPanelItemStyle.SubtitleItemTop
            itemIndex == groupSize - 1 -> TvPlayerPanelItemStyle.SubtitleItemBottom
            else -> TvPlayerPanelItemStyle.SubtitleItemMiddle
        }
    }

    private data class SubtitleLanguageGroup(
        val key: String,
        val displayName: String,
        val items: List<IndexedValue<SubtitleData>>,
    )

    private fun qualityLabel(quality: Int): String {
        return com.lagradost.cloudstream3.utils.Qualities.getStringByInt(quality)
    }

    private fun formatSourceMenuLabel(link: ExtractorLink): String {
        val quality = qualityLabel(link.quality)
        return if (quality.isBlank()) {
            link.name
        } else {
            "${link.name} $quality"
        }
    }

    private fun sourceQualityHeaderLabel(
        link: ExtractorLink,
        unknownQualityLabel: String,
    ): String {
        return qualityLabel(link.quality).ifBlank { unknownQualityLabel }
    }

    private fun formatSourceDetailsTexts(
        link: ExtractorLink,
        unknownQualityLabel: String,
    ): List<String> {
        val quality = qualityLabel(link.quality).ifBlank { unknownQualityLabel }
        val host = extractHostFromUrl(link.url)
        return buildList {
            add(quality)
            link.source.takeIf { it.isNotBlank() }?.let(::add)
            add(link.type.name)
            host?.let(::add)
            if (link is DrmExtractorLink) {
                add("DRM")
            }
            if (link.audioTracks.isNotEmpty()) {
                add("A${link.audioTracks.size}")
            }
        }.distinct()
    }

    private fun buildSubtitleLanguageGroups(
        subtitles: List<SubtitleData>,
        selectedSubtitleIndex: Int,
    ): List<SubtitleLanguageGroup> {
        val indexedSubtitles = subtitles.withIndex().toList()
        val selectedGroupKey = indexedSubtitles.getOrNull(selectedSubtitleIndex)?.value?.let(::subtitleLanguageKey)
        val grouped = indexedSubtitles.groupBy { indexedSubtitle ->
            subtitleLanguageKey(indexedSubtitle.value)
        }

        return grouped.map { (groupKey, groupItems) ->
            SubtitleLanguageGroup(
                key = groupKey,
                displayName = subtitleLanguageDisplayName(groupKey),
                items = groupItems.sortedWith(
                    compareBy<IndexedValue<SubtitleData>> { indexedSubtitle ->
                        indexedSubtitle.value.nameSuffix.toIntOrNull() ?: 0
                    }.thenBy { indexedSubtitle ->
                        indexedSubtitle.value.name
                    }
                ),
            )
        }.sortedWith(
            compareByDescending<SubtitleLanguageGroup> { group ->
                group.key == selectedGroupKey
            }.thenBy { group ->
                subtitleLanguagePreferredRank(group.key)
            }.thenBy { group ->
                group.displayName.lowercase(Locale.getDefault())
            }
        )
    }

    private fun subtitleGroupItemId(languageKey: String): String {
        return languageKey.replace(Regex("[^A-Za-z0-9_]"), "_")
    }

    private fun subtitleLanguagePreferredRank(languageKey: String): Int {
        return when (languageKey.substringBefore('-')) {
            "pl" -> 0
            "en" -> 1
            else -> 2
        }
    }

    private fun subtitleLanguageKey(subtitle: SubtitleData): String {
        val rawCode = subtitle.languageCode
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: subtitle.getIETF_tag()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            ?: "und"

        return rawCode.lowercase(Locale.ROOT)
    }

    private fun subtitleLanguageDisplayName(languageKey: String): String {
        if (languageKey == "und") return "Unknown"

        val baseLanguage = languageKey.substringBefore('-')
        val displayLocale = Locale.getDefault()
        val localizedName = Locale.forLanguageTag(baseLanguage).getDisplayLanguage(displayLocale)
            .takeIf { it.isNotBlank() && !it.equals(baseLanguage, ignoreCase = true) }
            ?: languageKey

        return localizedName.replaceFirstChar { firstChar ->
            if (firstChar.isLowerCase()) {
                firstChar.titlecase(displayLocale)
            } else {
                firstChar.toString()
            }
        }
    }

    private fun formatSubtitleDetailsTexts(subtitle: SubtitleData): List<String> {
        val format = formatSubtitleMimeType(subtitle.mimeType)
        val host = extractHostFromUrl(subtitle.getFixedUrl())
        return buildList {
            subtitle.getIETF_tag()?.takeIf { it.isNotBlank() }?.let(::add)
            format?.let(::add)
            host?.let(::add)
        }.distinct()
    }

    private fun formatSubtitleMimeType(mimeType: String?): String? {
        if (mimeType.isNullOrBlank()) return null
        return when {
            mimeType.contains("subrip", ignoreCase = true) -> "SRT"
            mimeType.contains("vtt", ignoreCase = true) -> "VTT"
            mimeType.contains("ttml", ignoreCase = true) -> "TTML"
            mimeType.contains("/") -> mimeType.substringAfterLast("/").uppercase()
            else -> mimeType.uppercase()
        }
    }

    private fun extractHostFromUrl(url: String): String? {
        return runCatching {
            android.net.Uri.parse(url).host
        }.getOrNull()
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
    }

    private fun formatAudioTrackLabel(
        index: Int,
        track: com.lagradost.cloudstream3.AudioFile,
    ): String {
        val host = runCatching {
            android.net.Uri.parse(track.url).host
        }.getOrNull()
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }

        return if (host == null) {
            "Track ${index + 1}"
        } else {
            "Track ${index + 1} ($host)"
        }
    }
}
