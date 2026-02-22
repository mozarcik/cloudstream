package com.lagradost.cloudstream3.tv.presentation.screens.search

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedLoadState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class SearchSectionUiState(
    val id: String,
    val title: String,
    val state: HomeFeedLoadState = HomeFeedLoadState.Loading,
) {
    val items: PersistentList<MediaItemCompat>
        get() = (state as? HomeFeedLoadState.Success)?.items ?: persistentListOf()

    val isInteractive: Boolean
        get() = items.isNotEmpty()
}

@Immutable
data class SearchScreenUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val sections: PersistentList<SearchSectionUiState> = persistentListOf(),
)
