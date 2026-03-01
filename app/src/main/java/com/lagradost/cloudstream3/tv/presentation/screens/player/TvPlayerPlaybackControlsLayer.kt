package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.widget.ProgressBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DotSeparatedRow
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

internal sealed interface TvPlayerControlsEvent {
    data object PlayPause : TvPlayerControlsEvent
    data object OpenSources : TvPlayerControlsEvent
    data object OpenVideoTracks : TvPlayerControlsEvent
    data object OpenSubtitles : TvPlayerControlsEvent
    data object OpenTracks : TvPlayerControlsEvent
    data object SyncSubtitles : TvPlayerControlsEvent
    data object ToggleResizeMode : TvPlayerControlsEvent
    data object Restart : TvPlayerControlsEvent
    data object NextEpisode : TvPlayerControlsEvent
    data object SeekBackward : TvPlayerControlsEvent
    data object SeekForward : TvPlayerControlsEvent
}

private object PlayerControlsTokens {
    val OverlayHorizontalPadding = 48.dp
    val OverlayVerticalPadding = 20.dp
    val MetadataToTimelineSpacing = 16.dp
    val TimelineToControlsSpacing = 18.dp

    val PlayButtonSize = 64.dp
    val PlayIconSize = 28.dp
    const val PlayFocusScale = 1.05f

    val SecondaryButtonSize = 42.dp
    val SecondaryIconSize = 17.dp
    const val SecondaryFocusScale = 1.05f
    const val SecondaryContainerAlpha = 0.2f

    val ButtonsSpacing = 8.dp

    val TooltipShape = RoundedCornerShape(12.dp)
    val TooltipHorizontalPadding = 16.dp
    val TooltipVerticalPadding = 6.dp
    val TooltipTonalElevation = 5.dp
    const val TooltipAlpha = 0.95f
    val TooltipVerticalOffset = 16.dp
    val TooltipMaxWidth = 220.dp
    const val TooltipFadeInMs = 150
    const val TooltipFadeOutMs = 0

    val TimelineContainerHeight = 24.dp
    val TimelineInactiveTrackHeight = 6.dp
    val TimelineFocusedTrackHeight = 8.dp
    val TimelineTrackPadding = 8.dp
    const val TimelineFocusAnimationMs = 120
}

private data class PlayerControlTooltipState(
    val text: String,
    val anchorCenterXPx: Float,
    val anchorTopYPx: Float,
)

private enum class PlayerControlFocusTarget {
    Restart,
    Video,
    Sources,
    PlayPause,
    NextEpisode,
    Subtitles,
    Sync,
    Audio,
    AspectRatio,
}

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
                controlsEnabled = controlsEnabled,
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
    controlsEnabled: Boolean,
    showAudioTracksButton: Boolean,
    showVideoTracksButton: Boolean,
    showSyncButton: Boolean,
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
            .padding(
                horizontal = PlayerControlsTokens.OverlayHorizontalPadding,
                vertical = PlayerControlsTokens.OverlayVerticalPadding,
            ),
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

        Spacer(modifier = Modifier.height(PlayerControlsTokens.MetadataToTimelineSpacing))

        PlaybackTimelineSection(
            exoPlayer = exoPlayer,
            controlsEnabled = controlsEnabled,
            timelineFocusRequester = timelineFocusRequester,
            onPlaybackProgress = onPlaybackProgress,
            onControlsEvent = onControlsEvent,
        )

        Spacer(modifier = Modifier.height(PlayerControlsTokens.TimelineToControlsSpacing))

        PlayerBottomControlBar(
            isPlaying = isPlaying,
            controlsEnabled = controlsEnabled,
            showAudioTracksButton = showAudioTracksButton,
            showVideoTracksButton = showVideoTracksButton,
            showSyncButton = showSyncButton,
            showNextEpisodeButton = showNextEpisodeButton,
            playPauseFocusRequester = playPauseFocusRequester,
            onControlsEvent = onControlsEvent,
        )
    }
}

