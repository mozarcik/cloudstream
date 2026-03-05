package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.utils.VideoDownloadManager

@Immutable
internal data class DetailsDownloadButtonUiState(
    val episodeId: Int? = null,
    val status: VideoDownloadManager.DownloadType? = null,
    val progressFraction: Float = 0f,
)
