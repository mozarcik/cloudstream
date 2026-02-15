package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.widget.ProgressBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DotSeparatedRow
import com.lagradost.cloudstream3.utils.ExtractorLink

internal sealed interface TvPlayerControlsEvent {
    data object PlayPause : TvPlayerControlsEvent
    data object OpenSources : TvPlayerControlsEvent
    data object OpenSubtitles : TvPlayerControlsEvent
    data object OpenTracks : TvPlayerControlsEvent
    data object SyncSubtitles : TvPlayerControlsEvent
    data object ToggleResizeMode : TvPlayerControlsEvent
    data object Restart : TvPlayerControlsEvent
    data object NextEpisode : TvPlayerControlsEvent
    data object SeekBackward : TvPlayerControlsEvent
    data object SeekForward : TvPlayerControlsEvent
}

@Composable
internal fun PlaybackControlsLayer(
    visible: Boolean,
    metadata: TvPlayerMetadata,
    link: ExtractorLink,
    isPlaying: Boolean,
    showTracksButton: Boolean,
    showNextEpisodeButton: Boolean,
    playPauseFocusRequester: FocusRequester,
    timelineFocusRequester: FocusRequester,
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

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 170)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 220),
                initialOffsetY = { fullHeight -> fullHeight / 3 }
            ),
        exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
            slideOutVertically(
                animationSpec = tween(durationMillis = 180),
                targetOffsetY = { fullHeight -> fullHeight / 3 }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayGradient)
        ) {
            PlayerOverlay(
                metadata = metadata,
                link = link,
                isPlaying = isPlaying,
                showTracksButton = showTracksButton,
                showNextEpisodeButton = showNextEpisodeButton,
                playPauseFocusRequester = playPauseFocusRequester,
                timelineFocusRequester = timelineFocusRequester,
                exoPlayer = exoPlayer,
                onPlaybackProgress = onPlaybackProgress,
                onControlsEvent = onControlsEvent,
            )
        }
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

