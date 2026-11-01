package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import androidx.tv.material3.ColorScheme
import com.lagradost.cloudstream3.tv.util.rememberArtworkColorRoles
import com.lagradost.cloudstream3.tv.util.toTvColorScheme

@Composable
internal fun rememberDetailsDynamicColorScheme(
    artworkUrl: String?,
    baseColorScheme: ColorScheme,
): ColorScheme {
    val artworkColorRoles = rememberArtworkColorRoles(
        artworkUrl = artworkUrl,
        isDarkScheme = baseColorScheme.background.luminance() <= 0.5f,
    )

    return remember(baseColorScheme, artworkColorRoles) {
        artworkColorRoles?.toTvColorScheme() ?: baseColorScheme
    }
}
