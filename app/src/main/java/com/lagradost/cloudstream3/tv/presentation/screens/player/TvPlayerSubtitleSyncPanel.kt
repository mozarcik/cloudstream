package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.SlidingSidePanel
import com.lagradost.cloudstream3.ui.player.SubtitleCue
import kotlinx.coroutines.delay
import java.util.Locale

private object SubtitleSyncTokens {
    const val DebugTag = "TvSubtitleSync"
    const val EmptyCuesPollLogIntervalMs = 3_000L
    const val PositionTickMs = 200L
    const val CuesPollMs = 600L
    const val SmallStepMs = 100L
    const val LargeStepMs = 1_000L
    const val ActiveLinesPaddingCount = 4
    const val PanelWidthFraction = 0.58f
    const val ProgressFocusAnimationMs = 120

    val PanelOuterPadding = 12.dp
    val PanelInnerPadding = 20.dp
    val ColumnsSpacing = 18.dp
    val ColumnWeightDialogs = 0.58f
    val ColumnWeightControls = 0.42f
    val DialogListTopPadding = 10.dp
    val DialogListMaxHeight = 620.dp
    val DelayValueTopPadding = 12.dp
    val DelayValueBottomPadding = 18.dp
    val ControlsButtonsSpacing = 8.dp
    val ActiveProgressHeight = 5.dp
    val InactiveProgressHeight = 3.dp
    val ActiveProgressTopPadding = 6.dp
    val ButtonsContentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
}

