package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.tv.data.entities.MovieDetails

internal fun MovieDetails.hasDetailsAdditionalInfo(): Boolean {
    return listOf(
        status,
        originalLanguage,
        budget,
        revenue,
    ).any { value -> value.isNotBlank() }
}
