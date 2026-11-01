package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.annotation.StringRes
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails

enum class DetailsScreenMode(
    @StringRes val loadingTitleRes: Int,
    val unavailableFallbackType: TvType,
    val allowsBookmark: Boolean,
    val allowsExtendedActions: Boolean,
    val downloadPanelTestTag: String,
    val bookmarkPanelTestTag: String? = null,
) {
    Movie(
        loadingTitleRes = R.string.movies_singular,
        unavailableFallbackType = TvType.Movie,
        allowsBookmark = true,
        allowsExtendedActions = true,
        downloadPanelTestTag = "movie_download_sources_side_panel",
        bookmarkPanelTestTag = "movie_bookmark_side_panel",
    ),
    TvSeries(
        loadingTitleRes = R.string.tv_series_singular,
        unavailableFallbackType = TvType.TvSeries,
        allowsBookmark = true,
        allowsExtendedActions = true,
        downloadPanelTestTag = "tv_series_download_sources_side_panel",
        bookmarkPanelTestTag = "tv_series_bookmark_side_panel",
    ),
    Media(
        loadingTitleRes = R.string.loading,
        unavailableFallbackType = TvType.Movie,
        allowsBookmark = false,
        allowsExtendedActions = false,
        downloadPanelTestTag = "media_download_sources_side_panel",
    );
}

internal fun DetailsScreenMode.isSeriesContent(details: MovieDetails): Boolean {
    return when (this) {
        DetailsScreenMode.Movie -> false
        DetailsScreenMode.TvSeries -> true
        DetailsScreenMode.Media -> {
            details.seasonCount != null ||
                details.episodeCount != null ||
                details.seasons.isNotEmpty()
        }
    }
}
