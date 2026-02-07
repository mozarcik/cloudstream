package com.lagradost.cloudstream3.tv.data.entities

data class MovieDetails(
    val id: String,
    val name: String,
    val description: String,
    val posterUri: String,
    val seasons: List<TvSeason> = emptyList(),
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val currentSeason: Int? = null,
    val currentEpisode: Int? = null,
    // Optional fields (may not be available from all providers)
    val cast: List<MovieCast> = emptyList(),
    val similarMovies: MovieList = emptyList(),
    val videoUri: String = "",
    val subtitleUri: String? = null,
    val pgRating: String = "",
    val releaseDate: String = "",
    val categories: List<String> = emptyList(),
    val duration: String = "",
    val director: String = "",
    val screenplay: String = "",
    val music: String = "",
    val status: String = "",
    val originalLanguage: String = "",
    val budget: String = "",
    val revenue: String = "",
)
