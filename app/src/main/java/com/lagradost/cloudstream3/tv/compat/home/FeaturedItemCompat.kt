package com.lagradost.cloudstream3.tv.compat.home

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.isEpisodeBased
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap

@Immutable
data class FeaturedItemCompat(
    val id: String,
    val name: String,
    val description: String? = null,
    val supportingLabel: String? = null,
    val year: Int? = null,
    val scoreLabel: String? = null,
    val durationMinutes: Int? = null,
    val posterUri: String = "",
    val backdropUri: String = "",
    val logoUri: String? = null,
    val imageHeaders: PersistentMap<String, String> = persistentMapOf(),
    val navigationTarget: MediaItemCompat,
)

internal fun LoadResponse.toFeaturedItemCompat(): FeaturedItemCompat {
    val normalizedDescription = plot?.trim().takeUnless { it.isNullOrBlank() }
    val castLabel = actors
        ?.asSequence()
        ?.map { actorData -> actorData.actor.name.trim() }
        ?.filter { actorName -> actorName.isNotBlank() }
        ?.take(FEATURED_CAST_PREVIEW_SIZE)
        ?.toList()
        ?.takeIf { castNames -> castNames.isNotEmpty() }
        ?.joinToString(separator = ", ")
    val tagsLabel = tags
        ?.asSequence()
        ?.map { tag -> tag.trim() }
        ?.filter { tag -> tag.isNotBlank() }
        ?.take(FEATURED_TAG_PREVIEW_SIZE)
        ?.toList()
        ?.takeIf { tagNames -> tagNames.isNotEmpty() }
        ?.joinToString(separator = " • ")
    val supportingLabel = castLabel ?: tagsLabel
    val backdropUri = backgroundPosterUrl
        ?.takeIf { url -> url.isNotBlank() }
        ?: posterUrl.orEmpty()
    val posterUri = posterUrl
        ?.takeIf { url -> url.isNotBlank() }
        ?: backdropUri

    return FeaturedItemCompat(
        id = "${apiName}_${url.hashCode()}",
        name = name,
        description = normalizedDescription,
        supportingLabel = supportingLabel,
        year = year,
        scoreLabel = score?.toStringNull(
            minScore = FEATURED_MIN_SCORE,
            maxScore = FEATURED_SCORE_SCALE,
            decimals = FEATURED_SCORE_DECIMALS,
            removeTrailingZeros = false
        ),
        durationMinutes = duration?.takeIf { minutes -> minutes > 0 },
        posterUri = posterUri,
        backdropUri = backdropUri,
        logoUri = logoUrl?.takeIf { logo -> logo.isNotBlank() },
        imageHeaders = posterHeaders.orEmpty().toPersistentMap(),
        navigationTarget = toFeaturedNavigationTarget(
            posterUri = posterUri,
            backdropUri = backdropUri,
            description = normalizedDescription
        )
    )
}

private fun LoadResponse.toFeaturedNavigationTarget(
    posterUri: String,
    backdropUri: String,
    description: String?,
): MediaItemCompat {
    val itemId = "${apiName}_${url.hashCode()}"

    return when {
        type.isEpisodeBased() -> {
            MediaItemCompat.TvSeries(
                id = itemId,
                url = url,
                apiName = apiName,
                name = name,
                posterUri = posterUri,
                type = type,
                score = score,
                backdropUri = backdropUri,
                description = description,
                year = year,
                episodes = null
            )
        }

        type == TvType.Movie || type == TvType.AnimeMovie -> {
            MediaItemCompat.Movie(
                id = itemId,
                url = url,
                apiName = apiName,
                name = name,
                posterUri = posterUri,
                type = type,
                score = score,
                backdropUri = backdropUri,
                description = description,
                year = year
            )
        }

        else -> {
            MediaItemCompat.Other(
                id = itemId,
                url = url,
                apiName = apiName,
                name = name,
                posterUri = posterUri,
                type = type,
                score = score,
                backdropUri = backdropUri,
                description = description
            )
        }
    }
}

private const val FEATURED_CAST_PREVIEW_SIZE = 3
private const val FEATURED_TAG_PREVIEW_SIZE = 4
private const val FEATURED_MIN_SCORE = 0.1
private const val FEATURED_SCORE_SCALE = 10
private const val FEATURED_SCORE_DECIMALS = 1
