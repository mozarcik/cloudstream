package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.res.stringResource
import com.lagradost.cloudstream3.R
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlaybackTimelineUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.rememberPlaybackTimelineUiState
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import kotlin.math.abs
import kotlin.math.roundToInt

/** Time, track and remaining time in one row (mockup PlayerControls: 24 px times, 8 px track). */
@Composable
internal fun PlaybackTimelineSection(
    exoPlayer: ExoPlayer,
    controlsEnabled: Boolean,
    timelineFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    seekStepMs: Long,
    accent: Color,
    onPlaybackProgress: (Long, Long) -> Unit,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
) {
    val timelineState = rememberPlaybackTimelineUiState(
        player = exoPlayer,
        onPlaybackProgress = onPlaybackProgress,
        updateIntervalMs = 500L,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TvDimens.PlayerSidePadding),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(TvDimens.PlayerTimelineGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatPlaybackTime(timelineState.value.positionMs),
            style = TvTextStyles.PlayerTime,
            color = TvColors.Text,
            maxLines = 1,
            // Reserved width: the bar must not jump when the total duration becomes known.
            modifier = Modifier.widthIn(min = TvDimens.PlayerTimeLabelMinWidth),
        )
        PlaybackTimeline(
            state = timelineState,
            controlsEnabled = controlsEnabled,
            focusRequester = timelineFocusRequester,
            downFocusRequester = downFocusRequester,
            seekStepMs = seekStepMs,
            accent = accent,
            onControlsEvent = onControlsEvent,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatRemainingTime(timelineState.value.positionMs, timelineState.value.durationMs),
            style = TvTextStyles.PlayerTime,
            color = TvColors.PlayerValue,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.widthIn(min = TvDimens.PlayerTimeLabelMinWidth),
        )
    }
}

