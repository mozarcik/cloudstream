package com.lagradost.cloudstream3.tv.presentation.screens.search

import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

data class SearchSectionUiState(
    val id: String,
    val title: String,
    val items: List<MediaItemCompat>,
)

data class SearchScreenUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val sections: List<SearchSectionUiState> = emptyList(),
)
