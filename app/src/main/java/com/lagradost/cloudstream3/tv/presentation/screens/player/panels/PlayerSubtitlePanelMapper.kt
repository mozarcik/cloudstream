package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.tv.material3.Icon
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
                actionToken = TvPlayerPanelItemAction.DisableSubtitles,
            )
        )
        add(
            SidePanelMenuItem(
                id = SubtitleLoadFromFileItemId,
                title = playerString(
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
                    title = playerString(
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
                    title = playerString(
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

        subtitleLanguageGroups.forEach { group ->
            val headerId = "subtitle_lang_${subtitleGroupItemId(group.key)}"
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

            group.items.forEach { indexedSubtitle ->
                val index = indexedSubtitle.index
                val subtitle = indexedSubtitle.value
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
