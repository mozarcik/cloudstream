package com.lagradost.cloudstream3.tv.presentation.screens.player.subtitlesync

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.media3.exoplayer.ExoPlayer
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerSubtitleSyncController
import com.lagradost.cloudstream3.ui.player.SubtitleCue

@Stable
internal class PlayerSubtitleSyncStateHolder(
    val listState: LazyListState,
    private val player: ExoPlayer,
    private val subtitleSyncController: TvPlayerSubtitleSyncController,
    private val hasActiveSubtitleTrack: Boolean,
    private val onSubtitleDelayChanged: (Long) -> Unit,
) {
    val firstDialogFocusRequester = FocusRequester()
    val firstControlFocusRequester = FocusRequester()
    val minusLargeStepFocusRequester = FocusRequester()
    val minusSmallStepFocusRequester = FocusRequester()

    var hasDialogListFocus by mutableStateOf(false)
    var subtitleCues by mutableStateOf(emptyList<SubtitleCue>())
    var playerPositionMs by mutableLongStateOf(player.currentPosition.coerceAtLeast(0L))
    var subtitleDelayMs by mutableLongStateOf(subtitleSyncController.subtitleDelayMs())
    var initialFocusRequested by mutableStateOf(false)
    private var previousCueCount by mutableStateOf(-1)
    private var lastEmptyLogTimestampMs by mutableLongStateOf(0L)

    val controlsEnabled: Boolean
        get() = hasActiveSubtitleTrack

    val subtitleTimelinePositionMs by derivedStateOf {
        playerPositionMs - subtitleDelayMs
    }

    val cueUiModels by derivedStateOf {
        subtitleCues.map { cue ->
            PlayerSubtitleCueUiModel(
                cue = cue,
                joinedText = cue.text.joinToString(separator = " "),
                timestampRange = cue.formatTimestampRange(),
            )
        }
    }

    val activeCueIndex by derivedStateOf {
        findActiveCueIndex(
            cues = subtitleCues,
            playbackPositionMs = subtitleTimelinePositionMs,
        )
    }

    val initialFocusCueIndex by derivedStateOf {
        activeCueIndex.coerceAtLeast(0)
    }

    fun onPanelVisibilityChanged(visible: Boolean) {
        if (visible) {
            debugLog(
                "panel opened: hasActiveSubtitleTrack=$hasActiveSubtitleTrack delayMs=$subtitleDelayMs",
            )
        } else {
            debugLog("panel closed")
            initialFocusRequested = false
            hasDialogListFocus = false
        }
    }

    fun updateSubtitleDelay(newDelayMs: Long) {
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
        updateSubtitleDelay(playerNowMs - cue.startTimeMs)
    }

    fun debugPollCues() {
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
            if (nowMs - lastEmptyLogTimestampMs >= PlayerSubtitleSyncTokens.EmptyCuesPollLogIntervalMs) {
                lastEmptyLogTimestampMs = nowMs
                debugLog("poll cues: still empty")
            }
        }
        subtitleCues = polledCues
    }

    private fun debugLog(message: String) {
        Log.i(PlayerSubtitleSyncTokens.DebugTag, message)
    }
}

@Composable
internal fun rememberPlayerSubtitleSyncStateHolder(
    player: ExoPlayer,
    subtitleSyncController: TvPlayerSubtitleSyncController,
    hasActiveSubtitleTrack: Boolean,
    onSubtitleDelayChanged: (Long) -> Unit,
): PlayerSubtitleSyncStateHolder {
    val listState = rememberLazyListState()
    return remember(player, subtitleSyncController, hasActiveSubtitleTrack, onSubtitleDelayChanged) {
        PlayerSubtitleSyncStateHolder(
            listState = listState,
            player = player,
            subtitleSyncController = subtitleSyncController,
            hasActiveSubtitleTrack = hasActiveSubtitleTrack,
            onSubtitleDelayChanged = onSubtitleDelayChanged,
        )
    }
}
