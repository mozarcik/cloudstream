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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Update
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

private fun librarySortIcon(method: ListSorting?): ImageVector {
    return when (method) {
        null,
        ListSorting.Query -> Icons.AutoMirrored.Outlined.Sort
        ListSorting.RatingHigh,
        ListSorting.RatingLow -> Icons.Outlined.StarOutline
        ListSorting.AlphabeticalA,
        ListSorting.AlphabeticalZ -> Icons.Outlined.SortByAlpha
        ListSorting.UpdatedNew -> Icons.Outlined.Update
        ListSorting.UpdatedOld -> Icons.Outlined.History
        ListSorting.ReleaseDateNew,
        ListSorting.ReleaseDateOld -> Icons.Outlined.CalendarToday
    }
}

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
    var shouldRestoreSortButtonFocus by rememberSaveable(section.id) { mutableStateOf(true) }
    var sortResetGridToken by rememberSaveable(section.id) { mutableIntStateOf(0) }
    var firstGridItemFocusRequestToken by rememberSaveable(section.id) { mutableIntStateOf(0) }
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
            shouldRestoreSortButtonFocus = true
            return@LaunchedEffect
        }

        if (!isSortPanelOpen && wasSortPanelOpen) {
            if (shouldRestoreSortButtonFocus) {
                sortButtonFocusRequestToken += 1
            }
            shouldRestoreSortButtonFocus = true
        }
        wasSortPanelOpen = isSortPanelOpen
    }

    LaunchedEffect(sortResetGridToken, isSortPanelOpen, sortedGridItems) {
        if (sortResetGridToken <= 0 || isSortPanelOpen || sortedGridItems.isEmpty()) {
            return@LaunchedEffect
        }

        gridState.scrollToItem(0)
        firstGridItemFocusRequestToken = sortResetGridToken
    }

    BackHandler(enabled = isSortPanelOpen) {
        isSortPanelOpen = false
    }

    FocusRequestEffect(
        requester = if (hasSortOptions) {
            sortButtonFocusRequester
        } else {
            firstItemFocusRequester
        },
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

    FocusRequestEffect(
        requester = firstItemFocusRequester,
        requestKey = firstGridItemFocusRequestToken,
        enabled = firstGridItemFocusRequestToken > 0 &&
            !isSortPanelOpen &&
            sortedGridItems.isNotEmpty(),
        onFocused = {
            hasInitialFocusBeenHandled = true
        }
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
                            sortMethod = selectedSortMethod,
                            focusRequester = sortButtonFocusRequester,
                            upFocusRequester = topBarFocusRequester,
                            downFocusRequester = firstItemFocusRequester,
                            onClick = {
                                shouldRestoreSortButtonFocus = true
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
                    shouldRestoreSortButtonFocus = false
                    sortResetGridToken += 1
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
    sortMethod: ListSorting?,
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
                imageVector = librarySortIcon(sortMethod),
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
    val panelOptions = remember(supportedSortingMethods, selectedSortMethod) {
        buildLibrarySortPanelOptions(
            supportedSortingMethods = supportedSortingMethods,
            selectedSortMethod = selectedSortMethod,
        )
    }
    val panelItems = panelOptions.map { option ->
        val method = option.method
        SidePanelMenuItem(
            id = option.id,
            title = stringResource((method ?: ListSorting.Query).stringRes),
            selected = option.selected,
            onClick = { onSortSelected(method) },
            leadingContent = {
                Icon(
                    imageVector = librarySortIcon(method),
                    contentDescription = null
                )
            }
        )
    }

    MenuListSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        title = stringResource(R.string.sort_by),
        items = panelItems,
        initialFocusedItemId = selectedSortMethod?.let { method ->
            "library_sort_${method.name}"
        } ?: "library_sort_default",
        showSelectionRadio = true,
        selectionIndicatorStyle = SidePanelSelectionIndicatorStyle.Checkmark,
        panelTestTag = "library_sort_panel",
    )
}

internal fun buildLibrarySortPanelOptions(
    supportedSortingMethods: List<ListSorting>,
    selectedSortMethod: ListSorting?,
): List<LibrarySortPanelOption> {
    return buildList {
        add(
            LibrarySortPanelOption(
                id = "library_sort_default",
                method = null,
                selected = selectedSortMethod == null,
            )
        )

        supportedSortingMethods.forEach { method ->
            add(
                LibrarySortPanelOption(
                    id = "library_sort_${method.name}",
                    method = method,
                    selected = selectedSortMethod == method,
                )
            )
        }
    }
}

internal data class LibrarySortPanelOption(
    val id: String,
    val method: ListSorting?,
    val selected: Boolean,
)
