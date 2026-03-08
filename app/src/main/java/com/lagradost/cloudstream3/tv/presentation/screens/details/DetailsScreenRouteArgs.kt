package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.lifecycle.SavedStateHandle
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.compat.UnavailableDetailsCompat
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DetailsLoadingPreview
import com.lagradost.cloudstream3.tv.presentation.screens.unavailable.UnavailableDetailsUiModel

internal data class DetailsRouteSource(
    val url: String,
    val apiName: String,
)

internal data class DetailsScreenRouteArgs(
    val sourceUrl: String?,
    val sourceApiName: String?,
    val loadingPreview: DetailsLoadingPreview,
    val unavailableDetails: UnavailableDetailsUiModel,
) {
    val source: DetailsRouteSource?
        get() {
            val resolvedUrl = sourceUrl ?: return null
            val resolvedApiName = sourceApiName ?: return null
            return DetailsRouteSource(
                url = resolvedUrl,
                apiName = resolvedApiName,
            )
        }

    val shouldShowUnavailableState: Boolean
        get() = source != null
}

internal fun SavedStateHandle.toDetailsScreenRouteArgs(
    mode: DetailsScreenMode,
): DetailsScreenRouteArgs {
    val sourceApiName = get<String>(DetailsScreenNavigation.ApiNameBundleKey)
        ?.takeIf { it.isNotBlank() }

    return DetailsScreenRouteArgs(
        sourceUrl = get<String>(DetailsScreenNavigation.UrlBundleKey)
            ?.takeIf { it.isNotBlank() },
        sourceApiName = sourceApiName,
        loadingPreview = DetailsLoadingPreview(
            title = get<String>(DetailsScreenNavigation.LoadingTitleBundleKey)
                ?.takeIf { it.isNotBlank() },
            posterUri = get<String>(DetailsScreenNavigation.LoadingPosterBundleKey)
                ?.takeIf { it.isNotBlank() },
            backdropUri = get<String>(DetailsScreenNavigation.LoadingBackdropBundleKey)
                ?.takeIf { it.isNotBlank() },
        ),
        unavailableDetails = UnavailableDetailsUiModel(
            title = get<String>(DetailsScreenNavigation.LoadingTitleBundleKey)
                ?.takeIf { it.isNotBlank() }
                .orEmpty(),
            posterUrl = get<String>(DetailsScreenNavigation.LoadingPosterBundleKey)
                ?.takeIf { it.isNotBlank() },
            backdropUrl = get<String>(DetailsScreenNavigation.LoadingBackdropBundleKey)
                ?.takeIf { it.isNotBlank() },
            description = get<String>(DetailsScreenNavigation.LoadingDescriptionBundleKey)
                ?.takeIf { it.isNotBlank() },
            type = get<String>(DetailsScreenNavigation.LoadingTypeBundleKey)
                .toTvTypeOrNull()
                ?: mode.unavailableFallbackType,
            year = get<Int>(DetailsScreenNavigation.LoadingYearBundleKey),
            providerName = get<String>(DetailsScreenNavigation.LoadingProviderBundleKey)
                ?.takeIf { it.isNotBlank() }
                ?: sourceApiName,
        )
    )
}

internal fun DetailsScreenRouteArgs.canRemoveFromLibrary(): Boolean {
    val source = source ?: return false
    return UnavailableDetailsCompat.isInLocalLibrary(
        sourceUrl = source.url,
        apiName = source.apiName,
    )
}

private fun String?.toTvTypeOrNull(): TvType? {
    if (this.isNullOrBlank()) return null
    return TvType.entries.firstOrNull { type ->
        type.name == this
    }
}
