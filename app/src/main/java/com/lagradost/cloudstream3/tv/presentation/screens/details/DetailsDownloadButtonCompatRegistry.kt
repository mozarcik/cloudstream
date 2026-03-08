package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.content.Context
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat

internal class DetailsDownloadButtonCompatRegistry {
    private var appContext: Context? = null
    private var defaultActionsCompat: MovieDetailsEpisodeActionsCompat? = null
    private var pendingActionsCompat: MovieDetailsEpisodeActionsCompat? = null

    fun updateContext(context: Context?) {
        if (context != null) {
            appContext = context.applicationContext
        }
    }

    fun setDefaultCompat(compat: MovieDetailsEpisodeActionsCompat) {
        defaultActionsCompat = compat
        pendingActionsCompat = null
    }

    fun setPendingCompat(compat: MovieDetailsEpisodeActionsCompat) {
        pendingActionsCompat = compat
    }

    fun clearPendingCompat() {
        pendingActionsCompat = null
    }

    fun resolveDefaultCompat(): MovieDetailsEpisodeActionsCompat? {
        return defaultActionsCompat
    }

    fun resolvePendingOrDefaultCompat(): MovieDetailsEpisodeActionsCompat? {
        return pendingActionsCompat ?: defaultActionsCompat
    }

    fun resolveAppContext(): Context? {
        return appContext
    }
}
