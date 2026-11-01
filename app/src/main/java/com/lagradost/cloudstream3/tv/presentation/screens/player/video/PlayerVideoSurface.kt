package com.lagradost.cloudstream3.tv.presentation.screens.player.video

import android.view.View
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
import androidx.media3.ui.SubtitleView
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

private fun applyLegacySubtitleStyle(
    subtitleView: SubtitleView?,
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

private fun PlayerView.hideInternalSubtitleView() {
    subtitleView?.visibility = View.GONE
}

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
                hideInternalSubtitleView()
            }
        },
        update = { view ->
            if (view.player !== player) {
                view.player = player
            }
            if (view.resizeMode != resizeMode) {
                view.resizeMode = resizeMode
            }
            view.hideInternalSubtitleView()
        },
    )
}

@Composable
internal fun TvPlayerSubtitleLayer(
    player: ExoPlayer,
    subtitleSyncController: TvPlayerSubtitleSyncController,
    controlsVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val subtitleControlsVisibleOffsetPx = with(LocalDensity.current) {
        PlayerControlsTokens.SubtitleControlsVisibleOffset.toPx()
    }
    val currentControlsVisible = rememberUpdatedState(controlsVisible)
    val currentSubtitleControlsVisibleOffsetPx = rememberUpdatedState(subtitleControlsVisibleOffsetPx)
    var subtitleView by remember { mutableStateOf<SubtitleView?>(null) }

    DisposableEffect(player, subtitleSyncController, subtitleView) {
        val resolvedSubtitleView = subtitleView
        if (resolvedSubtitleView == null) {
            onDispose { }
        } else {
            subtitleSyncController.attachSubtitleView(resolvedSubtitleView)
            applyLegacySubtitleStyle(
                subtitleView = resolvedSubtitleView,
                controlsVisible = currentControlsVisible.value,
                controlsVisibleOffsetPx = currentSubtitleControlsVisibleOffsetPx.value,
            )
            val listener = object : Player.Listener {
                override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                    applyLegacySubtitleStyle(
                        subtitleView = resolvedSubtitleView,
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
            SubtitleView(viewContext).apply {
                applyLegacySubtitleStyle(
                    subtitleView = this,
                    controlsVisible = controlsVisible,
                    controlsVisibleOffsetPx = subtitleControlsVisibleOffsetPx,
                )
                subtitleView = this
            }
        },
        update = { view ->
            applyLegacySubtitleStyle(
                subtitleView = view,
                controlsVisible = controlsVisible,
                controlsVisibleOffsetPx = subtitleControlsVisibleOffsetPx,
            )
            if (subtitleView !== view) {
                subtitleView = view
            }
        },
    )
}
