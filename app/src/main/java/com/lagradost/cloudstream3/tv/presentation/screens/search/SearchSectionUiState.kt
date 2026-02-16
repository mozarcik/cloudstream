package com.lagradost.cloudstream3.tv.presentation.screens.search

import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedLoadState

data class SearchSectionUiState(
    val id: String,
    val title: String,
    val state: HomeFeedLoadState = HomeFeedLoadState.Loading,
) {
    val items: List<MediaItemCompat>
        get() = (state as? HomeFeedLoadState.Success)?.items.orEmpty()

    val isInteractive: Boolean
        get() = items.isNotEmpty()
}

data class SearchScreenUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val sections: List<SearchSectionUiState> = emptyList(),
)