@Composable
private fun PlaybackTimelineSection(
    exoPlayer: ExoPlayer,
    controlsEnabled: Boolean,
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
            controlsEnabled = controlsEnabled,
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
private fun PlayerBottomControlBar(
    isPlaying: Boolean,
    controlsEnabled: Boolean,
    showAudioTracksButton: Boolean,
    showVideoTracksButton: Boolean,
    showSyncButton: Boolean,
    showNextEpisodeButton: Boolean,
    playPauseFocusRequester: FocusRequester,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
) {
    val restartFocusRequester = remember { FocusRequester() }
    val videoFocusRequester = remember { FocusRequester() }
    val sourcesFocusRequester = remember { FocusRequester() }
    val nextEpisodeFocusRequester = remember { FocusRequester() }
    val subtitlesFocusRequester = remember { FocusRequester() }
    val syncFocusRequester = remember { FocusRequester() }
    val audioFocusRequester = remember { FocusRequester() }
    val aspectRatioFocusRequester = remember { FocusRequester() }

    val playRightFocusRequester = if (showNextEpisodeButton) {
        nextEpisodeFocusRequester
    } else {
        subtitlesFocusRequester
    }
    val subtitlesLeftFocusRequester = if (showNextEpisodeButton) {
        nextEpisodeFocusRequester
    } else {
        playPauseFocusRequester
    }
    val subtitlesRightFocusRequester = when {
        showSyncButton -> syncFocusRequester
        showAudioTracksButton -> audioFocusRequester
        else -> aspectRatioFocusRequester
    }
    val syncRightFocusRequester = if (showAudioTracksButton) {
        audioFocusRequester
    } else {
        aspectRatioFocusRequester
    }
    val audioLeftFocusRequester = if (showSyncButton) {
        syncFocusRequester
    } else {
        subtitlesFocusRequester
    }
    val aspectLeftFocusRequester = when {
        showAudioTracksButton -> audioFocusRequester
        showSyncButton -> syncFocusRequester
        else -> subtitlesFocusRequester
    }
    val restartRightFocusRequester = if (showVideoTracksButton) {
        videoFocusRequester
    } else {
        sourcesFocusRequester
    }
    val sourceLeftFocusRequester = if (showVideoTracksButton) {
        videoFocusRequester
    } else {
        restartFocusRequester
    }
    var controlsBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var tooltipState by remember { mutableStateOf<PlayerControlTooltipState?>(null) }
    var lastFocusedControl by remember { mutableStateOf(PlayerControlFocusTarget.PlayPause) }
    var wasControlsEnabled by remember { mutableStateOf(controlsEnabled) }

    fun resolveFocusRequester(target: PlayerControlFocusTarget): FocusRequester {
        return when (target) {
            PlayerControlFocusTarget.Restart -> restartFocusRequester
            PlayerControlFocusTarget.Video -> {
                if (showVideoTracksButton) {
                    videoFocusRequester
                } else {
                    sourcesFocusRequester
                }
            }
            PlayerControlFocusTarget.Sources -> sourcesFocusRequester
            PlayerControlFocusTarget.PlayPause -> playPauseFocusRequester
            PlayerControlFocusTarget.NextEpisode -> {
                if (showNextEpisodeButton) {
                    nextEpisodeFocusRequester
                } else {
                    playPauseFocusRequester
                }
            }
            PlayerControlFocusTarget.Subtitles -> subtitlesFocusRequester
            PlayerControlFocusTarget.Sync -> {
                when {
                    showSyncButton -> syncFocusRequester
                    showAudioTracksButton -> audioFocusRequester
                    else -> aspectRatioFocusRequester
                }
            }
            PlayerControlFocusTarget.Audio -> {
                when {
                    showAudioTracksButton -> audioFocusRequester
                    showSyncButton -> syncFocusRequester
                    else -> aspectRatioFocusRequester
                }
            }
            PlayerControlFocusTarget.AspectRatio -> aspectRatioFocusRequester
        }
    }

    fun onControlFocused(tooltipText: String, boundsInRoot: Rect) {
        val parentBounds = controlsBoundsInRoot ?: return
        tooltipState = PlayerControlTooltipState(
            text = tooltipText,
            anchorCenterXPx = boundsInRoot.center.x - parentBounds.left,
            anchorTopYPx = boundsInRoot.top - parentBounds.top,
        )
    }

    fun onControlFocusLost() {
        tooltipState = null
    }

    LaunchedEffect(controlsEnabled, showNextEpisodeButton, showSyncButton, showAudioTracksButton, showVideoTracksButton) {
        val shouldRestoreFocus = controlsEnabled && !wasControlsEnabled
        wasControlsEnabled = controlsEnabled
        if (!shouldRestoreFocus) return@LaunchedEffect

        val restoreFocusRequester = resolveFocusRequester(lastFocusedControl)
        repeat(12) {
            if (restoreFocusRequester.requestFocus()) {
                return@LaunchedEffect
            }
            delay(16)
        }
    }

    LaunchedEffect(controlsEnabled) {
        if (!controlsEnabled) {
            tooltipState = null
        }
    }

    val playToNearButtonOffset = (PlayerControlsTokens.PlayButtonSize / 2) +
        PlayerControlsTokens.ButtonsSpacing +
        (PlayerControlsTokens.SecondaryButtonSize / 2)
    val sourceOffsetX = -playToNearButtonOffset
    val videoOffsetX = sourceOffsetX - PlayerControlsTokens.SecondaryButtonSize - PlayerControlsTokens.ButtonsSpacing
    val restartOffsetX = if (showVideoTracksButton) {
        videoOffsetX - PlayerControlsTokens.SecondaryButtonSize - PlayerControlsTokens.ButtonsSpacing
    } else {
        videoOffsetX
    }
    val nextOffsetX = playToNearButtonOffset

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                controlsBoundsInRoot = coordinates.boundsInRoot()
            },
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(PlayerControlsTokens.ButtonsSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SubtitlesControlButton(
                controlsEnabled = controlsEnabled,
                focusRequester = subtitlesFocusRequester,
                leftFocusRequester = subtitlesLeftFocusRequester,
                rightFocusRequester = subtitlesRightFocusRequester,
                onClick = { onControlsEvent(TvPlayerControlsEvent.OpenSubtitles) },
                onTooltipVisible = ::onControlFocused,
                onTooltipHidden = ::onControlFocusLost,
                onFocused = { lastFocusedControl = PlayerControlFocusTarget.Subtitles },
            )
            if (showSyncButton) {
                SyncControlButton(
                    controlsEnabled = controlsEnabled,
                    focusRequester = syncFocusRequester,
                    leftFocusRequester = subtitlesFocusRequester,
                    rightFocusRequester = syncRightFocusRequester,
                    onClick = { onControlsEvent(TvPlayerControlsEvent.SyncSubtitles) },
                    onTooltipVisible = ::onControlFocused,
                    onTooltipHidden = ::onControlFocusLost,
                    onFocused = { lastFocusedControl = PlayerControlFocusTarget.Sync },
                )
            }
            if (showAudioTracksButton) {
                AudioTracksControlButton(
                    controlsEnabled = controlsEnabled,
                    focusRequester = audioFocusRequester,
                    leftFocusRequester = audioLeftFocusRequester,
                    rightFocusRequester = aspectRatioFocusRequester,
                    onClick = { onControlsEvent(TvPlayerControlsEvent.OpenTracks) },
                    onTooltipVisible = ::onControlFocused,
                    onTooltipHidden = ::onControlFocusLost,
                    onFocused = { lastFocusedControl = PlayerControlFocusTarget.Audio },
                )
            }
            AspectRatioControlButton(
                controlsEnabled = controlsEnabled,
                focusRequester = aspectRatioFocusRequester,
                leftFocusRequester = aspectLeftFocusRequester,
                rightFocusRequester = null,
                onClick = { onControlsEvent(TvPlayerControlsEvent.ToggleResizeMode) },
                onTooltipVisible = ::onControlFocused,
                onTooltipHidden = ::onControlFocusLost,
                onFocused = { lastFocusedControl = PlayerControlFocusTarget.AspectRatio },
            )
        }

        if (showNextEpisodeButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = nextOffsetX),
            ) {
                NextEpisodeControlButton(
                    controlsEnabled = controlsEnabled,
                    focusRequester = nextEpisodeFocusRequester,
                    leftFocusRequester = playPauseFocusRequester,
                    rightFocusRequester = subtitlesFocusRequester,
                    onClick = { onControlsEvent(TvPlayerControlsEvent.NextEpisode) },
                    onTooltipVisible = ::onControlFocused,
                    onTooltipHidden = ::onControlFocusLost,
                    onFocused = { lastFocusedControl = PlayerControlFocusTarget.NextEpisode },
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = sourceOffsetX),
        ) {
            SourcesControlButton(
                controlsEnabled = controlsEnabled,
                focusRequester = sourcesFocusRequester,
                leftFocusRequester = sourceLeftFocusRequester,
                rightFocusRequester = playPauseFocusRequester,
                onClick = { onControlsEvent(TvPlayerControlsEvent.OpenSources) },
                onTooltipVisible = ::onControlFocused,
                onTooltipHidden = ::onControlFocusLost,
                onFocused = { lastFocusedControl = PlayerControlFocusTarget.Sources },
            )
        }

        if (showVideoTracksButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = videoOffsetX),
            ) {
                VideoTracksControlButton(
                    controlsEnabled = controlsEnabled,
                    focusRequester = videoFocusRequester,
                    leftFocusRequester = restartFocusRequester,
                    rightFocusRequester = sourcesFocusRequester,
                    onClick = { onControlsEvent(TvPlayerControlsEvent.OpenVideoTracks) },
                    onTooltipVisible = ::onControlFocused,
                    onTooltipHidden = ::onControlFocusLost,
                    onFocused = { lastFocusedControl = PlayerControlFocusTarget.Video },
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = restartOffsetX),
        ) {
            RestartControlButton(
                controlsEnabled = controlsEnabled,
                focusRequester = restartFocusRequester,
                leftFocusRequester = null,
                rightFocusRequester = restartRightFocusRequester,
                onClick = { onControlsEvent(TvPlayerControlsEvent.Restart) },
                onTooltipVisible = ::onControlFocused,
                onTooltipHidden = ::onControlFocusLost,
                onFocused = { lastFocusedControl = PlayerControlFocusTarget.Restart },
            )
        }

        Box(modifier = Modifier.align(Alignment.Center)) {
            PlayPauseControlButton(
                isPlaying = isPlaying,
                controlsEnabled = controlsEnabled,
                focusRequester = playPauseFocusRequester,
                leftFocusRequester = sourcesFocusRequester,
                rightFocusRequester = playRightFocusRequester,
                onClick = { onControlsEvent(TvPlayerControlsEvent.PlayPause) },
                onTooltipVisible = ::onControlFocused,
                onTooltipHidden = ::onControlFocusLost,
                onFocused = { lastFocusedControl = PlayerControlFocusTarget.PlayPause },
            )
        }

        AnimatedVisibility(
            visible = tooltipState != null,
            enter = fadeIn(animationSpec = tween(durationMillis = PlayerControlsTokens.TooltipFadeInMs)),
            exit = fadeOut(animationSpec = tween(durationMillis = PlayerControlsTokens.TooltipFadeOutMs)),
            modifier = Modifier
                .fillMaxWidth()
                .height(PlayerControlsTokens.PlayButtonSize),
        ) {
            tooltipState?.let { activeTooltip ->
                PlayerGlobalTooltip(
                    tooltipState = activeTooltip,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PlayerGlobalTooltip(
    tooltipState: PlayerControlTooltipState,
    modifier: Modifier = Modifier,
) {
    val verticalOffsetPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        PlayerControlsTokens.TooltipVerticalOffset.toPx()
    }

    androidx.compose.ui.layout.Layout(
        content = {
            Surface(
                shape = PlayerControlsTokens.TooltipShape,
                tonalElevation = PlayerControlsTokens.TooltipTonalElevation,
                colors = SurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = PlayerControlsTokens.TooltipAlpha),
                ),
                modifier = Modifier
                    .alpha(PlayerControlsTokens.TooltipAlpha)
                    .widthIn(max = PlayerControlsTokens.TooltipMaxWidth),
            ) {
                Text(
                    text = tooltipState.text,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        horizontal = PlayerControlsTokens.TooltipHorizontalPadding,
                        vertical = PlayerControlsTokens.TooltipVerticalPadding,
                    ),
                )
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val tooltipPlaceable = measurables.first().measure(
            constraints.copy(minWidth = 0, minHeight = 0),
        )

        val maxX = (constraints.maxWidth - tooltipPlaceable.width).coerceAtLeast(0)
        val targetX = (tooltipState.anchorCenterXPx - (tooltipPlaceable.width / 2f))
            .roundToInt()
            .coerceIn(0, maxX)
        val targetY = (tooltipState.anchorTopYPx - verticalOffsetPx - tooltipPlaceable.height).roundToInt()

        layout(constraints.maxWidth, constraints.maxHeight) {
            tooltipPlaceable.placeRelative(x = targetX, y = targetY)
        }
    }
}

@Composable
private fun RestartControlButton(
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onClick: () -> Unit,
    onTooltipVisible: (String, Rect) -> Unit,
    onTooltipHidden: () -> Unit,
    onFocused: () -> Unit,
) {
    PlayerSecondaryControlButton(
        icon = Icons.Default.Replay,
        tooltipText = stringResource(R.string.restart),
        onClick = onClick,
        controlsEnabled = controlsEnabled,
        focusRequester = focusRequester,
        leftFocusRequester = leftFocusRequester,
        rightFocusRequester = rightFocusRequester,
        onTooltipVisible = onTooltipVisible,
        onTooltipHidden = onTooltipHidden,
        onFocused = onFocused,
    )
}

@Composable
private fun SourcesControlButton(
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onClick: () -> Unit,
    onTooltipVisible: (String, Rect) -> Unit,
    onTooltipHidden: () -> Unit,
    onFocused: () -> Unit,
) {
    PlayerSecondaryControlButton(
        icon = Icons.Default.Source,
        tooltipText = stringResource(R.string.sources),
        onClick = onClick,
        controlsEnabled = controlsEnabled,
        focusRequester = focusRequester,
        leftFocusRequester = leftFocusRequester,
        rightFocusRequester = rightFocusRequester,
        onTooltipVisible = onTooltipVisible,
        onTooltipHidden = onTooltipHidden,
        onFocused = onFocused,
    )
}

@Composable
private fun PlayPauseControlButton(
    isPlaying: Boolean,
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onClick: () -> Unit,
    onTooltipVisible: (String, Rect) -> Unit,
    onTooltipHidden: () -> Unit,
    onFocused: () -> Unit,
) {
    PlayerPrimaryControlButton(
        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
        tooltipText = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.home_play),
        onClick = onClick,
        controlsEnabled = controlsEnabled,
        focusRequester = focusRequester,
        leftFocusRequester = leftFocusRequester,
        rightFocusRequester = rightFocusRequester,
        onTooltipVisible = onTooltipVisible,
        onTooltipHidden = onTooltipHidden,
        onFocused = onFocused,
    )
}

