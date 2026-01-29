/*
 * CloudStream TV - Feed ViewModel
 * Manages feed categories from selected API source
 */

package com.lagradost.cloudstream3.tv.presentation.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import com.lagradost.cloudstream3.tv.compat.home.FeedRepository
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class FeedViewModel(
    private val feedRepository: FeedRepository
) : ViewModel() {
    
    private val _feeds = MutableStateFlow<List<FeedCategory>>(emptyList())
    val feeds: StateFlow<List<FeedCategory>> = _feeds.asStateFlow()
    
    init {
        observeApiChanges()
        // Wait for API providers to load before loading feeds
        viewModelScope.launch {
            loadFeeds()
        }
    }
    
    private fun loadFeeds() {
        viewModelScope.launch {
            // Wait for API providers to be available
            val api = SourceRepository.waitForApiOrNull()
            if (api == null) {
                Log.e(TAG, "No API providers available after timeout")
                _feeds.value = emptyList()
                return@launch
            }
            
            Log.d(TAG, "Loading feeds for API: ${api.name}")
            
            feedRepository.getFeedCategories(api)
                .catch { e ->
                    Log.e(TAG, "Error loading feeds", e)
                    _feeds.value = emptyList()
                }
                .collect { categories ->
                    Log.d(TAG, "Loaded ${categories.size} feeds")
                    _feeds.value = categories
                }
        }
    }
    
    private fun observeApiChanges() {
        viewModelScope.launch {
            SourceRepository.selectedApi.collect { api ->
                if (api != null) {
                    Log.d(TAG, "API changed, reloading feeds for: ${api.name}")
                    loadFeeds()
                }
            }
        }
    }
    
    companion object {
        private const val TAG = "FeedViewModel"
    }
}
