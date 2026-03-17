package com.lagradost.cloudstream3.tv.presentation.screens.library

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.ui.library.ListSorting
import kotlinx.collections.immutable.PersistentList

@Immutable
data class LibrarySectionUiState(
    val id: String,
    val title: String,
    val previewItems: PersistentList<MediaItemCompat>,
    val gridItems: PersistentList<LibraryGridItemUiState>,
    val supportedSortingMethods: PersistentList<ListSorting>,
)

@Immutable
data class LibraryGridItemUiState(
    val mediaItem: MediaItemCompat,
    val originalIndex: Int,
    val name: String,
    val personalRatingHundred: Int? = null,
    val lastUpdatedUnixTime: Long? = null,
    val releaseDateUnixTimeMs: Long? = null,
)
