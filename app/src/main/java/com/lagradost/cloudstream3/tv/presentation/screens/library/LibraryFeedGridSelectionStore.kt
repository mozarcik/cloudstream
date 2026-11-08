package com.lagradost.cloudstream3.tv.presentation.screens.library

import com.lagradost.cloudstream3.tv.presentation.screens.grid.TvGridFocusStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LibraryFeedGridSelectionStore {
    private val _selectedSection = MutableStateFlow<LibrarySectionUiState?>(null)
    val selectedSection: StateFlow<LibrarySectionUiState?> = _selectedSection.asStateFlow()

    val focus = TvGridFocusStore(keyPrefix = "library_feed_grid")

    /** Account / provider the list belongs to, shown next to the list name. */
    var accountName: String? = null
        private set

    fun setAccountName(value: String?) {
        accountName = value
    }

    fun setSelectedSection(section: LibrarySectionUiState) {
        // Re-opening the list that is already shown keeps the focused poster.
        if (_selectedSection.value?.id != section.id) focus.reset()
        _selectedSection.value = section
    }
}
