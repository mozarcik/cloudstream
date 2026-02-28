@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.sourceId

@Composable
fun QuickSourcesRow(
    quickSources: List<MainAPI>,
    allSourcesCount: Int,
    selectedSource: MainAPI?,
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
                            canFocus = isInteractive
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
                    modifier = modifier
                        .focusRequester(moreButtonFocusRequester)
                        .focusProperties {
                            canFocus = isInteractive
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
            style = MaterialTheme.typography.labelMedium,
        )
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}
