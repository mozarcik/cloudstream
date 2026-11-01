package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.lifecycle.SavedStateHandle

sealed interface PlayerStartTarget {
    data object Default : PlayerStartTarget
    data class DirectEpisodeData(val data: String) : PlayerStartTarget
    data class ResumeEpisode(val episodeId: Int) : PlayerStartTarget
    data class DownloadedEpisode(val episodeId: Int) : PlayerStartTarget

    /** Resume entry of a removed provider replayed through another one; never put into navigation. */
    data class RemappedResume(val episodeId: Int, val previousParentId: Int) : PlayerStartTarget
}

internal object PlayerScreenNavigation {
    const val UrlBundleKey = "url"
    const val ApiNameBundleKey = "apiName"
    const val PlaybackTargetBundleKey = "playbackTarget"
    private const val ResumeEpisodePrefix = "__resume_episode__:"
    private const val DownloadEpisodePrefix = "__download_episode__:"

    fun toNavigationArg(target: PlayerStartTarget): String {
        return when (target) {
            PlayerStartTarget.Default -> ""
            is PlayerStartTarget.DirectEpisodeData -> target.data
            is PlayerStartTarget.ResumeEpisode -> "$ResumeEpisodePrefix${target.episodeId}"
            is PlayerStartTarget.DownloadedEpisode -> "$DownloadEpisodePrefix${target.episodeId}"
            is PlayerStartTarget.RemappedResume -> ""
        }
    }

    fun fromNavigationArg(rawValue: String?): PlayerStartTarget {
        if (rawValue.isNullOrBlank()) {
            return PlayerStartTarget.Default
        }

        parseEpisodeId(rawValue = rawValue, prefix = DownloadEpisodePrefix)?.let { episodeId ->
            return PlayerStartTarget.DownloadedEpisode(episodeId)
        }
        parseEpisodeId(rawValue = rawValue, prefix = ResumeEpisodePrefix)?.let { episodeId ->
            return PlayerStartTarget.ResumeEpisode(episodeId)
        }

        return PlayerStartTarget.DirectEpisodeData(rawValue)
    }

    private fun parseEpisodeId(
        rawValue: String,
        prefix: String,
    ): Int? {
        if (!rawValue.startsWith(prefix)) return null
        return rawValue
            .removePrefix(prefix)
            .toIntOrNull()
    }
}

internal fun createPlayerSavedStateHandle(
    url: String,
    apiName: String,
    playbackTarget: String,
): SavedStateHandle {
    return SavedStateHandle().apply {
        set(PlayerScreenNavigation.UrlBundleKey, url)
        set(PlayerScreenNavigation.ApiNameBundleKey, apiName)
        set(PlayerScreenNavigation.PlaybackTargetBundleKey, playbackTarget)
    }
}
