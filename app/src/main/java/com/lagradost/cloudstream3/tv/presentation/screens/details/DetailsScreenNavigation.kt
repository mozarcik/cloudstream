package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle

object DetailsScreenNavigation {
    const val UrlBundleKey = "url"
    const val ApiNameBundleKey = "apiName"
    const val LoadingTitleBundleKey = "detailsLoadingTitle"
    const val LoadingPosterBundleKey = "detailsLoadingPoster"
    const val LoadingBackdropBundleKey = "detailsLoadingBackdrop"
    const val LoadingDescriptionBundleKey = "detailsLoadingDescription"
    const val LoadingYearBundleKey = "detailsLoadingYear"
    const val LoadingTypeBundleKey = "detailsLoadingType"
    const val LoadingProviderBundleKey = "detailsLoadingProvider"
    const val ResolveTitleBundleKey = "detailsResolveTitle"
    const val ResolveTmdbIdBundleKey = "detailsResolveTmdbId"
    const val ResolvePreferredApiBundleKey = "detailsResolvePreferredApi"
    const val ResolveTypeBundleKey = "detailsResolveType"
}

@Immutable
data class DetailsLoadingState(
    val title: String? = null,
    val posterUri: String? = null,
    val backdropUri: String? = null,
    val description: String? = null,
    val year: Int? = null,
    val typeName: String? = null,
    val providerName: String? = null,
)

internal fun SavedStateHandle.saveDetailsLoadingState(
    loadingState: DetailsLoadingState,
) {
    set(DetailsScreenNavigation.LoadingTitleBundleKey, loadingState.title)
    set(DetailsScreenNavigation.LoadingPosterBundleKey, loadingState.posterUri)
    set(DetailsScreenNavigation.LoadingBackdropBundleKey, loadingState.backdropUri)
    set(DetailsScreenNavigation.LoadingDescriptionBundleKey, loadingState.description)
    set(DetailsScreenNavigation.LoadingYearBundleKey, loadingState.year)
    set(DetailsScreenNavigation.LoadingTypeBundleKey, loadingState.typeName)
    set(DetailsScreenNavigation.LoadingProviderBundleKey, loadingState.providerName)
}

internal fun SavedStateHandle.consumeDetailsLoadingState(): DetailsLoadingState {
    return DetailsLoadingState(
        title = remove<String>(DetailsScreenNavigation.LoadingTitleBundleKey),
        posterUri = remove<String>(DetailsScreenNavigation.LoadingPosterBundleKey),
        backdropUri = remove<String>(DetailsScreenNavigation.LoadingBackdropBundleKey),
        description = remove<String>(DetailsScreenNavigation.LoadingDescriptionBundleKey),
        year = remove<Int>(DetailsScreenNavigation.LoadingYearBundleKey),
        typeName = remove<String>(DetailsScreenNavigation.LoadingTypeBundleKey),
        providerName = remove<String>(DetailsScreenNavigation.LoadingProviderBundleKey),
    )
}

internal fun SavedStateHandle.saveDetailsRouteSource(
    source: DetailsRouteSource?,
) {
    set(DetailsScreenNavigation.UrlBundleKey, source?.url)
    set(DetailsScreenNavigation.ApiNameBundleKey, source?.apiName)
}

internal fun SavedStateHandle.consumeDetailsRouteSource(): DetailsRouteSource? {
    val url = remove<String>(DetailsScreenNavigation.UrlBundleKey)
        ?.takeIf { it.isNotBlank() }
        ?: return null
    val apiName = remove<String>(DetailsScreenNavigation.ApiNameBundleKey)
        ?.takeIf { it.isNotBlank() }
        ?: return null
    return DetailsRouteSource(url = url, apiName = apiName)
}

internal fun SavedStateHandle.saveDetailsTmdbResolveRequest(
    request: DetailsTmdbResolveRequest?,
) {
    set(DetailsScreenNavigation.ResolveTitleBundleKey, request?.title)
    set(DetailsScreenNavigation.ResolveTmdbIdBundleKey, request?.tmdbId)
    set(DetailsScreenNavigation.ResolvePreferredApiBundleKey, request?.preferredApiName)
    set(DetailsScreenNavigation.ResolveTypeBundleKey, request?.expectedType?.name)
}

internal fun SavedStateHandle.consumeDetailsTmdbResolveRequest(): DetailsTmdbResolveRequest? {
    val title = remove<String>(DetailsScreenNavigation.ResolveTitleBundleKey)
        ?.takeIf { it.isNotBlank() }
        ?: return null
    val tmdbId = remove<Int>(DetailsScreenNavigation.ResolveTmdbIdBundleKey)
        ?.takeIf { it > 0 }
        ?: return null
    return DetailsTmdbResolveRequest(
        title = title,
        tmdbId = tmdbId,
        preferredApiName = remove<String>(DetailsScreenNavigation.ResolvePreferredApiBundleKey)
            ?.takeIf { it.isNotBlank() },
        expectedType = remove<String>(DetailsScreenNavigation.ResolveTypeBundleKey)
            .toTvTypeOrNull(),
    )
}

internal fun createDetailsSavedStateHandle(
    url: String?,
    apiName: String?,
    loadingState: DetailsLoadingState,
    tmdbResolveRequest: DetailsTmdbResolveRequest? = null,
): SavedStateHandle {
    return SavedStateHandle().apply {
        saveDetailsRouteSource(
            source = url?.takeIf { it.isNotBlank() }?.let { nonBlankUrl ->
                val nonBlankApiName = apiName?.takeIf { it.isNotBlank() } ?: return@let null
                DetailsRouteSource(
                    url = nonBlankUrl,
                    apiName = nonBlankApiName,
                )
            }
        )
        saveDetailsLoadingState(loadingState)
        saveDetailsTmdbResolveRequest(tmdbResolveRequest)
    }
}

internal fun String?.toTvTypeOrNull(): com.lagradost.cloudstream3.TvType? {
    if (this.isNullOrBlank()) return null
    return com.lagradost.cloudstream3.TvType.entries.firstOrNull { type ->
        type.name == this
    }
}
