package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
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
    isSeries: Boolean = false,
): DetailsVisualsState {
    // Colors come from the backdrop (DetailsDynamicColor*); until they are extracted the warm (movie) /
    // cool (series) design tint is used. Focus fill is the tinted light color with the page color as text.
    val fallback = remember(baseColorScheme, isSeries) { designDetailsColorScheme(baseColorScheme, isSeries) }
    val artworkUrl = remember(details.id, details.backdropUri, details.posterUri) {
        details.backdropUri.takeIf { it.isNotBlank() }
            ?: details.posterUri.takeIf { it.isNotBlank() }
    }
    val dynamicColorScheme = rememberDetailsDynamicColorScheme(
        artworkUrl = artworkUrl,
        baseColorScheme = fallback,
    )
    return remember(dynamicColorScheme, fallback) {
        val scheme = if (dynamicColorScheme === fallback) {
            fallback
        } else {
            dynamicColorScheme.copy(
                inverseSurface = dynamicColorScheme.onSurface,
                inverseOnSurface = dynamicColorScheme.background,
            )
        }
        DetailsVisualsState(colorScheme = scheme)
    }
}

private fun designDetailsColorScheme(base: ColorScheme, isSeries: Boolean): ColorScheme {
    return if (isSeries) {
        base.copy(
            primary = TvColors.DetailsSeriesPrimary,
            onPrimary = TvColors.DetailsSeriesOnPrimary,
            background = TvColors.DetailsSeriesBackground,
            surface = TvColors.DetailsSeriesBackground,
            onSurface = TvColors.DetailsSeriesOnSurface,
            onSurfaceVariant = TvColors.DetailsSeriesOnSurfaceVariant,
            inverseSurface = TvColors.DetailsSeriesInverseSurface,
            inverseOnSurface = TvColors.DetailsSeriesBackground,
        )
    } else {
        base.copy(
            primary = TvColors.DetailsMoviePrimary,
            onPrimary = TvColors.DetailsMovieOnPrimary,
            background = TvColors.DetailsMovieBackground,
            surface = TvColors.DetailsMovieBackground,
            onSurface = TvColors.DetailsMovieOnSurface,
            onSurfaceVariant = TvColors.DetailsMovieOnSurfaceVariant,
            inverseSurface = TvColors.DetailsMovieInverseSurface,
            inverseOnSurface = TvColors.DetailsMovieBackground,
        )
    }
}
