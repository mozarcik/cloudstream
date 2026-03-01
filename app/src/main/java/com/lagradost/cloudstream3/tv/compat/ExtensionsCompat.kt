package com.lagradost.cloudstream3.tv.compat

import android.app.Activity
import android.content.Context
import com.lagradost.cloudstream3.CloudStreamApp.Companion.context as appContext
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.plugins.RepositoryManager
import com.lagradost.cloudstream3.ui.settings.extensions.REPOSITORIES_KEY
import com.lagradost.cloudstream3.ui.settings.extensions.RepositoryData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Compatibility layer for Extensions management.
 * Bridges old Fragment-based code with new Compose TV UI.
 * Does NOT modify original code.
 */
object ExtensionsCompat {
    private fun requireAppContext(): Context {
        return requireNotNull(appContext) { "Application context is unavailable" }
    }

    internal fun resolveInstalledPluginFile(
        context: Context,
        repositoryUrl: String,
        internalName: String
    ): File? {
        val expectedFile = PluginManager.getPluginPath(
            context = context,
            internalName = internalName,
            repositoryUrl = repositoryUrl
        )
        if (expectedFile.exists()) return expectedFile

        return PluginManager.getPluginsOnline()
            .firstOrNull { pluginData -> pluginData.filePath == expectedFile.absolutePath }
            ?.let { pluginData -> File(pluginData.filePath) }
            ?: PluginManager.getPluginsOnline()
                .firstOrNull { pluginData -> pluginData.internalName == internalName }
                ?.let { pluginData -> File(pluginData.filePath) }
    }

    internal fun resolveInstalledPluginInstance(
        context: Context,
        repositoryUrl: String,
        internalName: String
    ): com.lagradost.cloudstream3.plugins.Plugin? {
        val pluginFile = resolveInstalledPluginFile(
            context = context,
            repositoryUrl = repositoryUrl,
            internalName = internalName
        ) ?: return null

        return PluginManager.plugins[pluginFile.absolutePath] as? com.lagradost.cloudstream3.plugins.Plugin
    }

    // ========== Repository Operations ==========

    /**
     * Get all repositories (user-added + prebuilt)
     */
    suspend fun getRepositories(): Result<List<RepositoryItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val userRepos = getKey<Array<RepositoryData>>(REPOSITORIES_KEY) ?: emptyArray()

            (userRepos.toList() + RepositoryManager.PREBUILT_REPOSITORIES.toList()).map { repo ->
                RepositoryItem(
                    name = repo.name,
                    url = repo.url,
                    iconUrl = repo.iconUrl?.takeIf { it != "null" && it.isNotBlank() },
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
            val parsedUrl = RepositoryManager.parseRepoUrl(url.trim())
                ?: throw IllegalArgumentException("Invalid repository URL")

            val repository = RepositoryManager.parseRepository(parsedUrl)
                ?: throw IllegalArgumentException("No repository found at this URL")

            val finalName = name?.takeIf { it.isNotBlank() } ?: repository.name
            val newRepo = RepositoryData(repository.iconUrl, finalName, parsedUrl)
            RepositoryManager.addRepository(newRepo)

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
            if (RepositoryManager.PREBUILT_REPOSITORIES.any { it.url == url }) {
                throw IllegalArgumentException("Cannot remove prebuilt repository")
            }

            val currentRepos = getKey<Array<RepositoryData>>(REPOSITORIES_KEY) ?: emptyArray()
            val updatedRepos = currentRepos.filter { repo -> repo.url != url }.toTypedArray()
            setKey(REPOSITORIES_KEY, updatedRepos)
        }
    }

    // ========== Plugin Operations ==========

    /**
     * Get all plugins from a specific repository
     */
    suspend fun getPluginsFromRepository(repoUrl: String): Result<List<PluginItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val repoPlugins = RepositoryManager.getRepoPlugins(repoUrl)
                ?: throw IllegalStateException("Failed to fetch plugins from repository")

            val context = appContext
            val installedPlugins = PluginManager.getPluginsOnline()