@Composable
private fun NextEpisodeControlButton(
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onClick: () -> Unit,
    onTooltipVisible: (String, Rect) -> Unit,
    onTooltipHidden: () -> Unit,
    onFocused: () -> Unit,
) {
    PlayerSecondaryControlButton(
        icon = Icons.Default.SkipNext,
        tooltipText = stringResource(R.string.next_episode),
        onClick = onClick,
        controlsEnabled = controlsEnabled,
        focusRequester = focusRequester,
        leftFocusRequester = leftFocusRequester,
        rightFocusRequester = rightFocusRequester,
        onTooltipVisible = onTooltipVisible,
        onTooltipHidden = onTooltipHidden,
        onFocused = onFocused,
    )
}

@Composable
private fun SubtitlesControlButton(
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onClick: () -> Unit,
    onTooltipVisible: (String, Rect) -> Unit,
    onTooltipHidden: () -> Unit,
    onFocused: () -> Unit,
) {
    PlayerSecondaryControlButton(
        icon = Icons.Default.Subtitles,
        tooltipText = stringResource(R.string.player_subtitles_settings),
        onClick = onClick,
        controlsEnabled = controlsEnabled,
        focusRequester = focusRequester,
        leftFocusRequester = leftFocusRequester,
        rightFocusRequester = rightFocusRequester,
        onTooltipVisible = onTooltipVisible,
        onTooltipHidden = onTooltipHidden,
        onFocused = onFocused,
    )
}

