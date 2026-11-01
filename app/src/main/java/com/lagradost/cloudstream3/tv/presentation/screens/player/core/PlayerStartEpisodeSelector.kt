package com.lagradost.cloudstream3.tv.presentation.screens.player.core

import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerStartTarget
import com.lagradost.cloudstream3.ui.result.ResultEpisode

internal object PlayerStartEpisodeSelector {
    fun resolveSelectedEpisodeIndex(
        startTarget: PlayerStartTarget,
        episodes: List<ResultEpisode>,
        fallbackEpisodeData: String?,
    ): Int {
        val preferredIndex = when (startTarget) {
            is PlayerStartTarget.ResumeEpisode -> episodes.indexOfFirst { episode ->
                episode.id == startTarget.episodeId
            }

            is PlayerStartTarget.DirectEpisodeData -> episodes.indexOfFirst { episode ->
                episode.data == startTarget.data
            }

            PlayerStartTarget.Default -> -1
            is PlayerStartTarget.DownloadedEpisode -> -1
        }
        if (preferredIndex >= 0) {
            return preferredIndex
        }

        val fallbackIndex = episodes.indexOfFirst { episode ->
            fallbackEpisodeData != null && episode.data == fallbackEpisodeData
        }
        return fallbackIndex.takeIf { index -> index >= 0 } ?: 0
    }
}
