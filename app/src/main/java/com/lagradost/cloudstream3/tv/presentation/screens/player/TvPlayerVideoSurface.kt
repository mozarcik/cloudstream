package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.lagradost.cloudstream3.ui.player.CustomDecoder
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesFragment.Companion.setSubtitleViewStyle
import com.lagradost.cloudstream3.utils.UIHelper.toPx

private fun PlayerView.applyLegacySubtitleStyle() {
    val style = CustomDecoder.style
    subtitleView?.let { view ->
        view.translationY = -style.elevation.toPx.toFloat()
        setSubtitleViewStyle(
            view = view,
            data = style,
            applyElevation = true,
        )
    }
}

@Composable
internal fun TvPlayerVideoSurface(
    player: ExoPlayer,
    resizeMode: Int,
    subtitleSyncController: TvPlayerSubtitleSyncController,
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
                subtitleSyncController.attachSubtitleView(subtitleView)
                applyLegacySubtitleStyle()
            }
        },
        update = { view ->
            if (view.player !== player) {
                view.player = player
            }
            if (view.resizeMode != resizeMode) {
                view.resizeMode = resizeMode
            }
            subtitleSyncController.attachSubtitleView(view.subtitleView)
            // WHY: PlayerView potrafi nadpisać styl po podpięciu nowego playera/tracków.
            // Reaplikujemy styl legacy, aby zawsze respektować zapisane ustawienia napisów.
            view.applyLegacySubtitleStyle()
        },
    )
}
