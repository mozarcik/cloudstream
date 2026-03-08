package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.tv.material3.ColorScheme
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails

@Immutable
internal data class DetailsVisualsState(
    val colorScheme: ColorScheme,
)

@Composable
internal fun rememberDetailsVisualsState(
    details: MovieDetails,
    baseColorScheme: ColorScheme,
): DetailsVisualsState {
    val artworkKey = remember(details.id, details.posterUri) {
        "details:${details.id}:${details.posterUri}"
    }
    val dynamicColorScheme = rememberDetailsDynamicColorScheme(
        artworkKey = artworkKey,
        artworkUrl = details.posterUri.takeIf { it.isNotBlank() },
        baseColorScheme = baseColorScheme,
    )

    return remember(dynamicColorScheme) {
        DetailsVisualsState(colorScheme = dynamicColorScheme)
    }
}
