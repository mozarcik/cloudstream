package com.lagradost.cloudstream3.tv.data.entities

data class TvEpisode(
    val id: String,
    val data: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val title: String,
    val description: String,
    val durationMinutes: Int?,
    val ratingText: String?,
    val releaseDateMillis: Long?,
    val posterUri: String,
)

data class TvSeason(
    val id: String,
    val seasonNumber: Int?,
    val displaySeasonNumber: Int?,
    val title: String?,
    val episodes: List<TvEpisode>,
)
