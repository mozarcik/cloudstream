package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerSpinner
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerMetadata
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.utils.ExtractorLink

@Composable
internal fun PlaybackControlsLayer(
    visible: Boolean,
    controlsEnabled: Boolean,
    panelCloseCount: Int,
    metadata: TvPlayerMetadata,
    link: ExtractorLink,
    isPlaying: Boolean,
    barConfig: PlayerBottomControlBarConfig,
    seekStepMs: Long,
    playPauseFocusRequester: FocusRequester,
    timelineFocusRequester: FocusRequester,
    exoPlayer: ExoPlayer,
    onPlaybackProgress: (Long, Long) -> Unit,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
) {
    val overlayAlpha = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = TvDimens.FocusAnimationMs),
        label = "player_overlay_alpha",
    )

    Box(
        contentAlignment = Alignment.BottomStart,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value },
    ) {
        PlayerOverlay(
            metadata = metadata,
            link = link,
            isPlaying = isPlaying,
            controlsEnabled = controlsEnabled && visible,
            controlsShown = visible,
            panelCloseCount = panelCloseCount,
            barConfig = barConfig,
            seekStepMs = seekStepMs,
            playPauseFocusRequester = playPauseFocusRequester,
            timelineFocusRequester = timelineFocusRequester,
            exoPlayer = exoPlayer,
            onPlaybackProgress = onPlaybackProgress,
            onControlsEvent = onControlsEvent,
        )
    }
}

/** Buffering while playing (mockup PlayerErrors): only the centered spinner, the picture stays as it is. */
@Composable
internal fun BufferingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        PlayerSpinner(
            size = 24.dp,
            strokeWidth = 2.5.dp,
            color = TvColors.Text,
            trackColor = Color.White.copy(alpha = 0.25f),
        )
    }
}
