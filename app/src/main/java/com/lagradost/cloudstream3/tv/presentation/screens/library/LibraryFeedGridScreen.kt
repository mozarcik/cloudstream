package com.lagradost.cloudstream3.tv.presentation.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.screens.grid.TvGridSort
import com.lagradost.cloudstream3.tv.presentation.screens.grid.TvGridSortOption
import com.lagradost.cloudstream3.tv.presentation.screens.grid.TvShowMoreGridScreen
import com.lagradost.cloudstream3.ui.library.ListSorting
import kotlinx.coroutines.flow.flowOf

/**
 * Library "Show more": the shared grid showing one list. Sorting is offered only when the list's
 * provider supports it, and only with the methods it supports.
 */
@Composable
fun LibraryFeedGridScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onBack: () -> Unit,
    restoreFocusToken: Int = 0,
) {
    val selectedSection by LibraryFeedGridSelectionStore.selectedSection.collectAsState()
    val section = selectedSection
    if (section == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var selectedSortOrdinal by rememberSaveable(section.id) { mutableStateOf<Int?>(null) }
    val selectedSortMethod = selectedSortOrdinal?.let { ordinal -> ListSorting.entries.getOrNull(ordinal) }

    val sortOptions = buildLibrarySortPanelOptions(
        supportedSortingMethods = section.supportedSortingMethods,
        selectedSortMethod = selectedSortMethod,
    )
    val sort = if (section.supportedSortingMethods.isEmpty()) {
        null
    } else {
        TvGridSort(
            options = sortOptions.map { option ->
                TvGridSortOption(
                    id = option.id,
                    label = stringResource((option.method ?: ListSorting.Query).stringRes),
                )
            },
            selectedId = sortOptions.first { option -> option.selected }.id,
            onSelected = { id ->
                selectedSortOrdinal = sortOptions.firstOrNull { option -> option.id == id }?.method?.ordinal
            },
        )
    }

    val pagingItems = remember(section.gridItems, selectedSortMethod) {
        flowOf(
            PagingData.from(
                sortLibraryGridItems(items = section.gridItems, method = selectedSortMethod)
                    .map { item -> item.mediaItem }
            )
        )
    }.collectAsLazyPagingItems()

    TvShowMoreGridScreen(
        title = section.title,
        subtitle = LibraryFeedGridSelectionStore.accountName,
        pagingItems = pagingItems,
        focusStore = LibraryFeedGridSelectionStore.focus,
        onMediaClick = onMediaClick,
        screenKey = section.id,
        sort = sort,
        restoreFocusToken = restoreFocusToken,
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
