package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.runtime.Composable
import androidx.media3.exoplayer.ExoPlayer
import com.lagradost.cloudstream3.tv.presentation.screens.player.subtitlesync.PlayerSubtitleSyncPanel

@Composable
internal fun SubtitleSyncSidePanel(
    visible: Boolean,
    player: ExoPlayer,
    subtitleSyncController: TvPlayerSubtitleSyncController,
    hasActiveSubtitleTrack: Boolean,
    onCloseRequested: () -> Unit,
    onSubtitleDelayChanged: (Long) -> Unit,
) {
    PlayerSubtitleSyncPanel(
        visible = visible,
        player = player,
        subtitleSyncController = subtitleSyncController,
        hasActiveSubtitleTrack = hasActiveSubtitleTrack,
        onCloseRequested = onCloseRequested,
        onSubtitleDelayChanged = onSubtitleDelayChanged,
    )
}
