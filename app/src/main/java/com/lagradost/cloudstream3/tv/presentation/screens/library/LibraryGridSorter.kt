package com.lagradost.cloudstream3.tv.presentation.screens.library

import com.lagradost.cloudstream3.ui.library.ListSorting
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal fun sortLibraryGridItems(
    items: List<LibraryGridItemUiState>,
    method: ListSorting?,
): PersistentList<LibraryGridItemUiState> {
    val sortedItems = when (method) {
        null -> items.sortedBy { item -> item.originalIndex }
        ListSorting.Query -> items.sortedBy { item -> item.originalIndex }
        ListSorting.RatingHigh -> items.sortedBy { item ->
            -(item.personalRatingHundred ?: 0)
        }
        ListSorting.RatingLow -> items.sortedBy { item ->
            item.personalRatingHundred ?: 0
        }
        ListSorting.AlphabeticalA -> items.sortedBy { item -> item.name }
        ListSorting.AlphabeticalZ -> items.sortedBy { item -> item.name }.reversed()
        ListSorting.UpdatedNew -> items.sortedBy { item ->
            item.lastUpdatedUnixTime?.times(-1)
        }
        ListSorting.UpdatedOld -> items.sortedBy { item -> item.lastUpdatedUnixTime }
        ListSorting.ReleaseDateNew -> items.sortedByDescending { item ->
            item.releaseDateUnixTimeMs
        }
        ListSorting.ReleaseDateOld -> items.sortedBy { item -> item.releaseDateUnixTimeMs }
    }

    return sortedItems.toPersistentList()
}

internal fun resolveSupportedLibrarySortingMethods(
    supportedMethods: Set<ListSorting>,
): PersistentList<ListSorting> {
    return ListSorting.entries
        .filter { method ->
            method != ListSorting.Query && supportedMethods.contains(method)
        }
        .toPersistentList()
}