@Composable
internal fun SubtitleSyncSidePanel(
    visible: Boolean,
    player: ExoPlayer,
    subtitleSyncController: TvPlayerSubtitleSyncController,
    hasActiveSubtitleTrack: Boolean,
    onCloseRequested: () -> Unit,
    onSubtitleDelayChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val firstDialogFocusRequester = remember { FocusRequester() }
    val firstControlFocusRequester = remember { FocusRequester() }
    val minusLargeStepFocusRequester = remember { FocusRequester() }
    val minusSmallStepFocusRequester = remember { FocusRequester() }
    var hasDialogListFocus by remember { mutableStateOf(false) }
    var subtitleCues by remember(subtitleSyncController, hasActiveSubtitleTrack) {
        mutableStateOf(emptyList<SubtitleCue>())
    }
    var playerPositionMs by remember(player) {
        mutableLongStateOf(player.currentPosition.coerceAtLeast(0L))
    }
    var subtitleDelayMs by remember(subtitleSyncController) {
        mutableLongStateOf(subtitleSyncController.subtitleDelayMs())
    }
    var initialFocusRequested by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            debugLog(
                "panel opened: hasActiveSubtitleTrack=$hasActiveSubtitleTrack delayMs=$subtitleDelayMs",
            )
            return@LaunchedEffect
        }
        debugLog("panel closed")
        initialFocusRequested = false
        hasDialogListFocus = false
    }

    LaunchedEffect(visible, subtitleSyncController, hasActiveSubtitleTrack) {
        if (!visible) return@LaunchedEffect
        subtitleDelayMs = subtitleSyncController.subtitleDelayMs()
        if (!hasActiveSubtitleTrack) {
            subtitleCues = emptyList()
            debugLog("poll cues: inactive subtitle track -> empty state")
            return@LaunchedEffect
        }

        var previousCueCount = -1
        var lastEmptyLogTimestampMs = 0L
        while (true) {
            val polledCues = subtitleSyncController.subtitleCues()
            val polledCount = polledCues.size
            if (polledCount != previousCueCount) {
                previousCueCount = polledCount
                debugLog(
                    "poll cues: count=$polledCount" +
                        " firstStartMs=${polledCues.firstOrNull()?.startTimeMs}" +
                        " lastStartMs=${polledCues.lastOrNull()?.startTimeMs}",
                )
            } else if (polledCount == 0) {
                val nowMs = SystemClock.elapsedRealtime()
                if (nowMs - lastEmptyLogTimestampMs >= SubtitleSyncTokens.EmptyCuesPollLogIntervalMs) {
                    lastEmptyLogTimestampMs = nowMs
                    debugLog("poll cues: still empty")
                }
            }
            subtitleCues = polledCues
            delay(SubtitleSyncTokens.CuesPollMs)
        }
    }

    LaunchedEffect(visible, player) {
        if (!visible) return@LaunchedEffect
        while (true) {
            playerPositionMs = player.currentPosition.coerceAtLeast(0L)
            delay(SubtitleSyncTokens.PositionTickMs)
        }
    }

    val subtitleTimelinePositionMs by remember(playerPositionMs, subtitleDelayMs) {
        derivedStateOf { playerPositionMs - subtitleDelayMs }
    }
    val activeCueIndex by remember(subtitleCues, subtitleTimelinePositionMs) {
        derivedStateOf {
            findActiveCueIndex(
                cues = subtitleCues,
                playbackPositionMs = subtitleTimelinePositionMs,
            )
        }
    }
    val initialFocusCueIndex by remember(activeCueIndex) {
        derivedStateOf { activeCueIndex.coerceAtLeast(0) }
    }

    LaunchedEffect(visible, subtitleCues, initialFocusCueIndex) {
        if (!visible || initialFocusRequested) return@LaunchedEffect
        if (subtitleCues.isNotEmpty()) {
            val scrollTargetIndex = (initialFocusCueIndex - SubtitleSyncTokens.ActiveLinesPaddingCount)
                .coerceAtLeast(0)
            listState.scrollToItem(scrollTargetIndex)
            repeat(20) {
                if (firstDialogFocusRequester.requestFocus()) {
                    initialFocusRequested = true
                    return@LaunchedEffect
                }
                delay(16)
            }
        }

        if (firstControlFocusRequester.requestFocus()) {
            initialFocusRequested = true
        }
    }

    LaunchedEffect(visible, activeCueIndex, hasDialogListFocus) {
        if (!visible || hasDialogListFocus || subtitleCues.isEmpty()) return@LaunchedEffect
        val scrollTargetIndex = (activeCueIndex - SubtitleSyncTokens.ActiveLinesPaddingCount)
            .coerceAtLeast(0)
        listState.animateScrollToItem(scrollTargetIndex)
    }

    fun setSubtitleDelay(newDelayMs: Long) {
        subtitleSyncController.setSubtitleDelayMs(
            player = player,
            newSubtitleDelayMs = newDelayMs,
        )
        subtitleDelayMs = subtitleSyncController.subtitleDelayMs()
        onSubtitleDelayChanged(subtitleDelayMs)
    }

    fun syncToCue(cue: SubtitleCue) {
        val playerNowMs = player.currentPosition.coerceAtLeast(0L)
        debugLog(
            "syncToCue: cueStartMs=${cue.startTimeMs} cueEndMs=${cue.endTimeMs} playerNowMs=$playerNowMs",
        )
        setSubtitleDelay(playerNowMs - cue.startTimeMs)
    }

    val controlsEnabled = hasActiveSubtitleTrack

    SlidingSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        widthFraction = SubtitleSyncTokens.PanelWidthFraction,
        panelBackgroundColor = MaterialTheme.colorScheme.surface,
        panelOuterPadding = androidx.compose.foundation.layout.PaddingValues(SubtitleSyncTokens.PanelOuterPadding),
        panelPadding = androidx.compose.foundation.layout.PaddingValues(SubtitleSyncTokens.PanelInnerPadding),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(SubtitleSyncTokens.ColumnsSpacing),
        ) {
            Column(
                modifier = Modifier
                    .weight(SubtitleSyncTokens.ColumnWeightDialogs)
                    .fillMaxHeight(),
            ) {
                Text(
                    text = stringResource(R.string.subtitle_offset),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!hasActiveSubtitleTrack) {
                    SubtitleSyncEmptyState(
                        message = stringResource(R.string.tv_player_subtitle_sync_enable_subtitles),
                    )
                } else if (subtitleCues.isEmpty()) {
                    SubtitleSyncEmptyState(
                        message = stringResource(R.string.no_subtitles_loaded),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = SubtitleSyncTokens.DialogListTopPadding)
                            .heightIn(max = SubtitleSyncTokens.DialogListMaxHeight),
                        state = listState,
                    ) {
                        itemsIndexed(
                            items = subtitleCues,
                            key = { index, cue -> "${cue.startTimeMs}_${cue.endTimeMs}_$index" },
                        ) { index, cue ->
                            val isActive = index == activeCueIndex
                            val progressFraction = cue.progressFor(subtitleTimelinePositionMs)
                            var isItemFocused by remember(cue.startTimeMs, cue.endTimeMs, index) {
                                mutableStateOf(false)
                            }
                            ListItem(
                                selected = isActive,
                                onClick = {
                                    syncToCue(cue)
                                },
                                enabled = hasActiveSubtitleTrack,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (index == initialFocusCueIndex) {
                                            Modifier.focusRequester(firstDialogFocusRequester)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .focusProperties {
                                        right = if (controlsEnabled) {
                                            firstControlFocusRequester
                                        } else {
                                            FocusRequester.Default
                                        }
                                    }
                                    .onFocusChanged { focusState ->
                                        hasDialogListFocus = focusState.hasFocus
                                        isItemFocused = focusState.isFocused
                                    },
                                headlineContent = {
                                    Column {
                                        Text(
                                            text = cue.text.joinToString(separator = " "),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        Text(
                                            text = cue.formatTimestampRange(),
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.alpha(0.72f),
                                        )
                                        if (isActive) {
                                            SubtitleSyncProgressBar(
                                                progress = progressFraction,
                                                isFocused = isItemFocused,
                                                modifier = Modifier.padding(top = SubtitleSyncTokens.ActiveProgressTopPadding),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(SubtitleSyncTokens.ColumnWeightControls)
                    .fillMaxHeight(),
            ) {
                Text(
                    text = stringResource(R.string.subtitle_offset_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = formatSubtitleDelayLabel(subtitleDelayMs),
                    modifier = Modifier.padding(
                        top = SubtitleSyncTokens.DelayValueTopPadding,
                        bottom = SubtitleSyncTokens.DelayValueBottomPadding,
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SubtitleSyncTokens.ControlsButtonsSpacing),
                ) {
                    OutlinedButton(
                        onClick = { setSubtitleDelay(subtitleDelayMs - SubtitleSyncTokens.LargeStepMs) },
                        enabled = controlsEnabled,
                        contentPadding = SubtitleSyncTokens.ButtonsContentPadding,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(minusLargeStepFocusRequester)
                            .focusProperties {
                                left = firstDialogFocusRequester
                            },
                    ) {
                        Text(
                            text = "-1.0 s",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    OutlinedButton(
                        onClick = { setSubtitleDelay(subtitleDelayMs + SubtitleSyncTokens.LargeStepMs) },
                        enabled = controlsEnabled,
                        contentPadding = SubtitleSyncTokens.ButtonsContentPadding,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(firstControlFocusRequester)
                            .focusProperties {
                                left = minusLargeStepFocusRequester
                            },
                    ) {
                        Text(
                            text = "+1.0 s",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = SubtitleSyncTokens.ControlsButtonsSpacing),
                    horizontalArrangement = Arrangement.spacedBy(SubtitleSyncTokens.ControlsButtonsSpacing),
                ) {
                    Button(
                        onClick = { setSubtitleDelay(subtitleDelayMs - SubtitleSyncTokens.SmallStepMs) },
                        enabled = controlsEnabled,
                        contentPadding = SubtitleSyncTokens.ButtonsContentPadding,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(minusSmallStepFocusRequester)
                            .focusProperties {
                                left = firstDialogFocusRequester
                            },
                    ) {
                        Text(
                            text = "-100 ms",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Button(
                        onClick = { setSubtitleDelay(subtitleDelayMs + SubtitleSyncTokens.SmallStepMs) },
                        enabled = controlsEnabled,
                        contentPadding = SubtitleSyncTokens.ButtonsContentPadding,
                        modifier = Modifier
                            .weight(1f)
                            .focusProperties {
                                left = minusSmallStepFocusRequester
                            },
                    ) {
                        Text(
                            text = "+100 ms",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }

                OutlinedButton(
                    onClick = { setSubtitleDelay(0L) },
                    enabled = controlsEnabled,
                    modifier = Modifier
                        .padding(top = SubtitleSyncTokens.ControlsButtonsSpacing)
                        .fillMaxWidth()
                        .focusProperties {
                            left = firstDialogFocusRequester
                        },
                ) {
                    Text(
                        text = "${stringResource(R.string.reset_btn)} (0 ms)",
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

private fun debugLog(message: String) {
    Log.i(SubtitleSyncTokens.DebugTag, message)
}

@Composable
private fun SubtitleSyncEmptyState(
    message: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .padding(top = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

@Composable
private fun SubtitleSyncProgressBar(
    progress: Float,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
) {
    val indicatorColor = MaterialTheme.colorScheme.primary
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) {
            SubtitleSyncTokens.ActiveProgressHeight.value / SubtitleSyncTokens.InactiveProgressHeight.value
        } else {
            1f
        },
        animationSpec = tween(durationMillis = SubtitleSyncTokens.ProgressFocusAnimationMs),
        label = "subtitle_sync_progress_focus_scale",
    )
    val trackAlpha = if (isFocused) 0.32f else 0.18f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SubtitleSyncTokens.InactiveProgressHeight)
            .graphicsLayer {
                scaleY = focusScale
            }
            .subtitleSyncProgressTrack(
                color = indicatorColor.copy(alpha = trackAlpha),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .subtitleSyncProgressTrack(color = indicatorColor),
        )
    }
}

private fun Modifier.subtitleSyncProgressTrack(
    color: Color,
): Modifier {
    return background(
        color = color,
        shape = CircleShape,
    )
}

private fun formatSubtitleDelayLabel(delayMs: Long): String {
    return String.format(
        Locale.US,
        "%+.1f s",
        delayMs / 1000f,
    )
}

private fun findActiveCueIndex(
    cues: List<SubtitleCue>,
    playbackPositionMs: Long,
): Int {
    if (cues.isEmpty()) return -1
    val activeIndex = cues.indexOfFirst { cue ->
        playbackPositionMs in cue.startTimeMs until cue.endTimeMs
    }
    if (activeIndex >= 0) return activeIndex

    val nearestFutureIndex = cues.indexOfFirst { cue ->
        cue.startTimeMs > playbackPositionMs
    }
    return if (nearestFutureIndex >= 0) {
        nearestFutureIndex
    } else {
        cues.lastIndex
    }
}

private fun SubtitleCue.progressFor(playbackPositionMs: Long): Float {
    if (durationMs <= 0L) return 0f
    val rawProgress = (playbackPositionMs - startTimeMs).toFloat() / durationMs.toFloat()
    return rawProgress.coerceIn(0f, 1f)
}

private fun SubtitleCue.formatTimestampRange(): String {
    return "${formatTimestamp(startTimeMs)} - ${formatTimestamp(endTimeMs)}"
}

private fun formatTimestamp(timeMs: Long): String {
    val absolute = timeMs.coerceAtLeast(0L)
    val totalSeconds = absolute / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val millis = absolute % 1000L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    } else {
        String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
    }
}
