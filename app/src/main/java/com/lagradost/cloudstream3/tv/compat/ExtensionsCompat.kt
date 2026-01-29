package com.lagradost.cloudstream3.tv.compat

import android.app.Activity
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.plugins.PluginData
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.plugins.RepositoryManager
import com.lagradost.cloudstream3.plugins.SitePlugin
import com.lagradost.cloudstream3.ui.settings.extensions.REPOSITORIES_KEY
import com.lagradost.cloudstream3.ui.settings.extensions.RepositoryData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compatibility layer for Extensions management.
 * Bridges old Fragment-based code with new Compose TV UI.
 * Does NOT modify original code.
 */
object ExtensionsCompat {
    
    // ========== Repository Operations ==========
    
    /**
     * Get all repositories (user-added + prebuilt)
     */
    suspend fun getRepositories(): Result<List<RepositoryItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val userRepos = getKey<Array<RepositoryData>>(REPOSITORIES_KEY) ?: emptyArray()
            android.util.Log.d("ExtensionsCompat", "getRepositories: getKey returned ${userRepos.size} user repos")
            android.util.Log.d("ExtensionsCompat", "getRepositories: PREBUILT_REPOSITORIES has ${RepositoryManager.PREBUILT_REPOSITORIES.size} repos")
            
            val allRepos = userRepos.toList() + RepositoryManager.PREBUILT_REPOSITORIES.toList()
            android.util.Log.d("ExtensionsCompat", "getRepositories: Total repos before mapping: ${allRepos.size}")
            
