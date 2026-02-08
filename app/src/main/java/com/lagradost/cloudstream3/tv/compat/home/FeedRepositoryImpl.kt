package com.lagradost.cloudstream3.tv.compat.home

import android.util.Log
import com.lagradost.cloudstream3.CloudStreamApp.Companion.context
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.SearchResponseMapper.toMediaItemCompat
import com.lagradost.cloudstream3.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Implementation of FeedRepository that fetches data from MainAPI providers
 */
class FeedRepositoryImpl : FeedRepository {
    
    companion object {
        private const val TAG = "FeedRepository"
    }
    
    /**
     * Get all feed categories from the API's mainPage definition
     */
    override fun getFeedCategories(api: MainAPI): Flow<List<FeedCategory>> = flow {
        try {
            Log.d(TAG, "Loading feed categories for ${api.name}")
            
            // Get mainPage requests from API
            val mainPageRequests = api.mainPage

            // Convert to FeedCategory objects using the existing MainPageRequest from FeedCategory.kt
            val providerCategories = mainPageRequests.mapIndexed { index, request ->
                FeedCategory(
                    id = "${api.name}_${request.name}_$index",
                    name = request.name,
                    mainPageRequest = MainPageRequest(
                        data = request.data,
                        name = request.name,
                        horizontalImages = request.horizontalImages
                    )
                )
            }

            val continueWatchingItems = runCatching {
                getContinueWatchingMediaItems()
            }.onFailure { error ->
                Log.e(TAG, "Failed to load continue watching feed categories", error)
            }.getOrDefault(emptyList())
            val categories = if (continueWatchingItems.isNotEmpty()) {
                val continueWatchingName = context?.getString(R.string.continue_watching)
                    ?: "Continue Watching"
                listOf(FeedCategory.continueWatching(continueWatchingName)) + providerCategories
            } else {
                providerCategories
            }
            
            Log.d(TAG, "Loaded ${categories.size} categories for ${api.name}: ${categories.map { it.name }}")
            emit(categories)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading categories for ${api.name}", e)
            emit(emptyList())
        }
    }
    
    /**
     * Fetch media items for a specific feed category and page
     */
    override suspend fun getMediaForFeed(
        api: MainAPI,
        category: FeedCategory,
        page: Int
    ): Result<MediaListCompat> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (category.isContinueWatching) {
                val continueWatchingItems = if (page == 1) {
                    runCatching {
                        getContinueWatchingMediaItems()
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to load continue watching media items", error)
                    }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
                Log.d(TAG, "Loaded ${continueWatchingItems.size} continue watching items for page $page")
                return@withContext Result.success(continueWatchingItems)
            }

            // Get the MainPageRequest data
            val requestData = category.mainPageRequest
                ?: return@withContext Result.failure(IllegalArgumentException("Category has no request data"))
            
            Log.d(TAG, "Fetching page $page for category '${category.name}' from ${api.name}")
            
            // Convert our MainPageRequest to CloudStream's MainPageRequest
            val mainPageRequest = com.lagradost.cloudstream3.MainPageRequest(
                data = requestData.data,
                name = requestData.name,
                horizontalImages = requestData.horizontalImages
            )
            
            // Call API's getMainPage method
            val response = api.getMainPage(page, mainPageRequest)
            
            if (response == null) {
                Log.w(TAG, "API ${api.name} returned null response for ${category.name}")
                return@withContext Result.success(emptyList())
            }
            
            // Find the matching HomePageList by name
            val homePageList = response.items.find { it.name == category.name }
            
            if (homePageList == null) {
                Log.w(TAG, "No HomePageList found with name '${category.name}' in response from ${api.name}")
                Log.d(TAG, "Available lists: ${response.items.map { it.name }}")
                return@withContext Result.success(emptyList())
            }
            
            // Map SearchResponse items to MediaItemCompat
            val mediaItems = homePageList.list.map { searchResponse ->
                searchResponse.toMediaItemCompat()
            }
            
            Log.d(TAG, "Loaded ${mediaItems.size} items for '${category.name}', page $page")
            
            if (mediaItems.isNotEmpty()) {
                Log.d(TAG, "First item: ${mediaItems.first().name} (${mediaItems.first()::class.simpleName})")
            }
            
            Result.success(mediaItems)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading media for category '${category.name}', page $page", e)
            Result.failure(e)
        }
    }

    private suspend fun getContinueWatchingMediaItems(): MediaListCompat = withContext(Dispatchers.IO) {
        HomeViewModel.getResumeWatching()
            .orEmpty()
            .map { resumeItem -> resumeItem.toMediaItemCompat() }
            .distinctBy { mediaItem -> "${mediaItem.apiName}|${mediaItem.url}" }
    }
}
