@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.sourceId
import com.lagradost.cloudstream3.tv.presentation.common.MenuListSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem

@Composable
fun SourcesMorePanel(
    visible: Boolean,
    sources: List<MainAPI>,
    selectedSource: MainAPI?,
    pinnedSourceIds: Set<String>,
    usageCountBySourceId: Map<String, Int>,
    onSourceSelected: (MainAPI) -> Unit,
    onTogglePin: (MainAPI) -> Unit,
    onCloseRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedSourceId = selectedSource?.sourceId()
    val menuItems = remember(
        sources,
        selectedSourceId,
        pinnedSourceIds,
        usageCountBySourceId,
        onSourceSelected,
        onTogglePin,
    ) {
        buildSourceMenuItems(
            sources = sources,
            selectedSourceId = selectedSourceId,
            pinnedSourceIds = pinnedSourceIds,
            usageCountBySourceId = usageCountBySourceId,
            onSourceSelected = onSourceSelected,
            onTogglePin = onTogglePin
        )
    }

    MenuListSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        title = stringResource(R.string.pick_source),
        items = menuItems,
        panelWidth = 340.dp,
        initialFocusedItemId = selectedSourceId ?: menuItems.firstOrNull()?.id,
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
    usageCountBySourceId: Map<String, Int>,
    onSourceSelected: (MainAPI) -> Unit,
    onTogglePin: (MainAPI) -> Unit,
): List<SidePanelMenuItem> {
    return sources.map { source ->
        val sourceId = source.sourceId()
        val isSelected = sourceId == selectedSourceId
        val isPinned = pinnedSourceIds.contains(sourceId)
        val usageCount = usageCountBySourceId[sourceId] ?: 0

        SidePanelMenuItem(
            id = sourceId,
            title = source.name,
            selected = isSelected,
            testTag = "sources_item_$sourceId",
            onClick = { onSourceSelected(source) },
            onMenuClick = { onTogglePin(source) },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (usageCount > 0) {
                        Text(
                            text = usageCount.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.alpha(0.62f)
                        )
                    }
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { }
                    )
                }
            }
        )
    }
}
