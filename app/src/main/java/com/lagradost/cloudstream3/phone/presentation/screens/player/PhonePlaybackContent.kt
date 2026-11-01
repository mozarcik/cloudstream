package com.lagradost.cloudstream3.phone.presentation.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.phone.presentation.common.CsIconButton
import com.lagradost.cloudstream3.phone.presentation.theme.LocalPhoneAccent
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneShapes
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerCatalogUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerIcons
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerResizeMode
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerScreenActions
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerSeekPreferencesState
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerSubtitleSyncController
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.handlePlayerControlsEvent
import com.lagradost.cloudstream3.tv.presentation.screens.player.core.seekPlayerBy
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.BufferingOverlay
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerBottomControlBarConfig
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerOverlayStateHolder
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.TvPlayerControlsEvent
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.formatPlaybackTime
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.formatRemainingTime
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.playerEpisodeLabel
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.rememberPlayerBottomControlBarConfig
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.runtime.PlayerRuntimeTracksStateHolder
import com.lagradost.cloudstream3.tv.presentation.screens.player.video.TvPlayerSubtitleLayer
import com.lagradost.cloudstream3.tv.presentation.screens.player.video.TvPlayerVideoSurface
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelItemAction
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.requiresPlaybackRestore
import com.lagradost.cloudstream3.tv.presentation.screens.player.shouldRestorePlaybackForSubtitleEncodingChange
import kotlinx.coroutines.delay

private const val AutoHideMs = 5_000L
private const val DoubleTapSeekMs = 10_000L

