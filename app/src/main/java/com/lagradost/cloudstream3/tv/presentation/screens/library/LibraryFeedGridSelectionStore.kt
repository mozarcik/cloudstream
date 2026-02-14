package com.lagradost.cloudstream3.tv.presentation.screens.library

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LibraryFeedGridSelectionStore {
    private val _selectedSection = MutableStateFlow<LibrarySectionUiState?>(null)
    val selectedSection: StateFlow<LibrarySectionUiState?> = _selectedSection.asStateFlow()

    fun setSelectedSection(section: LibrarySectionUiState) {
        _selectedSection.value = section
    }
}
