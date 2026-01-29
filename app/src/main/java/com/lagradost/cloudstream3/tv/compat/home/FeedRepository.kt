package com.lagradost.cloudstream3.tv.compat.home

import com.lagradost.cloudstream3.MainAPI
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing feed categories and media items
 */
interface FeedRepository {
    
    /**
     * Get all feed categories for the given API provider
     * @param api The MainAPI provider to get categories from
     * @return Flow emitting list of feed categories
     */
    fun getFeedCategories(api: MainAPI): Flow<List<FeedCategory>>
    
    /**
     * Get media items for a specific feed category
     * @param api The MainAPI provider
     * @param category The feed category to fetch items for
     * @param page Page number for pagination (starts at 1)
     * @return Result containing list of media items or error
     */
    suspend fun getMediaForFeed(
        api: MainAPI,
        category: FeedCategory,
        page: Int
    ): Result<MediaListCompat>
}
