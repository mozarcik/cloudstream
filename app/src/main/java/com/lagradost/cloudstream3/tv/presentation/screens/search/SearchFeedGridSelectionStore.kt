package com.lagradost.cloudstream3.tv.presentation.screens.search

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SearchFeedGridSelectionStore {
    private val _selectedSection = MutableStateFlow<SearchSectionUiState?>(null)
    val selectedSection: StateFlow<SearchSectionUiState?> = _selectedSection.asStateFlow()

    fun setSelectedSection(section: SearchSectionUiState) {
        _selectedSection.value = section
    }
}
