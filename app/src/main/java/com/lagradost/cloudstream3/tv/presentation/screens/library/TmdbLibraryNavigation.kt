package com.lagradost.cloudstream3.tv.presentation.screens.library

import com.lagradost.cloudstream3.syncproviders.providers.TMDB_PROVIDER_NAME
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsTmdbResolveRequest

internal object TmdbLibraryNavigation {
    fun resolveDetailsRequest(
        item: MediaItemCompat,
        preferredApiName: String?,
    ): DetailsTmdbResolveRequest? {
        if (item.apiName != TMDB_PROVIDER_NAME) {
            return null
        }
        val title = item.name.trim().takeIf { it.isNotBlank() } ?: return null
        val tmdbId = item.tmdbId ?: return null

        return DetailsTmdbResolveRequest(
            title = title,
            tmdbId = tmdbId,
            preferredApiName = preferredApiName?.trim()?.takeIf { it.isNotBlank() },
            expectedType = item.type,
        )
    }
}
