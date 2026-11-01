package com.lagradost.cloudstream3.tv.presentation.common

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val POSTER_PALETTE_CACHE_MAX_ENTRIES = 320
private const val POSTER_PALETTE_SAMPLE_SIZE_PX = 64
private const val POSTER_PALETTE_PARALLELISM = 2
private const val POSTER_PALETTE_MAX_PENDING_TASKS = 24
private const val POSTER_PALETTE_MIN_ALPHA = 24
private const val POSTER_PALETTE_PIXEL_STRIDE = 2
private const val POSTER_PALETTE_TARGET_LIGHTNESS = 0.50f
private const val POSTER_PALETTE_LIGHTNESS_MIN = 0.38f
private const val POSTER_PALETTE_LIGHTNESS_MAX = 0.56f
private const val POSTER_PALETTE_SATURATION_MIN = 0.45f
private const val POSTER_PALETTE_HUE_BUCKETS = 24
private const val POSTER_PALETTE_BASE_SATURATION_WEIGHT = 0.08f
private const val POSTER_PALETTE_MIN_LIGHTNESS_SCORE = 0.25f

@Stable
class PaletteCache(
    private val maxEntries: Int = POSTER_PALETTE_CACHE_MAX_ENTRIES,
    private val parallelism: Int = POSTER_PALETTE_PARALLELISM,
    private val maxPendingTasks: Int = POSTER_PALETTE_MAX_PENDING_TASKS,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val lruCache = LruCache<String, Int>(maxEntries)
    private val memoryMap = LinkedHashMap<String, Color>(maxEntries, 0.75f, true)
    private val queuedKeys = mutableSetOf<String>()
    private val inFlightKeys = mutableSetOf<String>()
    private val listenersByKey = mutableMapOf<String, MutableList<(Color) -> Unit>>()
    private val tasks = Channel<PaletteTask>(capacity = maxPendingTasks)

    init {
        repeat(parallelism.coerceAtLeast(1)) {
            scope.launch {
                processTasks()
            }
        }
    }

    @Synchronized
    fun get(contentKey: String): Color? {
        memoryMap[contentKey]?.let { cached ->
            return cached
        }

        val lruColor = lruCache.get(contentKey)?.let { Color(it) }
        if (lruColor != null) {
            memoryMap[contentKey] = lruColor
            trimMemoryCacheLocked()
        }
        return lruColor
    }

    fun resolveFromDrawableAsync(
        contentKey: String,
        drawable: Drawable?,
        fallbackColor: Color,
        highPriority: Boolean = false,
        onResolved: (Color) -> Unit,
    ) {
        get(contentKey)?.let { cached ->
            onResolved(cached)
            return
        }

        if (drawable == null) {
            onResolved(fallbackColor)
            return
        }

        val shouldEnqueueTask = synchronized(this) {
            listenersByKey.getOrPut(contentKey) { mutableListOf() }.add(onResolved)
            if (inFlightKeys.contains(contentKey) || queuedKeys.contains(contentKey)) {
                false
            } else {
                queuedKeys.add(contentKey)
                true
            }
        }
        if (!shouldEnqueueTask) {
            return
        }

        val enqueueResult = tasks.trySend(
            PaletteTask(
                contentKey = contentKey,
                drawable = drawable,
                fallbackColor = fallbackColor,
                highPriority = highPriority
            )
        )
        if (enqueueResult.isSuccess) {
            return
        }

        if (highPriority) {
            // WHY: dla sfocusowanego elementu nie chcemy zostawać na fallback.
            scope.launch {
                tasks.send(
                    PaletteTask(
                        contentKey = contentKey,
                        drawable = drawable,
                        fallbackColor = fallbackColor,
                        highPriority = true
                    )
                )
            }
            return
        }

        // WHY: przy szybkim scrollu ograniczamy backlog zadań niskiego priorytetu.
        val listeners = synchronized(this) {
            queuedKeys.remove(contentKey)
            listenersByKey.remove(contentKey).orEmpty()
        }
        dispatchResolvedColor(listeners = listeners, color = fallbackColor)
    }

    @Synchronized
    private fun put(contentKey: String, color: Color) {
        memoryMap[contentKey] = color
        trimMemoryCacheLocked()
        lruCache.put(contentKey, color.toArgb())
    }

    @Synchronized
    private fun trimMemoryCacheLocked() {
        while (memoryMap.size > maxEntries) {
            val iterator = memoryMap.entries.iterator()
            if (!iterator.hasNext()) {
                return
            }
            iterator.next()
            iterator.remove()
        }
    }

    private suspend fun processTasks() {
        for (task in tasks) {
            synchronized(this) {
                queuedKeys.remove(task.contentKey)
                inFlightKeys.add(task.contentKey)
            }

            val resolvedColorOrNull = runCatching {
                val sampledBitmap = task.drawable.toPaletteSampleOrNull(POSTER_PALETTE_SAMPLE_SIZE_PX)
                sampledBitmap?.let { bitmap ->
                    resolveAccentColorFromBitmap(bitmap = bitmap)
                }
            }.getOrNull()
            val resolvedColor = resolvedColorOrNull ?: task.fallbackColor

            if (resolvedColorOrNull != null) {
                put(task.contentKey, resolvedColorOrNull)
            }

            val listeners = synchronized(this) {
                inFlightKeys.remove(task.contentKey)
                listenersByKey.remove(task.contentKey).orEmpty()
            }
            dispatchResolvedColor(listeners = listeners, color = resolvedColor)
        }
    }

    private fun dispatchResolvedColor(
        listeners: List<(Color) -> Unit>,
        color: Color,
    ) {
        if (listeners.isEmpty()) {
            return
        }
        scope.launch {
            withContext(Dispatchers.Main.immediate) {
                listeners.forEach { listener ->
                    listener(color)
                }
            }
        }
    }
}

