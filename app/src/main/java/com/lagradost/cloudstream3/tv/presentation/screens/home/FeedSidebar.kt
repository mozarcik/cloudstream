package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
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
 * @param onCloseRequested Callback to close sidebar overlay
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
    onCloseRequested: () -> Unit,
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

    val drawerShape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 16.dp)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = drawerShape
            ),
        shape = drawerShape,
        colors = androidx.tv.material3.SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 16.dp)
        ) {
            Column(
                modifier = if (isSourcePickerOpen) Modifier.weight(0.42f) else Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        SidebarSectionHeader(sourceLabel)
                    }

                    item {
                        SidebarDrawerRow(
                            title = currentSourceName,
                            subtitle = stringResource(R.string.pick_source),
                            iconResId = R.drawable.ic_baseline_filter_list_24,
                            isFocused = isSourceRowFocused || isSourcePickerOpen,
                            isSelected = true,
                            onClick = { openSourcePicker() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(sourceRowFocusRequester)
                                .onFocusChanged { isSourceRowFocused = it.isFocused }
                                .onPreviewKeyEvent { keyEvent ->
                                    if (
                                        keyEvent.type == KeyEventType.KeyDown &&
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
                        Spacer(modifier = Modifier.height(6.dp))
                        SidebarSectionHeader(categoriesLabel)
                    }

                    itemsIndexed(feeds) { index, feed ->
                        var isFocused by remember { mutableStateOf(false) }
                        val isSelected = index == selectedIndex
                        SidebarDrawerRow(
                            title = feed.name,
                            subtitle = if (isSelected) stringResource(R.string.tv_selected) else null,
                            iconResId = R.drawable.ic_baseline_play_arrow_24,
                            isFocused = isFocused,
                            isSelected = isSelected,
                            onClick = { onFeedSelected(index) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    isFocused = focusState.isFocused
                                }
                                .onPreviewKeyEvent { keyEvent ->
                                    if (
                                        !isSourcePickerOpen &&
                                        keyEvent.type == KeyEventType.KeyDown &&
                                        keyEvent.key == Key.DirectionRight
                                    ) {
                                        onCloseRequested()
                                        true
                                    } else {
                                        false
                                    }
                                }
                                .let {
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
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
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
                                SidebarDrawerRow(
                                    title = source.name,
                                    subtitle = if (isSelected) stringResource(R.string.tv_selected) else null,
                                    iconResId = R.drawable.ic_baseline_play_arrow_24,
                                    isFocused = isFocused,
                                    isSelected = isSelected,
                                    onClick = { onSourceSelected(source) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isFocused = it.isFocused }
                                        .onPreviewKeyEvent { keyEvent ->
                                            if (
                                                keyEvent.type == KeyEventType.KeyDown &&
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
private fun SidebarDrawerRow(
    title: String,
    subtitle: String?,
    iconResId: Int,
    isFocused: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowShape = RoundedCornerShape(16.dp)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = rowShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.015f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.34f)
                ),
                shape = rowShape
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) {
                Color.White.copy(alpha = 0.08f)
            } else {
                Color.Transparent
            },
            focusedContainerColor = Color.White.copy(alpha = 0.18f)
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (isFocused) {
                            Color.White.copy(alpha = 0.95f)
                        } else {
                            Color.White.copy(alpha = 0.26f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    tint = if (isFocused) Color.Black else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.94f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isFocused) {
                            Color.White.copy(alpha = 0.84f)
                        } else {
                            Color.White.copy(alpha = 0.54f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
        color = Color.White.copy(alpha = 0.54f),
        modifier = Modifier.padding(start = 12.dp, bottom = 3.dp, top = 2.dp)
    )
}
