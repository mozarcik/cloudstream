package com.lagradost.cloudstream3.tv.compat

import androidx.annotation.DrawableRes
import com.lagradost.cloudstream3.utils.VideoDownloadManager

data class MovieDetailsCompatPanelItem(
    val id: Int,
    val label: String,
    @DrawableRes val iconRes: Int? = null,
    val key: String = "movie_action_$id",
    val titleMaxLines: Int = 1,
    val supportingTexts: List<String> = emptyList(),
    val isSectionHeader: Boolean = false,
)

data class MovieDetailsCompatSelectionRequest(
    val title: String,
    val options: List<MovieDetailsCompatPanelItem>,
    val onOptionSelected: suspend (Int) -> MovieDetailsCompatActionOutcome,
)

data class MovieDetailsCompatDownloadSnapshot(
    val episodeId: Int,
    val status: VideoDownloadManager.DownloadType?,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val hasPendingRequest: Boolean,
)

sealed interface MovieDetailsCompatActionOutcome {
    data object Completed : MovieDetailsCompatActionOutcome
    data class OpenSelection(
        val request: MovieDetailsCompatSelectionRequest,
    ) : MovieDetailsCompatActionOutcome
}
