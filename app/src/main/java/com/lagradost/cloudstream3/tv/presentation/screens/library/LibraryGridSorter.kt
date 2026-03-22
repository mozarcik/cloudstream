package com.lagradost.cloudstream3.tv.presentation.screens.library

import com.lagradost.cloudstream3.ui.library.ListSorting
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

private val LibraryGridOriginalOrderComparator =
    compareBy<LibraryGridItemUiState> { item -> item.originalIndex }

private val LibraryGridAlphabeticalAscendingComparator =
    compareBy<LibraryGridItemUiState, String>(String.CASE_INSENSITIVE_ORDER) { item -> item.name }
        .thenBy { item -> item.name }
        .thenBy { item -> item.originalIndex }

private val LibraryGridAlphabeticalDescendingComparator =
    compareByDescending<LibraryGridItemUiState, String>(String.CASE_INSENSITIVE_ORDER) { item ->
        item.name
    }
        .thenByDescending { item -> item.name }
        .thenBy { item -> item.originalIndex }

private val LibraryGridRatingHighComparator =
    compareByDescending<LibraryGridItemUiState> { item -> item.personalRatingHundred != null }
        .thenByDescending { item -> item.personalRatingHundred ?: Int.MIN_VALUE }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { item -> item.name }
        .thenBy { item -> item.name }
        .thenBy { item -> item.originalIndex }

private val LibraryGridRatingLowComparator =
    compareByDescending<LibraryGridItemUiState> { item -> item.personalRatingHundred != null }
        .thenBy { item -> item.personalRatingHundred ?: Int.MAX_VALUE }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { item -> item.name }
        .thenBy { item -> item.name }
        .thenBy { item -> item.originalIndex }

private val LibraryGridUpdatedNewComparator =
    compareByDescending<LibraryGridItemUiState> { item -> item.lastUpdatedUnixTime != null }
        .thenByDescending { item -> item.lastUpdatedUnixTime ?: Long.MIN_VALUE }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { item -> item.name }
        .thenBy { item -> item.name }
        .thenBy { item -> item.originalIndex }

private val LibraryGridUpdatedOldComparator =
    compareByDescending<LibraryGridItemUiState> { item -> item.lastUpdatedUnixTime != null }
        .thenBy { item -> item.lastUpdatedUnixTime ?: Long.MAX_VALUE }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { item -> item.name }
        .thenBy { item -> item.name }
        .thenBy { item -> item.originalIndex }

private val LibraryGridReleaseDateNewComparator =
    compareByDescending<LibraryGridItemUiState> { item -> item.releaseDateUnixTimeMs != null }
        .thenByDescending { item -> item.releaseDateUnixTimeMs ?: Long.MIN_VALUE }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { item -> item.name }
        .thenBy { item -> item.name }
        .thenBy { item -> item.originalIndex }

private val LibraryGridReleaseDateOldComparator =
    compareByDescending<LibraryGridItemUiState> { item -> item.releaseDateUnixTimeMs != null }
        .thenBy { item -> item.releaseDateUnixTimeMs ?: Long.MAX_VALUE }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { item -> item.name }
        .thenBy { item -> item.name }
        .thenBy { item -> item.originalIndex }

internal fun sortLibraryGridItems(
    items: List<LibraryGridItemUiState>,
    method: ListSorting?,
): PersistentList<LibraryGridItemUiState> {
    val sortedItems = when (method) {
        null -> items.sortedWith(LibraryGridOriginalOrderComparator)
        ListSorting.Query -> items.sortedWith(LibraryGridOriginalOrderComparator)
        ListSorting.RatingHigh -> items.sortedWith(LibraryGridRatingHighComparator)
        ListSorting.RatingLow -> items.sortedWith(LibraryGridRatingLowComparator)
        ListSorting.AlphabeticalA -> items.sortedWith(LibraryGridAlphabeticalAscendingComparator)
        ListSorting.AlphabeticalZ -> items.sortedWith(LibraryGridAlphabeticalDescendingComparator)
        ListSorting.UpdatedNew -> items.sortedWith(LibraryGridUpdatedNewComparator)
        ListSorting.UpdatedOld -> items.sortedWith(LibraryGridUpdatedOldComparator)
        ListSorting.ReleaseDateNew -> items.sortedWith(LibraryGridReleaseDateNewComparator)
        ListSorting.ReleaseDateOld -> items.sortedWith(LibraryGridReleaseDateOldComparator)
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
