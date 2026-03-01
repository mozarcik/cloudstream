package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.lifecycle.SavedStateHandle

internal object PlayerScreenNavigation {
    const val UrlBundleKey = "url"
    const val ApiNameBundleKey = "apiName"
    const val EpisodeDataBundleKey = "episodeData"
}

internal fun createPlayerSavedStateHandle(
    url: String,
    apiName: String,
    episodeData: String,
): SavedStateHandle {
    return SavedStateHandle().apply {
        set(PlayerScreenNavigation.UrlBundleKey, url)
        set(PlayerScreenNavigation.ApiNameBundleKey, apiName)
        set(PlayerScreenNavigation.EpisodeDataBundleKey, episodeData)
    }
}
