package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.CloudStreamApp

internal fun playerString(
    resId: Int,
    fallback: String,
): String {
    return CloudStreamApp.context?.getString(resId) ?: fallback
}
