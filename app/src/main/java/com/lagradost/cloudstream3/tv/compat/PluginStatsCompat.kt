package com.lagradost.cloudstream3.tv.compat

import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.plugins.RepositoryManager
import com.lagradost.cloudstream3.ui.settings.extensions.REPOSITORIES_KEY
import com.lagradost.cloudstream3.ui.settings.extensions.RepositoryData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compatibility layer for plugin statistics.
 * Bridges old Fragment-based code with new Compose TV UI.
 */
object PluginStatsCompat {
    
    /**
     * Load plugin statistics
     * Returns stats about downloaded, disabled, and available plugins
     */
    suspend fun loadPluginStats(): Result<PluginStats> = withContext(Dispatchers.IO) {
        runCatching {
            // Get all repositories
            val urls = (getKey<Array<RepositoryData>>(REPOSITORIES_KEY)
                ?: emptyArray()) + RepositoryManager.PREBUILT_REPOSITORIES
            
            // Get all online plugins from all repositories
            val onlinePlugins = urls.toList().amap {
                RepositoryManager.getRepoPlugins(it.url)?.toList() ?: emptyList()
            }.flatten().distinctBy { it.second.url }
            
            // Get installed plugins
            val installedPlugins = PluginManager.getPluginsOnline()
            
            // Match installed plugins with online plugins
            val outdatedPlugins = installedPlugins.map { savedData ->
                onlinePlugins.filter { onlineData -> 
                    savedData.internalName == onlineData.second.internalName 
                }.map { onlineData ->
                    PluginManager.OnlinePluginData(savedData, onlineData)
                }
            }.flatten().distinctBy { it.onlineData.second.url }
            
            val total = onlinePlugins.count()
            val downloadedTotal = outdatedPlugins.count()
            val downloaded = downloadedTotal
            val notDownloaded = total - downloadedTotal
            
            PluginStats(
                total = total,
                downloaded = downloaded,
                notDownloaded = notDownloaded
            )
        }
    }
}

/**
 * Plugin statistics
 */
data class PluginStats(
    val total: Int,
    val downloaded: Int,
    val notDownloaded: Int
)
