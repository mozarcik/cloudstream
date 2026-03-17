package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.lagradost.cloudstream3.tv.presentation.screens.player.core.PlayerSessionController
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.rememberPlayerOverlayStateHolder
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.player.runtime.rememberPlayerRuntimeTracksStateHolder
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@Composable
internal fun PlayerPlaybackScreen(
    state: TvPlayerUiState.Ready,
    catalogState: PlayerCatalogUiState,
    panelsState: TvPlayerPanelsUiState,
    seekPreferencesState: TvPlayerSeekPreferencesState,
    actions: PlayerScreenActions,
    panelEffects: SharedFlow<TvPlayerPanelEffect>,
    playerSessionController: PlayerSessionController,
) {
    val exoPlayer = playerSessionController.player
    val subtitleSyncController = playerSessionController.subtitleSyncController

    var playerResizeMode by remember { mutableStateOf(PlayerResizeMode.Fit) }
    var playbackRestoreRequest by remember { mutableStateOf<PlayerPlaybackRestoreRequest?>(null) }
    var lastConsumedPlaybackRestoreRequestId by remember { mutableStateOf<Long?>(null) }
    var nextPlaybackRestoreRequestId by remember { mutableStateOf(0L) }

    val selectedSubtitle = catalogState.selectedSubtitle
    val selectedSubtitleId = catalogState.selectedSubtitleId
    val subtitleDelayByTrackId = remember(
        state.episodeId,
        state.metadata.title,
        state.metadata.subtitle,
        state.metadata.year,
    ) {
        mutableStateMapOf<String, Long>()
    }
    val initialSubtitleDelayMs = selectedSubtitleId?.let(subtitleDelayByTrackId::get) ?: 0L
    val playbackRestoreRequestKey = playbackRestoreRequestKey(
        activeRequest = playbackRestoreRequest,
        lastConsumedRequestId = lastConsumedPlaybackRestoreRequestId,
    )

    val overlayState = rememberPlayerOverlayStateHolder(
        linkUrl = state.link.url,
        initialIsPlaying = exoPlayer.isPlaying,
        initialPlaybackState = exoPlayer.playbackState,
        initialPlayWhenReady = exoPlayer.playWhenReady,
    )
    val runtimeTracksState = rememberPlayerRuntimeTracksStateHolder(
        linkUrl = state.link.url,
        player = exoPlayer,
    )

    val rootFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val timelineFocusRequester = remember { FocusRequester() }
    val controlsInteractionEvents = remember {
        MutableSharedFlow<Unit>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    }

    val activePanel = panelsState.activePanel
    val hasSidePanel = activePanel != TvPlayerSidePanel.None ||
        runtimeTracksState.audioPanelVisible ||
        runtimeTracksState.videoPanelVisible ||
        runtimeTracksState.subtitleSyncPanelVisible ||
        overlayState.sourceErrorDialogEffect != null

    PlayerPlaybackListenerEffect(
        exoPlayer = exoPlayer,
        subtitleSyncController = subtitleSyncController,
        overlayState = overlayState,
        runtimeTracksState = runtimeTracksState,
        selectedSubtitleId = selectedSubtitleId,
        actions = actions,
        onPendingPlaybackRestoreCaptured = { positionMs, playWhenReady ->
            val nextRequestId = nextPlaybackRestoreRequestId + 1L
            nextPlaybackRestoreRequestId = nextRequestId
            playbackRestoreRequest = PlayerPlaybackRestoreRequest(
                requestId = nextRequestId,
                positionMs = positionMs,
                playWhenReady = playWhenReady,
            )
        },
    )
    PlayerPlaybackLoadEffect(
        state = state,
        selectedSubtitle = selectedSubtitle,
        selectedSubtitleId = selectedSubtitleId,
        isCurrentSourceReady = catalogState.isCurrentSourceReady,
        subtitleSelectionSource = catalogState.subtitleSelectionSource,
        initialSubtitleDelayMs = initialSubtitleDelayMs,
        playbackRestoreRequest = playbackRestoreRequest,
        playbackRestoreRequestKey = playbackRestoreRequestKey,
        defaultPlayerPositionMs = state.resumePositionMs,
        defaultPlayerPlayWhenReady = true,
        playerSessionController = playerSessionController,
        overlayState = overlayState,
        runtimeTracksState = runtimeTracksState,
        onPlaybackRestoreConsumed = { consumedRequestId ->
            if (consumedRequestId != null) {
                lastConsumedPlaybackRestoreRequestId = consumedRequestId
            }
            if (playbackRestoreRequest?.requestId == consumedRequestId) {
                playbackRestoreRequest = null
            }
        },
    )
    PlayerPlaybackSubtitleDelayEffect(
        subtitleDelayMs = initialSubtitleDelayMs,
        playerSessionController = playerSessionController,
    )
    PlayerExtractorVerificationEffect(state = state)
    PlayerPlaybackFocusEffects(
        overlayState = overlayState,
        hasSidePanel = hasSidePanel,
        controlsInteractionEvents = controlsInteractionEvents,
        playPauseFocusRequester = playPauseFocusRequester,
        rootFocusRequester = rootFocusRequester,
    )
    PlayerKeepScreenOnEffect(
        keepScreenOn = shouldKeepPlaybackScreenOn(
            isPlaying = overlayState.isPlaying,
            playerWantsToPlay = overlayState.playerWantsToPlay,
            playbackState = overlayState.playerPlaybackState,
        ),
    )
    PlayerPanelEffectsCollector(
        panelEffects = panelEffects,
        overlayState = overlayState,
        onOpenSubtitleFilePicker = actions.onOpenSubtitleFilePicker,
    )

    PlayerPlaybackBackHandler(
        overlayState = overlayState,
        runtimeTracksState = runtimeTracksState,
        activePanel = activePanel,
        actions = actions,
    )

    PlayerPlaybackContent(
        state = state,
        catalogState = catalogState,
        panelsState = panelsState,
        overlayState = overlayState,
        runtimeTracksState = runtimeTracksState,
        exoPlayer = exoPlayer,
        subtitleSyncController = subtitleSyncController,
        selectedSubtitle = selectedSubtitle,
        selectedSubtitleId = selectedSubtitleId,
        playerResizeMode = playerResizeMode,
        seekPreferencesState = seekPreferencesState,
        rootFocusRequester = rootFocusRequester,
        playPauseFocusRequester = playPauseFocusRequester,
        timelineFocusRequester = timelineFocusRequester,
        controlsInteractionEvents = controlsInteractionEvents,
        actions = actions,
        onPendingPlaybackRestoreCaptured = { positionMs, playWhenReady ->
            val nextRequestId = nextPlaybackRestoreRequestId + 1L
            nextPlaybackRestoreRequestId = nextRequestId
            playbackRestoreRequest = PlayerPlaybackRestoreRequest(
                requestId = nextRequestId,
                positionMs = positionMs,
                playWhenReady = playWhenReady,
            )
        },
        onRuntimeSubtitleDelayChanged = { updatedDelayMs ->
            selectedSubtitleId?.let { subtitleId ->
                subtitleDelayByTrackId[subtitleId] = updatedDelayMs
            }
        },
        onPlayerResizeModeChanged = { updatedMode ->
            playerResizeMode = updatedMode
        },
    )
}
