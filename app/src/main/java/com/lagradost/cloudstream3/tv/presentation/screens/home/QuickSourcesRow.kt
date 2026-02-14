@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.sourceId
import com.lagradost.cloudstream3.tv.presentation.common.MenuListSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem

private val SourceChipShape = RoundedCornerShape(16.dp)
private val MoreButtonShape = RoundedCornerShape(16.dp)

@Composable
fun QuickSourcesRow(
    quickSources: List<MainAPI>,
    allSourcesCount: Int,
    selectedSource: MainAPI?,
    pinnedSourceIds: Set<String>,
    rowEntryFocusRequester: FocusRequester,
    moreButtonFocusRequester: FocusRequester,
    isInteractive: Boolean,
    downFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    onSourceSelected: (MainAPI) -> Unit,
    onMoreClick: () -> Unit,
) {
    val selectedSourceId = selectedSource?.sourceId()
    val quickSourceIds = remember(quickSources) { quickSources.map { source -> source.sourceId() } }
    val quickSourceRequesters = remember(quickSourceIds) {
        quickSourceIds.map { FocusRequester() }
    }
    val selectedIndex = quickSources.indexOfFirst { source ->
        source.sourceId() == selectedSourceId
    }.let { index ->
        if (index < 0) 0 else index
    }
    val canShowMoreButton = allSourcesCount > 0

    fun requesterForChip(index: Int, entryChipIndex: Int): FocusRequester {
        return if (index == entryChipIndex) {
            rowEntryFocusRequester
        } else {
            quickSourceRequesters[index]
        }
    }

    SubcomposeLayout(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sources_row")
    ) { constraints ->
        val horizontalPaddingPx = minOf(4.dp.roundToPx(), constraints.maxWidth / 2)
        val availableWidth = (constraints.maxWidth - (horizontalPaddingPx * 2)).coerceAtLeast(0)
        val looseConstraints = constraints.copy(
            minWidth = 0,
            minHeight = 0,
            maxWidth = availableWidth
        )
        val itemSpacingPx = 10.dp.roundToPx()

        if (quickSources.isEmpty()) {
            val loadingPlaceable = subcompose("loading") {
                Text(
                    text = stringResource(id = R.string.loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }.first().measure(looseConstraints)
            val layoutHeight = loadingPlaceable.height.coerceIn(
                minimumValue = constraints.minHeight,
                maximumValue = constraints.maxHeight
            )
            return@SubcomposeLayout layout(constraints.maxWidth, layoutHeight) {
                loadingPlaceable.placeRelative(
                    x = horizontalPaddingPx,
                    y = (layoutHeight - loadingPlaceable.height) / 2
                )
            }
        }

        val measuredSourceWidths = quickSources.mapIndexed { index, source ->
            subcompose("measure_source_$index") {
                QuickSourceFilterChip(
                    sourceName = source.name,
                    isSelected = source.sourceId() == selectedSourceId,
                    onClick = {},
                )
            }.first().measure(looseConstraints)
        }

        val measuredMoreButton = if (canShowMoreButton) {
            subcompose("measure_more_button") {
                MoreSourcesButton(onClick = { })
            }
                .first()
                .measure(looseConstraints)
        } else {
            null
        }

        var allSourcesWidth = 0
        measuredSourceWidths.forEachIndexed { index, placeable ->
            allSourcesWidth += placeable.width
            if (index > 0) allSourcesWidth += itemSpacingPx
        }

        val hasOverflow = allSourcesWidth > availableWidth
        val showMoreButton = hasOverflow && measuredMoreButton != null

        var visibleSourcesCount = measuredSourceWidths.size
        if (showMoreButton) {
            visibleSourcesCount = 0
            var usedWidth = 0
            measuredSourceWidths.forEach { placeable ->
                val spacingBeforeSource = if (visibleSourcesCount > 0) itemSpacingPx else 0
                val widthAfterSource = usedWidth + spacingBeforeSource + placeable.width
                val widthWithMore = widthAfterSource + itemSpacingPx + measuredMoreButton.width
                if (widthWithMore <= availableWidth) {
                    usedWidth = widthAfterSource
                    visibleSourcesCount += 1
                }
            }
            if (visibleSourcesCount == 0 && measuredSourceWidths.isNotEmpty()) {
                visibleSourcesCount = 1
            }
        }

        val entryChipIndex = when {
            visibleSourcesCount <= 0 -> -1
            selectedIndex < visibleSourcesCount -> selectedIndex
            else -> 0
        }

        val visibleSourcePlaceables = (0 until visibleSourcesCount).map { index ->
            val source = quickSources[index]
            val sourceId = source.sourceId()

            subcompose("source_$index") {
                QuickSourceFilterChip(
                    sourceName = source.name,
                    isSelected = sourceId == selectedSourceId,
                    onClick = { onSourceSelected(source) },
                    modifier = modifier
                        .focusRequester(requesterForChip(index, entryChipIndex))
                        .focusProperties {
                            down = downFocusRequester ?: FocusRequester.Default
                        }
                        .testTag("source_chip_$sourceId")
                )
            }.first().measure(looseConstraints)
        }

        val moreButtonPlaceable = if (showMoreButton) {
            subcompose("more_button") {
                MoreSourcesButton(
                    onClick = onMoreClick,
                    modifier = modifier.focusProperties {
                        down = downFocusRequester ?: FocusRequester.Default
                    }
                )
            }.first().measure(looseConstraints)
        } else {
            null
        }

        val contentHeight = maxOf(
            visibleSourcePlaceables.maxOfOrNull { placeable -> placeable.height } ?: 0,
            moreButtonPlaceable?.height ?: 0
        )
        val layoutHeight = contentHeight.coerceIn(
            minimumValue = constraints.minHeight,
            maximumValue = constraints.maxHeight
        )
        layout(constraints.maxWidth, layoutHeight) {
            var xPosition = horizontalPaddingPx
            visibleSourcePlaceables.forEachIndexed { index, placeable ->
                placeable.placeRelative(x = xPosition, y = (layoutHeight - placeable.height) / 2)
                xPosition += placeable.width
                if (index < visibleSourcePlaceables.lastIndex || moreButtonPlaceable != null) {
                    xPosition += itemSpacingPx
                }
            }
            moreButtonPlaceable?.placeRelative(
                x = xPosition,
                y = (layoutHeight - moreButtonPlaceable.height) / 2
            )
        }
    }
}

@Composable
private fun QuickSourceFilterChip(
    sourceName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        onClick = onClick,
        selected = isSelected,
        modifier = modifier.widthIn(max = 220.dp),
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        },
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.labelMedium) {
            Text(
                text = sourceName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MoreSourcesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        onClick = onClick,
        selected = false,
        modifier = modifier
            .widthIn(max = 220.dp),
    ) {
        Text(
            text = stringResource(R.string.tv_home_more_sources),
            style = MaterialTheme.typography.bodyLarge,
        )
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SourcesMorePanel(
    visible: Boolean,
    sources: List<MainAPI>,
    selectedSource: MainAPI?,
    pinnedSourceIds: Set<String>,
    usageCountBySourceId: Map<String, Int>,
    sortMode: SourceSortMode,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSortModeChange: (SourceSortMode) -> Unit,
    onSourceSelected: (MainAPI) -> Unit,
    onTogglePin: (MainAPI) -> Unit,
    onCloseRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedSourceId = selectedSource?.sourceId()
    val menuItems = sources.map { source ->
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

    MenuListSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        title = stringResource(R.string.pick_source),
        items = menuItems,
        panelWidth = 340.dp,
        initialFocusedItemId = selectedSourceId ?: menuItems.firstOrNull()?.id,
        modifier = modifier,
        panelTestTag = "sources_panel",
        headerContent = {
            SourceSearchField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
            )

            Spacer(modifier = Modifier.height(10.dp))

            SourcesSortModeRow(
                selectedSortMode = sortMode,
                onSortModeChange = onSortModeChange
            )

            Spacer(modifier = Modifier.height(12.dp))
        },
        emptyContent = {
            Text(
                text = stringResource(R.string.tv_home_no_sources_found),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    )
}

@Composable
private fun SourceSearchField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isFocused) {
                    Color.White.copy(alpha = 0.12f)
                } else {
                    Color.White.copy(alpha = 0.08f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 1.dp else 0.dp,
                color = Color.White.copy(alpha = 0.38f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize
            ),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (isFocused) Color.White else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(16.dp)
                    )
                    Box {
                        if (value.isBlank()) {
                            Text(
                                text = stringResource(R.string.tv_home_search_sources_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
private fun SourcesSortModeRow(
    selectedSortMode: SourceSortMode,
    onSortModeChange: (SourceSortMode) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SortModeButton(
            text = stringResource(R.string.tv_home_sort_most_used),
            selected = selectedSortMode == SourceSortMode.MOST_USED,
            onClick = { onSortModeChange(SourceSortMode.MOST_USED) }
        )
        SortModeButton(
            text = stringResource(R.string.tv_home_sort_az),
            selected = selectedSortMode == SourceSortMode.AZ,
            onClick = { onSortModeChange(SourceSortMode.AZ) }
        )
    }
}

@Composable
private fun SortModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) {
                Color.White.copy(alpha = 0.22f)
            } else {
                Color.White.copy(alpha = 0.08f)
            },
            focusedContainerColor = Color.White.copy(alpha = 0.26f)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.38f)),
                shape = RoundedCornerShape(12.dp)
            )
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}