@Composable
private fun SyncControlButton(
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onClick: () -> Unit,
    onTooltipVisible: (String, Rect) -> Unit,
    onTooltipHidden: () -> Unit,
    onFocused: () -> Unit,
) {
    PlayerSecondaryControlButton(
        icon = Icons.Default.Sync,
        tooltipText = stringResource(R.string.subtitle_offset),
        onClick = onClick,
        controlsEnabled = controlsEnabled,
        focusRequester = focusRequester,
        leftFocusRequester = leftFocusRequester,
        rightFocusRequester = rightFocusRequester,
        onTooltipVisible = onTooltipVisible,
        onTooltipHidden = onTooltipHidden,
        onFocused = onFocused,
    )
}

@Composable
private fun AudioTracksControlButton(
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onClick: () -> Unit,
    onTooltipVisible: (String, Rect) -> Unit,
    onTooltipHidden: () -> Unit,
    onFocused: () -> Unit,
) {
    PlayerSecondaryControlButton(
        icon = Icons.Default.Audiotrack,
        tooltipText = stringResource(R.string.audio_tracks),
        onClick = onClick,
        controlsEnabled = controlsEnabled,
        focusRequester = focusRequester,
        leftFocusRequester = leftFocusRequester,
        rightFocusRequester = rightFocusRequester,
        onTooltipVisible = onTooltipVisible,
        onTooltipHidden = onTooltipHidden,
        onFocused = onFocused,
    )
}

