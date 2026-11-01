package com.lagradost.cloudstream3.tv.presentation.screens.player.subtitlesync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import com.lagradost.cloudstream3.tv.presentation.common.SlidingSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerSubtitleSyncController
import kotlinx.coroutines.delay

@Composable
internal fun PlayerSubtitleSyncPanel(
    visible: Boolean,
    player: ExoPlayer,
    subtitleSyncController: TvPlayerSubtitleSyncController,
    hasActiveSubtitleTrack: Boolean,
    subtitleInfo: String,
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

    var customDialogVisible by remember { mutableStateOf(false) }

    SlidingSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        panelWidth = TvDimens.PlayerSyncPanelWidth,
        overlayColor = Color.Black.copy(alpha = 0.25f),
        panelPadding = PaddingValues(
            start = TvDimens.PlayerSyncPanelPadding,
            end = TvDimens.PlayerSyncPanelPadding,
            top = TvDimens.PlayerPanelTopPadding,
        ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 11.dp),
            ) {
                Text(
                    text = stringResource(R.string.subtitle_offset),
                    style = TvTextStyles.PlayerPanelTitle,
                    color = TvColors.Text,
                )
                Text(
                    text = subtitleInfo,
                    style = TvTextStyles.PlayerValue,
                    color = TvColors.LabelPanel,
                    maxLines = 1,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(PlayerSubtitleSyncTokens.ColumnsSpacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                PlayerSubtitleSyncDialogColumn(
                    stateHolder = stateHolder,
                    hasActiveSubtitleTrack = hasActiveSubtitleTrack,
                )
                Spacer(
                    modifier = Modifier
                        .width(1.dp)
                        .height(450.dp)
                        .background(TvColors.PlayerSubtleDivider),
                )
                PlayerSubtitleSyncControlsColumn(
                    stateHolder = stateHolder,
                    onCustomValueRequested = { customDialogVisible = true },
                )
            }
        }
    }

    if (customDialogVisible) {
        PlayerSubtitleDelayDialog(
            initialDelayMs = stateHolder.subtitleDelayMs,
            onApply = { delayMs ->
                stateHolder.updateSubtitleDelay(delayMs)
                customDialogVisible = false
            },
            onDismiss = { customDialogVisible = false },
        )
    }
}
