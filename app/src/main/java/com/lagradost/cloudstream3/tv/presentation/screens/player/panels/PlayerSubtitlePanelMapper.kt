package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.tv.data.util.extractHostFromUrl
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerIcons
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelTitleStyle
import com.lagradost.cloudstream3.ui.player.SubtitleData
import java.util.Locale

private data class SubtitleLanguageGroup(
    val key: String,
    val displayName: String,
    val items: List<IndexedValue<SubtitleData>>,
)

internal fun buildSubtitlePanelItems(
    subtitles: List<SubtitleData>,
    selectedSubtitleIndex: Int,
    subtitleEncodingLabel: String,
    preferredSubtitleLanguageKey: String,
    preferredSubtitleBaseLanguageKey: String,
    showOnlineSubtitleActions: Boolean,
    showFirstAvailableSubtitleAction: Boolean,
): PlayerPanelItems {
    val selectedSubtitleGroupKey = subtitles.getOrNull(selectedSubtitleIndex)?.let(::subtitleLanguageKey)
    val subtitleLanguageGroups = buildSubtitleLanguageGroups(
        subtitles = subtitles,
        preferredSubtitleLanguageKey = preferredSubtitleLanguageKey,
        preferredSubtitleBaseLanguageKey = preferredSubtitleBaseLanguageKey,
    )
    val subtitleItems = buildList {
        add(
            SidePanelMenuItem(
                id = "subtitle_none",
                title = playerString(
                    resId = R.string.no_subtitles,
                    fallback = "No subtitles",
                ),
                selected = selectedSubtitleIndex == -1,
                showLeadingRadio = true,
                titleTextStyle = TvTextStyles.PlayerPanelRow,
                actionToken = TvPlayerPanelItemAction.DisableSubtitles,
            )
        )
        add(
            SidePanelMenuItem(
                id = SubtitleEncodingItemId,
                title = playerString(
                    resId = R.string.subtitles_encoding,
                    fallback = "Subtitle encoding",
                ),
                supportingTexts = listOf(subtitleEncodingLabel),
                titleTextStyle = TvTextStyles.PlayerPanelRow,
                leadingContent = { PanelIcon(PlayerIcons.Encoding) },
                showChevron = true,
                actionToken = TvPlayerPanelItemAction.OpenSubtitleEncodingSelection,
            )
        )
        add(
            SidePanelMenuItem(
                id = SubtitleLoadFromFileItemId,
                title = playerString(
                    resId = R.string.player_load_subtitles,
                    fallback = "Load from file",
                ),
                titleTextStyle = TvTextStyles.PlayerPanelRow,
                leadingContent = { PanelIcon(PlayerIcons.Download) },
                actionToken = TvPlayerPanelItemAction.LoadSubtitleFromFile,
            )
        )

        if (showOnlineSubtitleActions) {
            add(
                SidePanelMenuItem(
                    id = SubtitleLoadFromInternetItemId,
                    title = playerString(
                        resId = R.string.player_load_subtitles_online,
                        fallback = "Load from Internet",
                    ),
                    titleTextStyle = TvTextStyles.PlayerPanelRow,
                    showChevron = true,
                    leadingContent = { PanelIcon(PlayerIcons.CloudDownload) },
                    actionToken = TvPlayerPanelItemAction.OpenOnlineSubtitles,
                )
            )
        }

        if (showFirstAvailableSubtitleAction) {
            add(
                SidePanelMenuItem(
                    id = SubtitleLoadFirstAvailableItemId,
                    title = playerString(
                        resId = R.string.player_load_one_subtitle_online,
                        fallback = "Load first available",
                    ),
                    titleTextStyle = TvTextStyles.PlayerPanelRow,
                    leadingContent = { PanelIcon(PlayerIcons.FirstAvailable) },
                    actionToken = TvPlayerPanelItemAction.LoadFirstAvailableSubtitle,
                )
            )
        }

        if (subtitleLanguageGroups.isNotEmpty()) {
            add(
                SidePanelMenuItem(
                    id = "subtitle_available_header",
                    title = playerString(R.string.tv_player_subtitles_available, "Available") +
                        " \u2022 ${subtitles.size}",
                    isSectionHeader = true,
                    enabled = false,
                )
            )
        }

        subtitleLanguageGroups.forEach { group ->
            val headerId = "subtitle_lang_${subtitleGroupItemId(group.key)}"
            val isExpanded = group.key == selectedSubtitleGroupKey

            add(
                SidePanelMenuItem(
                    id = headerId,
                    title = group.displayName,
                    titleTint = if (group.items.any { it.index == selectedSubtitleIndex }) TvColors.Selected else null,
                    trailingText = "\u2022 ${group.items.size}",
                    titleTextStyle = TvTextStyles.PlayerPanelRow,
                    titleStyle = SidePanelTitleStyle.SubtitleGroupHeader,
                    showChevron = true,
                    chevronExpanded = isExpanded,
                    expandableGroupKey = group.key,
                )
            )

            group.items.forEach { indexedSubtitle ->
                val index = indexedSubtitle.index
                val subtitle = indexedSubtitle.value
                add(
                    SidePanelMenuItem(
                        id = "subtitle_$index",
                        title = subtitle.name,
                        titleMaxLines = 2,
                        titleTextStyle = TvTextStyles.PlayerPanelRow,
                        contentInsetStart = 13.dp,
                        selected = selectedSubtitleIndex == index,
                        parentGroupKey = group.key,
                        supportingTexts = formatSubtitleDetailsTexts(subtitle),
                        showTrailingRadio = true,
                        actionToken = TvPlayerPanelItemAction.SelectSubtitle(index),
                    )
                )
            }
        }
    }

    val initialFocusedItemId = when {
        selectedSubtitleIndex == -1 -> "subtitle_none"
        selectedSubtitleGroupKey == null -> subtitleItems.firstOrNull { !it.isSectionHeader }?.id
        else -> "subtitle_$selectedSubtitleIndex"
    } ?: subtitleItems.firstOrNull { !it.isSectionHeader }?.id

    return PlayerPanelItems(
        items = subtitleItems,
        initialFocusedItemId = initialFocusedItemId,
    )
}

