package com.lagradost.cloudstream3.tv.presentation.screens.search

import com.lagradost.cloudstream3.tv.presentation.screens.grid.TvGridFocusStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SearchFeedGridSelectionStore {
    private val _selectedSection = MutableStateFlow<SearchSectionUiState?>(null)
    val selectedSection: StateFlow<SearchSectionUiState?> = _selectedSection.asStateFlow()

    val focus = TvGridFocusStore(keyPrefix = "search_feed_grid")

    /** Query the opened section was searched with, shown next to the source name. */
    var query: String = ""
        private set

    fun setQuery(value: String) {
        query = value
    }

    fun setSelectedSection(section: SearchSectionUiState) {
        focus.reset()
        _selectedSection.value = section
    }
}
