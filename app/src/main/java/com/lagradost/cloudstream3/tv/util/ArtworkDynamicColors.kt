package com.lagradost.cloudstream3.tv.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeTonalSpot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ArtworkSeedColorCacheMaxEntries = 64
private const val ArtworkDynamicColorSampleSizePx = 96
private const val ArtworkDynamicColorQuantizedColorCount = 64
private const val ArtworkDynamicColorSeedCandidates = 4
private const val ArtworkDynamicColorContrastLevel = 0.0

@Immutable
internal data class ArtworkColorRoles(
    val isDarkScheme: Boolean,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val inversePrimary: Color,
)

private object ArtworkSeedColorCache {
    private val cache = LruCache<String, Int>(ArtworkSeedColorCacheMaxEntries)

    @Synchronized
    fun get(key: String): Int? = cache.get(key)

    @Synchronized
    fun put(key: String, value: Int) {
        cache.put(key, value)
    }
}

@Composable
internal fun rememberArtworkColorRoles(
    artworkUrl: String?,
    isDarkScheme: Boolean,
): ArtworkColorRoles? {
    val context = LocalContext.current
    val normalizedArtworkUrl = remember(artworkUrl) {
        artworkUrl?.trim()?.takeIf { url -> url.isNotBlank() }
    }
    var resolvedSeedArgb by remember(normalizedArtworkUrl) {
        mutableStateOf(normalizedArtworkUrl?.let(ArtworkSeedColorCache::get))
    }

    LaunchedEffect(normalizedArtworkUrl, resolvedSeedArgb) {
        if (resolvedSeedArgb != null || normalizedArtworkUrl == null) {
            return@LaunchedEffect
        }

        val loadedSeedArgb = resolveArtworkSeedArgb(context, normalizedArtworkUrl)
        if (loadedSeedArgb != null) {
            ArtworkSeedColorCache.put(normalizedArtworkUrl, loadedSeedArgb)
            resolvedSeedArgb = loadedSeedArgb
        }
    }

    return remember(resolvedSeedArgb, isDarkScheme) {
        resolvedSeedArgb?.toArtworkColorRoles(isDarkScheme = isDarkScheme)
    }
}

internal suspend fun warmArtworkSeedColor(
    context: Context,
    artworkUrl: String?,
) {
    val normalizedArtworkUrl = artworkUrl?.trim()?.takeIf { url -> url.isNotBlank() } ?: return
    if (ArtworkSeedColorCache.get(normalizedArtworkUrl) != null) {
        return
    }

    resolveArtworkSeedArgb(context, normalizedArtworkUrl)?.let { seedArgb ->
        ArtworkSeedColorCache.put(normalizedArtworkUrl, seedArgb)
    }
}

internal fun ArtworkColorRoles.toTvColorScheme(): ColorScheme {
    return if (isDarkScheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            scrim = scrim,
            inversePrimary = inversePrimary,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            scrim = scrim,
            inversePrimary = inversePrimary,
        )
    }
}

private suspend fun resolveArtworkSeedArgb(
    context: Context,
    artworkUrl: String,
): Int? {
    return withContext(Dispatchers.IO) {
        val drawable = loadArtworkDrawableOrNull(
            context = context,
            artworkUrl = artworkUrl,
        ) ?: return@withContext null

        tvTraceSection("artwork_seed_extract") {
            drawable.toArtworkSeedArgbOrNull()
        }
    }
}

private suspend fun loadArtworkDrawableOrNull(
    context: Context,
    artworkUrl: String,
): Drawable? {
    val request = ImageRequest.Builder(context)
        .data(artworkUrl)
        .allowHardware(false)
        .build()
    val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult
        ?: return null
    return result.image.asDrawable(context.resources)
}

private suspend fun Drawable.toArtworkSeedArgbOrNull(): Int? {
    val sampledBitmap = toArtworkThemeSampleOrNull(ArtworkDynamicColorSampleSizePx) ?: return null
    return sampledBitmap.resolveArtworkSeedArgbOrNull()
}