@Composable
internal fun PhonePlaybackContent(
    state: TvPlayerUiState.Ready,
    catalogState: PlayerCatalogUiState,
    panelsState: TvPlayerPanelsUiState,
    overlayState: PlayerOverlayStateHolder,
    tracks: PlayerRuntimeTracksStateHolder,
    exoPlayer: ExoPlayer,
    subtitleSync: TvPlayerSubtitleSyncController,
    selectedSubtitle: com.lagradost.cloudstream3.ui.player.SubtitleData?,
    selectedSubtitleId: String?,
    subtitleDelayMs: Long,
    resizeMode: PlayerResizeMode,
    seekPreferences: TvPlayerSeekPreferencesState,
    actions: PlayerScreenActions,
    onPendingPlaybackRestoreCaptured: (Long, Boolean) -> Unit,
    onRuntimeSubtitleDelayChanged: (Long) -> Unit,
    onResizeModeChanged: (PlayerResizeMode) -> Unit,
) {
    val hasPanel = panelsState.activePanel != com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSidePanel.None ||
        tracks.audioPanelVisible || tracks.videoPanelVisible || tracks.subtitleSyncPanelVisible || overlayState.sourceErrorDialogEffect != null
    // Plain timestamp, not state: scrolling the option row touches it on every frame without recomposing.
    val lastInteractionMs = remember { longArrayOf(android.os.SystemClock.uptimeMillis()) }
    fun touch() { lastInteractionMs[0] = android.os.SystemClock.uptimeMillis() }

    // Auto-hide while playing and no panel is open, AutoHideMs after the last interaction.
    LaunchedEffect(overlayState.controlsVisible, overlayState.playerWantsToPlay, hasPanel) {
        if (overlayState.controlsVisible) touch()
        while (overlayState.controlsVisible && overlayState.playerWantsToPlay && !hasPanel) {
            delay(250)
            if (android.os.SystemClock.uptimeMillis() - lastInteractionMs[0] >= AutoHideMs) {
                overlayState.controlsVisible = false
            }
        }
    }

    val barConfig = rememberPlayerBottomControlBarConfig(
        state = state,
        selectedSubtitle = selectedSubtitle,
        hasSelectedSubtitle = catalogState.selectedSubtitleIndex >= 0,
        subtitleDelayMs = subtitleDelayMs,
        runtimeTracksState = tracks,
        playerResizeMode = resizeMode,
    )
    val widthDp = LocalConfiguration.current.screenWidthDp

    // Legacy gestures on the whole player (video and controls): a single tap shows the controls, and with the
    // controls visible it toggles pause. Taps that a control consumes (pills, timeline) are not seen as video taps.
    val gesturePrefs = rememberPhoneGesturePrefs()
    val gestureState = remember { PhoneGestureState() }
    var uiWasShowing by remember { mutableStateOf(false) }
    val gestureModifier = Modifier.phonePlayerGestures(
        enabled = !hasPanel,
        player = exoPlayer,
        prefs = gesturePrefs,
        state = gestureState,
        onSingleTap = {
            if (!overlayState.controlsVisible) {
                overlayState.controlsVisible = true
            } else if (exoPlayer.playWhenReady) {
                exoPlayer.pause()
            } else {
                exoPlayer.playWhenReady = true
            }
            touch()
        },
        onSwipeStart = { uiWasShowing = overlayState.controlsVisible; overlayState.controlsVisible = false },
        onGestureEnd = { hadSwipe ->
            // Like legacy: a swipe while paused brings the controls back.
            if (hadSwipe && uiWasShowing && !exoPlayer.isPlaying) overlayState.controlsVisible = true
            uiWasShowing = false
            touch()
        },
    )

    Box(Modifier.fillMaxSize().background(Color.Black).then(gestureModifier)) {
        TvPlayerVideoSurface(player = exoPlayer, resizeMode = resizeMode.resizeMode, modifier = Modifier.fillMaxSize())
        TvPlayerSubtitleLayer(exoPlayer, subtitleSync, overlayState.controlsVisible, Modifier.fillMaxSize())
        if (overlayState.showBufferingOverlay) BufferingOverlay()

        PhoneGestureOverlay(gestureState)

        val alpha by animateFloatAsState(if (overlayState.controlsVisible && !overlayState.upNextVisible) 1f else 0f, tween(150), label = "controls")
        if (alpha > 0.01f) {
            Box(
                Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.pointerInput(Unit) {
                    // Any finger activity on the controls (scrolling the option row, dragging the timeline) keeps them visible.
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            touch()
                        }
                    }
                },
            ) {
                ControlsLayer(
                    state = state, exoPlayer = exoPlayer, overlayState = overlayState, barConfig = barConfig,
                    enabled = !hasPanel && overlayState.controlsVisible,
                    onClose = actions.onBackPressed,
                    onProgress = actions.onPlaybackProgress,
                    onTouch = ::touch,
                    onEvent = { event ->
                        touch()
                        handlePlayerControlsEvent(
                            event = event, exoPlayer = exoPlayer, state = state, catalogState = catalogState,
                            selectedSubtitle = selectedSubtitle, selectedSubtitleId = selectedSubtitleId,
                            subtitleSyncController = subtitleSync, runtimeTracksState = tracks,
                            seekPreferencesState = seekPreferences, onOpenPanel = actions.onOpenPanel,
                            onPlayNextEpisode = actions.onPlayNextEpisode,
                            onToggleResizeMode = { onResizeModeChanged(resizeMode.next()) },
                            onSeekBy = { seekPlayerBy(exoPlayer, it) },
                        )
                    },
                    widthDp = widthDp,
                )
            }
        }

        state.upNext?.let { upNext ->
            PhoneUpNextCard(
                upNext = upNext, exoPlayer = exoPlayer,
                dismissed = overlayState.upNextDismissed, suppressed = hasPanel,
                onVisibleChanged = { overlayState.upNextVisible = it },
                onPlayNow = { exoPlayer.pause(); actions.onPlayNextEpisode(exoPlayer.currentPosition.coerceAtLeast(0L), exoPlayer.duration.coerceAtLeast(0L)) },
                onDismiss = { overlayState.upNextDismissed = true },
                modifier = Modifier.align(Alignment.BottomEnd).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Bottom)).padding(end = 28.dp, bottom = 24.dp),
            )
        }

        PhonePlayerPanels(
            sourceName = state.link.name.orEmpty(),
            panelsState = panelsState, tracks = tracks, overlayState = overlayState, exoPlayer = exoPlayer,
            subtitleSync = subtitleSync, selectedSubtitle = selectedSubtitle,
            onClosePanel = actions.onClosePanel,
            onSubtitlesBackPressed = actions.onSubtitlesSidePanelBackPressed,
            onItemAction = { action ->
                touch()
                val restore = action.requiresPlaybackRestore() ||
                    (action is TvPlayerPanelItemAction.SelectSubtitleEncodingOption &&
                        shouldRestorePlaybackForSubtitleEncodingChange(selectedSubtitle, panelsState.selectedSubtitleEncodingValue, action.value))
                if (restore) {
                    onPendingPlaybackRestoreCaptured(exoPlayer.currentPosition, exoPlayer.playWhenReady)
                }
                actions.onPanelItemAction(action)
            },
            onRetrySource = { index ->
                onPendingPlaybackRestoreCaptured(exoPlayer.currentPosition, exoPlayer.playWhenReady)
                actions.onRetrySource(index)
            },
            onRuntimeSubtitleDelayChanged = onRuntimeSubtitleDelayChanged,
        )
    }
}