private data class PaletteTask(
    val contentKey: String,
    val drawable: Drawable,
    val fallbackColor: Color,
    val highPriority: Boolean,
)

val DefaultPosterPaletteCache = PaletteCache()

private suspend fun Drawable.toPaletteSampleOrNull(sampleSizePx: Int): Bitmap? {
    if (sampleSizePx <= 0) {
        return null
    }

    if (this is BitmapDrawable) {
        val sourceBitmap = bitmap ?: return null
        if (sourceBitmap.width <= 0 || sourceBitmap.height <= 0) {
            return null
        }
        val softwareSource = sourceBitmap.toSoftwareBitmapOrNull() ?: return null
        val sampled = runCatching {
            if (softwareSource.width == sampleSizePx && softwareSource.height == sampleSizePx) {
                softwareSource
            } else {
                Bitmap.createScaledBitmap(
                    softwareSource,
                    sampleSizePx,
                    sampleSizePx,
                    true
                )
            }
        }.getOrNull()?.toSoftwareBitmapOrNull()
        return sampled
    }

    // WHY: część Drawable nie jest bezpieczna do renderowania poza Main thread.
    val sampled = withContext(Dispatchers.Main.immediate) {
        runCatching {
            toBitmap(
                width = sampleSizePx,
                height = sampleSizePx,
                config = Bitmap.Config.ARGB_8888
            )
        }.getOrNull()
    }
    return sampled
}

private fun resolveAccentColorFromBitmap(
    bitmap: Bitmap,
): Color? {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) {
        return null
    }

    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val bucketWeights = FloatArray(POSTER_PALETTE_HUE_BUCKETS)
    val bucketHueSum = FloatArray(POSTER_PALETTE_HUE_BUCKETS)
    val bucketSaturationSum = FloatArray(POSTER_PALETTE_HUE_BUCKETS)
    val bucketLightnessSum = FloatArray(POSTER_PALETTE_HUE_BUCKETS)
    val hsl = FloatArray(3)

    var redSum = 0L
    var greenSum = 0L
    var blueSum = 0L
    var sampleCount = 0

    var bestBucketIndex = 0
    var bestBucketWeight = 0f

    var pixelIndex = 0
    while (pixelIndex < pixels.size) {
        val argb = pixels[pixelIndex]
        pixelIndex += POSTER_PALETTE_PIXEL_STRIDE

        if (AndroidColor.alpha(argb) < POSTER_PALETTE_MIN_ALPHA) {
            continue
        }

        redSum += AndroidColor.red(argb).toLong()
        greenSum += AndroidColor.green(argb).toLong()
        blueSum += AndroidColor.blue(argb).toLong()
        sampleCount += 1

        ColorUtils.colorToHSL(argb, hsl)
        val saturation = hsl[1]
        val lightness = hsl[2]
        val lightnessScore = 1f - (
            abs(lightness - POSTER_PALETTE_TARGET_LIGHTNESS) / POSTER_PALETTE_TARGET_LIGHTNESS
            )
            .coerceIn(0f, 1f)
        val weight = saturation
            .coerceAtLeast(POSTER_PALETTE_BASE_SATURATION_WEIGHT) *
            lightnessScore.coerceAtLeast(POSTER_PALETTE_MIN_LIGHTNESS_SCORE)
        if (weight <= 0f) {
            continue
        }

        val bucketIndex = (
            (hsl[0] / 360f) * POSTER_PALETTE_HUE_BUCKETS
            ).toInt().coerceIn(0, POSTER_PALETTE_HUE_BUCKETS - 1)

        bucketWeights[bucketIndex] += weight
        bucketHueSum[bucketIndex] += hsl[0] * weight
        bucketSaturationSum[bucketIndex] += saturation * weight
        bucketLightnessSum[bucketIndex] += lightness * weight

        val bucketWeight = bucketWeights[bucketIndex]
        if (bucketWeight > bestBucketWeight) {
            bestBucketWeight = bucketWeight
            bestBucketIndex = bucketIndex
        }
    }

    if (sampleCount <= 0) {
        return null
    }

    val resolvedHsl = if (bestBucketWeight > 0f) {
        val inverseWeight = 1f / bestBucketWeight
        floatArrayOf(
            (bucketHueSum[bestBucketIndex] * inverseWeight).coerceIn(0f, 360f),
            (bucketSaturationSum[bestBucketIndex] * inverseWeight).coerceAtLeast(POSTER_PALETTE_SATURATION_MIN),
            (bucketLightnessSum[bestBucketIndex] * inverseWeight)
                .coerceIn(POSTER_PALETTE_LIGHTNESS_MIN, POSTER_PALETTE_LIGHTNESS_MAX)
        )
    } else {
        val averageColor = AndroidColor.rgb(
            (redSum / sampleCount).toInt().coerceIn(0, 255),
            (greenSum / sampleCount).toInt().coerceIn(0, 255),
            (blueSum / sampleCount).toInt().coerceIn(0, 255)
        )
        ColorUtils.colorToHSL(averageColor, hsl)
        floatArrayOf(
            hsl[0].coerceIn(0f, 360f),
            hsl[1].coerceAtLeast(POSTER_PALETTE_SATURATION_MIN),
            hsl[2].coerceIn(POSTER_PALETTE_LIGHTNESS_MIN, POSTER_PALETTE_LIGHTNESS_MAX)
        )
    }

    return Color(ColorUtils.HSLToColor(resolvedHsl))
}

private fun Bitmap.toSoftwareBitmapOrNull(): Bitmap? {
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