@Composable
private fun PlayerOverlay(
    metadata: TvPlayerMetadata,
    link: ExtractorLink,
    isPlaying: Boolean,
    showTracksButton: Boolean,
    showNextEpisodeButton: Boolean,
    playPauseFocusRequester: FocusRequester,
    timelineFocusRequester: FocusRequester,
    exoPlayer: ExoPlayer,
    onPlaybackProgress: (Long, Long) -> Unit,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
) {
    val episodeLabel = when {
        metadata.season != null && metadata.episode != null -> {
            "S${metadata.season}:E${metadata.episode}"
        }
        metadata.episode != null -> {
            "E${metadata.episode}"
        }
        else -> null
    }
    val episodeTitle = metadata.episodeTitle
        ?.takeIf { it.isNotBlank() && !it.equals(metadata.title, ignoreCase = true) }
    val episodeTexts = if (metadata.isEpisodeBased) {
        listOfNotNull(
            episodeLabel,
            episodeTitle,
        )
    } else {
        emptyList()
    }
    val infoTexts = listOfNotNull(
        metadata.year?.toString(),
        metadata.apiName.takeIf { it.isNotBlank() },
        qualityLabel(link.quality).takeIf { it.isNotBlank() },
        link.source.takeIf { it.isNotBlank() },
        link.type.name.takeIf { it.isNotBlank() },
        link.name.takeIf { it.isNotBlank() },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (metadata.title.isNotBlank()) {
            Text(
                text = metadata.title,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (episodeTexts.isNotEmpty()) {
            DotSeparatedRow(
                texts = episodeTexts,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                ),
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        if (infoTexts.isNotEmpty()) {
            DotSeparatedRow(
                texts = infoTexts,
                textStyle = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                ),
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PlaybackTimelineSection(
            exoPlayer = exoPlayer,
            timelineFocusRequester = timelineFocusRequester,
            onPlaybackProgress = onPlaybackProgress,
            onControlsEvent = onControlsEvent,
        )

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            PlayerPrimaryControlButton(
                icon = if (isPlaying) {
                    Icons.Default.Pause
                } else {
                    Icons.Default.PlayArrow
                },
                onClick = { onControlsEvent(TvPlayerControlsEvent.PlayPause) },
                modifier = Modifier
                    .align(Alignment.Center)
                    .focusRequester(playPauseFocusRequester),
                buttonSize = 64.dp,
                iconSize = 28.dp,
            )

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerSecondaryControlButton(
                    icon = Icons.Default.Source,
                    onClick = { onControlsEvent(TvPlayerControlsEvent.OpenSources) },
                )
                PlayerSecondaryControlButton(
                    icon = Icons.Default.Subtitles,
                    onClick = { onControlsEvent(TvPlayerControlsEvent.OpenSubtitles) },
                )
                if (showTracksButton) {
                    PlayerSecondaryControlButton(
                        icon = Icons.Default.Audiotrack,
                        onClick = { onControlsEvent(TvPlayerControlsEvent.OpenTracks) },
                    )
                }
                PlayerSecondaryControlButton(
                    icon = Icons.Default.Sync,
                    onClick = { onControlsEvent(TvPlayerControlsEvent.SyncSubtitles) },
                )
                PlayerSecondaryControlButton(
                    icon = Icons.Default.AspectRatio,
                    onClick = { onControlsEvent(TvPlayerControlsEvent.ToggleResizeMode) },
                )
                PlayerSecondaryControlButton(
                    icon = Icons.Default.Replay,
                    onClick = { onControlsEvent(TvPlayerControlsEvent.Restart) },
                )
                if (showNextEpisodeButton) {
                    PlayerSecondaryControlButton(
                        icon = Icons.Default.SkipNext,
                        onClick = { onControlsEvent(TvPlayerControlsEvent.NextEpisode) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackTimelineSection(
    exoPlayer: ExoPlayer,
    timelineFocusRequester: FocusRequester,
    onPlaybackProgress: (Long, Long) -> Unit,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
) {
    val timelineUiState = rememberPlaybackTimelineUiState(
        player = exoPlayer,
        onPlaybackProgress = onPlaybackProgress,
        updateIntervalMs = 500L,
    )
    val timelineState = timelineUiState.value
    val progressFraction by remember(timelineState.positionMs, timelineState.durationMs) {
        derivedStateOf {
            if (timelineState.durationMs > 0L) {
                (timelineState.positionMs.toFloat() / timelineState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatPlaybackTime(timelineState.positionMs),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.width(14.dp))
        PlaybackTimeline(
            progressFraction = progressFraction,
            focusRequester = timelineFocusRequester,
            onControlsEvent = onControlsEvent,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = formatPlaybackTime(timelineState.durationMs),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun PlayerPrimaryControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 48.dp,
    iconSize: Dp = 20.dp,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White,
            contentColor = Color.Black,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun PlayerSecondaryControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(42.dp),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.2f),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(width = 2.dp, color = Color.White),
                shape = CircleShape,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun PlaybackTimeline(
    progressFraction: Float,
    focusRequester: FocusRequester,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeProgress = progressFraction.coerceIn(0f, 1f)
    val isFocused = remember { androidx.compose.runtime.mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(24.dp)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onControlsEvent(TvPlayerControlsEvent.SeekBackward)
                        true
                    }

                    Key.DirectionRight -> {
                        onControlsEvent(TvPlayerControlsEvent.SeekForward)
                        true
                    }

                    else -> false
                }
            }
            .onFocusChanged { focusState ->
                isFocused.value = focusState.isFocused
            }
            .focusable()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (isFocused.value) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.25f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(safeProgress)
                .height(8.dp)
                .background(Color.White)
        )
    }
}

private fun qualityLabel(quality: Int): String {
    return com.lagradost.cloudstream3.utils.Qualities.getStringByInt(quality)
}

private fun formatPlaybackTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
