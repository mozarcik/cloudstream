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
import com.lagradost.cloudstream3.tv.data.repositories.DetailsSecondaryLoadResult
import com.lagradost.cloudstream3.tv.data.repositories.mergeSecondary
import com.lagradost.cloudstream3.ui.result.VideoWatchState
import com.lagradost.cloudstream3.ui.result.getId
import com.lagradost.cloudstream3.utils.DataStoreHelper.getVideoWatchState

private const val DebugTag = "TvDetailsMapper"

/**
 * Mapper converting CloudStream LoadResponse to Compose MovieDetails.
 * Supports Movie, TvSeries, Anime, and other media types
 */
fun LoadResponse.toMovieDetails(): MovieDetails {
    Log.d(DebugTag, "map:start input=${this::class.java.simpleName} type=${this.type}")

    return toPrimaryMovieDetails().mergeSecondary(toSecondaryMovieDetails())
}

fun LoadResponse.toPrimaryMovieDetails(): MovieDetails {
    return when (this) {
        is MovieLoadResponse -> this.toPrimaryMovieDetails()
        is TvSeriesLoadResponse -> this.toPrimaryMovieDetails()
        is AnimeLoadResponse -> this.toPrimaryMovieDetails()
        else -> this.toPrimaryGenericMovieDetails()
    }
}

fun LoadResponse.toSecondaryMovieDetails(): DetailsSecondaryLoadResult {
    return when (this) {
        is MovieLoadResponse -> this.toSecondaryMovieDetails()
        is TvSeriesLoadResponse -> this.toSecondaryMovieDetails()
        is AnimeLoadResponse -> this.toSecondaryMovieDetails()
        else -> this.toSecondaryGenericMovieDetails()
    }
}

private fun MovieLoadResponse.toPrimaryMovieDetails(): MovieDetails {
    Log.d(
        DebugTag,
        "map:movie:primary name=${this.name} type=${this.type} year=${this.year}"
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
    )
}

private fun MovieLoadResponse.toSecondaryMovieDetails(): DetailsSecondaryLoadResult {
    return DetailsSecondaryLoadResult(
        cast = this.actors.toMovieCastList(),
        similarMovies = this.recommendations.toMovieList(),
    )
}

private fun TvSeriesLoadResponse.toPrimaryMovieDetails(): MovieDetails {
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
    val rawSeasonSummary = episodes.toRawSeasonSummary()
    Log.d(
        DebugTag,
        "map:tvSeries:primary name=${this.name} rawEpisodes=${episodes.size} rawSeasons=$rawSeasonSummary seasonNames=${this.seasonNames?.size ?: 0} seasonCount=$seasonCount episodeCount=$episodeCount mappedSeasons=${seasons.size} mapped=${mappedSeasonSummary}"
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
    )
}

private fun TvSeriesLoadResponse.toSecondaryMovieDetails(): DetailsSecondaryLoadResult {
    val resumeEpisode = this.episodes.findCurrentEpisode(mainId = this.getId())
    return DetailsSecondaryLoadResult(
        cast = this.actors.toMovieCastList(),
        similarMovies = this.recommendations.toMovieList(),
        currentSeason = resumeEpisode?.season?.takeIf { it > 0 },
        currentEpisode = resumeEpisode?.episode,
    )
}

private fun AnimeLoadResponse.toPrimaryMovieDetails(): MovieDetails {
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
    val rawSeasonSummary = episodes.toRawSeasonSummary()
    Log.d(
        DebugTag,
        "map:anime:primary name=${this.name} rawEpisodes=${episodes.size} rawSeasons=$rawSeasonSummary seasonNames=${this.seasonNames?.size ?: 0} seasonCount=$seasonCount episodeCount=$episodeCount mappedSeasons=${seasons.size} mapped=${mappedSeasonSummary}"
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
    )
}

private fun AnimeLoadResponse.toSecondaryMovieDetails(): DetailsSecondaryLoadResult {
    return DetailsSecondaryLoadResult(
        cast = this.actors.toMovieCastList(),
        similarMovies = this.recommendations.toMovieList(),
    )
}

private fun LoadResponse.toPrimaryGenericMovieDetails(): MovieDetails {
    Log.d(DebugTag, "map:generic:primary name=${this.name} type=${this.type}")

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
    )
}

