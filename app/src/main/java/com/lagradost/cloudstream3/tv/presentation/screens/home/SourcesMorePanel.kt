@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.compat.home.sourceId
import com.lagradost.cloudstream3.tv.presentation.common.MenuListSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/** Group a source is listed under (after "Pinned on Home"), by the kind of content it offers. */
internal enum class SourceGroup { MoviesSeries, Anime, Live, Other }

internal fun MainAPI.sourceGroup(): SourceGroup {
    val types = supportedTypes
    return when {
        types.any { it == TvType.Anime || it == TvType.OVA || it == TvType.AnimeMovie } -> SourceGroup.Anime
        types.any { it == TvType.Movie || it == TvType.TvSeries || it == TvType.Cartoon || it == TvType.Documentary } ->
            SourceGroup.MoviesSeries
        types.any { it == TvType.Live } -> SourceGroup.Live
        else -> SourceGroup.Other
    }
}

/** Every installed source: pinned ones first, then grouped by content type; OK picks, long OK pins. */
@Composable
fun SourcesMorePanel(
    visible: Boolean,
    sources: List<MainAPI>,
    selectedSource: MainAPI?,
    pinnedSourceIds: Set<String>,
    onSourceSelected: (MainAPI) -> Unit,
    onTogglePin: (MainAPI) -> Unit,
    onCloseRequested: () -> Unit,
    onExitUpRequested: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val selectedSourceId = selectedSource?.sourceId()
    val pinnedTitle = stringResource(R.string.tv_sources_group_pinned)
    val groupTitles = mapOf(
        SourceGroup.MoviesSeries to stringResource(R.string.tv_sources_group_movies_series),
        SourceGroup.Anime to stringResource(R.string.anime),
        SourceGroup.Live to stringResource(R.string.tv_sources_group_live),
        SourceGroup.Other to stringResource(R.string.others),
    )
    val menuItems = remember(
        sources, selectedSourceId, pinnedSourceIds, onSourceSelected, onTogglePin, pinnedTitle, groupTitles,
    ) {
        buildSourceMenuItems(
            sources = sources,
            selectedSourceId = selectedSourceId,
            pinnedSourceIds = pinnedSourceIds,
            pinnedTitle = pinnedTitle,
            groupTitles = groupTitles,
            onSourceSelected = onSourceSelected,
            onTogglePin = onTogglePin,
        )
    }

    MenuListSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        title = stringResource(R.string.tv_sources_panel_title),
        subtitle = stringResource(R.string.tv_sources_panel_subtitle, sources.size),
        items = menuItems,
        panelWidth = TvDimens.PanelWidth,
        initialFocusedItemId = selectedSourceId ?: menuItems.firstOrNull { !it.isSectionHeader }?.id,
        onExitUpRequested = onExitUpRequested,
        modifier = modifier,
        panelTestTag = "sources_panel",
        emptyContent = {
            Text(
                text = stringResource(R.string.tv_home_no_sources_found),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    )
}

private fun buildSourceMenuItems(
    sources: List<MainAPI>,
    selectedSourceId: String?,
    pinnedSourceIds: Set<String>,
    pinnedTitle: String,
    groupTitles: Map<SourceGroup, String>,
    onSourceSelected: (MainAPI) -> Unit,
    onTogglePin: (MainAPI) -> Unit,
): List<SidePanelMenuItem> {
    fun row(source: MainAPI): SidePanelMenuItem {
        val sourceId = source.sourceId()
        val isPinned = sourceId in pinnedSourceIds
        return SidePanelMenuItem(
            id = sourceId,
            title = source.name,
            selected = sourceId == selectedSourceId,
            testTag = "sources_item_$sourceId",
            leadingTileText = source.name.take(2),
            onClick = { onSourceSelected(source) },
            onLongClick = { onTogglePin(source) },
            // The remote's Menu key keeps pinning as well.
            onMenuClick = { onTogglePin(source) },
            trailingContent = {
                SourceRowTrailing(
                    language = source.lang,
                    isPinned = isPinned,
                    isSelected = sourceId == selectedSourceId,
                )
            },
        )
    }

    val pinned = sources.filter { it.sourceId() in pinnedSourceIds }
    val rest = sources.filter { it.sourceId() !in pinnedSourceIds }.groupBy { it.sourceGroup() }

    return buildList {
        if (pinned.isNotEmpty()) {
            add(SidePanelMenuItem(id = "group:pinned", title = pinnedTitle, isSectionHeader = true))
            pinned.forEach { add(row(it)) }
        }
        SourceGroup.entries.forEach { group ->
            val groupSources = rest[group].orEmpty()
            if (groupSources.isEmpty()) return@forEach
            add(SidePanelMenuItem(id = "group:${group.name}", title = groupTitles.getValue(group), isSectionHeader = true))
            groupSources.forEach { add(row(it)) }
        }
    }
}

@Composable
private fun SourceRowTrailing(language: String, isPinned: Boolean, isSelected: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = language.uppercase(), style = TvTextStyles.Meta, color = TvColors.Label)
        if (isPinned) {
            Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(14.dp))
        }
        if (isSelected) {
            Row(
                Modifier
                    .size(TvDimens.NavMenuDotSize)
                    .background(TvColors.Accent, CircleShape)
            ) {}
        }
    }
}