@Composable
private fun VideoTracksControlButton(
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onClick: () -> Unit,
    onTooltipVisible: (String, Rect) -> Unit,
    onTooltipHidden: () -> Unit,
    onFocused: () -> Unit,
) {
    PlayerSecondaryControlButton(
        icon = Icons.Default.HighQuality,
        tooltipText = stringResource(R.string.video_tracks),
        onClick = onClick,
        controlsEnabled = controlsEnabled,
        focusRequester = focusRequester,
        leftFocusRequester = leftFocusRequester,
        rightFocusRequester = rightFocusRequester,
        onTooltipVisible = onTooltipVisible,
        onTooltipHidden = onTooltipHidden,
        onFocused = onFocused,
    )
}

@Composable
private fun AspectRatioControlButton(
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onClick: () -> Unit,
    onTooltipVisible: (String, Rect) -> Unit,
    onTooltipHidden: () -> Unit,
    onFocused: () -> Unit,
) {
    PlayerSecondaryControlButton(
        icon = Icons.Default.AspectRatio,
        tooltipText = stringResource(R.string.video_aspect_ratio_resize),
        onClick = onClick,
        controlsEnabled = controlsEnabled,
        focusRequester = focusRequester,
        leftFocusRequester = leftFocusRequester,
        rightFocusRequester = rightFocusRequester,
        onTooltipVisible = onTooltipVisible,
        onTooltipHidden = onTooltipHidden,
        onFocused = onFocused,
    )
}

