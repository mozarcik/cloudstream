package com.lagradost.cloudstream3.phone.presentation.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.phone.presentation.common.CsButton
import com.lagradost.cloudstream3.phone.presentation.theme.LocalPhoneAccent
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerCatalogUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerPlaybackSubtitleDelayEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerPlaybackLoadEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerPlaybackListenerEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerExtractorVerificationEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerKeepScreenOnEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerPanelEffectsCollector
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerPlaybackBackHandler
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerPlaybackRestoreRequest
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerResizeMode
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerScreenActions
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerSeekPreferencesState
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.core.PlayerSessionController
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerNoticeStack
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.playerAccent
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.rememberPlayerOverlayStateHolder
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.player.runtime.rememberPlayerRuntimeTracksStateHolder
import kotlinx.coroutines.flow.SharedFlow

@Composable
internal fun PhonePlayerScreen(
    uiState: TvPlayerUiState,
    catalogState: PlayerCatalogUiState,
    panelsState: TvPlayerPanelsUiState,
    seekPreferences: TvPlayerSeekPreferencesState,
    actions: PlayerScreenActions,
    panelEffects: SharedFlow<TvPlayerPanelEffect>,
    notices: PlayerNoticeStack,
    session: PlayerSessionController,
) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (uiState) {
            is TvPlayerUiState.LoadingSources -> {
                BackHandler(onBack = actions.onBackPressed)
                PlayerKeepScreenOnEffect(keepScreenOn = true)
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(uiState.metadata.title, style = PhoneText.Level, color = PhoneColors.Text)
                    CircularProgressIndicator(Modifier.padding(4.dp), color = LocalPhoneAccent.current)
                    Text(stringResource(R.string.phone_player_loading_sources, uiState.loadedSources), style = PhoneText.Meta, color = PhoneColors.Text2)
                    if (uiState.canSkip) CsButton(stringResource(R.string.phone_player_skip_loading), actions.onSkipLoading)
                }
            }
            is TvPlayerUiState.Error -> {
                BackHandler(onBack = actions.onBackPressed)
                Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(uiState.metadata.title, style = PhoneText.Level.copy(fontSize = 26.sp), color = PhoneColors.Text)
                    Text(uiState.error.message, style = PhoneText.Row.copy(fontSize = 18.sp, lineHeight = 26.sp), color = PhoneColors.Body, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Row2(actions)
                }
            }
            is TvPlayerUiState.Ready -> PhonePlaybackScreen(
                state = uiState, catalogState = catalogState, panelsState = panelsState,
                seekPreferences = seekPreferences, actions = actions, panelEffects = panelEffects,
                notices = notices, session = session,
            )
        }
        PhoneNoticeLayer(stack = notices, modifier = Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun Row2(actions: PlayerScreenActions) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CsButton(stringResource(R.string.phone_retry), actions.onRetry, primary = true)
        CsButton(stringResource(R.string.phone_back), actions.onBackPressed)
    }
}

