package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.data.entities.MovieCast
import com.lagradost.cloudstream3.tv.data.util.StringConstants
import com.lagradost.cloudstream3.tv.presentation.common.PosterMoviesRow
import com.lagradost.cloudstream3.tv.presentation.screens.movies.CastAndCrewList
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.AdditionalInfoSection
import com.lagradost.cloudstream3.tv.presentation.utils.Padding

internal fun LazyListScope.detailsSecondaryItems(
    isSecondaryContentLoading: Boolean,
    hasAdditionalInfo: Boolean,
    detailsName: String,
    detailsStatus: String,
    detailsOriginalLanguage: String,
    detailsBudget: String,
    detailsRevenue: String,
    cast: List<MovieCast>,
    similarMovies: List<Movie>,
    details: com.lagradost.cloudstream3.tv.data.entities.MovieDetails,
    childPadding: Padding,
    refreshScreenWithNewItem: (Movie) -> Unit,
) {
    if (isSecondaryContentLoading) {
        return
    }

    if (cast.isNotEmpty()) {
        item {
            CastAndCrewList(castAndCrew = cast)
        }
    }

    if (similarMovies.isNotEmpty()) {
        item {
            PosterMoviesRow(
                title = StringConstants.Composable.movieDetailsScreenSimilarTo(detailsName),
                titleStyle = MaterialTheme.typography.titleMedium,
                movieList = similarMovies,
                onMovieSelected = refreshScreenWithNewItem
            )
        }
    }

    val shouldShowAdditionalInfo = hasAdditionalInfo || listOf(
        detailsStatus,
        detailsOriginalLanguage,
        detailsBudget,
        detailsRevenue,
    ).any { value -> value.isNotBlank() }
    if (!shouldShowAdditionalInfo) {
        return
    }

    item {
        Box(
            modifier = Modifier
                .padding(start = childPadding.start, end = childPadding.end)
                .padding(DetailsBottomDividerPadding)
                .fillMaxWidth()
                .height(1.dp)
                .alpha(0.15f)
                .background(MaterialTheme.colorScheme.onSurface)
        )
    }

    item {
        AdditionalInfoSection(tvSeriesDetails = details)
    }
}
