package com.lagradost.cloudstream3.tv.presentation.screens.library

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

/** One list on the Library screen; [key] is the same key the menu uses for a pinned list. */
@Immutable
internal data class LibraryListRow(
    val key: String,
    val title: String,
    val count: Int,
    val pinned: Boolean,
    val previewItems: List<MediaItemCompat>,
)

/** One account's lists (TMDB, AniList, Simkl, Local, ...). */
@Immutable
internal data class LibraryListGroup(
    val providerName: String,
    val rows: List<LibraryListRow>,
)

@Immutable
internal data class LibraryListsUiState(
    val groups: List<LibraryListGroup> = emptyList(),
    val listCount: Int = 0,
    val pinnedCount: Int = 0,
    val isLoading: Boolean = true,
)
