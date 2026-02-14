/*
 * CloudStream TV - Media Grid ViewModel
 * Manages media grid state with pagination
 */

package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import com.lagradost.cloudstream3.tv.compat.home.FeedRepository
import com.lagradost.cloudstream3.tv.compat.home.HomeGridPagingSource
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * ViewModel for MediaGrid with pagination support
 * Task 5.1: Paging3 integration
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MediaGridViewModel(
    private val feedRepository: FeedRepository
) : ViewModel() {
    
    private val _selectedFeed = MutableStateFlow<FeedCategory?>(null)
    val selectedFeed: StateFlow<FeedCategory?> = _selectedFeed.asStateFlow()
    
    /**
     * Paging flow that reacts to feed selection changes
     */
    val pagingData: Flow<PagingData<MediaItemCompat>> = _selectedFeed.flatMapLatest { feed ->
        if (feed == null) {
            flowOf(PagingData.empty())
        } else {
            // Use flow to wait for API before creating pager
            kotlinx.coroutines.flow.flow {
                val api = SourceRepository.waitForApiOrNull()
                if (api == null) {
                    emit(PagingData.empty())
                } else {
                    createPagerForFeed(feed, api).collect { emit(it) }
                }
            }
        }
    }
    
    /**
     * Select a feed to load media items
     */
    fun selectFeed(feed: FeedCategory) {
        _selectedFeed.value = feed
    }
    
    /**
     * Creates a Pager for the given feed
     */
    private fun createPagerForFeed(feed: FeedCategory, api: com.lagradost.cloudstream3.MainAPI): Flow<PagingData<MediaItemCompat>> {
        return Pager(
            config = PagingConfig(
                pageSize = 40,
                prefetchDistance = 8,
                enablePlaceholders = false,
                initialLoadSize = 40
            ),
            pagingSourceFactory = {
                HomeGridPagingSource(api, feed, feedRepository)
            }
        ).flow.cachedIn(viewModelScope)
    }
    
}
