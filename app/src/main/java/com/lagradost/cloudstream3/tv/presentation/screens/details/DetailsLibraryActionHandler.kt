package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.util.Log
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository
import com.lagradost.cloudstream3.ui.WatchType

internal class DetailsLibraryActionHandler(
    private val repository: MovieRepository,
    private val libraryOverridesStateHolder: DetailsLibraryOverridesStateHolder,
) {
    suspend fun toggleFavorite(
        currentState: DetailsScreenUiState.Done,
        source: DetailsRouteSource?,
    ) {
        val resolvedSource = source ?: run {
            Log.e(DebugTag, "cannot toggle favorite due to missing args")
            return
        }

        val targetFavoriteState = !currentState.details.isFavorite
        val detailsId = currentState.details.id
        try {
            repository.setMediaFavorite(
                url = resolvedSource.url,
                apiName = resolvedSource.apiName,
                isFavorite = targetFavoriteState
            )
            libraryOverridesStateHolder.updateFavorite(detailsId, targetFavoriteState)
            Log.d(DebugTag, "favorite toggled detailsId=$detailsId isFavorite=$targetFavoriteState")
        } catch (e: Exception) {
            Log.e(
                DebugTag,
                "failed to toggle favorite for api=${resolvedSource.apiName} url=${resolvedSource.url}",
                e
            )
        }
    }

    suspend fun updateBookmark(
        currentState: DetailsScreenUiState.Done,
        source: DetailsRouteSource?,
        status: WatchType,
    ) {
        val resolvedSource = source ?: run {
            Log.e(DebugTag, "cannot update bookmark due to missing args")
            return
        }

        val detailsId = currentState.details.id
        try {
            repository.setMediaBookmarkStatus(
                url = resolvedSource.url,
                apiName = resolvedSource.apiName,
                status = status
            )
            libraryOverridesStateHolder.updateBookmark(detailsId, status)
            Log.d(DebugTag, "bookmark updated detailsId=$detailsId status=$status")
        } catch (e: Exception) {
            Log.e(
                DebugTag,
                "failed to update bookmark for api=${resolvedSource.apiName} url=${resolvedSource.url}",
                e
            )
        }
    }

    private companion object {
        private const val DebugTag = "TvDetailsVM"
    }
}
