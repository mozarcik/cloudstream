package com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions

import com.lagradost.cloudstream3.tv.compat.PluginItem
import com.lagradost.cloudstream3.tv.compat.PluginStats
import com.lagradost.cloudstream3.tv.compat.RepositoryItem

/**
 * UI State for Extensions screen
 */
sealed class ExtensionsUiState {
    /**
     * Loading initial data (first time)
     */
    object Loading : ExtensionsUiState()
    
    /**
     * Ready with data
     */
    data class Ready(
        val repositories: List<RepositoryItem>,
        val pluginStats: PluginStats?,
        // Map<repoUrl, plugins or null if loading>
        // null = not loaded yet, empty list = loaded but no plugins
        val pluginsByRepo: Map<String, List<PluginItem>?> = emptyMap()
    ) : ExtensionsUiState()
    
    /**
     * Error state
     */
    data class Error(val message: String) : ExtensionsUiState()
}
