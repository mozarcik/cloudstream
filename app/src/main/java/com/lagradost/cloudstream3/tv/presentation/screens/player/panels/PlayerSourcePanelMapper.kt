package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.tv.material3.Icon
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.data.util.buildSourcePanelEntries
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelSupportingStyle
import com.lagradost.cloudstream3.utils.ExtractorLink

private const val UnknownQualityLabel = "Unknown"

internal data class PlayerPanelItems(
    val items: List<SidePanelMenuItem>,
    val initialFocusedItemId: String?,
)

internal fun buildSourcePanelItems(
    orderedLinks: List<ExtractorLink>,
    currentSourceIndex: Int,
    sourceStates: Map<String, TvPlayerSourceState>,
): PlayerPanelItems {
    val sourceItems = buildSourcePanelEntries(
        orderedLinks = orderedLinks,
        itemKeyPrefix = "source",
        unknownQualityLabel = UnknownQualityLabel,
    ).map { entry ->
        if (entry.isSectionHeader) {
            SidePanelMenuItem(
                id = entry.key,
                title = entry.title,
                isSectionHeader = true,
                enabled = false,
            )
        } else {
            val index = entry.sourceIndex ?: error("Source item missing source index: ${entry.key}")
            val link = orderedLinks[index]
            val sourceState = sourceStates[link.url]
            val isErroredSource = sourceState?.status == TvPlayerSourceStatus.Error

            SidePanelMenuItem(
                id = entry.key,
                title = entry.title,
                titleMaxLines = entry.titleMaxLines,
                selected = index == currentSourceIndex,
                supportingTexts = if (isErroredSource) {
                    listOf(sourceErrorSupportingText(sourceState = sourceState))
                } else {
                    entry.supportingTexts
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
        }
    }
    val initialFocusedItemId = sourceItems.firstOrNull { item ->
        item.id == "source_${currentSourceIndex}" && !item.isSectionHeader
    }?.id ?: sourceItems.firstOrNull { !it.isSectionHeader }?.id

    return PlayerPanelItems(
        items = sourceItems,
        initialFocusedItemId = initialFocusedItemId,
    )
}

private fun sourceErrorSupportingText(sourceState: TvPlayerSourceState?): String {
    val httpCode = sourceState?.httpCode
    return if (httpCode != null) {
        val template = playerString(
            resId = R.string.tv_player_source_failed_to_load_with_code,
            fallback = "Failed to load (%1\$d)",
        )
        runCatching {
            template.format(httpCode)
        }.getOrElse {
            "Failed to load ($httpCode)"
        }
    } else {
        playerString(
            resId = R.string.tv_player_source_unavailable,
            fallback = "Unavailable",
        )
    }
}