/** Same glue as the TV playback screen (state holders + effects); only the content differs. */
@Composable
internal fun PhonePlaybackScreen(
    state: TvPlayerUiState.Ready,
    catalogState: PlayerCatalogUiState,
    panelsState: TvPlayerPanelsUiState,
    seekPreferences: TvPlayerSeekPreferencesState,
    actions: PlayerScreenActions,
    panelEffects: SharedFlow<TvPlayerPanelEffect>,
    notices: PlayerNoticeStack,
    session: PlayerSessionController,
) {
    val exoPlayer = session.player
    val subtitleSync = session.subtitleSyncController
    var resizeMode by remember { mutableStateOf(PlayerResizeMode.Fit) }
    var restoreRequest by remember { mutableStateOf<PlayerPlaybackRestoreRequest?>(null) }
    var lastConsumedRestoreId by remember { mutableStateOf<Long?>(null) }
    var nextRestoreId by remember { mutableLongStateOf(0L) }

    val selectedSubtitle = catalogState.selectedSubtitle
    val selectedSubtitleId = catalogState.selectedSubtitleId
    val delayByTrack = remember(state.episodeId, state.metadata.title, state.metadata.subtitle, state.metadata.year) { mutableStateMapOf<String, Long>() }
    val subtitleDelayMs = selectedSubtitleId?.let(delayByTrack::get) ?: 0L
    val restoreKey = com.lagradost.cloudstream3.tv.presentation.screens.player.playbackRestoreRequestKey(restoreRequest, lastConsumedRestoreId)

    val overlay = rememberPlayerOverlayStateHolder(state.link.url, exoPlayer.isPlaying, exoPlayer.playbackState, exoPlayer.playWhenReady)
    val tracks = rememberPlayerRuntimeTracksStateHolder(state.link.url, exoPlayer)

    val captureRestore: (Long, Boolean) -> Unit = { positionMs, playWhenReady ->
        nextRestoreId += 1L
        restoreRequest = PlayerPlaybackRestoreRequest(nextRestoreId, positionMs, playWhenReady)
    }

    PlayerPlaybackListenerEffect(
        exoPlayer = exoPlayer, subtitleSyncController = subtitleSync, overlayState = overlay,
        runtimeTracksState = tracks, selectedSubtitleId = selectedSubtitleId, actions = actions,
        onPendingPlaybackRestoreCaptured = captureRestore,
    )
    PlayerPlaybackLoadEffect(
        state = state, selectedSubtitle = selectedSubtitle, selectedSubtitleId = selectedSubtitleId,
        isCurrentSourceReady = catalogState.isCurrentSourceReady,
        subtitleSelectionSource = catalogState.subtitleSelectionSource,
        initialSubtitleDelayMs = subtitleDelayMs, playbackRestoreRequest = restoreRequest,
        playbackRestoreRequestKey = restoreKey, defaultPlayerPositionMs = state.resumePositionMs,
        defaultPlayerPlayWhenReady = true, playerSessionController = session,
        overlayState = overlay, runtimeTracksState = tracks,
        onPlaybackRestoreConsumed = { consumedId ->
            if (consumedId != null) lastConsumedRestoreId = consumedId
            if (restoreRequest?.requestId == consumedId) restoreRequest = null
        },
    )
    PlayerPlaybackSubtitleDelayEffect(subtitleDelayMs, selectedSubtitle != null, session)
    PlayerExtractorVerificationEffect(state)
    PlayerKeepScreenOnEffect(
        keepScreenOn = com.lagradost.cloudstream3.tv.presentation.screens.player.shouldKeepPlaybackScreenOn(
            overlay.isPlaying, overlay.playerWantsToPlay, overlay.playerPlaybackState,
        ),
    )
    PlayerPanelEffectsCollector(panelEffects, overlay, notices, actions.onOpenSubtitleFilePicker)
    PlayerPlaybackBackHandler(overlay, tracks, panelsState.activePanel, actions)

    CompositionLocalProviderAccent(playerAccent(state.metadata)) {
        PhonePlaybackContent(
            state = state, catalogState = catalogState, panelsState = panelsState,
            overlayState = overlay, tracks = tracks, exoPlayer = exoPlayer, subtitleSync = subtitleSync,
            selectedSubtitle = selectedSubtitle, selectedSubtitleId = selectedSubtitleId,
            subtitleDelayMs = subtitleDelayMs, resizeMode = resizeMode, seekPreferences = seekPreferences,
            actions = actions, onPendingPlaybackRestoreCaptured = captureRestore,
            onRuntimeSubtitleDelayChanged = { ms -> selectedSubtitleId?.let { delayByTrack[it] = ms } },
            onResizeModeChanged = { resizeMode = it },
        )
    }
}

@Composable
private fun CompositionLocalProviderAccent(accent: Color, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalPhoneAccent provides accent, content = content)
}
