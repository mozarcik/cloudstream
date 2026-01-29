package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import java.util.Locale

/**
 * Sidebar component displaying list of feed categories
 * Task 3.3: Added focus support with FocusRequester
 * 
 * @param feeds List of feed categories to display
 * @param selectedIndex Currently selected feed index
 * @param currentSourceName Currently selected source name
 * @param availableSources Available API sources
 * @param selectedSource Currently selected API source
 * @param onFeedSelected Callback when a feed is selected
 * @param onSourceSelected Callback when source is selected
 * @param focusRequester Focus requester to auto-focus currently selected item
 * @param modifier Modifier for the sidebar container
 */
@Composable
fun FeedSidebar(
    feeds: List<FeedCategory>,
    selectedIndex: Int,
    currentSourceName: String,
    availableSources: List<MainAPI>,
    selectedSource: MainAPI?,
    onFeedSelected: (Int) -> Unit,
    onSourceSelected: (MainAPI) -> Unit,
    onSourcePickerVisibilityChanged: (Boolean) -> Unit = {},
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    var isSourcePickerOpen by remember { mutableStateOf(false) }
    var wasSourcePickerOpen by remember { mutableStateOf(false) }
    var isSourceRowFocused by remember { mutableStateOf(false) }
    val sourceRowFocusRequester = remember { FocusRequester() }
    val sourceListFocusRequester = remember { FocusRequester() }
    val focusedFeedIndex = selectedIndex.coerceIn(0, (feeds.lastIndex).coerceAtLeast(0))
    val sourceLabel = stringResource(R.string.home_source).uppercase(Locale.getDefault())
    val categoriesLabel = stringResource(R.string.tv_home_categories).uppercase(Locale.getDefault())
    val pickSourceLabel = stringResource(R.string.pick_source).uppercase(Locale.getDefault())
    val selectedSourceIndex = availableSources.indexOfFirst { it.name == selectedSource?.name }
        .coerceAtLeast(0)

    fun openSourcePicker() {
        if (availableSources.isEmpty()) return
        isSourcePickerOpen = true
    }

    fun closeSourcePicker() {
        isSourcePickerOpen = false
    }

    BackHandler(enabled = isSourcePickerOpen) {
        closeSourcePicker()
    }

    LaunchedEffect(isSourcePickerOpen, selectedSourceIndex, availableSources.size) {
        onSourcePickerVisibilityChanged(isSourcePickerOpen)
        if (isSourcePickerOpen && availableSources.isNotEmpty()) {
            kotlinx.coroutines.delay(100)
            sourceListFocusRequester.requestFocus()
        } else if (wasSourcePickerOpen) {
            kotlinx.coroutines.delay(80)
            sourceRowFocusRequester.requestFocus()
        }
        wasSourcePickerOpen = isSourcePickerOpen
    }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        colors = androidx.tv.material3.SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = if (isSourcePickerOpen) Modifier.weight(0.42f) else Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        SidebarSectionHeader(sourceLabel)
                    }

                    item {
                        ListItem(
                            selected = isSourceRowFocused || isSourcePickerOpen,
                            onClick = { openSourcePicker() },
                            headlineContent = {
                                Text(
                                    text = currentSourceName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSourceRowFocused) Color.Black else Color.White
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(sourceRowFocusRequester)
                                .onFocusChanged { isSourceRowFocused = it.isFocused }
                                .onPreviewKeyEvent { keyEvent ->
                                    if (
                                        keyEvent.type == KeyEventType.KeyUp &&
                                        keyEvent.key == Key.DirectionRight
                                    ) {
                                        openSourcePicker()
                                        true
                                    } else {
                                        false
                                    }
                                }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        SidebarSectionHeader(categoriesLabel)
                    }

                    // Feed items
                    itemsIndexed(feeds) { index, feed ->
                        var isFocused by remember { mutableStateOf(false) }

                        ListItem(
                            selected = isFocused,
                            onClick = { onFeedSelected(index) },
                            headlineContent = {
                                Text(
                                    text = feed.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = when {
                                        isFocused -> Color.Black
                                        index == selectedIndex -> Color.White
                                        else -> Color.Gray
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    isFocused = focusState.isFocused
                                }
                                .let {
                                    // Focus currently selected feed when sidebar opens.
                                    if (index == focusedFeedIndex) it.focusRequester(focusRequester)
                                    else it
                                }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isSourcePickerOpen,
                enter = slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it / 3 }) + fadeOut(),
                modifier = Modifier.weight(0.58f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            SidebarSectionHeader(pickSourceLabel)
                        }

                        if (availableSources.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.loading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        } else {
                            itemsIndexed(availableSources) { index, source ->
                                var isFocused by remember { mutableStateOf(false) }
                                val isSelected = source.name == selectedSource?.name
                                ListItem(
                                    selected = isFocused,
                                    onClick = { onSourceSelected(source) },
                                    headlineContent = {
                                        Text(
                                            text = source.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isFocused) Color.Black else Color.White
                                        )
                                    },
                                    supportingContent = {
                                        if (isSelected) {
                                            Text(
                                                text = stringResource(R.string.tv_selected),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isFocused) Color.Black.copy(alpha = 0.75f) else Color.Gray
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isFocused = it.isFocused }
                                        .onPreviewKeyEvent { keyEvent ->
                                            if (
                                                keyEvent.type == KeyEventType.KeyUp &&
                                                keyEvent.key == Key.DirectionLeft
                                            ) {
                                                closeSourcePicker()
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                        .let {
                                            if (index == selectedSourceIndex) {
                                                it.focusRequester(sourceListFocusRequester)
                                            } else {
                                                it
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
        color = Color.White.copy(alpha = 0.65f),
        modifier = Modifier.padding(start = 12.dp, bottom = 2.dp, top = 2.dp)
    )
}
