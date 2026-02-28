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

internal fun createDetailsSavedStateHandle(
    url: String,
    apiName: String,
    loadingState: DetailsLoadingState,
): SavedStateHandle {
    return SavedStateHandle().apply {
        set(DetailsScreenNavigation.UrlBundleKey, url)
        set(DetailsScreenNavigation.ApiNameBundleKey, apiName)
        saveDetailsLoadingState(loadingState)
    }
}
