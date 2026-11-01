package com.lagradost.cloudstream3.tv.presentation.screens.gallery

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Debug-only entry point to the component gallery. Settings > Updates ("Component gallery") and the
 * `tv_component_gallery` intent extra of `TvMainActivity` set [pending]; `TvApp` navigates and clears it.
 * Only wired when `BuildConfig.DEBUG`.
 */
object TvDebugGallery {
    const val INTENT_EXTRA = "tv_component_gallery"

    val pending = MutableStateFlow(false)

    fun open() {
        pending.value = true
    }
}