private fun Bitmap.resolveArtworkSeedArgbOrNull(): Int? {
    if (width <= 0 || height <= 0 || isRecycled) {
        return null
    }

    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)

    val colorToCount = QuantizerCelebi.quantize(
        pixels,
        ArtworkDynamicColorQuantizedColorCount,
    )
    if (colorToCount.isEmpty()) {
        return null
    }

    val rankedColors = Score.score(
        colorToCount,
        ArtworkDynamicColorSeedCandidates,
        null,
        true,
    )

    return rankedColors.firstOrNull()
        ?: colorToCount.entries.maxByOrNull { entry -> entry.value }?.key
}

private fun Int.toArtworkColorRoles(
    isDarkScheme: Boolean,
): ArtworkColorRoles {
    val scheme = SchemeTonalSpot(
        sourceColorHct = Hct.fromInt(this),
        isDark = isDarkScheme,
        contrastLevel = ArtworkDynamicColorContrastLevel,
        specVersion = ColorSpec.SpecVersion.SPEC_2021,
        platform = DynamicScheme.Platform.PHONE,
    )

    return ArtworkColorRoles(
        isDarkScheme = isDarkScheme,
        primary = Color(scheme.primary),
        onPrimary = Color(scheme.onPrimary),
        primaryContainer = Color(scheme.primaryContainer),
        onPrimaryContainer = Color(scheme.onPrimaryContainer),
        secondary = Color(scheme.secondary),
        onSecondary = Color(scheme.onSecondary),
        secondaryContainer = Color(scheme.secondaryContainer),
        onSecondaryContainer = Color(scheme.onSecondaryContainer),
        tertiary = Color(scheme.tertiary),
        onTertiary = Color(scheme.onTertiary),
        tertiaryContainer = Color(scheme.tertiaryContainer),
        onTertiaryContainer = Color(scheme.onTertiaryContainer),
        background = Color(scheme.background),
        onBackground = Color(scheme.onBackground),
        surface = Color(scheme.surface),
        onSurface = Color(scheme.onSurface),
        surfaceVariant = Color(scheme.surfaceVariant),
        onSurfaceVariant = Color(scheme.onSurfaceVariant),
        outline = Color(scheme.outline),
        outlineVariant = Color(scheme.outlineVariant),
        scrim = Color(scheme.scrim),
        inversePrimary = Color(scheme.inversePrimary),
    )
}

private suspend fun Drawable.toArtworkThemeSampleOrNull(
    sampleSizePx: Int,
): Bitmap? {
    if (sampleSizePx <= 0) {
        return null
    }

    if (this is BitmapDrawable) {
        val sourceBitmap = bitmap ?: return null
        if (sourceBitmap.width <= 0 || sourceBitmap.height <= 0) {
            return null
        }
        val softwareBitmap = sourceBitmap.toArgb8888BitmapOrNull() ?: return null
        return runCatching {
            if (softwareBitmap.width == sampleSizePx && softwareBitmap.height == sampleSizePx) {
                softwareBitmap
            } else {
                Bitmap.createScaledBitmap(
                    softwareBitmap,
                    sampleSizePx,
                    sampleSizePx,
                    true,
                ).toArgb8888BitmapOrNull()
            }
        }.getOrNull()
    }

    val sampledBitmap = withContext(Dispatchers.Main.immediate) {
        runCatching {
            toBitmap(
                width = sampleSizePx,
                height = sampleSizePx,
                config = Bitmap.Config.ARGB_8888,
            )
        }.getOrNull()
    }
    return sampledBitmap?.toArgb8888BitmapOrNull()
}

private fun Bitmap.toArgb8888BitmapOrNull(): Bitmap? {
    if (isRecycled) {
        return null
    }
    if (config == Bitmap.Config.ARGB_8888) {
        return this
    }
    return runCatching {
        copy(Bitmap.Config.ARGB_8888, false)
    }.getOrNull()
}
