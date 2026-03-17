package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelItemAction
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPlaybackErrorDetails
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.player.runtime.PlayerEmbeddedSubtitleSnapshot

@Stable
internal class PlayerScreenActions(
    val onBackPressed: () -> Unit,
    val onSkipLoading: () -> Unit,
    val onRetry: () -> Unit,
    val onPlaybackReady: () -> Unit,
    val onPlaybackEnded: (Long, Long) -> Unit,
    val onPlaybackProgress: (Long, Long) -> Unit,
    val onPlaybackStopped: (Long, Long) -> Unit,
    val onPlayNextEpisode: (Long, Long) -> Unit,
    val onRetrySource: (Int) -> Unit,
    val onOpenPanel: (TvPlayerSidePanel) -> Unit,
    val onClosePanel: () -> Unit,
    val onDisableSubtitlesFromPlaybackError: () -> Unit,
    val onPanelItemAction: (TvPlayerPanelItemAction) -> Unit,
    val onPlaybackError: (TvPlayerPlaybackErrorDetails?) -> Unit,
    val onOpenSubtitleFilePicker: () -> Unit,
    val onSubtitlesSidePanelBackPressed: () -> Boolean,
    val onEmbeddedSubtitlesChanged: (List<PlayerEmbeddedSubtitleSnapshot>) -> Unit,
)

@Composable
internal fun rememberPlayerScreenActions(
    onBackPressed: () -> Unit,
    tvPlayerScreenViewModel: TvPlayerScreenViewModel,
    onOpenSubtitleFilePicker: () -> Unit,
): PlayerScreenActions {
    return remember(
        onBackPressed,
        tvPlayerScreenViewModel,
        onOpenSubtitleFilePicker,
    ) {
        PlayerScreenActions(
            onBackPressed = onBackPressed,
            onSkipLoading = tvPlayerScreenViewModel::skipLoading,
            onRetry = tvPlayerScreenViewModel::retry,
            onPlaybackReady = tvPlayerScreenViewModel::onPlaybackReady,
            onPlaybackEnded = tvPlayerScreenViewModel::onPlaybackEnded,
            onPlaybackProgress = tvPlayerScreenViewModel::onPlaybackProgress,
            onPlaybackStopped = tvPlayerScreenViewModel::onPlaybackStopped,
            onPlayNextEpisode = tvPlayerScreenViewModel::playNextEpisode,
            onRetrySource = tvPlayerScreenViewModel::retrySource,
            onOpenPanel = tvPlayerScreenViewModel::openPanel,
            onClosePanel = tvPlayerScreenViewModel::closePanel,
            onDisableSubtitlesFromPlaybackError = tvPlayerScreenViewModel::disableSubtitlesFromPlaybackError,
            onPanelItemAction = tvPlayerScreenViewModel::onPanelItemAction,
            onPlaybackError = tvPlayerScreenViewModel::onPlaybackError,
            onOpenSubtitleFilePicker = onOpenSubtitleFilePicker,
            onSubtitlesSidePanelBackPressed = tvPlayerScreenViewModel::onSubtitlesSidePanelBackPressed,
            onEmbeddedSubtitlesChanged = tvPlayerScreenViewModel::onEmbeddedSubtitlesChanged,
        )
    }
}
