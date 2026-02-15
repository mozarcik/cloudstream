package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
internal fun TvPlayerVideoSurface(
    player: ExoPlayer,
    resizeMode: Int,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = false
                setShutterBackgroundColor("#000000".toColorInt())
                setBackgroundColor("#000000".toColorInt())
                this.resizeMode = resizeMode
            }
        },
        update = { view ->
            if (view.player !== player) {
                view.player = player
            }
            if (view.resizeMode != resizeMode) {
                view.resizeMode = resizeMode
            }
        },
    )
}
