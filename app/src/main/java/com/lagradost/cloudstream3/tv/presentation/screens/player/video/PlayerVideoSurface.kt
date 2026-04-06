package com.lagradost.cloudstream3.tv.presentation.screens.player.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerSubtitleSyncController
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerControlsTokens
import com.lagradost.cloudstream3.ui.player.CustomDecoder
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesFragment.Companion.setSubtitleViewStyle
import com.lagradost.cloudstream3.utils.UIHelper.toPx

internal fun resolvedSubtitleTranslationY(
    baseTranslationYPx: Float,
    controlsVisibleOffsetPx: Float,
    controlsVisible: Boolean,
): Float {
    return if (controlsVisible) {
        baseTranslationYPx - controlsVisibleOffsetPx
    } else {
        baseTranslationYPx
    }
}

private fun PlayerView.applyLegacySubtitleStyle(
    controlsVisible: Boolean,
    controlsVisibleOffsetPx: Float,
) {
    val style = CustomDecoder.style
    subtitleView?.let { view ->
        view.translationY = resolvedSubtitleTranslationY(
            baseTranslationYPx = -style.elevation.toPx.toFloat(),
            controlsVisibleOffsetPx = controlsVisibleOffsetPx,
            controlsVisible = controlsVisible,
        )
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
    controlsVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val subtitleControlsVisibleOffsetPx = with(LocalDensity.current) {
        PlayerControlsTokens.SubtitleControlsVisibleOffset.toPx()
    }
    val currentControlsVisible = rememberUpdatedState(controlsVisible)
    val currentSubtitleControlsVisibleOffsetPx = rememberUpdatedState(subtitleControlsVisibleOffsetPx)
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    DisposableEffect(player, subtitleSyncController, playerView) {
        val resolvedPlayerView = playerView
        if (resolvedPlayerView == null) {
            onDispose { }
        } else {
            subtitleSyncController.attachSubtitleView(resolvedPlayerView.subtitleView)
            resolvedPlayerView.applyLegacySubtitleStyle(
                controlsVisible = currentControlsVisible.value,
                controlsVisibleOffsetPx = currentSubtitleControlsVisibleOffsetPx.value,
            )
            val listener = object : Player.Listener {
                override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                    resolvedPlayerView.applyLegacySubtitleStyle(
                        controlsVisible = currentControlsVisible.value,
                        controlsVisibleOffsetPx = currentSubtitleControlsVisibleOffsetPx.value,
                    )
                }
            }
            player.addListener(listener)
            onDispose {
                player.removeListener(listener)
                subtitleSyncController.clearSubtitleView()
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = false
                setShutterBackgroundColor("#000000".toColorInt())
                setBackgroundColor("#000000".toColorInt())
                this.resizeMode = resizeMode
                applyLegacySubtitleStyle(
                    controlsVisible = controlsVisible,
                    controlsVisibleOffsetPx = subtitleControlsVisibleOffsetPx,
                )
                playerView = this
            }
        },
        update = { view ->
            if (view.player !== player) {
                view.player = player
            }
            if (view.resizeMode != resizeMode) {
                view.resizeMode = resizeMode
            }
            view.applyLegacySubtitleStyle(
                controlsVisible = controlsVisible,
                controlsVisibleOffsetPx = subtitleControlsVisibleOffsetPx,
            )
            if (playerView !== view) {
                playerView = view
            }
        },
    )
}