@Composable
private fun ControlsLayer(
    state: TvPlayerUiState.Ready,
    exoPlayer: ExoPlayer,
    overlayState: PlayerOverlayStateHolder,
    barConfig: PlayerBottomControlBarConfig,
    enabled: Boolean,
    onClose: () -> Unit,
    onProgress: (Long, Long) -> Unit,
    onTouch: () -> Unit,
    onEvent: (TvPlayerControlsEvent) -> Unit,
    widthDp: Int,
) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(130.dp).background(Brush.verticalGradient(listOf(Color(0xC7000000), Color.Transparent))))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(200.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD1000000)))))

        // Keep the controls clear of the navigation bar / cutout whenever the system bars are on screen.
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical))) {
        Column(Modifier.align(Alignment.TopStart).padding(start = 28.dp, top = 20.dp, end = 80.dp)) {
            val m = state.metadata
            Text(m.title, style = PhoneText.Level.copy(fontSize = 26.sp), color = PhoneColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val episode = listOfNotNull(playerEpisodeLabel(m).takeIf { m.isEpisodeBased }, m.episodeTitle?.takeIf { it.isNotBlank() }).joinToString(" • ")
            if (episode.isNotEmpty()) Text(episode, style = PhoneText.Row.copy(fontSize = 14.sp), color = PhoneColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            val info = listOfNotNull(state.link.name.takeIf { it.isNotBlank() }, state.link.quality.takeIf { it > 0 }?.let { "${it}p" }).joinToString(" • ")
            if (info.isNotEmpty()) Text(info, style = PhoneText.Meta.copy(fontSize = 12.sp), color = Color(0xFFC8CEDA), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        CsIconButton(
            Icons.Default.Close, stringResource(R.string.phone_player_close), onClose,
            Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 20.dp), container = Color(0x24FFFFFF), size = 44.dp,
        )

        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            PhoneTimeline(exoPlayer, enabled, onProgress, onTouch, Modifier.padding(horizontal = 28.dp))
            OptionRow(overlayState.playerWantsToPlay, barConfig, enabled, onEvent)
        }
        }
    }
}

