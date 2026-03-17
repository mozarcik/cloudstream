@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.lagradost.cloudstream3.tv.presentation.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done

private val LibrarySourceSelectorHorizontalPadding = 24.dp
private val LibrarySourceSelectorSpacing = 10.dp
private val LibrarySourceSelectorMaxWidth = 220.dp

@Composable
fun LibrarySourceSelectorRow(
    sources: List<String>,
    selectedSource: String,
    rowEntryFocusRequester: FocusRequester,
    isInteractive: Boolean,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    onSourceSelected: (String) -> Unit,
) {
    val sourceRequesters = remember(sources) {
        sources.map { FocusRequester() }
    }
    val selectedIndex = sources.indexOf(selectedSource).let { index ->
        if (index < 0) 0 else index
    }

    fun requesterFor(index: Int): FocusRequester {
        return if (index == selectedIndex) {
            rowEntryFocusRequester
        } else {
            sourceRequesters[index]
        }
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(LibrarySourceSelectorSpacing),
        contentPadding = PaddingValues(horizontal = LibrarySourceSelectorHorizontalPadding),
        modifier = modifier.fillMaxWidth()
    ) {
        itemsIndexed(
            items = sources,
            key = { _, source -> source }
        ) { index, source ->
            val isSelected = source == selectedSource
            FilterChip(
                selected = isSelected,
                onClick = { onSourceSelected(source) },
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
                modifier = Modifier
                    .widthIn(max = LibrarySourceSelectorMaxWidth)
                    .focusRequester(requesterFor(index))
                    .focusProperties {
                        canFocus = isInteractive
                        up = upFocusRequester ?: FocusRequester.Default
                        down = downFocusRequester ?: FocusRequester.Default
                    }
            ) {
                ProvideTextStyle(value = MaterialTheme.typography.labelMedium) {
                    Text(
                        text = source,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
