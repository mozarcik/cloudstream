package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.ui.focus.FocusRequester
import com.lagradost.cloudstream3.R
import kotlinx.coroutines.delay

private const val FocusRequestRetryCount = 20
private const val FocusRequestRetryDelayMs = 16L

internal enum class PlayerResizeMode(
    val resizeMode: Int,
    val labelResId: Int,
) {
    Fit(
        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
        labelResId = R.string.resize_fit,
    ),
    Fill(
        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL,
        labelResId = R.string.resize_fill,
    ),
    Zoom(
        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        labelResId = R.string.resize_zoom,
    ),
    ;

    fun next(): PlayerResizeMode {
        return when (this) {
            Fit -> Fill
            Fill -> Zoom
            Zoom -> Fit
        }
    }
}

internal suspend fun requestFocusWithRetry(
    focusRequester: FocusRequester,
): Boolean {
    repeat(FocusRequestRetryCount) {
        if (focusRequester.requestFocus()) {
            return true
        }
        delay(FocusRequestRetryDelayMs)
    }
    return false
}
