package com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.tv.compat.ExtensionsCompat
import com.lagradost.cloudstream3.tv.compat.PluginItem
import com.lagradost.cloudstream3.tv.compat.PluginStatsCompat
import com.lagradost.cloudstream3.tv.compat.RepositoryItem
import com.lagradost.cloudstream3.ui.settings.extensions.REPOSITORIES_KEY
import com.lagradost.cloudstream3.utils.DataStore.getSharedPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Extensions management
 */
class ExtensionsViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow<ExtensionsUiState>(ExtensionsUiState.Loading)
    val uiState: StateFlow<ExtensionsUiState> = _uiState.asStateFlow()
    
    private var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var context: Context? = null
    private var reloadJob: kotlinx.coroutines.Job? = null
    
    init {
        viewModelScope.launch {
            loadData()
        }
    }
    
    /**
     * Setup SharedPreferences listener to auto-reload when plugins add repositories
     */
    fun setupPrefsListener(context: Context) {
        this.context = context
        
        prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == REPOSITORIES_KEY) {
                // Cancel previous reload job and schedule a new one (debounce)
                reloadJob?.cancel()
                reloadJob = viewModelScope.launch {
                    // Wait 500ms - if another change comes, this will be cancelled
                    kotlinx.coroutines.delay(500)
                    loadData()
                }
            }
        }
        
        context.getSharedPrefs().registerOnSharedPreferenceChangeListener(prefsListener)
    }
    
    override fun onCleared() {
        super.onCleared()
        reloadJob?.cancel()
        prefsListener?.let { listener ->
            context?.getSharedPrefs()?.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    /**
     * Load repositories and plugin stats
     */
    private suspend fun loadData() {
        if (_uiState.value !is ExtensionsUiState.Ready) {
            _uiState.value = ExtensionsUiState.Loading
        }
        
        // Load repositories
        val reposResult = ExtensionsCompat.getRepositories()
        if (reposResult.isFailure) {
            _uiState.value = ExtensionsUiState.Error(
                reposResult.exceptionOrNull()?.message ?: "Failed to load repositories"
            )
            return
        }
        
        val repositories = reposResult.getOrNull() ?: emptyList()
        
        // Load stats
        val statsResult = PluginStatsCompat.loadPluginStats()
        val stats = statsResult.getOrNull()
        
        // Keep existing plugins cache during refresh (don't show "loading" for repos that were already loaded)
        val currentPlugins = if (_uiState.value is ExtensionsUiState.Ready) {
            (_uiState.value as ExtensionsUiState.Ready).pluginsByRepo
        } else {
            emptyMap()
        }
        
        _uiState.value = ExtensionsUiState.Ready(
            repositories = repositories,
            pluginStats = stats,
            pluginsByRepo = currentPlugins
        )
    }
    
    /**
     * Load plugins for a specific repository
     */
    fun loadPluginsForRepo(repoUrl: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is ExtensionsUiState.Ready) {
                return@launch
            }
            
            // Skip if already loaded (has non-null value in map)
            val existingPlugins = currentState.pluginsByRepo[repoUrl]
            if (existingPlugins != null) {
                return@launch
            }
            
            // Skip if currently loading (has null value but key exists)
            if (currentState.pluginsByRepo.containsKey(repoUrl)) {
                return@launch
            }
            
            // Mark as loading (null value)
            _uiState.value = currentState.copy(
                pluginsByRepo = currentState.pluginsByRepo + (repoUrl to null)
            )
            
            // Load plugins
            val pluginsResult = ExtensionsCompat.getPluginsFromRepository(repoUrl)
            if (pluginsResult.isSuccess) {
                val plugins = pluginsResult.getOrNull() ?: emptyList()
                
                // Get fresh state in case it changed during loading
                val freshState = _uiState.value
                if (freshState is ExtensionsUiState.Ready) {
                    val updatedMap = freshState.pluginsByRepo + (repoUrl to plugins)
                    _uiState.value = freshState.copy(pluginsByRepo = updatedMap)
                }
            } else {
                
                // Get fresh state in case it changed during loading
                val freshState = _uiState.value
                if (freshState is ExtensionsUiState.Ready) {
                    // On error, set empty list (not null) to indicate loaded but failed
                    val updatedMap = freshState.pluginsByRepo + (repoUrl to emptyList())
                    _uiState.value = freshState.copy(pluginsByRepo = updatedMap)
                }
            }
        }
    }
    
    /**
     * Get plugins for a specific repository from cache
     */
    fun getPluginsForRepo(repoUrl: String): List<PluginItem> {
        val currentState = _uiState.value
        if (currentState !is ExtensionsUiState.Ready) return emptyList()
        
        return currentState.pluginsByRepo[repoUrl] ?: emptyList()
    }
    
    /**
     * Add a new repository with URL parsing and automatic name detection
     * Name is optional - if not provided, will be fetched from repository metadata
     */
    fun addRepository(name: String?, url: String, onResult: (Result<RepositoryItem>) -> Unit) {
        viewModelScope.launch {
            val result = ExtensionsCompat.addRepository(name, url)
            
            if (result.isSuccess) {
                // Reload all data (stats, repositories, plugins) BEFORE calling onResult
                loadData()
                
                // Also load plugins for the new repository
                result.getOrNull()?.let { repo ->
                    loadPluginsForRepo(repo.url)
                }
            }
            
            // Call onResult AFTER data is reloaded so UI shows updated list
            onResult(result)
        }
    }
    
    /**
     * Remove a repository
     */
    fun removeRepository(url: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = ExtensionsCompat.removeRepository(url)
            
            if (result.isSuccess) {
                loadData() // Reload to remove from list BEFORE onResult
            }
            
            onResult(result)
        }
    }
    
    /**
     * Download a plugin
     */
    fun downloadPlugin(activity: Activity, plugin: PluginItem, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = ExtensionsCompat.downloadPlugin(activity, plugin)
            
            if (result.isSuccess) {
                // Note: loadData() will be called automatically by SharedPreferences listener
                // when plugin adds new repositories
                
                // Update plugin status in current list (change status from NOT_DOWNLOADED to DOWNLOADED)
                val currentState = _uiState.value
                if (currentState is ExtensionsUiState.Ready) {
                    val pluginsList = currentState.pluginsByRepo[plugin.repositoryUrl]
                    if (pluginsList != null) {
                        val updatedPlugins = pluginsList.map { p ->
                            if (p.internalName == plugin.internalName) {
                                p.copy(status = com.lagradost.cloudstream3.tv.compat.PluginStatus.DOWNLOADED)
                            } else {
                                p
                            }
                        }
                        _uiState.value = currentState.copy(
                            pluginsByRepo = currentState.pluginsByRepo + (plugin.repositoryUrl to updatedPlugins)
                        )
                    }
                }
            } else {
            }
            
            // Call onResult after reload so UI shows updated state
            onResult(result)
        }
    }
    
    /**
     * Delete a plugin
     */
    fun deletePlugin(plugin: PluginItem, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = ExtensionsCompat.deletePlugin(plugin.internalName)
            
            if (result.isSuccess) {
                // Reload all data first
                loadData()
                // Then reload plugins for this repo
                loadPluginsForRepo(plugin.repositoryUrl)
            }
            
            onResult(result)
        }
    }
    
    /**
     * Update a plugin
     */
    fun updatePlugin(activity: Activity, plugin: PluginItem, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = ExtensionsCompat.updatePlugin(activity, plugin)
            
            if (result.isSuccess) {
                // Reload all data first
                loadData()
                // Then reload plugins for this repo
                loadPluginsForRepo(plugin.repositoryUrl)
            }
            
            onResult(result)
        }
    }
}
