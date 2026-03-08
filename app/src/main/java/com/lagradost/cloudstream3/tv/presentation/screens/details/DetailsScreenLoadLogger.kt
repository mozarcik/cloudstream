package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.util.Log

internal object DetailsScreenLoadLogger {
    private const val DebugTag = "TvDetailsVM"

    fun logMissingArgs() {
        Log.e(DebugTag, "missing navigation args")
    }

    fun logPrimaryLoaded(
        primary: DetailsPrimaryStageResult,
        mode: DetailsScreenMode,
    ) {
        Log.d(
            DebugTag,
            "loaded primary details id=${primary.details.id} name=${primary.details.name} mode=$mode seasonCount=${primary.details.seasonCount} episodeCount=${primary.details.episodeCount} seasons=${primary.details.seasons.size}"
        )
    }

    fun logSecondaryLoaded(secondary: DetailsSecondaryStageResult) {
        Log.d(
            DebugTag,
            "loaded secondary details id=${secondary.details.id} cast=${secondary.details.cast.size} similar=${secondary.details.similarMovies.size} currentSeason=${secondary.details.currentSeason} currentEpisode=${secondary.details.currentEpisode}"
        )
    }

    fun logSecondaryFailure(
        source: DetailsRouteSource,
        mode: DetailsScreenMode,
        error: Throwable,
    ) {
        Log.e(
            DebugTag,
            "failed loading secondary details for api=${source.apiName} url=${source.url} mode=$mode",
            error
        )
    }

    fun logPrimaryFailure(
        source: DetailsRouteSource,
        mode: DetailsScreenMode,
        error: Throwable,
    ) {
        Log.e(
            DebugTag,
            "failed loading primary details for api=${source.apiName} url=${source.url} mode=$mode",
            error
        )
    }
}
