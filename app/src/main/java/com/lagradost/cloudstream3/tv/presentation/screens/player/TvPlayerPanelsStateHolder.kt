package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelSupportingStyle
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelTitleStyle
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesFragment.Companion.getAutoSelectLanguageTagIETF
import com.lagradost.cloudstream3.utils.DrmExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLink
import java.util.Locale

private const val UnknownQualityLabel = "Unknown"
private val SubtitleItemBackgroundColor = Color(0xFF0A0A0B)
private val SubtitleItemShapeSingle = RoundedCornerShape(10.dp)
private val SubtitleItemShapeTop = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
private val SubtitleItemShapeBottom = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
private val SubtitleItemShapeMiddle = RoundedCornerShape(0.dp)
private val SubtitleItemFocusedShape = RoundedCornerShape(10.dp)

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

    fun onSourceChanged(newLink: ExtractorLink?) {
        activePanel = TvPlayerSidePanel.None
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
            TvPlayerPanelItemAction.SelectOnlineSubtitlesLanguage,
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
                        SidePanelMenuItem(
                            id = "source_header_${qualityHeader.replace(" ", "_")}",
                            title = qualityHeader,
                            isSectionHeader = true,
                            enabled = false,
                        )
                    )

                    indexedLinks.forEach { indexedLink ->
                        val index = indexedLink.index
                        val link = indexedLink.value
                        val sourceState = sourceStates[link.url]
                        val isErroredSource = sourceState?.status == TvPlayerSourceStatus.Error
                        add(
                            SidePanelMenuItem(
                                id = "source_$index",
                                title = link.name.ifBlank { formatSourceMenuLabel(link) },
                                titleMaxLines = 2,
                                selected = index == currentSourceIndex,
                                supportingTexts = if (isErroredSource) {
                                    listOf(sourceErrorSupportingText(sourceState = sourceState))
                                } else {
                                    formatSourceDetailsTexts(
                                        link = link,
                                        unknownQualityLabel = UnknownQualityLabel,
                                    )
                                },
                                supportingStyle = if (isErroredSource) {
                                    SidePanelSupportingStyle.SourceError
                                } else {
                                    SidePanelSupportingStyle.SourceOption
                                },
                                actionToken = if (isErroredSource) {
                                    TvPlayerPanelItemAction.InspectSourceError(index = index)
                                } else {
                                    TvPlayerPanelItemAction.SelectSource(index = index)
                                },
                                trailingContent = if (isErroredSource) {
                                    {
                                        Icon(
                                            imageVector = Icons.Outlined.ErrorOutline,
                                            contentDescription = null,
                                        )
                                    }
                                } else {
                                    null
                                },
                            )
                        )
                    }
                }
        }
        val sourceInitialFocusedItemId = sourceItems.firstOrNull { item ->
            item.id == "source_${currentSourceIndex}" && !item.isSectionHeader
        }?.id ?: sourceItems.firstOrNull { !it.isSectionHeader }?.id

        val selectedSubtitleGroupKey = subtitles.getOrNull(selectedSubtitleIndex)?.let(::subtitleLanguageKey)
        val subtitleLanguageGroups = buildSubtitleLanguageGroups(
            subtitles = subtitles,
        )
        val subtitleItems = buildList {
            add(
                SidePanelMenuItem(
                    id = "subtitle_none",
                    title = stringFromAppContext(
                        resId = R.string.no_subtitles,
                        fallback = "No subtitles",
                    ),
                    selected = selectedSubtitleIndex == -1,
                    actionToken = TvPlayerPanelItemAction.DisableSubtitles,
                )
            )
            add(
                SidePanelMenuItem(
                    id = SubtitleLoadFromFileItemId,
                    title = stringFromAppContext(
                        resId = R.string.player_load_subtitles,
                        fallback = "Load from file",
                    ),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                        )
                    },
                    actionToken = TvPlayerPanelItemAction.LoadSubtitleFromFile,
                )
            )

            if (showOnlineSubtitleActions) {
                add(
                    SidePanelMenuItem(
                        id = SubtitleLoadFromInternetItemId,
                        title = stringFromAppContext(
                            resId = R.string.player_load_subtitles_online,
                            fallback = "Load from Internet",
                        ),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                            )
                        },
                        actionToken = TvPlayerPanelItemAction.OpenOnlineSubtitles,
                    )
                )
            }

            if (showFirstAvailableSubtitleAction) {
                add(
                    SidePanelMenuItem(
                        id = SubtitleLoadFirstAvailableItemId,
                        title = stringFromAppContext(
                            resId = R.string.player_load_one_subtitle_online,
                            fallback = "Load first available",
                        ),
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                            )
                        },
                        actionToken = TvPlayerPanelItemAction.LoadFirstAvailableSubtitle,
                    )
                )
            }

            /*
             * Keep language groups below utility actions in fixed order:
             * none -> file -> internet -> first available.
             */
            subtitleLanguageGroups.forEach { group ->
                val groupItemId = subtitleGroupItemId(group.key)
                val headerId = "subtitle_lang_$groupItemId"
                val isExpanded = group.key == selectedSubtitleGroupKey

                add(
                    SidePanelMenuItem(
                        id = headerId,
                        title = group.displayName,
                        selected = group.items.any { indexedSubtitle ->
                            indexedSubtitle.index == selectedSubtitleIndex
                        },
                        titleStyle = SidePanelTitleStyle.SubtitleGroupHeader,
                        showChevron = true,
                        chevronExpanded = isExpanded,
                        expandableGroupKey = group.key,
                    )
                )

                group.items.forEachIndexed { itemIndex, indexedSubtitle ->
                    val index = indexedSubtitle.index
                    val subtitle = indexedSubtitle.value
                    val subtitleStyle = subtitleItemStyle(itemIndex = itemIndex, groupSize = group.items.size)
                    add(
                        SidePanelMenuItem(
                            id = "subtitle_$index",
                            title = subtitle.name,
                            titleMaxLines = 2,
                            titleStyle = SidePanelTitleStyle.SubtitleItem,
                            selected = selectedSubtitleIndex == index,
                            parentGroupKey = group.key,
                            supportingTexts = formatSubtitleDetailsTexts(subtitle),
                            showTrailingRadio = true,
                            actionToken = TvPlayerPanelItemAction.SelectSubtitle(index),
                            itemBackgroundColor = subtitleItemBackgroundColor(subtitleStyle),
                            itemBackgroundShape = subtitleItemBackgroundShape(subtitleStyle),
                            focusedItemShape = subtitleItemFocusedShape(subtitleStyle),
                        )
                    )
                }
            }
        }
        val initialSubtitleFocusedItemId = when {
            selectedSubtitleIndex == -1 -> "subtitle_none"
            selectedSubtitleGroupKey == null -> subtitleItems.firstOrNull { !it.isSectionHeader }?.id
            else -> "subtitle_$selectedSubtitleIndex"
        }

        val trackItems = buildList {
            add(
                SidePanelMenuItem(
                    id = "track_default",
                    title = stringFromAppContext(
                        resId = R.string.action_default,
                        fallback = "Default",
                    ),
                    selected = selectedAudioTrackIndex == -1,
                    actionToken = TvPlayerPanelItemAction.SelectDefaultTrack,
                )
            )
            currentLink.audioTracks.forEachIndexed { index, audioTrack ->
                add(
                    SidePanelMenuItem(
                        id = "track_$index",
                        title = formatAudioTrackLabel(index = index, track = audioTrack),
                        selected = selectedAudioTrackIndex == index,
                        actionToken = TvPlayerPanelItemAction.SelectTrack(index),
                    )
                )
            }
        }
        val trackInitialFocusedItemId = trackItems.firstOrNull { it.selected }?.id
            ?: trackItems.firstOrNull()?.id

        return TvPlayerPanelsUiState(
            activePanel = activePanel,
            sourceItems = sourceItems,
            subtitleItems = subtitleItems,
            trackItems = trackItems,
            sourceInitialFocusedItemId = sourceInitialFocusedItemId,
            subtitleInitialFocusedItemId = initialSubtitleFocusedItemId
                ?: subtitleItems.firstOrNull { !it.isSectionHeader }?.id,
            trackInitialFocusedItemId = trackInitialFocusedItemId,
        )
    }

    private fun subtitleItemBackgroundColor(style: TvPlayerPanelItemStyle): Color? {
        return when (style) {
            TvPlayerPanelItemStyle.SubtitleItemSingle,
            TvPlayerPanelItemStyle.SubtitleItemTop,
            TvPlayerPanelItemStyle.SubtitleItemMiddle,
            TvPlayerPanelItemStyle.SubtitleItemBottom -> SubtitleItemBackgroundColor
            else -> null
        }
    }

    private fun subtitleItemBackgroundShape(style: TvPlayerPanelItemStyle): Shape? {
        return when (style) {
            TvPlayerPanelItemStyle.SubtitleItemSingle -> SubtitleItemShapeSingle
            TvPlayerPanelItemStyle.SubtitleItemTop -> SubtitleItemShapeTop
            TvPlayerPanelItemStyle.SubtitleItemBottom -> SubtitleItemShapeBottom
            TvPlayerPanelItemStyle.SubtitleItemMiddle -> SubtitleItemShapeMiddle
            else -> null
        }
    }

    private fun subtitleItemFocusedShape(style: TvPlayerPanelItemStyle): Shape? {
        return when (style) {
            TvPlayerPanelItemStyle.SubtitleItemSingle,
            TvPlayerPanelItemStyle.SubtitleItemTop,
            TvPlayerPanelItemStyle.SubtitleItemMiddle,
            TvPlayerPanelItemStyle.SubtitleItemBottom -> SubtitleItemFocusedShape
            else -> null
        }
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

    private fun sourceErrorSupportingText(sourceState: TvPlayerSourceState?): String {
        val httpCode = sourceState?.httpCode
        return if (httpCode != null) {
            val template = stringFromAppContext(
                resId = R.string.tv_player_source_failed_to_load_with_code,
                fallback = "Failed to load (%1\$d)",
            )
            runCatching {
                template.format(httpCode)
            }.getOrElse {
                "Failed to load ($httpCode)"
            }
        } else {
            stringFromAppContext(
                resId = R.string.tv_player_source_unavailable,
                fallback = "Unavailable",
            )
        }
    }

    private fun buildSubtitleLanguageGroups(
        subtitles: List<SubtitleData>,
    ): List<SubtitleLanguageGroup> {
        val indexedSubtitles = subtitles.withIndex().toList()
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
            compareBy<SubtitleLanguageGroup> { group ->
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
        if (preferredSubtitleLanguageKey.isBlank()) return 1
        val normalizedLanguageKey = languageKey.lowercase(Locale.ROOT)
        if (normalizedLanguageKey == preferredSubtitleLanguageKey) return 0
        if (normalizedLanguageKey.substringBefore('-') == preferredSubtitleBaseLanguageKey) return 0
        return 1
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

    private fun stringFromAppContext(
        resId: Int,
        fallback: String,
    ): String {
        return CloudStreamApp.context?.getString(resId) ?: fallback
    }
}
