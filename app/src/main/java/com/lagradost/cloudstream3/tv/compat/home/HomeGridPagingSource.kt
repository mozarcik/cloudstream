/*
 * CloudStream TV - Home Grid Paging Source
 * Loads media items for selected feed with pagination
 */

package com.lagradost.cloudstream3.tv.compat.home

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.lagradost.cloudstream3.MainAPI

/**
 * Paging source for loading media items from a feed
 * Task 5.1: Pagination support
 * 
 * @param api The API provider to fetch data from
 * @param feed The feed category to load
 * @param feedRepository Repository for feed operations
 */
class HomeGridPagingSource(
    private val api: MainAPI,
    private val feed: FeedCategory,
    private val feedRepository: FeedRepository
) : PagingSource<Int, MediaItemCompat>() {
    
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItemCompat> {
        return try {
            val page = params.key ?: 1
            Log.d(TAG, "Loading page $page for feed: ${feed.name}")
            
            val result = feedRepository.getMediaForFeed(api, feed, page)
            
            result.fold(
                onSuccess = { items ->
                    Log.d(TAG, "Loaded ${items.size} items for ${feed.name}, page $page")
                    LoadResult.Page(
                        data = items,
                        prevKey = if (page == 1) null else page - 1,
                        nextKey = if (items.isEmpty()) null else page + 1
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "Error loading page $page for ${feed.name}", e)
                    LoadResult.Error(e)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading page for ${feed.name}", e)
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<Int, MediaItemCompat>): Int? {
        // Return the page number closest to the current scroll position
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
    
    companion object {
        private const val TAG = "HomeGridPagingSource"
    }
}
