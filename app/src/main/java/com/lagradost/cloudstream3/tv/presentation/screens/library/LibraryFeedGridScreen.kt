@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.lagradost.cloudstream3.tv.presentation.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.HaloHost
import com.lagradost.cloudstream3.tv.presentation.common.MenuListSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelSelectionIndicatorStyle
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.screens.home.MediaGridStatic
import com.lagradost.cloudstream3.ui.library.ListSorting

private val LibraryFeedGridHeaderHorizontalPadding = 24.dp
private val LibraryFeedGridHeaderVerticalPadding = 12.dp
private val LibraryFeedGridHeaderSpacing = 12.dp
private val LibraryFeedGridSortChipMaxWidth = 220.dp

@Composable
fun LibraryFeedGridScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onBack: () -> Unit,
    onScroll: (Boolean) -> Unit,
    topBarFocusRequester: FocusRequester,
    restoreFocusToken: Int = 0,
) {
    val selectedSection by LibraryFeedGridSelectionStore.selectedSection.collectAsState()

    if (selectedSection == null) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    val section = selectedSection ?: return
    val gridState = rememberLazyGridState()
    val firstItemFocusRequester = remember { FocusRequester() }
    val sortButtonFocusRequester = remember { FocusRequester() }
    val pendingRestoreTargetId = LibraryFeedGridSelectionStore.pendingRestoreTargetId
    val hasSortOptions = section.supportedSortingMethods.isNotEmpty()

    var selectedSortMethodOrdinal by rememberSaveable(section.id) { mutableStateOf<Int?>(null) }
    var isSortPanelOpen by rememberSaveable(section.id) { mutableStateOf(false) }
    var wasSortPanelOpen by rememberSaveable(section.id) { mutableStateOf(false) }
    var sortButtonFocusRequestToken by rememberSaveable(section.id) { mutableIntStateOf(0) }
    var hasInitialFocusBeenHandled by rememberSaveable(section.id) { mutableStateOf(false) }

    val selectedSortMethod = remember(selectedSortMethodOrdinal) {
        selectedSortMethodOrdinal?.let { ordinal ->
            ListSorting.entries.getOrNull(ordinal)
        }
    }
    val sortedGridItems = remember(section.gridItems, selectedSortMethod) {
        sortLibraryGridItems(
            items = section.gridItems,
            method = selectedSortMethod
        )
    }
    val displayItems = remember(sortedGridItems) {
        sortedGridItems.map { item -> item.mediaItem }
    }
    val currentSortLabel = stringResource(
        id = selectedSortMethod?.stringRes ?: ListSorting.Query.stringRes
    )

    LaunchedEffect(Unit) {
        onScroll(true)
    }

    LaunchedEffect(isSortPanelOpen, hasSortOptions) {
        if (!hasSortOptions) {
            wasSortPanelOpen = false
            return@LaunchedEffect
        }

        if (!isSortPanelOpen && wasSortPanelOpen) {
            sortButtonFocusRequestToken += 1
        }
        wasSortPanelOpen = isSortPanelOpen
    }

    BackHandler(enabled = isSortPanelOpen) {
        isSortPanelOpen = false
    }

    FocusRequestEffect(
        requester = firstItemFocusRequester,
        requestKey = section.id,
        enabled = pendingRestoreTargetId == null &&
            !isSortPanelOpen &&
            !hasInitialFocusBeenHandled,
        onFocused = {
            hasInitialFocusBeenHandled = true
        }
    )

    FocusRequestEffect(
        requester = sortButtonFocusRequester,
        requestKey = sortButtonFocusRequestToken,
        enabled = sortButtonFocusRequestToken > 0
    )

    HaloHost(
        modifier = Modifier.fillMaxSize()
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LibraryFeedGridHeaderSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(
                            start = LibraryFeedGridHeaderHorizontalPadding,
                            top = LibraryFeedGridHeaderVerticalPadding,
                            end = LibraryFeedGridHeaderHorizontalPadding,
                            bottom = LibraryFeedGridHeaderVerticalPadding
                        )
                ) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    if (hasSortOptions) {
                        LibraryGridSortChip(
                            label = currentSortLabel,
                            focusRequester = sortButtonFocusRequester,
                            upFocusRequester = topBarFocusRequester,
                            downFocusRequester = firstItemFocusRequester,
                            onClick = {
                                isSortPanelOpen = true
                            }
                        )
                    }
                }

                MediaGridStatic(
                    items = displayItems,
                    onMediaClick = { item ->
                        LibraryFeedGridSelectionStore.scheduleRestoreToLastFocused()
                        onMediaClick(item)
                    },
                    gridState = gridState,
                    firstItemFocusRequester = firstItemFocusRequester,
                    upFocusRequester = if (hasSortOptions) {
                        sortButtonFocusRequester
                    } else {
                        topBarFocusRequester
                    },
                    focusKeyPrefix = "library_feed_grid",
                    pendingRestoreFocusTargetId = pendingRestoreTargetId,
                    restoreFocusToken = restoreFocusToken,
                    onItemFocused = LibraryFeedGridSelectionStore::onTargetFocused,
                    onRestoreFocusConsumed = { targetId ->
                        hasInitialFocusBeenHandled = true
                        LibraryFeedGridSelectionStore.clearPendingRestore(targetId)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
            }

            LibrarySortPanel(
                visible = isSortPanelOpen,
                supportedSortingMethods = section.supportedSortingMethods,
                selectedSortMethod = selectedSortMethod,
                onSortSelected = { method ->
                    selectedSortMethodOrdinal = method?.ordinal
                    isSortPanelOpen = false
                },
                onCloseRequested = {
                    isSortPanelOpen = false
                }
            )
        }
    }
}