@Composable
private fun PlayerPrimaryControlButton(
    icon: ImageVector,
    tooltipText: String,
    onClick: () -> Unit,
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onTooltipVisible: (String, Rect) -> Unit,
    onTooltipHidden: () -> Unit,
    onFocused: () -> Unit,
) {
    PlayerControlButton(
        icon = icon,
        tooltipText = tooltipText,
        onClick = onClick,
        controlsEnabled = controlsEnabled,
        focusRequester = focusRequester,
        leftFocusRequester = leftFocusRequester,
        rightFocusRequester = rightFocusRequester,
        buttonSize = PlayerControlsTokens.PlayButtonSize,
        iconSize = PlayerControlsTokens.PlayIconSize,
        focusedScale = PlayerControlsTokens.PlayFocusScale,
        containerColor = Color.White,
        contentColor = Color.Black,
        focusedContainerColor = Color.White,
        focusedContentColor = Color.Black,
        focusedBorder = Border.None,
        onTooltipVisible = onTooltipVisible,
        onTooltipHidden = onTooltipHidden,
        onFocused = onFocused,
    )
}

@Composable
private fun PlayerSecondaryControlButton(
    icon: ImageVector,
    tooltipText: String,
    onClick: () -> Unit,
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    buttonSize: Dp = PlayerControlsTokens.SecondaryButtonSize,
    iconSize: Dp = PlayerControlsTokens.SecondaryIconSize,
    focusedScale: Float = PlayerControlsTokens.SecondaryFocusScale,
    containerColor: Color = Color.White.copy(alpha = PlayerControlsTokens.SecondaryContainerAlpha),
    onTooltipVisible: (String, Rect) -> Unit,
    onTooltipHidden: () -> Unit,
    onFocused: () -> Unit,
) {
    PlayerControlButton(
        icon = icon,
        tooltipText = tooltipText,
        onClick = onClick,
        controlsEnabled = controlsEnabled,
        focusRequester = focusRequester,
        leftFocusRequester = leftFocusRequester,
        rightFocusRequester = rightFocusRequester,
        buttonSize = buttonSize,
        iconSize = iconSize,
        focusedScale = focusedScale,
        containerColor = containerColor,
        contentColor = Color.White,
        focusedContainerColor = Color.White,
        focusedContentColor = Color.Black,
        focusedBorder = Border.None,
        onTooltipVisible = onTooltipVisible,
        onTooltipHidden = onTooltipHidden,
        onFocused = onFocused,
    )
}

