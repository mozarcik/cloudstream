package com.lagradost.cloudstream3.tv.compat

import com.lagradost.cloudstream3.APIHolder.unixTimeMS
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.ui.WatchType
import com.lagradost.cloudstream3.ui.result.getId
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.DataStoreHelper.getBookmarkedData
import com.lagradost.cloudstream3.utils.DataStoreHelper.getFavoritesData
import com.lagradost.cloudstream3.utils.DataStoreHelper.getResultWatchState
import com.lagradost.cloudstream3.utils.DataStoreHelper.removeFavoritesData
import com.lagradost.cloudstream3.utils.DataStoreHelper.setBookmarkedData
import com.lagradost.cloudstream3.utils.DataStoreHelper.setFavoritesData
import com.lagradost.cloudstream3.utils.DataStoreHelper.setResultWatchState

object FavoritesCompat {
    fun addToFavorites(loadResponse: LoadResponse): Boolean {
        val loadResponseId = loadResponse.getId()
        if (getFavoritesData(loadResponseId) != null) {
            return false
        }

        setFavoritesData(
            loadResponseId,
            DataStoreHelper.FavoritesData(
                favoritesTime = unixTimeMS,
                id = loadResponseId,
                latestUpdatedTime = unixTimeMS,
                name = loadResponse.name,
                url = loadResponse.url,
                apiName = loadResponse.apiName,
                type = loadResponse.type,
                posterUrl = loadResponse.posterUrl ?: loadResponse.backgroundPosterUrl,
                year = loadResponse.year,
                syncData = loadResponse.syncData,
                plot = loadResponse.plot,
                score = loadResponse.score,
                tags = loadResponse.tags
            )
        )
        MainActivity.reloadLibraryEvent(true)

        return true
    }

    fun removeFromFavorites(loadResponse: LoadResponse): Boolean {
        val loadResponseId = loadResponse.getId()
        if (getFavoritesData(loadResponseId) == null) {
            return false
        }

        removeFavoritesData(loadResponseId)
        MainActivity.reloadLibraryEvent(true)

        return true
    }

    fun setBookmarkStatus(
        loadResponse: LoadResponse,
        status: WatchType,
    ): Boolean {
        val loadResponseId = loadResponse.getId()
        val currentStatus = getResultWatchState(loadResponseId)

        setResultWatchState(loadResponseId, status.internalId)

        if (status != WatchType.NONE) {
            val current = getBookmarkedData(loadResponseId)
            setBookmarkedData(
                loadResponseId,
                DataStoreHelper.BookmarkedData(
                    bookmarkedTime = current?.bookmarkedTime ?: unixTimeMS,
                    id = loadResponseId,
                    latestUpdatedTime = unixTimeMS,
                    name = loadResponse.name,
                    url = loadResponse.url,
                    apiName = loadResponse.apiName,
                    type = loadResponse.type,
                    posterUrl = loadResponse.posterUrl ?: loadResponse.backgroundPosterUrl,
                    year = loadResponse.year,
                    syncData = loadResponse.syncData,
                    plot = loadResponse.plot,
                    score = loadResponse.score,
                    tags = loadResponse.tags
                )
            )
        }

        if (currentStatus != status) {
            MainActivity.bookmarksUpdatedEvent(true)
            MainActivity.reloadLibraryEvent(true)
            return true
        }

        return false
    }

    fun markLibraryState(
        movieDetails: MovieDetails,
        loadResponse: LoadResponse,
    ): MovieDetails {
        val loadResponseId = loadResponse.getId()
        val watchStatus = getResultWatchState(loadResponseId)
        val isFavorite = getFavoritesData(loadResponseId) != null

        return movieDetails.copy(
            isFavorite = isFavorite,
            isBookmarked = watchStatus != WatchType.NONE,
            bookmarkLabelRes = watchStatus.stringRes.takeIf { watchStatus != WatchType.NONE },
        )
    }
}
