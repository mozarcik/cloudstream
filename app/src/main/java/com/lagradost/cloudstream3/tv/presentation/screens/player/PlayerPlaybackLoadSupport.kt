package com.lagradost.cloudstream3.tv.presentation.screens.player

import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.player.SubtitleOrigin

internal data class PlayerPlaybackRestoreRequest(
    val requestId: Long,
    val positionMs: Long,
    val playWhenReady: Boolean,
)

internal fun playbackRestoreRequestKey(
    activeRequest: PlayerPlaybackRestoreRequest?,
    lastConsumedRequestId: Long?,
): Long? {
    return activeRequest?.requestId ?: lastConsumedRequestId
}

internal fun shouldAttemptRuntimeSubtitleSelection(
    isLinkChanged: Boolean,
    isSubtitleChanged: Boolean,
    selectedSubtitle: SubtitleData?,
    subtitleSelectionSource: TvPlayerSubtitleSelectionSource,
    isCurrentSourceReady: Boolean,
): Boolean {
    if (isLinkChanged || !isSubtitleChanged) {
        return false
    }

    return when (subtitleSelectionSource) {
        TvPlayerSubtitleSelectionSource.User -> true
        TvPlayerSubtitleSelectionSource.Auto -> {
            isCurrentSourceReady || selectedSubtitle?.origin == SubtitleOrigin.EMBEDDED_IN_VIDEO
        }
        TvPlayerSubtitleSelectionSource.None,
        TvPlayerSubtitleSelectionSource.PlaybackErrorRecovery -> false
    }
}
