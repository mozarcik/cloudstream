package com.lagradost.cloudstream3.tv.compat.resume

import androidx.compose.runtime.Immutable

@Immutable
data class ResumePlaybackContext(
    val parentId: Int?,
    val episodeId: Int?,
    val season: Int?,
    val episode: Int?,
    val isFromDownload: Boolean,
)

@Immutable
data class ContinueWatchingState(
    val progress: Float? = null,
    val remainingMs: Long? = null,
    val hasBackdrop: Boolean = false,
    val resume: ResumePlaybackContext,
) {
    val parentId: Int?
        get() = resume.parentId

    val episodeId: Int?
        get() = resume.episodeId

    val season: Int?
        get() = resume.season

    val episode: Int?
        get() = resume.episode

    val isFromDownload: Boolean
        get() = resume.isFromDownload
}