@Composable
private fun LibraryGridSortChip(
    label: String,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = null
            )
        },
        modifier = Modifier
            .widthIn(max = LibraryFeedGridSortChipMaxWidth)
            .focusRequester(focusRequester)
            .focusProperties {
                up = upFocusRequester
                down = downFocusRequester
            }
            .padding(start = 8.dp)
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.labelMedium) {
            Text(
                text = "${stringResource(R.string.sort)}: $label",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LibrarySortPanel(
    visible: Boolean,
    supportedSortingMethods: List<ListSorting>,
    selectedSortMethod: ListSorting?,
    onSortSelected: (ListSorting?) -> Unit,
    onCloseRequested: () -> Unit,
) {
    val panelItems = remember(supportedSortingMethods, selectedSortMethod, onSortSelected) {
        buildList {
            add(
                SidePanelMenuItem(
                    id = "library_sort_default",
                    title = "None",
                    selected = selectedSortMethod == null,
                    onClick = { onSortSelected(null) }
                )
            )

            supportedSortingMethods.forEach { method ->
                add(
                    SidePanelMenuItem(
                        id = "library_sort_${method.name}",
                        title = method.name,
                        selected = selectedSortMethod == method,
                        onClick = { onSortSelected(method) }
                    )
                )
            }
        }
    }

    MenuListSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        title = stringResource(R.string.sort_by),
        items = panelItems.map { item ->
            item.copy(
                title = when (item.id) {
                    "library_sort_default" -> stringResource(ListSorting.Query.stringRes)
                    else -> {
                        val method = supportedSortingMethods.firstOrNull { sort ->
                            item.id == "library_sort_${sort.name}"
                        }
                        if (method != null) {
                            stringResource(method.stringRes)
                        } else {
                            item.title
                        }
                    }
                }
            )
        },
        initialFocusedItemId = selectedSortMethod?.let { method ->
            "library_sort_${method.name}"
        } ?: "library_sort_default",
        showSelectionRadio = true,
        selectionIndicatorStyle = SidePanelSelectionIndicatorStyle.Checkmark,
        panelTestTag = "library_sort_panel",
    )
}
