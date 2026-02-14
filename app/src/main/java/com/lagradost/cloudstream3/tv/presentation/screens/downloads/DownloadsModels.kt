package com.lagradost.cloudstream3.tv.presentation.screens.downloads

enum class DownloadMediaType {
    Movie,
    Series,
    Media,
}

sealed interface DownloadState {
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long?,
        val speedBytesPerSec: Long?,
        val etaSeconds: Long?,
    ) : DownloadState

    data class Downloaded(
        val fileSizeBytes: Long,
    ) : DownloadState

    data class Failed(
        val errorMessage: String?,
    ) : DownloadState

    data class Paused(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long?,
    ) : DownloadState
}

data class DownloadItemUiModel(
    val id: String,
    val episodeId: Int,
    val parentId: Int,
    val title: String,
    val episodeName: String?,
    val episodeNumber: Int?,
    val seasonNumber: Int?,
    val description: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val mediaType: DownloadMediaType,
    val sourceUrl: String,
    val apiName: String,
    val state: DownloadState,
    val startedAtMillis: Long,
    val updatedAtMillis: Long,
    val downloadedAtMillis: Long? = null,
)

data class DownloadsUiState(
    val downloadingItems: List<DownloadItemUiModel> = emptyList(),
    val downloadedItems: List<DownloadItemUiModel> = emptyList(),
)
