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
    val artworkUrl = remember(details.id, details.backdropUri, details.posterUri) {
        details.backdropUri.takeIf { it.isNotBlank() }
            ?: details.posterUri.takeIf { it.isNotBlank() }
    }
    val dynamicColorScheme = rememberDetailsDynamicColorScheme(
        artworkUrl = artworkUrl,
        baseColorScheme = baseColorScheme,
    )

    return remember(dynamicColorScheme) {
        DetailsVisualsState(colorScheme = dynamicColorScheme)
    }
}
