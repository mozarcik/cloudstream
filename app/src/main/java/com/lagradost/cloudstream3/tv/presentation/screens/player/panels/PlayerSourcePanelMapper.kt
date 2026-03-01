package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.tv.material3.Icon
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelSupportingStyle
import com.lagradost.cloudstream3.utils.DrmExtractorLink
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
    val initialFocusedItemId = sourceItems.firstOrNull { item ->
        item.id == "source_${currentSourceIndex}" && !item.isSectionHeader
    }?.id ?: sourceItems.firstOrNull { !it.isSectionHeader }?.id

    return PlayerPanelItems(
        items = sourceItems,
        initialFocusedItemId = initialFocusedItemId,
    )
}

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

private fun extractHostFromUrl(url: String): String? {
    return runCatching {
        android.net.Uri.parse(url).host
    }.getOrNull()
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }
}