private fun buildSubtitleLanguageGroups(
    subtitles: List<SubtitleData>,
    preferredSubtitleLanguageKey: String,
    preferredSubtitleBaseLanguageKey: String,
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
            subtitleLanguagePreferredRank(
                languageKey = group.key,
                preferredSubtitleLanguageKey = preferredSubtitleLanguageKey,
                preferredSubtitleBaseLanguageKey = preferredSubtitleBaseLanguageKey,
            )
        }.thenBy { group ->
            group.displayName.lowercase(Locale.getDefault())
        }
    )
}

private fun subtitleGroupItemId(languageKey: String): String {
    return languageKey.replace(Regex("[^A-Za-z0-9_]"), "_")
}

private fun subtitleLanguagePreferredRank(
    languageKey: String,
    preferredSubtitleLanguageKey: String,
    preferredSubtitleBaseLanguageKey: String,
): Int {
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

/** "Polish • SRT" shown in the sync panel header. */
internal fun subtitleHeaderInfo(subtitle: SubtitleData): String =
    listOfNotNull(subtitleValueLabel(subtitle), formatSubtitleMimeType(subtitle.mimeType)).joinToString(" \u2022 ")

/** Language shown on the Subtitles control ("Polish"); falls back to the track name. */
internal fun subtitleValueLabel(subtitle: SubtitleData): String {
    val label = subtitleLanguageDisplayName(subtitleLanguageKey(subtitle))
    return if (label == "Unknown") subtitle.name else label
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

@androidx.compose.runtime.Composable
internal fun PanelIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
) {
    if (tint == androidx.compose.ui.graphics.Color.Unspecified) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
    } else {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
    }
}