            allRepos.mapIndexed { index, repo ->
                val sanitizedIconUrl = repo.iconUrl?.takeIf { it != "null" && it.isNotBlank() }
                android.util.Log.d("ExtensionsCompat", "getRepositories[$index]: ${repo.name}, url=${repo.url}")
                
                RepositoryItem(
                    name = repo.name,
                    url = repo.url,
                    iconUrl = sanitizedIconUrl,
                    isPrebuilt = RepositoryManager.PREBUILT_REPOSITORIES.any { it.url == repo.url }
                )
            }
        }
    }
    
    /**
     * Add a new repository with URL parsing and automatic name detection
     * Following the logic from ExtensionsFragment.kt:214-244
     */
    suspend fun addRepository(name: String?, url: String): Result<RepositoryItem> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Parse and validate URL
            val parsedUrl = RepositoryManager.parseRepoUrl(url.trim())
                ?: throw IllegalArgumentException("Invalid repository URL")
            
            // 2. Fetch repository metadata
            val repository = RepositoryManager.parseRepository(parsedUrl)
                ?: throw IllegalArgumentException("No repository found at this URL")
            
            // 3. Use provided name or fallback to repository name from metadata
            val finalName = if (!name.isNullOrBlank()) name else repository.name
            
            // 4. Create repository data
            val newRepo = RepositoryData(repository.iconUrl, finalName, parsedUrl)
            
            // 5. Add using RepositoryManager (handles duplicates internally)
            RepositoryManager.addRepository(newRepo)
            
            // 6. Return the added repository
            RepositoryItem(
                name = finalName,
                url = parsedUrl,
                iconUrl = repository.iconUrl,
                isPrebuilt = false
            )
        }
    }
    
    /**
     * Remove a repository (only user-added, not prebuilt)
     */
    suspend fun removeRepository(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Check if it's a prebuilt repo
            if (RepositoryManager.PREBUILT_REPOSITORIES.any { it.url == url }) {
                throw IllegalArgumentException("Cannot remove prebuilt repository")
            }
            
            val currentRepos = getKey<Array<RepositoryData>>(REPOSITORIES_KEY) ?: emptyArray()
            val updatedRepos = currentRepos.filter { it.url != url }.toTypedArray()
            
            setKey(REPOSITORIES_KEY, updatedRepos)
        }
    }
    
    // ========== Plugin Operations ==========
    
    /**
     * Get all plugins from a specific repository
     */
    suspend fun getPluginsFromRepository(repoUrl: String): Result<List<PluginItem>> = withContext(Dispatchers.IO) {
        android.util.Log.d("ExtensionsCompat", "getPluginsFromRepository called for: $repoUrl")
        runCatching {
            android.util.Log.d("ExtensionsCompat", "Calling RepositoryManager.getRepoPlugins($repoUrl)")
            val repoPlugins = RepositoryManager.getRepoPlugins(repoUrl)
            
            if (repoPlugins == null) {
                android.util.Log.e("ExtensionsCompat", "RepositoryManager.getRepoPlugins returned null for $repoUrl")
                throw IllegalStateException("Failed to fetch plugins from repository")
            }
            
            android.util.Log.d("ExtensionsCompat", "Got ${repoPlugins.size} plugins from repository")
            
            val installedPlugins = PluginManager.getPluginsOnline()
            android.util.Log.d("ExtensionsCompat", "Found ${installedPlugins.size} installed plugins")
            
            val result = repoPlugins.map { (repo, sitePlugin) ->
                val installedPlugin = installedPlugins.find { it.internalName == sitePlugin.internalName }
                
                val sanitizedIconUrl = sitePlugin.iconUrl?.takeIf { it != "null" && it.isNotBlank() }
                
                PluginItem(
                    internalName = sitePlugin.internalName,
                    name = sitePlugin.name,
                    description = sitePlugin.description,
                    authors = sitePlugin.authors,
                    version = sitePlugin.version,
                    iconUrl = sanitizedIconUrl,
                    repositoryUrl = repoUrl,
                    url = sitePlugin.url,
                    status = when {
                        installedPlugin == null -> PluginStatus.NOT_DOWNLOADED
                        installedPlugin.version < sitePlugin.version -> PluginStatus.UPDATE_AVAILABLE
                        else -> PluginStatus.DOWNLOADED
                    }
                )
            }
            
            android.util.Log.d("ExtensionsCompat", "Returning ${result.size} PluginItems")
            result
        }
    }
    
    /**
     * Download and install a plugin
     */
    suspend fun downloadPlugin(activity: Activity, plugin: PluginItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            PluginManager.downloadPlugin(
                activity = activity,
                pluginUrl = plugin.url,
                internalName = plugin.internalName,
                repositoryUrl = plugin.repositoryUrl,
                loadPlugin = true
            )
            Unit
        }
    }
    
    /**
     * Delete an installed plugin
     */
    suspend fun deletePlugin(internalName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val installedPlugins = PluginManager.getPluginsOnline()
            val pluginData = installedPlugins.find { it.internalName == internalName }
                ?: throw IllegalArgumentException("Plugin not found")
            
            PluginManager.unloadPlugin(pluginData.filePath)
        }
    }
    
    /**
     * Update a plugin to the latest version
     */
    suspend fun updatePlugin(activity: Activity, plugin: PluginItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Delete old version first
            val installedPlugins = PluginManager.getPluginsOnline()
            val oldPlugin = installedPlugins.find { it.internalName == plugin.internalName }
            if (oldPlugin != null) {
                PluginManager.unloadPlugin(oldPlugin.filePath)
            }
            
            // Download new version
            PluginManager.downloadPlugin(
                activity = activity,
                pluginUrl = plugin.url,
                internalName = plugin.internalName,
                repositoryUrl = plugin.repositoryUrl,
                loadPlugin = true
            )
            Unit
        }
    }
    
    /**
     * Check if a plugin has settings
     * TODO: Implement proper check when plugin settings API is clear
     */
    fun hasPluginSettings(internalName: String): Boolean {
        // For now, return false. Plugin settings will be implemented in Phase 2
        return false
    }
}

/**
 * Represents a repository
 */
data class RepositoryItem(
    val name: String,
    val url: String,
    val iconUrl: String?,
    val isPrebuilt: Boolean
)

/**
 * Represents a plugin
 */
data class PluginItem(
    val internalName: String,
    val name: String,
    val description: String?,
    val authors: List<String>,
    val version: Int,
    val iconUrl: String?,
    val repositoryUrl: String,
    val url: String,
    val status: PluginStatus
)

/**
 * Plugin status
 */
enum class PluginStatus {
    NOT_DOWNLOADED,
    DOWNLOADED,
    UPDATE_AVAILABLE
}
