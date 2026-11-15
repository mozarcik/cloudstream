package com.lagradost.cloudstream3.homeserver

/** Item of the server library (a video stored on the home S3). */
data class ServerLibraryItem(
    val id: String,
    val title: String,
    val poster: String?,
    val parentId: Int?,
    val episodeId: Int?,
    val apiName: String,
    val url: String,
    val season: Int?,
    val episode: Int?,
    val size: Long,
    val createdAt: Long,
)

data class ServerSubtitle(val lang: String, val url: String)

/** Presigned, header-free URLs ready for the player. */
data class ServerPlayable(
    val item: ServerLibraryItem,
    val videoUrl: String,
    val subtitles: List<ServerSubtitle>,
)

data class UploadMetadata(
    val title: String,
    val filename: String,
    val size: Long,
    val poster: String?,
    val parentId: Int?,
    val episodeId: Int?,
    val apiName: String,
    val url: String,
    val season: Int?,
    val episode: Int?,
)
