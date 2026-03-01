package com.lagradost.cloudstream3.tv.presentation.screens.player.subtitlesync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.tv.presentation.common.SlidingSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerSubtitleSyncController
import kotlinx.coroutines.delay

@Composable
internal fun PlayerSubtitleSyncPanel(
    visible: Boolean,
    player: ExoPlayer,
    subtitleSyncController: TvPlayerSubtitleSyncController,
    hasActiveSubtitleTrack: Boolean,
    onCloseRequested: () -> Unit,
    onSubtitleDelayChanged: (Long) -> Unit,
) {
    val stateHolder = rememberPlayerSubtitleSyncStateHolder(
        player = player,
        subtitleSyncController = subtitleSyncController,
        hasActiveSubtitleTrack = hasActiveSubtitleTrack,
        onSubtitleDelayChanged = onSubtitleDelayChanged,
    )

    LaunchedEffect(visible) {
        stateHolder.onPanelVisibilityChanged(visible)
    }

    LaunchedEffect(visible, subtitleSyncController, hasActiveSubtitleTrack) {
        if (!visible) return@LaunchedEffect
        stateHolder.subtitleDelayMs = subtitleSyncController.subtitleDelayMs()
        if (!hasActiveSubtitleTrack) {
            stateHolder.subtitleCues = emptyList()
            return@LaunchedEffect
        }

        while (true) {
            stateHolder.debugPollCues()
            delay(PlayerSubtitleSyncTokens.CuesPollMs)
        }
    }

    LaunchedEffect(visible, player) {
        if (!visible) return@LaunchedEffect
        while (true) {
            stateHolder.playerPositionMs = player.currentPosition.coerceAtLeast(0L)
            delay(PlayerSubtitleSyncTokens.PositionTickMs)
        }
    }

    LaunchedEffect(visible, stateHolder.subtitleCues, stateHolder.initialFocusCueIndex) {
        if (!visible || stateHolder.initialFocusRequested) return@LaunchedEffect
        if (stateHolder.subtitleCues.isNotEmpty()) {
            val scrollTargetIndex = (stateHolder.initialFocusCueIndex - PlayerSubtitleSyncTokens.ActiveLinesPaddingCount)
                .coerceAtLeast(0)
            stateHolder.listState.scrollToItem(scrollTargetIndex)
            repeat(20) {
                if (stateHolder.firstDialogFocusRequester.requestFocus()) {
                    stateHolder.initialFocusRequested = true
                    return@LaunchedEffect
                }
                delay(16)
            }
        }

        if (stateHolder.firstControlFocusRequester.requestFocus()) {
            stateHolder.initialFocusRequested = true
        }
    }

    LaunchedEffect(visible, stateHolder.activeCueIndex, stateHolder.hasDialogListFocus) {
        if (!visible || stateHolder.hasDialogListFocus || stateHolder.subtitleCues.isEmpty()) return@LaunchedEffect
        val scrollTargetIndex = (stateHolder.activeCueIndex - PlayerSubtitleSyncTokens.ActiveLinesPaddingCount)
            .coerceAtLeast(0)
        if (stateHolder.listState.isItemOutsideViewport(scrollTargetIndex)) {
            stateHolder.listState.animateScrollToItem(scrollTargetIndex)
        }
    }

    SlidingSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        widthFraction = PlayerSubtitleSyncTokens.PanelWidthFraction,
        panelBackgroundColor = MaterialTheme.colorScheme.surface,
        panelOuterPadding = PaddingValues(PlayerSubtitleSyncTokens.PanelOuterPadding),
        panelPadding = PaddingValues(PlayerSubtitleSyncTokens.PanelInnerPadding),
    ) {
        Row(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(PlayerSubtitleSyncTokens.ColumnsSpacing),
        ) {
            PlayerSubtitleSyncDialogColumn(
                stateHolder = stateHolder,
                hasActiveSubtitleTrack = hasActiveSubtitleTrack,
            )
            PlayerSubtitleSyncControlsColumn(stateHolder = stateHolder)
        }
    }
}