@Composable
private fun PlayerControlButton(
    icon: ImageVector,
    tooltipText: String,
    onClick: () -> Unit,
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    buttonSize: Dp,
    iconSize: Dp,
    focusedScale: Float,
    containerColor: Color,
    contentColor: Color,
    focusedContainerColor: Color,
    focusedContentColor: Color,
    focusedBorder: Border,
    onTooltipVisible: (String, Rect) -> Unit,
    onTooltipHidden: () -> Unit,
    onFocused: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    var buttonBoundsInRoot by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = Modifier.size(buttonSize),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            enabled = controlsEnabled,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onGloballyPositioned { coordinates ->
                    buttonBoundsInRoot = coordinates.boundsInRoot()
                    if (isFocused) {
                        buttonBoundsInRoot?.let { bounds ->
                            onTooltipVisible(tooltipText, bounds)
                        }
                    }
                }
                .onFocusChanged { focusState ->
                    val nowFocused = focusState.isFocused
                    if (isFocused == nowFocused) return@onFocusChanged

                    isFocused = nowFocused
                    if (nowFocused) {
                        onFocused()
                        buttonBoundsInRoot?.let { bounds ->
                            onTooltipVisible(tooltipText, bounds)
                        }
                    } else {
                        onTooltipHidden()
                    }
                }
                .horizontalFocusLink(
                    canFocus = controlsEnabled,
                    leftFocusRequester = leftFocusRequester,
                    rightFocusRequester = rightFocusRequester,
                ),
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = containerColor,
                contentColor = contentColor,
                focusedContainerColor = focusedContainerColor,
                focusedContentColor = focusedContentColor,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = focusedBorder,
                pressedBorder = Border.None,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tooltipText,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}

private fun Modifier.horizontalFocusLink(
    canFocus: Boolean,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
): Modifier {
    if (canFocus && leftFocusRequester == null && rightFocusRequester == null) {
        return this
    }

    return focusProperties {
        this.canFocus = canFocus
        leftFocusRequester?.let { left = it }
        rightFocusRequester?.let { right = it }
    }
}

@Composable
private fun PlaybackTimeline(
    progressFraction: Float,
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeProgress = progressFraction.coerceIn(0f, 1f)
    var isFocused by remember { mutableStateOf(false) }
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) {
            PlayerControlsTokens.TimelineFocusedTrackHeight.value /
                PlayerControlsTokens.TimelineInactiveTrackHeight.value
        } else {
            1f
        },
        animationSpec = tween(durationMillis = PlayerControlsTokens.TimelineFocusAnimationMs),
        label = "playback_timeline_focus_scale",
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackAlpha = if (isFocused) 0.34f else 0.20f

    Box(
        modifier = modifier
            .height(PlayerControlsTokens.TimelineContainerHeight)
            .focusRequester(focusRequester)
            .focusProperties {
                canFocus = controlsEnabled
            }
            .onPreviewKeyEvent { event ->
                if (!controlsEnabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
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
                isFocused = focusState.isFocused
            }
            .focusable(enabled = controlsEnabled)
            .padding(vertical = PlayerControlsTokens.TimelineTrackPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlayerControlsTokens.TimelineInactiveTrackHeight)
                .graphicsLayer {
                    scaleY = focusScale
                }
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = trackAlpha)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(safeProgress)
                    .background(primaryColor)
            )
        }
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
