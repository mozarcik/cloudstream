package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import android.widget.ProgressBar
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerMetadata
import com.lagradost.cloudstream3.utils.ExtractorLink

@Composable
internal fun PlaybackControlsLayer(
    visible: Boolean,
    controlsEnabled: Boolean,
    metadata: TvPlayerMetadata,
    link: ExtractorLink,
    isPlaying: Boolean,
    showAudioTracksButton: Boolean,
    showVideoTracksButton: Boolean,
    showSyncButton: Boolean,
    showNextEpisodeButton: Boolean,
    playPauseFocusRequester: androidx.compose.ui.focus.FocusRequester,
    timelineFocusRequester: androidx.compose.ui.focus.FocusRequester,
    exoPlayer: ExoPlayer,
    onPlaybackProgress: (Long, Long) -> Unit,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
) {
    val overlayGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.15f),
                Color.Black.copy(alpha = 0.55f),
                Color.Black.copy(alpha = 0.82f),
            )
        )
    }

    val overlayAlpha = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "player_overlay_alpha",
    ).value
    val overlayOffsetY = animateFloatAsState(
        targetValue = if (visible) 0f else 56f,
        animationSpec = tween(durationMillis = 180),
        label = "player_overlay_offset_y",
    ).value
    val overlayOffsetPx = with(LocalDensity.current) {
        overlayOffsetY.dp.toPx()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = overlayAlpha
                translationY = overlayOffsetPx
            }
            .background(overlayGradient)
    ) {
        PlayerOverlay(
            metadata = metadata,
            link = link,
            isPlaying = isPlaying,
            controlsEnabled = controlsEnabled && visible,
            showAudioTracksButton = showAudioTracksButton,
            showVideoTracksButton = showVideoTracksButton,
            showSyncButton = showSyncButton,
            showNextEpisodeButton = showNextEpisodeButton,
            playPauseFocusRequester = playPauseFocusRequester,
            timelineFocusRequester = timelineFocusRequester,
            exoPlayer = exoPlayer,
            onPlaybackProgress = onPlaybackProgress,
            onControlsEvent = onControlsEvent,
        )
    }
}

@Composable
internal fun BufferingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AndroidView(
                factory = { progressContext ->
                    ProgressBar(progressContext).apply {
                        isIndeterminate = true
                    }
                },
                modifier = Modifier.size(56.dp),
            )

            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
