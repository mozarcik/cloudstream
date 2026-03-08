package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.tv.material3.ColorScheme
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import androidx.compose.ui.platform.LocalContext
import com.lagradost.cloudstream3.tv.util.tvTraceSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun rememberDetailsDynamicColorScheme(
    artworkKey: String,
    artworkUrl: String?,
    baseColorScheme: ColorScheme,
): ColorScheme {
    val context = LocalContext.current
    var resolvedPalette by remember(artworkKey) {
        mutableStateOf(DetailsArtworkPaletteCache.get(artworkKey))
    }
    LaunchedEffect(artworkKey, artworkUrl, resolvedPalette) {
        if (resolvedPalette != null || artworkUrl.isNullOrBlank()) {
            return@LaunchedEffect
        }

        val loadedPalette = withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(artworkUrl)
                .allowHardware(false)
                .build()
            val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult
                ?: return@withContext null
            val drawable = result.image.asDrawable(context.resources)
            tvTraceSection("details_palette_extract") {
                drawable.toDetailsArtworkPaletteOrNull(
                    fallbackPrimary = baseColorScheme.primary,
                    fallbackSecondary = baseColorScheme.secondary,
                    fallbackTertiary = baseColorScheme.tertiary,
                    fallbackSurface = baseColorScheme.surface,
                    fallbackBackground = baseColorScheme.background,
                )
            }
        }

        if (loadedPalette != null) {
            DetailsArtworkPaletteCache.put(artworkKey, loadedPalette)
            resolvedPalette = loadedPalette
        }
    }

    return remember(baseColorScheme, resolvedPalette) {
        resolvedPalette?.let { palette ->
            baseColorScheme.toDetailsDynamicColorScheme(palette)
        } ?: baseColorScheme
    }
}