private fun LoadResponse.toSecondaryGenericMovieDetails(): DetailsSecondaryLoadResult {
    return DetailsSecondaryLoadResult(
        similarMovies = this.recommendations.toMovieList(),
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
    val missingSeasonBucket = resolveMissingSeasonBucket()
    val sortedEpisodes = this
        .asSequence()
        .filter { episode -> episode.season != null || episode.episode != null }
        .sortedWith(
            compareBy<Episode>(
                { episode -> episode.seasonSortOrder(missingSeasonBucket) },
                { it.episode ?: Int.MAX_VALUE }
            )
        )
        .toList()
    return sortedEpisodes.firstOrNull()
}

private fun List<Episode>.findCurrentEpisode(mainId: Int): Episode? {
    val missingSeasonBucket = resolveMissingSeasonBucket()
    val sortedEpisodes = this
        .asSequence()
        .filter { episode -> episode.season != null || episode.episode != null }
        .sortedWith(
            compareBy<Episode>(
                { episode -> episode.seasonSortOrder(missingSeasonBucket) },
                { it.episode ?: Int.MAX_VALUE }
            )
        )
        .toList()

    val watchedFlags = sortedEpisodes.mapIndexed { index, episode ->
        val episodeIndex = episode.episode ?: (index + 1)
        val episodeId = mainId + (episode.season?.times(100_000) ?: 0) + episodeIndex + 1
        getVideoWatchState(episodeId) == VideoWatchState.Watched
    }
    val lastWatchedIndex = watchedFlags.indexOfLast { watched -> watched }

    return sortedEpisodes.getOrNull(lastWatchedIndex + 1) ?: sortedEpisodes.firstOrNull()
}

private fun List<Episode>.toTvSeasons(
    seasonNames: List<SeasonData>?,
    defaultPosterUri: String,
    deduplicateBySeasonAndEpisode: Boolean = false
): List<TvSeason> {
    if (this.isEmpty()) return emptyList()

    val sourceEpisodes = if (deduplicateBySeasonAndEpisode) {
        val missingSeasonBucket = resolveMissingSeasonBucket()
        this.distinctBy { episode ->
            Triple(
                episode.normalizedSeasonBucket(missingSeasonBucket),
                episode.episode ?: Int.MAX_VALUE,
                episode.name.orEmpty()
            )
        }
    } else {
        this
    }

    val missingSeasonBucket = sourceEpisodes.resolveMissingSeasonBucket()
    val seasonDataBySeason = seasonNames.orEmpty().associateBy { it.season }
    val episodesBySeason = sourceEpisodes.groupBy { episode ->
        episode.normalizedSeasonBucket(missingSeasonBucket)
    }

    val mappedSeasons = episodesBySeason
        .entries
        .sortedBy { (seasonNumber, _) ->
            val seasonOrder = seasonDataBySeason[seasonNumber]
                ?.displaySeason
                ?.takeIf { it > 0 }
                ?: seasonNumber
            seasonOrder.toSeasonSortOrder()
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
                        seasonNumber = episode.season?.takeIf { it > 0 } ?: seasonNumber.takeIf { it > 0 },
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
        "map:toTvSeasons sourceEpisodes=${sourceEpisodes.size} dedupe=$deduplicateBySeasonAndEpisode missingSeasonBucket=$missingSeasonBucket rawSeasons=${sourceEpisodes.toRawSeasonSummary()} outputSeasons=${mappedSeasons.size} mapped=${mappedSeasons.toSeasonDebugSummary()}"
    )

    return mappedSeasons
}

private fun List<Episode>.resolveMissingSeasonBucket(): Int {
    return if (any { episode -> (episode.season ?: 0) > 0 }) 0 else 1
}

private fun Episode.normalizedSeasonBucket(missingSeasonBucket: Int): Int {
    return season?.takeIf { it > 0 } ?: missingSeasonBucket
}

private fun Episode.seasonSortOrder(missingSeasonBucket: Int): Int {
    return normalizedSeasonBucket(missingSeasonBucket).toSeasonSortOrder()
}

private fun Int.toSeasonSortOrder(): Int {
    return takeIf { it > 0 } ?: Int.MAX_VALUE
}

private fun List<Episode>.toRawSeasonSummary(): String {
    return groupBy { episode -> episode.season }
        .entries
        .sortedBy { (season, _) -> season ?: Int.MAX_VALUE }
        .joinToString(separator = ",") { (season, episodes) ->
            val label = season?.toString() ?: "null"
            "$label:${episodes.size}"
        }
}

private fun List<TvSeason>.toSeasonDebugSummary(): String {
    return joinToString(separator = ",") { season ->
        val seasonLabel = season.displaySeasonNumber ?: season.seasonNumber
        val episodePreview = season.episodes
            .take(4)
            .joinToString(separator = "|") { episode ->
                "${episode.seasonNumber ?: "null"}:${episode.episodeNumber ?: "null"}:${episode.title.take(18)}"
            }
        "bucket=${season.seasonNumber ?: "null"} display=${seasonLabel ?: "null"} count=${season.episodes.size} preview=[$episodePreview]"
    }
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

private fun List<com.lagradost.cloudstream3.ActorData>?.toMovieCastList(): List<MovieCast> {
    return this?.mapIndexed { index, actorData ->
        MovieCast(
            id = index.toString(),
            characterName = actorData.roleString ?: "",
            realName = actorData.actor.name,
            avatarUrl = actorData.actor.image ?: ""
        )
    } ?: emptyList()
}

private fun List<com.lagradost.cloudstream3.SearchResponse>?.toMovieList(): List<Movie> {
    return this?.mapIndexed { index, response ->
        Movie(
            id = index.toString(),
            posterUri = response.posterUrl ?: "",
            name = response.name,
            description = "",
        )
    } ?: emptyList()
}