            repoPlugins.map { (_, sitePlugin) ->
                val installedPluginPath = context?.let { appContext ->
                    resolveInstalledPluginFile(
                        context = appContext,
                        repositoryUrl = repoUrl,
                        internalName = sitePlugin.internalName
                    )?.absolutePath
                }
                val installedPlugin = installedPlugins.firstOrNull { pluginData ->
                    pluginData.filePath == installedPluginPath
                } ?: installedPlugins.firstOrNull { pluginData ->
                    pluginData.internalName == sitePlugin.internalName
                }

                PluginItem(
                    internalName = sitePlugin.internalName,
                    name = sitePlugin.name,
                    description = sitePlugin.description,
                    authors = sitePlugin.authors,
                    version = sitePlugin.version,
                    iconUrl = sitePlugin.iconUrl?.takeIf { it != "null" && it.isNotBlank() },
                    repositoryUrl = repoUrl,
                    url = sitePlugin.url,
                    status = when {
                        installedPlugin == null -> PluginStatus.NOT_DOWNLOADED
                        installedPlugin.version < sitePlugin.version -> PluginStatus.UPDATE_AVAILABLE
                        else -> PluginStatus.DOWNLOADED
                    },
                    hasSettings = installedPlugin != null && hasPluginSettings(
                        repositoryUrl = repoUrl,
                        internalName = sitePlugin.internalName
                    )
                )
            }
        }
    }

    /**
     * Download and install a plugin.
     */
    suspend fun downloadPlugin(activity: Activity, plugin: PluginItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val success = PluginManager.downloadPlugin(
                activity = activity,
                pluginUrl = plugin.url,
                internalName = plugin.internalName,
                repositoryUrl = plugin.repositoryUrl,
                loadPlugin = true
            )
            if (!success) {
                throw IllegalStateException(activity.getString(R.string.plugin_download_failed))
            }
            Unit
        }
    }

    /**
     * Delete an installed plugin.
     */
    suspend fun deletePlugin(plugin: PluginItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val context = requireAppContext()
            val pluginFile = resolveInstalledPluginFile(
                context = context,
                repositoryUrl = plugin.repositoryUrl,
                internalName = plugin.internalName
            ) ?: throw IllegalArgumentException("Plugin not found")

            val success = PluginManager.deletePlugin(pluginFile)
            if (!success) {
                throw IllegalStateException(context.getString(R.string.plugin_delete_failed))
            }
            Unit
        }
    }

    /**
     * Update a plugin to the latest version.
     */
    suspend fun updatePlugin(activity: Activity, plugin: PluginItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            resolveInstalledPluginFile(
                context = activity,
                repositoryUrl = plugin.repositoryUrl,
                internalName = plugin.internalName
            )?.let { existingFile ->
                PluginManager.unloadPlugin(existingFile.absolutePath)
            }

            val success = PluginManager.downloadPlugin(
                activity = activity,
                pluginUrl = plugin.url,
                internalName = plugin.internalName,
                repositoryUrl = plugin.repositoryUrl,
                loadPlugin = true
            )
            if (!success) {
                throw IllegalStateException(activity.getString(R.string.plugin_update_failed))
            }
            Unit
        }
    }

    /**
     * Check if a loaded plugin exposes settings via legacy callback.
     */
    fun hasPluginSettings(repositoryUrl: String, internalName: String): Boolean {
        val context = appContext ?: return false
        return resolveInstalledPluginInstance(
            context = context,
            repositoryUrl = repositoryUrl,
            internalName = internalName
        )?.openSettings != null
    }
}

/**
 * Represents a repository.
 */
data class RepositoryItem(
    val name: String,
    val url: String,
    val iconUrl: String?,
    val isPrebuilt: Boolean
)

/**
 * Represents a plugin.
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
    val status: PluginStatus,
    val hasSettings: Boolean
)

/**
 * Plugin status.
 */
enum class PluginStatus {
    NOT_DOWNLOADED,
    DOWNLOADED,
    UPDATE_AVAILABLE
}
