package com.lagradost.cloudstream3.tv.data.util

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.utils.DrmExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLink

private const val DefaultUnknownQualityLabel = "Unknown"

@Immutable
internal data class SourcePanelEntry(
    val key: String,
    val title: String,
    val titleMaxLines: Int = 1,
    val supportingTexts: List<String> = emptyList(),
    val isSectionHeader: Boolean = false,
    val sourceIndex: Int? = null,
)

internal fun buildSourcePanelEntries(
    orderedLinks: List<ExtractorLink>,
    itemKeyPrefix: String,
    unknownQualityLabel: String = DefaultUnknownQualityLabel,
): List<SourcePanelEntry> {
    return buildList {
        orderedLinks
            .withIndex()
            .groupBy { indexedLink ->
                sourceQualityHeaderLabel(
                    link = indexedLink.value,
                    unknownQualityLabel = unknownQualityLabel,
                )
            }
            .forEach { (qualityHeader, indexedLinks) ->
                add(
                    SourcePanelEntry(
                        key = "${itemKeyPrefix}_header_${qualityHeader.replace(" ", "_")}",
                        title = qualityHeader,
                        isSectionHeader = true,
                    )
                )

                indexedLinks.forEach { indexedLink ->
                    val link = indexedLink.value
                    add(
                        SourcePanelEntry(
                            key = "${itemKeyPrefix}_${indexedLink.index}",
                            sourceIndex = indexedLink.index,
                            title = link.name.ifBlank { formatSourceMenuLabel(link) },
                            titleMaxLines = 2,
                            supportingTexts = formatSourceDetailsTexts(
                                link = link,
                                unknownQualityLabel = unknownQualityLabel,
                            ),
                        )
                    )
                }
            }
    }
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

private fun extractHostFromUrl(url: String): String? {
    return runCatching {
        android.net.Uri.parse(url).host
    }.getOrNull()
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }
}
