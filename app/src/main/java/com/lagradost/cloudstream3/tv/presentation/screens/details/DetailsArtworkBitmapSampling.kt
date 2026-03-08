package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun Drawable.toDetailsThemeSampleOrNull(
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
