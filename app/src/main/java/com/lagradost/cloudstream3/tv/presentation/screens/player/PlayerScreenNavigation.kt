package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.lifecycle.SavedStateHandle

internal object PlayerScreenNavigation {
    const val UrlBundleKey = "url"
    const val ApiNameBundleKey = "apiName"
    const val EpisodeDataBundleKey = "episodeData"
    private const val DownloadEpisodePrefix = "__download_episode__:"

    fun buildDownloadedEpisodeData(episodeId: Int): String {
        return "$DownloadEpisodePrefix$episodeId"
    }

    fun parseDownloadedEpisodeId(episodeData: String?): Int? {
        if (episodeData.isNullOrBlank()) return null
        if (!episodeData.startsWith(DownloadEpisodePrefix)) return null
        return episodeData
            .removePrefix(DownloadEpisodePrefix)
            .toIntOrNull()
    }
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
