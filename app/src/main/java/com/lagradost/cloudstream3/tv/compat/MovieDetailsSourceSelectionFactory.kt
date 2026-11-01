package com.lagradost.cloudstream3.tv.compat

import com.lagradost.cloudstream3.tv.data.util.buildSourcePanelEntries
import com.lagradost.cloudstream3.utils.ExtractorLink

private const val NonInteractivePanelItemId = Int.MIN_VALUE

internal fun buildMovieDetailsSourceSelectionPanelItems(
    links: List<ExtractorLink>,
    itemKeyPrefix: String,
): List<MovieDetailsCompatPanelItem> {
    return buildSourcePanelEntries(
        orderedLinks = links,
        itemKeyPrefix = itemKeyPrefix,
    ).map { entry ->
        MovieDetailsCompatPanelItem(
            id = entry.sourceIndex ?: NonInteractivePanelItemId,
            key = entry.key,
            label = entry.title,
            titleMaxLines = entry.titleMaxLines,
            supportingTexts = entry.supportingTexts,
            isSectionHeader = entry.isSectionHeader,
        )
    }
}
