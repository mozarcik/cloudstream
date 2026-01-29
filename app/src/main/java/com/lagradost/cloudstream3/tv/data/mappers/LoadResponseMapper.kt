package com.lagradost.cloudstream3.tv.data.mappers

import android.util.Log
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.SeasonData
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.data.entities.MovieCast
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason

private const val DebugTag = "TvDetailsMapper"

/**
 * Mapper converting CloudStream LoadResponse to Compose MovieDetails.
 * Supports Movie, TvSeries, Anime, and other media types
 */
fun LoadResponse.toMovieDetails(): MovieDetails {
    Log.d(DebugTag, "map:start input=${this::class.java.simpleName} type=${this.type}")

    return when (this) {
        is MovieLoadResponse -> this.toMovieDetails()
        is TvSeriesLoadResponse -> this.toMovieDetails()
        is AnimeLoadResponse -> this.toMovieDetails()
        else -> this.toGenericMovieDetails()
    }
}

/**
 * Converts MovieLoadResponse to MovieDetails
 */
private fun MovieLoadResponse.toMovieDetails(): MovieDetails {
    Log.d(
        DebugTag,
        "map:movie name=${this.name} type=${this.type} year=${this.year} recommendations=${this.recommendations?.size ?: 0}"
    )

    return MovieDetails(
        id = this.url,
        videoUri = "",
        subtitleUri = null,
        posterUri = this.backgroundPosterUrl ?: "",
        name = this.name,
        description = this.plot ?: "",
        pgRating = this.contentRating ?: "",
        releaseDate = this.year?.toString() ?: "",
        categories = this.tags ?: emptyList(),
        duration = this.duration?.let { "${it}min" } ?: "",
        director = "",
        screenplay = "",
        music = "",
        cast = this.actors?.mapIndexed { index, actorData ->
            MovieCast(
                id = index.toString(),
                characterName = actorData.roleString ?: "",
                realName = actorData.actor.name,
                avatarUrl = actorData.actor.image ?: ""
            )
        } ?: emptyList(),
        status = "",
        originalLanguage = "",
        budget = "",
        revenue = "",
        similarMovies = this.recommendations?.mapIndexed {index, response ->
            Movie(
                id = index.toString(),
                posterUri = response.posterUrl ?: "",
                name = response.name,
                description = "",
            )
        } ?: emptyList(),
    )
}

/**
 * Converts TvSeriesLoadResponse to MovieDetails
 */
private fun TvSeriesLoadResponse.toMovieDetails(): MovieDetails {
    val episodes = this.episodes
    val seasonCount = episodes.extractSeasonCount(this.seasonNames?.mapNotNull { it.displaySeason ?: it.season })
    val episodeCount = episodes.extractEpisodeCount()
    val currentEpisode = episodes.findCurrentEpisode()
    val seasons = episodes.toTvSeasons(
        seasonNames = this.seasonNames,
        defaultPosterUri = this.backgroundPosterUrl ?: this.posterUrl ?: ""
    )
    val mappedSeasonSummary = seasons.joinToString(separator = ",") { season ->
        val seasonNumber = season.displaySeasonNumber ?: season.seasonNumber ?: -1
        "S${seasonNumber}:${season.episodes.size}"
    }
    Log.d(
        DebugTag,
        "map:tvSeries name=${this.name} rawEpisodes=${episodes.size} seasonNames=${this.seasonNames?.size ?: 0} seasonCount=$seasonCount episodeCount=$episodeCount mappedSeasons=${seasons.size} mapped=${mappedSeasonSummary}"
    )

    return MovieDetails(
        id = this.url,
        videoUri = "",
        subtitleUri = null,
        posterUri = this.backgroundPosterUrl ?: "",
        name = this.name,
        description = this.plot ?: "",
        seasons = seasons,
        seasonCount = seasonCount,
        episodeCount = episodeCount,
        currentSeason = currentEpisode?.season?.takeIf { it > 0 },
        currentEpisode = currentEpisode?.episode,
        pgRating = this.contentRating ?: "",
        releaseDate = this.year?.toString() ?: "",
        categories = this.tags ?: emptyList(),
        duration = this.duration?.let { "${it}min" } ?: "",
        director = "",
        screenplay = "",
        music = "",
        cast = this.actors?.mapIndexed { index, actorData ->
            MovieCast(
                id = index.toString(),
                characterName = actorData.roleString ?: "",
                realName = actorData.actor.name,
                avatarUrl = actorData.actor.image ?: ""
            )
        } ?: emptyList(),
        status = "",
        originalLanguage = "",
        budget = "",
        revenue = "",
        similarMovies = this.recommendations?.mapIndexed {index, response ->
            Movie(
                id = index.toString(),
                posterUri = response.posterUrl ?: "",
                name = response.name,
                description = "",
            )
        } ?: emptyList(),
    )
}

