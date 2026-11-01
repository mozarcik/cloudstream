package com.lagradost.cloudstream3.tv.compat

import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.ui.WatchType
import com.lagradost.cloudstream3.utils.DataStoreHelper

object UnavailableDetailsCompat {
    fun isInLocalLibrary(
        sourceUrl: String,
        apiName: String,
    ): Boolean {
        val normalizedUrl = sourceUrl.trim()
        val normalizedApiName = apiName.trim()
        if (normalizedUrl.isEmpty() || normalizedApiName.isEmpty()) {
            return false
        }

        val inFavorites = DataStoreHelper.getAllFavorites().any { item ->
            item.url == normalizedUrl && item.apiName == normalizedApiName
        }
        if (inFavorites) {
            return true
        }

        return DataStoreHelper.getAllBookmarkedData().any { item ->
            item.url == normalizedUrl && item.apiName == normalizedApiName
        }
    }

    fun removeFromLibrary(
        sourceUrl: String,
        apiName: String,
    ): Boolean {
        val normalizedUrl = sourceUrl.trim()
        val normalizedApiName = apiName.trim()
        if (normalizedUrl.isEmpty() || normalizedApiName.isEmpty()) {
            return false
        }

        val favoriteIds = DataStoreHelper.getAllFavorites()
            .asSequence()
            .filter { item ->
                item.url == normalizedUrl && item.apiName == normalizedApiName
            }
            .mapNotNull { item -> item.id }
            .toSet()
        favoriteIds.forEach { id ->
            DataStoreHelper.removeFavoritesData(id)
        }

        val bookmarkedIds = DataStoreHelper.getAllBookmarkedData()
            .asSequence()
            .filter { item ->
                item.url == normalizedUrl && item.apiName == normalizedApiName
            }
            .mapNotNull { item -> item.id }
            .toSet()
        bookmarkedIds.forEach { id ->
            DataStoreHelper.setResultWatchState(id, WatchType.NONE.internalId)
        }

        val removedAny = favoriteIds.isNotEmpty() || bookmarkedIds.isNotEmpty()
        if (!removedAny) {
            return false
        }

        if (bookmarkedIds.isNotEmpty()) {
            MainActivity.bookmarksUpdatedEvent(true)
        }
        MainActivity.reloadLibraryEvent(true)
        return true
    }
}
