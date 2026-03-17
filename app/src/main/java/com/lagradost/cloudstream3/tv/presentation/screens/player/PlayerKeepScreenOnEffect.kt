package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getActivity

@Composable
internal fun PlayerKeepScreenOnEffect(keepScreenOn: Boolean) {
    val activity = LocalContext.current.getActivity()

    DisposableEffect(activity, keepScreenOn) {
        val window = activity?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

internal fun shouldKeepPlaybackScreenOn(
    isPlaying: Boolean,
    playerWantsToPlay: Boolean,
    playbackState: Int,
): Boolean {
    return isPlaying ||
        playerWantsToPlay ||
        playbackState == Player.STATE_BUFFERING
}