@Composable
internal fun PlaybackTimeline(
    state: State<PlaybackTimelineUiState>,
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    seekStepMs: Long,
    accent: Color,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    var widthPx by remember { mutableLongStateOf(0L) }
    // Seek hint: where the seek started and how far the user has moved since (mockup "41:22 +10 s").
    var seekAnchorMs by remember { mutableLongStateOf(0L) }
    var seekDeltaMs by remember { mutableLongStateOf(0L) }
    val density = LocalDensity.current

    LaunchedEffect(seekDeltaMs) {
        if (seekDeltaMs != 0L) {
            kotlinx.coroutines.delay(PlayerControlsTokens.SeekHintVisibleMs)
            seekDeltaMs = 0L
        }
    }

    fun fractionOf(ms: Long, durationMs: Long): Float =
        if (durationMs > 0L) (ms.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .height(TvDimens.PlayerTimelineHeight)
            .testTag(PlayerControlsTestTags.Timeline)
            .onSizeChanged { widthPx = it.width.toLong() }
            .focusRequester(focusRequester)
            .focusProperties {
                canFocus = controlsEnabled
                down = downFocusRequester
            }
            .onPreviewKeyEvent { event ->
                if (!controlsEnabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft, Key.DirectionRight -> {
                        val sign = if (event.key == Key.DirectionLeft) -1L else 1L
                        if (seekDeltaMs == 0L) seekAnchorMs = state.value.positionMs
                        seekDeltaMs += sign * seekStepMs
                        onControlsEvent(
                            if (sign < 0) TvPlayerControlsEvent.SeekBackward else TvPlayerControlsEvent.SeekForward,
                        )
                        true
                    }

                    Key.DirectionCenter -> {
                        onControlsEvent(TvPlayerControlsEvent.PlayPause)
                        true
                    }

                    else -> false
                }
            }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(enabled = controlsEnabled)
            .drawBehind {
                val timeline = state.value
                val focused = isFocused
                val trackHeight = (if (focused) TvDimens.PlayerTimelineTrackFocused else TvDimens.PlayerTimelineTrack).toPx()
                val top = (size.height - trackHeight) / 2f
                val radius = CornerRadius(trackHeight / 2f)
                val hinting = seekDeltaMs != 0L
                val shownMs = if (hinting) {
                    (seekAnchorMs + seekDeltaMs).coerceIn(0L, timeline.durationMs.coerceAtLeast(0L))
                } else {
                    timeline.positionMs
                }
                val played = fractionOf(shownMs, timeline.durationMs)
                val buffered = fractionOf(timeline.bufferedMs, timeline.durationMs)

                drawRoundRect(
                    color = if (focused) TvColors.PlayerTrackFocused else TvColors.PlayerTrack,
                    topLeft = Offset(0f, top),
                    size = Size(size.width, trackHeight),
                    cornerRadius = radius,
                )
                if (buffered > 0f) {
                    drawRoundRect(
                        color = if (focused) TvColors.PlayerBufferedFocused else TvColors.PlayerBuffered,
                        topLeft = Offset(0f, top),
                        size = Size(size.width * buffered, trackHeight),
                        cornerRadius = radius,
                    )
                }
                if (played > 0f) {
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(0f, top),
                        size = Size(size.width * played, trackHeight),
                        cornerRadius = radius,
                    )
                }
                val thumbCenter = Offset(size.width * played, size.height / 2f)
                if (focused) {
                    val thumbRadius = TvDimens.PlayerTimelineThumbFocused.toPx() / 2f
                    val ring = 2.dp.toPx()
                    drawCircle(
                        color = accent.copy(alpha = 0.6f),
                        radius = thumbRadius + ring / 2f,
                        center = thumbCenter,
                        style = Stroke(width = ring),
                    )
                    drawCircle(color = TvColors.Focus, radius = thumbRadius, center = thumbCenter)
                } else {
                    drawCircle(
                        color = accent,
                        radius = TvDimens.PlayerTimelineThumb.toPx() / 2f,
                        center = thumbCenter,
                    )
                }
            },
    ) {
        if (seekDeltaMs != 0L && isFocused) {
            val timeline = state.value
            val minutesSecondsFormat = stringResource(R.string.tv_player_seek_min_sec)
            val shownMs = (seekAnchorMs + seekDeltaMs).coerceIn(0L, timeline.durationMs.coerceAtLeast(0L))
            val fraction = fractionOf(shownMs, timeline.durationMs)
            val label = remember(shownMs, seekDeltaMs) {
                buildAnnotatedString {
                    append(formatPlaybackTime(shownMs))
                    append("  ")
                    withStyle(SpanStyle(color = TvColors.OnFocusSecondary)) {
                        append(formatSeekDelta(seekDeltaMs, minutesSecondsFormat))
                    }
                }
            }
            val gapPx = with(density) { 6.dp.roundToPx() }
            val hintWidthPx = with(density) { 110.dp.roundToPx() }
            Text(
                text = label,
                style = TvTextStyles.PlayerOption,
                color = TvColors.OnFocus,
                maxLines = 1,
                modifier = Modifier
                    .offset {
                        val x = (widthPx * fraction - hintWidthPx / 2f)
                            .roundToInt()
                            .coerceIn(0, (widthPx.toInt() - hintWidthPx).coerceAtLeast(0))
                        IntOffset(x, -(gapPx + 26.dp.roundToPx()))
                    }
                    .width(110.dp)
                    .background(TvColors.Focus, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                    .padding(vertical = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

internal fun formatPlaybackTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/** "−32:40" (U+2212) as in the mockup; empty while the duration is unknown. */
internal fun formatRemainingTime(positionMs: Long, durationMs: Long): String {
    if (durationMs <= 0L) return ""
    return "−" + formatPlaybackTime((durationMs - positionMs).coerceAtLeast(0L))
}

/** "+10 s" under a minute, then "25 min 20 s" (or "25 min" when no seconds are left). */
internal fun formatSeekDelta(deltaMs: Long, minutesSecondsFormat: String): String {
    val totalSeconds = abs(deltaMs) / 1000L
    val sign = if (deltaMs < 0L) "−" else "+"
    val body = if (totalSeconds < 60L) {
        "$totalSeconds s"
    } else {
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        runCatching { minutesSecondsFormat.format(minutes, seconds) }
            .getOrElse { "$minutes min $seconds s" }
    }
    return sign + body
}
