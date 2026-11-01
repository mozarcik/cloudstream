package com.lagradost.cloudstream3.tv.presentation.screens.search

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SearchPrefillStore {
    private val _pendingQuery = MutableStateFlow<String?>(null)
    val pendingQuery: StateFlow<String?> = _pendingQuery.asStateFlow()

    fun setPendingQuery(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            return
        }
        _pendingQuery.value = normalizedQuery
    }

    fun clearPendingQuery() {
        _pendingQuery.value = null
    }
}