/**
 * Converts AnimeLoadResponse to MovieDetails
 */
private fun AnimeLoadResponse.toMovieDetails(): MovieDetails {
    val episodes = this.episodes.values.flatten()
    val seasonCount = episodes.extractSeasonCount()
    val episodeCount = episodes.extractEpisodeCount()
    val currentEpisode = episodes.findCurrentEpisode()
    val seasons = episodes.toTvSeasons(
        seasonNames = this.seasonNames,
        defaultPosterUri = this.backgroundPosterUrl ?: this.posterUrl ?: "",
        deduplicateBySeasonAndEpisode = true
    )
    val mappedSeasonSummary = seasons.joinToString(separator = ",") { season ->
        val seasonNumber = season.displaySeasonNumber ?: season.seasonNumber ?: -1
        "S${seasonNumber}:${season.episodes.size}"
    }
    Log.d(
        DebugTag,
        "map:anime name=${this.name} rawEpisodes=${episodes.size} seasonNames=${this.seasonNames?.size ?: 0} seasonCount=$seasonCount episodeCount=$episodeCount mappedSeasons=${seasons.size} mapped=${mappedSeasonSummary}"
    )

    return MovieDetails(
        id = this.url,
        videoUri = "",
        subtitleUri = null,
        posterUri = this.backgroundPosterUrl ?: "",
        name = this.name,
        description = this.plot ?: "",
        seasons = seasons,
        seasonCount = seasonCount,
        episodeCount = episodeCount,
        currentSeason = currentEpisode?.season?.takeIf { it > 0 },
        currentEpisode = currentEpisode?.episode,
        pgRating = this.contentRating ?: "",
        releaseDate = this.year?.toString() ?: "",
        categories = this.tags ?: emptyList(),
        duration = this.duration?.let { "${it}min" } ?: "",
        director = "",
        screenplay = "",
        music = "",
        cast = this.actors?.mapIndexed { index, actorData ->
            MovieCast(
                id = index.toString(),
                characterName = actorData.roleString ?: "",
                realName = actorData.actor.name,
                avatarUrl = actorData.actor.image ?: ""
            )
        } ?: emptyList(),
        status = "",
        originalLanguage = "",
        budget = "",
        revenue = "",
        similarMovies = this.recommendations?.mapIndexed {index, response ->
            Movie(
                id = index.toString(),
                posterUri = response.posterUrl ?: "",
                name = response.name,
                description = "",
            )
        } ?: emptyList(),
    )
}

/**
 * Generic converter for other LoadResponse types
 */
private fun LoadResponse.toGenericMovieDetails(): MovieDetails {
    Log.d(DebugTag, "map:generic name=${this.name} type=${this.type}")

    return MovieDetails(
        id = this.url,
        videoUri = "",
        subtitleUri = null,
        posterUri = this.backgroundPosterUrl ?: "",
        name = this.name,
        description = this.plot ?: "",
        pgRating = "",
        releaseDate = "",
        categories = this.tags ?: emptyList(),
        duration = "",
        director = "",
        screenplay = "",
        music = "",
        cast = emptyList(),
        status = "",
        originalLanguage = "",
        budget = "",
        revenue = "",
        similarMovies = this.recommendations?.mapIndexed {index, response ->
            Movie(
                id = index.toString(),
                posterUri = response.posterUrl ?: "",
                name = response.name,
                description = "",
            )
        } ?: emptyList(),
    )
}

private fun List<Episode>.extractSeasonCount(extraSeasons: List<Int>? = null): Int? {
    val fromEpisodes = this.mapNotNull { episode ->
        episode.season?.takeIf { it > 0 }
    }.distinct()
    val merged = (extraSeasons.orEmpty().filter { it > 0 } + fromEpisodes).distinct()

    return when {
        merged.isNotEmpty() -> merged.size
        this.isNotEmpty() -> 1
        else -> null
    }
}

