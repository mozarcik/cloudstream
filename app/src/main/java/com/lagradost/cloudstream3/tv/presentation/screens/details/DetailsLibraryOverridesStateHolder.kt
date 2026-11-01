package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.ui.WatchType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class DetailsLibraryOverridesStateHolder {

    private val _favoriteOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val favoriteOverrides: StateFlow<Map<String, Boolean>> = _favoriteOverrides.asStateFlow()

    private val _bookmarkOverrides = MutableStateFlow<Map<String, WatchType>>(emptyMap())
    val bookmarkOverrides: StateFlow<Map<String, WatchType>> = _bookmarkOverrides.asStateFlow()

    fun clear() {
        _favoriteOverrides.value = emptyMap()
        _bookmarkOverrides.value = emptyMap()
    }

    fun updateFavorite(
        detailsId: String,
        isFavorite: Boolean,
    ) {
        _favoriteOverrides.value = _favoriteOverrides.value + (detailsId to isFavorite)
    }

    fun updateBookmark(
        detailsId: String,
        status: WatchType,
    ) {
        _bookmarkOverrides.value = _bookmarkOverrides.value + (detailsId to status)
    }
}