@Composable
private fun OptionRow(isPlaying: Boolean, config: PlayerBottomControlBarConfig, enabled: Boolean, onEvent: (TvPlayerControlsEvent) -> Unit) {
    LazyRow(contentPadding = PaddingValues(start = 28.dp, end = 28.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item("play") { Pill(if (isPlaying) PlayerIcons.Pause else PlayerIcons.Play, stringResource(if (isPlaying) R.string.pause else R.string.home_play), null, enabled) { onEvent(TvPlayerControlsEvent.PlayPause) } }
        item("restart") { Pill(PlayerIcons.Restart, stringResource(R.string.restart), null, enabled) { onEvent(TvPlayerControlsEvent.Restart) } }
        if (config.showVideoTracksButton) item("video") { Pill(PlayerIcons.Video, stringResource(R.string.tv_player_control_video), config.videoValue, enabled) { onEvent(TvPlayerControlsEvent.OpenVideoTracks) } }
        item("sources") { Pill(PlayerIcons.Sources, stringResource(R.string.sources), null, enabled) { onEvent(TvPlayerControlsEvent.OpenSources) } }
        if (config.showNextEpisodeButton) item("next") { Pill(PlayerIcons.Next, stringResource(R.string.tv_player_control_next), config.nextValue, enabled) { onEvent(TvPlayerControlsEvent.NextEpisode) } }
        item("subs") { Pill(PlayerIcons.Subtitles, stringResource(R.string.player_subtitles_settings), config.subtitlesValue, enabled) { onEvent(TvPlayerControlsEvent.OpenSubtitles) } }
        if (config.showSyncButton) item("sync") { Pill(PlayerIcons.Sync, stringResource(R.string.tv_player_control_sync), config.syncValue, enabled) { onEvent(TvPlayerControlsEvent.SyncSubtitles) } }
        if (config.showAudioTracksButton) item("audio") { Pill(PlayerIcons.Audio, stringResource(R.string.tv_player_control_audio), config.audioValue, enabled) { onEvent(TvPlayerControlsEvent.OpenTracks) } }
        item("aspect") { Pill(PlayerIcons.Aspect, stringResource(R.string.tv_player_control_aspect), config.aspectValue, enabled) { onEvent(TvPlayerControlsEvent.ToggleResizeMode) } }
    }
}

/** Small option pill: icon, label and the current value (mockup "PlayerControls"). */
@Composable
private fun Pill(icon: ImageVector, label: String, value: String?, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.height(44.dp).clip(PhoneShapes.Pill).background(Color(0x24FFFFFF))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = PhoneColors.Text, modifier = Modifier.size(20.dp))
        Text(label, style = PhoneText.Button.copy(fontSize = 14.sp), color = PhoneColors.Text, maxLines = 1)
        if (!value.isNullOrBlank()) Text(value, style = PhoneText.Button.copy(fontSize = 14.sp), color = Color(0xFFB8BEC9), maxLines = 1)
    }
}

/** Position / duration read in this composable only (twice a second), so the rest of the overlay never recomposes with playback. */
@Composable
private fun PhoneTimeline(
    player: ExoPlayer,
    enabled: Boolean,
    onProgress: (Long, Long) -> Unit,
    onTouch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var positionMs by remember { mutableLongStateOf(player.currentPosition.coerceAtLeast(0L)) }
    var durationMs by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(player) {
        while (true) {
            if (dragFraction == null) positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.coerceAtLeast(0L)
            onProgress(positionMs, durationMs)
            delay(500)
        }
    }
    val accent = LocalPhoneAccent.current
    val shownMs = dragFraction?.let { (it * durationMs).toLong() } ?: positionMs
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(formatPlaybackTime(shownMs), style = PhoneText.Button.copy(fontSize = 13.sp), color = PhoneColors.Text, modifier = Modifier.width(52.dp))
        Box(
            Modifier.weight(1f).height(32.dp).pointerInput(enabled, durationMs) {
                if (!enabled || durationMs <= 0L) return@pointerInput
                detectTapGestures { offset ->
                    player.seekTo((offset.x / size.width * durationMs).toLong().coerceIn(0L, durationMs)); onTouch()
                }
            }.pointerInput(enabled, durationMs) {
                if (!enabled || durationMs <= 0L) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { dragFraction = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) },
                    onDragEnd = { dragFraction?.let { player.seekTo((it * durationMs).toLong()) }; dragFraction = null; onTouch() },
                    onDragCancel = { dragFraction = null },
                ) { change, drag ->
                    change.consume()
                    dragFraction = ((dragFraction ?: 0f) + drag / size.width).coerceIn(0f, 1f)
                    onTouch()
                }
            }.drawBehind {
                val h = 5.dp.toPx(); val y = (size.height - h) / 2f; val r = CornerRadius(h / 2f)
                drawRoundRect(Color(0x40FFFFFF), Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, h), r)
                val f = if (durationMs > 0) (shownMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                drawRoundRect(accent, Offset(0f, y), androidx.compose.ui.geometry.Size(size.width * f, h), r)
                drawCircle(accent, 8.dp.toPx(), Offset(size.width * f, size.height / 2f))
            },
        )
        Text(formatRemainingTime(shownMs, durationMs), style = PhoneText.Button.copy(fontSize = 13.sp), color = PhoneColors.Text, modifier = Modifier.width(60.dp))
    }
}