private fun List<Episode>.extractEpisodeCount(): Int? {
    if (this.isEmpty()) return null

    val distinctIndexedEpisodes = this.mapNotNull { episode ->
        episode.episode?.let { episodeNumber ->
            (episode.season ?: 0) to episodeNumber
        }
    }.distinct()

    return if (distinctIndexedEpisodes.isNotEmpty()) {
        distinctIndexedEpisodes.size
    } else {
        this.size
    }
}

private fun List<Episode>.findCurrentEpisode(): Episode? {
    return this
        .asSequence()
        .filter { episode -> episode.season != null || episode.episode != null }
        .sortedWith(compareBy<Episode>({ it.season ?: 0 }, { it.episode ?: Int.MAX_VALUE }))
        .firstOrNull()
}

private fun List<Episode>.toTvSeasons(
    seasonNames: List<SeasonData>?,
    defaultPosterUri: String,
    deduplicateBySeasonAndEpisode: Boolean = false
): List<TvSeason> {
    if (this.isEmpty()) return emptyList()

    val sourceEpisodes = if (deduplicateBySeasonAndEpisode) {
        this.distinctBy { episode ->
            Triple(
                episode.season?.takeIf { it > 0 } ?: 1,
                episode.episode ?: Int.MAX_VALUE,
                episode.name.orEmpty()
            )
        }
    } else {
        this
    }

    val seasonDataBySeason = seasonNames.orEmpty().associateBy { it.season }
    val episodesBySeason = sourceEpisodes.groupBy { episode ->
        episode.season?.takeIf { it > 0 } ?: 1
    }

    val mappedSeasons = episodesBySeason
        .entries
        .sortedBy { (seasonNumber, _) ->
            seasonDataBySeason[seasonNumber]?.displaySeason?.takeIf { it > 0 } ?: seasonNumber
        }
        .map { (seasonNumber, episodesForSeason) ->
            val seasonData = seasonDataBySeason[seasonNumber]
            val displaySeasonNumber = seasonData?.displaySeason?.takeIf { it > 0 } ?: seasonNumber
            val seasonTitle = seasonData?.name?.takeIf { it.isNotBlank() }

            val sortedEpisodes = episodesForSeason
                .sortedWith(compareBy<Episode>({ it.episode ?: Int.MAX_VALUE }, { it.name.orEmpty() }))
                .mapIndexed { index, episode ->
                    val episodeNumber = episode.episode?.takeIf { it > 0 }
                    TvEpisode(
                        id = listOf(
                            "s$seasonNumber",
                            "e${episodeNumber ?: "x"}",
                            episode.data.hashCode().toString(),
                            index.toString()
                        ).joinToString("-"),
                        data = episode.data,
                        seasonNumber = episode.season?.takeIf { it > 0 } ?: seasonNumber,
                        episodeNumber = episodeNumber,
                        title = episode.name.orEmpty(),
                        description = episode.description.orEmpty(),
                        durationMinutes = episode.extractDurationMinutes(),
                        ratingText = episode.extractRatingText(),
                        releaseDateMillis = episode.date,
                        posterUri = episode.posterUrl?.takeIf { it.isNotBlank() } ?: defaultPosterUri
                    )
                }

            TvSeason(
                id = "season-$seasonNumber",
                seasonNumber = seasonNumber,
                displaySeasonNumber = displaySeasonNumber,
                title = seasonTitle,
                episodes = sortedEpisodes
            )
        }

    Log.d(
        DebugTag,
        "map:toTvSeasons sourceEpisodes=${sourceEpisodes.size} dedupe=$deduplicateBySeasonAndEpisode outputSeasons=${mappedSeasons.size}"
    )

    return mappedSeasons
}

private fun Episode.extractDurationMinutes(): Int? {
    val runtime = this.runTime ?: return null
    if (runtime <= 0) return null

    return if (runtime in 5..240) {
        runtime
    } else {
        (runtime + 59) / 60
    }
}

private fun Episode.extractRatingText(): String? {
    return this.score
        ?.toString(maxScore = 10, decimals = 1, removeTrailingZeros = true)
        ?.takeIf { rating -> rating.isNotBlank() }
}
