package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelContentNavigationDirection
import com.lagradost.cloudstream3.tv.presentation.common.MenuListSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DotSeparatedRow
import com.lagradost.cloudstream3.ui.player.CustomDecoder.Companion.fixSubtitleAlignment
import com.lagradost.cloudstream3.ui.player.UpdatedDefaultExtractorsFactory
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesFragment.Companion.applyStyle
import com.lagradost.cloudstream3.utils.ExtractorLink
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

object TvPlayerScreen {
    const val UrlBundleKey = "url"
    const val ApiNameBundleKey = "apiName"
    const val EpisodeDataBundleKey = "episodeData"
}

private const val SubtitleSyncDebugTag = "TvSubtitleSync"
private const val FocusRequestRetryCount = 20
private const val FocusRequestRetryDelayMs = 16L

@Composable
fun TvPlayerScreen(
    onBackPressed: () -> Unit,
    tvPlayerScreenViewModel: TvPlayerScreenViewModel,
) {
    val seekPreferencesState = rememberTvPlayerSeekPreferencesState()

    when (val state = tvPlayerScreenViewModel.uiState.collectAsStateWithLifecycle().value) {
        is TvPlayerUiState.LoadingSources -> {
            LoadingSourcesState(
                state = state,
                onSkipLoading = tvPlayerScreenViewModel::skipLoading,
                onBackPressed = onBackPressed,
            )
        }

        is TvPlayerUiState.Ready -> {
            PlaybackState(
                state = state,
                seekPreferencesState = seekPreferencesState,
                onBackPressed = onBackPressed,
                tvPlayerScreenViewModel = tvPlayerScreenViewModel,
            )
        }

        is TvPlayerUiState.Error -> {
            ErrorState(
                state = state,
                onRetry = tvPlayerScreenViewModel::retry,
                onBackPressed = onBackPressed,
            )
        }
    }
}

@Composable
private fun LoadingSourcesState(
    state: TvPlayerUiState.LoadingSources,
    onSkipLoading: () -> Unit,
    onBackPressed: () -> Unit,
) {
    val loadingMetaTexts = listOfNotNull(
        state.metadata.year?.toString()?.takeIf { it.isNotBlank() },
        state.metadata.apiName.takeIf { it.isNotBlank() },
    )
    val skipFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.canSkip) {
        if (!state.canSkip) return@LaunchedEffect

        repeat(20) {
            if (skipFocusRequester.requestFocus()) {
                return@LaunchedEffect
            }
            delay(16)
        }
    }

    BackHandler(onBack = onBackPressed)

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomStart,
    ) {
        AsyncImage(
            model = state.metadata.backdropUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.88f)
                        ),
                        startY = 220f
                    )
                )
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Transparent
                        ),
                        endX = 850f
                    )
                )
        )

        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .padding(horizontal = 48.dp, vertical = 42.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.metadata.title.isNotBlank()) {
                Text(
                    text = state.metadata.title,
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (loadingMetaTexts.isNotEmpty()) {
                DotSeparatedRow(
                    texts = loadingMetaTexts,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
                    ),
                )
            }

            Text(
                text = stringResource(R.string.tv_player_loading_sources_progress, state.loadedSources),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
            )

            Button(
                onClick = onSkipLoading,
                enabled = state.canSkip,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .focusRequester(skipFocusRequester),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Text(
                    text = stringResource(R.string.skip_loading),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun ErrorState(
    state: TvPlayerUiState.Error,
    onRetry: () -> Unit,
    onBackPressed: () -> Unit,
) {
    BackHandler(onBack = onBackPressed)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.metadata.title.isNotBlank()) {
                Text(
                    text = state.metadata.title,
                    style = MaterialTheme.typography.displaySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = stringResource(state.messageResId),
                style = MaterialTheme.typography.titleLarge,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onRetry) {
                    Text(
                        text = stringResource(R.string.tv_player_retry),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                Button(onClick = onBackPressed) {
                    Text(
                        text = stringResource(R.string.go_back),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

private enum class PlayerResizeMode(
    val resizeMode: Int,
    val labelResId: Int,
) {
    Fit(
        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
        labelResId = R.string.resize_fit,
    ),
    Fill(
        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL,
        labelResId = R.string.resize_fill,
    ),
    Zoom(
        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        labelResId = R.string.resize_zoom,
    ),
    ;

    fun next(): PlayerResizeMode {
        return when (this) {
            Fit -> Fill
            Fill -> Zoom
            Zoom -> Fit
        }
    }
}

@Composable
private fun PlaybackState(
    state: TvPlayerUiState.Ready,
    seekPreferencesState: TvPlayerSeekPreferencesState,
    onBackPressed: () -> Unit,
    tvPlayerScreenViewModel: TvPlayerScreenViewModel,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var playerResizeMode by remember { mutableStateOf(PlayerResizeMode.Fit) }
    var pendingSeekPositionMs by remember { mutableStateOf<Long?>(null) }
    var pendingPlayWhenReady by remember { mutableStateOf<Boolean?>(null) }
    var sourceErrorDialogEffect by remember {
        mutableStateOf<TvPlayerPanelEffect.OpenSourceErrorDialog?>(null)
    }

    val selectedSubtitle = state.subtitles.getOrNull(state.selectedSubtitleIndex)
    val selectedSubtitleId = selectedSubtitle?.getId()
    val subtitleDelayByTrackId = remember(
        state.episodeId,
        state.metadata.title,
        state.metadata.subtitle,
        state.metadata.year,
    ) {
        mutableStateMapOf<String, Long>()
    }
    val initialSubtitleDelayMs = selectedSubtitleId?.let { subtitleId ->
        subtitleDelayByTrackId[subtitleId]
    } ?: 0L
    val initialPlayerPositionMs = pendingSeekPositionMs ?: state.resumePositionMs
    val initialPlayerPlayWhenReady = pendingPlayWhenReady ?: true

    val playerInstance = remember(
        state.link.url,
        selectedSubtitleId,
        initialPlayerPositionMs,
        initialPlayerPlayWhenReady,
    ) {
        createPlayer(
            context = context,
            link = state.link,
            subtitle = selectedSubtitle,
            audioTracks = state.link.audioTracks,
            subtitleDelayMs = initialSubtitleDelayMs,
            startPositionMs = initialPlayerPositionMs,
            startPlayWhenReady = initialPlayerPlayWhenReady,
        )
    }
    val exoPlayer = playerInstance.player
    val subtitleSyncController = playerInstance.subtitleSyncController

    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var playerPlaybackState by remember { mutableStateOf(exoPlayer.playbackState) }
    var playerWantsToPlay by remember { mutableStateOf(exoPlayer.playWhenReady) }
    var errorHandled by remember(state.link.url) { mutableStateOf(false) }
    var runtimeTrackPanelVisible by remember(state.link.url) { mutableStateOf(false) }
    var subtitleSyncPanelVisible by remember(state.link.url) { mutableStateOf(false) }
    var runtimeAudioTracks by remember(state.link.url) { mutableStateOf<List<PlayerAudioTrackOption>>(emptyList()) }
    var runtimeSelectedAudioTrackId by remember(state.link.url) { mutableStateOf<String?>(null) }
    var hasAppliedInitialAudioSelection by remember(state.link.url) { mutableStateOf(false) }
    val subtitleFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        pendingSeekPositionMs = exoPlayer.currentPosition
        pendingPlayWhenReady = exoPlayer.playWhenReady
        tvPlayerScreenViewModel.onSubtitleFileSelected(uri)
    }

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

    fun seekBy(deltaMs: Long) {
        seekPlayerBy(exoPlayer, deltaMs)
    }
    fun registerControlsInteraction() {
        controlsInteractionEvents.tryEmit(Unit)
    }
    fun toggleResizeMode() {
        playerResizeMode = playerResizeMode.next()
        Toast.makeText(
            context,
            context.getString(playerResizeMode.labelResId),
            Toast.LENGTH_SHORT,
        ).show()
    }
    fun selectRuntimeAudioTrack(track: PlayerAudioTrackOption?) {
        applyRuntimeAudioTrackSelection(
            player = exoPlayer,
            track = track,
        )
        runtimeSelectedAudioTrackId = track?.selectionId
    }
    fun refreshRuntimeAudioTracks(tracksSnapshot: Tracks = exoPlayer.currentTracks) {
        val extractedAudioTracks = extractRuntimeAudioTracks(tracksSnapshot)
        runtimeAudioTracks = extractedAudioTracks
        runtimeSelectedAudioTrackId = extractedAudioTracks.firstOrNull { track ->
            track.isSelected
        }?.selectionId
    }
    val activePanel = state.panels.activePanel
    val hasSidePanel = activePanel != TvPlayerSidePanel.None ||
        runtimeTrackPanelVisible ||
        subtitleSyncPanelVisible ||
        sourceErrorDialogEffect != null
    val showRuntimeTracksButton = runtimeAudioTracks.size > 1

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                playerPlaybackState = playbackState
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
                    refreshRuntimeAudioTracks()
                }
                if (playbackState == Player.STATE_READY) {
                    tvPlayerScreenViewModel.onPlaybackReady()
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                playerWantsToPlay = playWhenReady
            }

            override fun onTracksChanged(tracks: Tracks) {
                val extractedAudioTracks = extractRuntimeAudioTracks(tracks)
                runtimeAudioTracks = extractedAudioTracks
                runtimeSelectedAudioTrackId = extractedAudioTracks.firstOrNull { track ->
                    track.isSelected
                }?.selectionId
                if (!hasAppliedInitialAudioSelection && extractedAudioTracks.isNotEmpty()) {
                    hasAppliedInitialAudioSelection = true
                    if (runtimeSelectedAudioTrackId == null) {
                        val firstAudioTrack = extractedAudioTracks.first()
                        selectRuntimeAudioTrack(firstAudioTrack)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (errorHandled) return
                controlsVisible = true
                val currentPosition = exoPlayer.currentPosition
                val currentPlayWhenReady = exoPlayer.playWhenReady

                // If subtitle loading/parsing fails, keep current source and fall back to "no subtitles".
                if (state.selectedSubtitleIndex >= 0) {
                    pendingSeekPositionMs = currentPosition
                    pendingPlayWhenReady = currentPlayWhenReady
                    tvPlayerScreenViewModel.disableSubtitlesFromPlaybackError()
                    Toast.makeText(context, context.getString(R.string.no_subtitles), Toast.LENGTH_SHORT).show()
                    return
                }

                errorHandled = true
                pendingSeekPositionMs = currentPosition
                pendingPlayWhenReady = currentPlayWhenReady
                tvPlayerScreenViewModel.onPlaybackError(
                    error = error.toTvPlayerPlaybackErrorDetails(),
                )
            }
        }

        exoPlayer.addListener(listener)
        onDispose {
            val rawDuration = exoPlayer.duration
            val finalDurationMs = if (rawDuration == C.TIME_UNSET || rawDuration < 0L) 0L else rawDuration
            tvPlayerScreenViewModel.onPlaybackStopped(exoPlayer.currentPosition.coerceAtLeast(0L), finalDurationMs)
            exoPlayer.removeListener(listener)
            subtitleSyncController.clearSubtitleView()
            subtitleSyncController.clearTextRenderer()
            exoPlayer.release()
        }
    }

    LaunchedEffect(state.link.url, state.link.extractorData, state.link.source) {
        runCatching {
            APIHolder.getApiFromNameNull(state.link.source)?.extractorVerifierJob(state.link.extractorData)
        }
    }

    LaunchedEffect(exoPlayer, pendingSeekPositionMs, pendingPlayWhenReady) {
        refreshRuntimeAudioTracks()

        pendingSeekPositionMs?.let { resumePositionMs ->
            seekPlayerTo(exoPlayer, resumePositionMs)
            pendingSeekPositionMs = null
        }
        pendingPlayWhenReady?.let { shouldPlay ->
            exoPlayer.playWhenReady = shouldPlay
            if (shouldPlay) {
                exoPlayer.play()
            } else {
                exoPlayer.pause()
            }
            pendingPlayWhenReady = null
        }
    }

    LaunchedEffect(controlsVisible, hasSidePanel) {
        if (controlsVisible && !hasSidePanel) {
            val focused = requestFocusWithRetry(playPauseFocusRequester)
            if (!focused) {
                requestFocusWithRetry(rootFocusRequester)
            }
        } else if (!controlsVisible) {
            requestFocusWithRetry(rootFocusRequester)
        }
    }

    LaunchedEffect(controlsVisible, playerWantsToPlay, activePanel, runtimeTrackPanelVisible, controlsInteractionEvents) {
        if (!controlsVisible || !playerWantsToPlay || activePanel != TvPlayerSidePanel.None || runtimeTrackPanelVisible) {
            return@LaunchedEffect
        }

        controlsInteractionEvents.tryEmit(Unit)
        controlsInteractionEvents.collectLatest {
            delay(4_500L)
            controlsVisible = false
        }
    }

    LaunchedEffect(playerWantsToPlay) {
        if (!playerWantsToPlay) {
            controlsVisible = true
        }
    }

    LaunchedEffect(tvPlayerScreenViewModel) {
        tvPlayerScreenViewModel.panelEffects.collectLatest { effect ->
            when (effect) {
                is TvPlayerPanelEffect.OpenSourceErrorDialog -> {
                    sourceErrorDialogEffect = effect
                }

                TvPlayerPanelEffect.OpenSubtitleFilePicker -> {
                    subtitleFilePickerLauncher.launch(
                        arrayOf(
                            "text/plain",
                            "text/str",
                            "application/octet-stream",
                            MimeTypes.TEXT_UNKNOWN,
                            MimeTypes.TEXT_VTT,
                            MimeTypes.TEXT_SSA,
                            MimeTypes.APPLICATION_TTML,
                            MimeTypes.APPLICATION_MP4VTT,
                            MimeTypes.APPLICATION_SUBRIP,
                        )
                    )
                }
            }
        }
    }

    val showBufferingOverlay = playerWantsToPlay &&
        !isPlaying &&
        (playerPlaybackState == Player.STATE_BUFFERING || playerPlaybackState == Player.STATE_IDLE)

    BackHandler {
        if (sourceErrorDialogEffect != null) {
            sourceErrorDialogEffect = null
        } else if (subtitleSyncPanelVisible) {
            subtitleSyncPanelVisible = false
        } else if (runtimeTrackPanelVisible) {
            runtimeTrackPanelVisible = false
        } else if (activePanel == TvPlayerSidePanel.Subtitles &&
            tvPlayerScreenViewModel.onSubtitlesSidePanelBackPressed()
        ) {
            Unit
        } else if (activePanel != TvPlayerSidePanel.None) {
            tvPlayerScreenViewModel.closePanel()
        } else if (showBufferingOverlay) {
            onBackPressed()
        } else if (controlsVisible && playerWantsToPlay) {
            controlsVisible = false
        } else {
            onBackPressed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                registerControlsInteraction()

                if (hasSidePanel) {
                    return@onPreviewKeyEvent false
                }

                when (event.key) {
                    Key.DirectionCenter,
                    Key.Enter,
                    Key.NumPadEnter -> {
                        if (!controlsVisible) {
                            exoPlayer.pause()
                            controlsVisible = true
                            true
                        } else {
                            false
                        }
                    }

                    Key.DirectionUp,
                    Key.DirectionDown -> {
                        if (!controlsVisible) {
                            controlsVisible = true
                            true
                        } else {
                            false
                        }
                    }

                    Key.DirectionLeft -> {
                        if (!controlsVisible) {
                            seekBy(-seekPreferencesState.seekWhenControlsHiddenMs)
                            true
                        } else {
                            false
                        }
                    }

                    Key.DirectionRight -> {
                        if (!controlsVisible) {
                            seekBy(seekPreferencesState.seekWhenControlsHiddenMs)
                            true
                        } else {
                            false
                        }
                    }

                    else -> false
                }
            }
    ) {
        TvPlayerVideoSurface(
            player = exoPlayer,
            resizeMode = playerResizeMode.resizeMode,
            subtitleSyncController = subtitleSyncController,
            modifier = Modifier.fillMaxSize(),
        )

        if (showBufferingOverlay) {
            BufferingOverlay()
        }

        PlaybackControlsLayer(
            visible = controlsVisible,
            controlsEnabled = !hasSidePanel,
            metadata = state.metadata,
            link = state.link,
            isPlaying = isPlaying,
            showTracksButton = showRuntimeTracksButton,
            showSyncButton = state.selectedSubtitleIndex >= 0,
            showNextEpisodeButton = state.metadata.isEpisodeBased,
            playPauseFocusRequester = playPauseFocusRequester,
            timelineFocusRequester = timelineFocusRequester,
            exoPlayer = exoPlayer,
            onPlaybackProgress = tvPlayerScreenViewModel::onPlaybackProgress,
            onControlsEvent = { event ->
                registerControlsInteraction()
                when (event) {
                    TvPlayerControlsEvent.PlayPause -> {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.playWhenReady = true
                            exoPlayer.play()
                        }
                    }
                    TvPlayerControlsEvent.OpenSources -> {
                        tvPlayerScreenViewModel.openPanel(TvPlayerSidePanel.Sources)
                    }
                    TvPlayerControlsEvent.OpenSubtitles -> {
                        tvPlayerScreenViewModel.openPanel(TvPlayerSidePanel.Subtitles)
                    }
                    TvPlayerControlsEvent.OpenTracks -> {
                        runtimeTrackPanelVisible = runtimeAudioTracks.size > 1
                    }
                    TvPlayerControlsEvent.SyncSubtitles -> {
                        subtitleSyncDebugLog(
                            "open sync panel: selectedSubtitleIndex=${state.selectedSubtitleIndex}" +
                                " subtitleId=${selectedSubtitleId ?: "null"}" +
                                " subtitleName=${selectedSubtitle?.name ?: "null"}" +
                                " mime=${selectedSubtitle?.mimeType ?: "null"}" +
                                " language=${selectedSubtitle?.languageCode ?: "null"}" +
                                " playerPosMs=${exoPlayer.currentPosition.coerceAtLeast(0L)}",
                        )
                        subtitleSyncController.logDebugSnapshot(
                            player = exoPlayer,
                            reason = "open_panel",
                            subtitleId = selectedSubtitleId,
                            subtitleLabel = selectedSubtitle?.name,
                            subtitleMimeType = selectedSubtitle?.mimeType,
                        )
                        subtitleSyncPanelVisible = true
                    }
                    TvPlayerControlsEvent.ToggleResizeMode -> {
                        toggleResizeMode()
                    }
                    TvPlayerControlsEvent.Restart -> {
                        exoPlayer.seekTo(0L)
                        exoPlayer.playWhenReady = true
                        exoPlayer.play()
                    }
                    TvPlayerControlsEvent.NextEpisode -> Unit
                    TvPlayerControlsEvent.SeekBackward -> {
                        seekBy(-seekPreferencesState.seekWhenControlsVisibleMs)
                    }
                    TvPlayerControlsEvent.SeekForward -> {
                        seekBy(seekPreferencesState.seekWhenControlsVisibleMs)
                    }
                }
            },
        )

        if (activePanel != TvPlayerSidePanel.None) {
            PlayerSidePanels(
                panels = state.panels,
                onCloseRequested = tvPlayerScreenViewModel::closePanel,
                onSubtitlesBackRequested = tvPlayerScreenViewModel::onSubtitlesSidePanelBackPressed,
                onItemAction = { action ->
                    registerControlsInteraction()
                    if (action.requiresPlaybackRestore()) {
                        pendingSeekPositionMs = exoPlayer.currentPosition
                        pendingPlayWhenReady = exoPlayer.playWhenReady
                    }
                    tvPlayerScreenViewModel.onPanelItemAction(action)
                },
            )
        }

        if (runtimeTrackPanelVisible) {
            RuntimeAudioTracksSidePanel(
                tracks = runtimeAudioTracks,
                selectedTrackId = runtimeSelectedAudioTrackId,
                onCloseRequested = {
                    runtimeTrackPanelVisible = false
                },
                onSelectDefault = {
                    registerControlsInteraction()
                    selectRuntimeAudioTrack(null)
                    runtimeTrackPanelVisible = false
                },
                onSelectTrack = { track ->
                    registerControlsInteraction()
                    selectRuntimeAudioTrack(track)
                    runtimeTrackPanelVisible = false
                },
            )
        }

        if (subtitleSyncPanelVisible) {
            SubtitleSyncSidePanel(
                visible = true,
                player = exoPlayer,
                subtitleSyncController = subtitleSyncController,
                hasActiveSubtitleTrack = selectedSubtitle != null,
                onCloseRequested = {
                    subtitleSyncPanelVisible = false
                },
                onSubtitleDelayChanged = { updatedDelayMs ->
                    selectedSubtitleId?.let { subtitleId ->
                        subtitleDelayByTrackId[subtitleId] = updatedDelayMs
                    }
                },
            )
        }

        sourceErrorDialogEffect?.let { effect ->
            SourceErrorDialog(
                state = effect.dialog,
                onDismiss = {
                    sourceErrorDialogEffect = null
                },
                onRetry = {
                    sourceErrorDialogEffect = null
                    pendingSeekPositionMs = exoPlayer.currentPosition
                    pendingPlayWhenReady = exoPlayer.playWhenReady
                    tvPlayerScreenViewModel.retrySource(effect.dialog.sourceIndex)
                },
            )
        }
    }
}

@Composable
private fun SourceErrorDialog(
    state: TvPlayerSourceErrorDialog,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = androidx.tv.material3.ShapeDefaults.Large,
            colors = SurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
            ),
            modifier = Modifier.widthIn(max = 700.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.source_error),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = state.sourceLabel,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.go_back),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.tv_player_retry),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSidePanels(
    panels: TvPlayerPanelsUiState,
    onCloseRequested: () -> Unit,
    onSubtitlesBackRequested: () -> Boolean,
    onItemAction: (TvPlayerPanelItemAction) -> Unit,
) {
    when (panels.activePanel) {
        TvPlayerSidePanel.None -> Unit
        TvPlayerSidePanel.Sources -> {
            MenuListSidePanel(
                visible = true,
                onCloseRequested = onCloseRequested,
                title = stringResource(R.string.sources),
                items = panels.sourceItems,
                showSelectionRadio = true,
                initialFocusedItemId = panels.sourceInitialFocusedItemId,
                closeOnLeftPress = false,
                closeOnFocusExit = false,
                onActionTokenClick = { token ->
                    (token as? TvPlayerPanelItemAction)?.let(onItemAction)
                },
                onDirectionalActionToken = { token ->
                    (token as? TvPlayerPanelItemAction)?.let(onItemAction)
                },
            )
        }
        TvPlayerSidePanel.Subtitles -> {
            MenuListSidePanel(
                visible = true,
                onCloseRequested = {
                    if (!onSubtitlesBackRequested()) {
                        onCloseRequested()
                    }
                },
                title = when (panels.subtitlePanelScreen) {
                    TvPlayerSubtitlePanelScreen.Main -> stringResource(R.string.player_subtitles_settings)
                    TvPlayerSubtitlePanelScreen.OnlineSearch -> stringResource(R.string.player_load_subtitles_online)
                    TvPlayerSubtitlePanelScreen.OnlineLanguageSelection -> stringResource(R.string.subs_subtitle_languages)
                },
                items = when (panels.subtitlePanelScreen) {
                    TvPlayerSubtitlePanelScreen.Main -> panels.subtitleItems
                    TvPlayerSubtitlePanelScreen.OnlineSearch,
                    TvPlayerSubtitlePanelScreen.OnlineLanguageSelection -> panels.subtitleOnlineItems
                },
                showSelectionRadio = false,
                initialFocusedItemId = when (panels.subtitlePanelScreen) {
                    TvPlayerSubtitlePanelScreen.Main -> panels.subtitleInitialFocusedItemId
                    TvPlayerSubtitlePanelScreen.OnlineSearch,
                    TvPlayerSubtitlePanelScreen.OnlineLanguageSelection -> panels.subtitleOnlineInitialFocusedItemId
                },
                contentAnimationKey = panels.subtitlePanelScreen,
                contentNavigationDirection = when (panels.subtitlePanelNavigationDirection) {
                    TvPlayerSubtitlePanelNavigationDirection.Forward -> SidePanelContentNavigationDirection.Forward
                    TvPlayerSubtitlePanelNavigationDirection.Backward -> SidePanelContentNavigationDirection.Backward
                },
                closeOnLeftPress = false,
                closeOnFocusExit = false,
                onActionTokenClick = { token ->
                    (token as? TvPlayerPanelItemAction)?.let(onItemAction)
                },
                onDirectionalActionToken = { token ->
                    (token as? TvPlayerPanelItemAction)?.let(onItemAction)
                },
                onInlineTextFieldValueChanged = { token, value ->
                    if (token == TvPlayerPanelItemAction.EditOnlineSubtitlesQuery) {
                        onItemAction(
                            TvPlayerPanelItemAction.UpdateOnlineSubtitlesQuery(
                                query = value,
                            )
                        )
                    }
                },
            )
        }
        TvPlayerSidePanel.Tracks -> {
            MenuListSidePanel(
                visible = true,
                onCloseRequested = onCloseRequested,
                title = stringResource(R.string.audio_tracks),
                items = panels.trackItems,
                showSelectionRadio = true,
                initialFocusedItemId = panels.trackInitialFocusedItemId,
                closeOnLeftPress = false,
                closeOnFocusExit = false,
                onActionTokenClick = { token ->
                    (token as? TvPlayerPanelItemAction)?.let(onItemAction)
                },
                onDirectionalActionToken = { token ->
                    (token as? TvPlayerPanelItemAction)?.let(onItemAction)
                },
            )
        }
    }
}

@Composable
private fun RuntimeAudioTracksSidePanel(
    tracks: List<PlayerAudioTrackOption>,
    selectedTrackId: String?,
    onCloseRequested: () -> Unit,
    onSelectDefault: () -> Unit,
    onSelectTrack: (PlayerAudioTrackOption) -> Unit,
) {
    val defaultItemId = "runtime_track_default"
    val defaultTitle = stringResource(R.string.action_default)
    val items = remember(defaultTitle, tracks, selectedTrackId) {
        buildList {
            add(
                SidePanelMenuItem(
                    id = defaultItemId,
                    title = defaultTitle,
                    selected = selectedTrackId == null,
                    onClick = onSelectDefault,
                )
            )
            tracks.forEach { track ->
                add(
                    SidePanelMenuItem(
                        id = track.selectionId,
                        title = track.title,
                        supportingTexts = track.supportingTexts,
                        selected = selectedTrackId == track.selectionId,
                        onClick = {
                            onSelectTrack(track)
                        },
                    )
                )
            }
        }
    }
    val initialFocusedItemId = selectedTrackId?.takeIf { selectedId ->
        items.any { item -> item.id == selectedId }
    } ?: defaultItemId

    MenuListSidePanel(
        visible = true,
        onCloseRequested = onCloseRequested,
        title = stringResource(R.string.audio_tracks),
        items = items,
        showSelectionRadio = true,
        initialFocusedItemId = initialFocusedItemId,
        closeOnLeftPress = false,
        closeOnFocusExit = false,
    )
}

private fun TvPlayerPanelItemAction.requiresPlaybackRestore(): Boolean {
    return when (this) {
        is TvPlayerPanelItemAction.SelectSource,
        TvPlayerPanelItemAction.DisableSubtitles,
        is TvPlayerPanelItemAction.SelectSubtitle,
        is TvPlayerPanelItemAction.SelectOnlineSubtitleResult,
        TvPlayerPanelItemAction.LoadFirstAvailableSubtitle,
        TvPlayerPanelItemAction.SelectDefaultTrack,
        is TvPlayerPanelItemAction.SelectTrack -> true
        else -> false
    }
}

private fun PlaybackException.toTvPlayerPlaybackErrorDetails(): TvPlayerPlaybackErrorDetails {
    return TvPlayerPlaybackErrorDetails(
        exoErrorCode = errorCode,
        exoErrorName = errorCodeName,
        httpCode = findHttpStatusCode(),
    )
}

private fun PlaybackException.findHttpStatusCode(): Int? {
    var current: Throwable? = this
    while (current != null) {
        if (current is HttpDataSource.InvalidResponseCodeException) {
            return current.responseCode
        }
        current = current.cause
    }
    return null
}

private data class PlayerAudioTrackOption(
    val selectionId: String,
    val title: String,
    val supportingTexts: List<String>,
    val language: String?,
    val trackGroup: androidx.media3.common.TrackGroup,
    val trackIndex: Int,
    val isSelected: Boolean,
)

private data class RuntimeAudioTrackLabel(
    val title: String,
    val supportingTexts: List<String>,
)

private fun extractRuntimeAudioTracks(tracks: Tracks): List<PlayerAudioTrackOption> {
    val runtimeTracks = mutableListOf<PlayerAudioTrackOption>()
    var ordinal = 0
    tracks.groups.forEachIndexed { groupIndex, group ->
        if (group.type != C.TRACK_TYPE_AUDIO) {
            return@forEachIndexed
        }
        val trackGroup = group.mediaTrackGroup
        for (trackIndex in 0 until trackGroup.length) {
            if (!group.isTrackSupported(trackIndex)) {
                continue
            }
            val format = trackGroup.getFormat(trackIndex)
            val selectionId = "runtime_track_${groupIndex}_${trackIndex}_${format.id.orEmpty()}"
            val formattedLabel = formatRuntimeAudioTrackLabel(index = ordinal, format = format)
            runtimeTracks += PlayerAudioTrackOption(
                selectionId = selectionId,
                title = formattedLabel.title,
                supportingTexts = formattedLabel.supportingTexts,
                language = format.language,
                trackGroup = trackGroup,
                trackIndex = trackIndex,
                isSelected = group.isTrackSelected(trackIndex),
            )
            ordinal += 1
        }
    }
    return runtimeTracks
}

private fun formatRuntimeAudioTrackLabel(
    index: Int,
    format: Format,
): RuntimeAudioTrackLabel {
    val languageLabel = format.language
        ?.takeIf { it.isNotBlank() && !it.equals("und", ignoreCase = true) }
        ?.let(::languageDisplayName)
        ?: format.label?.takeIf { it.isNotBlank() }
        ?: "Audio ${index + 1}"
    val codecLabel = format.sampleMimeType
        ?.substringAfter('/')
        ?.uppercase(Locale.ROOT)
    val channelsLabel = when (format.channelCount) {
        1 -> "MONO"
        2 -> "STEREO"
        6 -> "5.1"
        8 -> "7.1"
        else -> format.channelCount.takeIf { it > 0 }?.let { "$it CH" }
    }
    return RuntimeAudioTrackLabel(
        title = languageLabel,
        supportingTexts = listOfNotNull(codecLabel, channelsLabel),
    )
}

private fun applyRuntimeAudioTrackSelection(
    player: ExoPlayer,
    track: PlayerAudioTrackOption?,
) {
    val newParameters = player.trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        .apply {
            if (track != null) {
                setOverrideForType(TrackSelectionOverride(track.trackGroup, track.trackIndex))
                track.language?.takeIf { it.isNotBlank() }?.let(::setPreferredAudioLanguage)
            } else {
                setPreferredAudioLanguage(null)
            }
        }
        .build()
    player.trackSelectionParameters = newParameters
}

private fun languageDisplayName(languageTag: String): String {
    val locale = Locale.forLanguageTag(languageTag)
    val languageCode = locale.language
    if (languageCode.isNullOrBlank()) {
        return languageTag
    }
    val localized = locale.getDisplayLanguage(Locale.getDefault())
    return localized.takeIf { it.isNotBlank() } ?: languageTag
}

private fun subtitleSyncDebugLog(message: String) {
    Log.i(SubtitleSyncDebugTag, message)
}

private fun createTvPlayerRenderersFactory(
    context: Context,
    subtitleSyncController: TvPlayerSubtitleSyncController,
): RenderersFactory {
    val baseFactory = runCatching {
        NextRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        }
    }.getOrElse {
        DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
        }
    }

    return RenderersFactory { eventHandler, videoRendererEventListener, audioRendererEventListener, textRendererOutput, metadataRendererOutput ->
        val style = com.lagradost.cloudstream3.ui.player.CustomDecoder.style
        val deduplicatedTexts = HashSet<CharSequence>()
        val combinedTextBuffer = StringBuilder()
        val styledTextOutput = TextOutput { cueGroup ->
            val cues = cueGroup.cues.filterNotNull()
            if (cues.isEmpty()) {
                if (!subtitleSyncController.dispatchStyledCues(emptyList())) {
                    textRendererOutput.onCues(cueGroup)
                }
                return@TextOutput
            }

            val (bitmapCues, textCues) = cues.partition { cue -> cue.bitmap != null }
            val styledBitmapCues = bitmapCues.map { bitmapCue ->
                bitmapCue
                    .buildUpon()
                    .fixSubtitleAlignment()
                    .applyStyle(style)
                    .build()
            }
            val styledTextCues = textCues.groupBy { textCue ->
                textCue.lineAnchor to textCue.position.times(1000.0f).toInt()
            }.mapNotNull { (_, entries) ->
                deduplicatedTexts.clear()
                combinedTextBuffer.clear()
                var lineCount = 0
                for (entry in entries) {
                    val text = entry.text ?: continue
                    if (!deduplicatedTexts.add(text)) continue
                    if (++lineCount > 1) {
                        combinedTextBuffer.append('\n')
                    }
                    combinedTextBuffer.append(text.trim())
                }
                entries.firstOrNull()
                    ?.buildUpon()
                    ?.setText(combinedTextBuffer.toString())
                    ?.fixSubtitleAlignment()
                    ?.applyStyle(style)
                    ?.build()
            }
            val combinedCues = styledBitmapCues + styledTextCues
            if (!subtitleSyncController.dispatchStyledCues(combinedCues)) {
                textRendererOutput.onCues(
                    CueGroup(
                        combinedCues,
                        cueGroup.presentationTimeUs,
                    )
                )
            }
        }
        baseFactory.createRenderers(
            eventHandler,
            videoRendererEventListener,
            audioRendererEventListener,
            styledTextOutput,
            metadataRendererOutput,
        ).map { renderer ->
            if (renderer is TextRenderer) {
                subtitleSyncDebugLog(
                    "createRenderers: replacing TextRenderer with synced decoder factory",
                )
                val syncedRenderer = TextRenderer(
                    styledTextOutput,
                    eventHandler.looper,
                    subtitleSyncController.subtitleDecoderFactory(),
                ).apply {
                    // WHY: parity z legacy playerem (CS3IPlayer) - bez tego część starszych
                    // formatów napisów nie przechodzi ścieżki dekodera kompatybilnej z offsetem.
                    @Suppress("DEPRECATION")
                    experimentalSetLegacyDecodingEnabled(true)
                }
                subtitleSyncController.attachTextRenderer(syncedRenderer)
                syncedRenderer
            } else {
                renderer
            }
        }.toTypedArray()
    }
}

private fun createPlayer(
    context: Context,
    link: ExtractorLink,
    subtitle: com.lagradost.cloudstream3.ui.player.SubtitleData?,
    audioTracks: List<com.lagradost.cloudstream3.AudioFile>,
    subtitleDelayMs: Long,
    startPositionMs: Long,
    startPlayWhenReady: Boolean,
): TvPlayerInstance {
    val subtitleSyncController = TvPlayerSubtitleSyncController(
        initialSubtitleDelayMs = subtitleDelayMs,
    )
    val subtitleConfiguration = subtitle?.let { subtitleData ->
        MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitleData.getFixedUrl()))
            .setMimeType(subtitleData.mimeType)
            .setLabel(subtitleData.name)
            .setLanguage(subtitleData.languageCode)
            .setId(subtitleData.getId())
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
    }
    subtitleSyncDebugLog(
        "createPlayer: subtitle=${if (subtitle == null) "null" else "id=${subtitle.getId()} name=${subtitle.name} mime=${subtitle.mimeType} lang=${subtitle.languageCode}"}" +
            " initialDelayMs=$subtitleDelayMs startPositionMs=$startPositionMs startPlayWhenReady=$startPlayWhenReady",
    )
    val extractorFactory = UpdatedDefaultExtractorsFactory()
        .setFragmentedMp4ExtractorFlags(FragmentedMp4Extractor.FLAG_MERGE_FRAGMENTED_SIDX)
    val videoDataSourceFactory = createDataSourceFactory(
        context = context,
        link = link,
    )
    val videoMediaSourceFactory = DefaultMediaSourceFactory(videoDataSourceFactory, extractorFactory)
        // WHY: parity z legacy subtitle sync.
        // Przy wartości `true` Media3 parsuje napisy podczas ekstrakcji i przekazuje CuesWithTiming,
        // omijając CustomSubtitleDecoderFactory (brak listy cue do panelu synchronizacji).
        .experimentalParseSubtitlesDuringExtraction(false)

    val videoMediaItem = MediaItem.Builder()
        .setUri(link.url)
        .build()
    val videoMediaSource = videoMediaSourceFactory.createMediaSource(videoMediaItem)
    val subtitleMediaSource = if (subtitle != null && subtitleConfiguration != null) {
        val subtitleDataSourceFactory = createDataSourceFactory(
            context = context,
            link = link,
            globalExtraHeaders = subtitle.headers,
        )
        subtitleSyncDebugLog(
            "createPlayer: creating subtitle source id=${subtitle.getId()} uri=${subtitle.getFixedUrl()}",
        )
        SingleSampleMediaSource.Factory(subtitleDataSourceFactory)
            .createMediaSource(subtitleConfiguration, C.TIME_UNSET)
    } else {
        null
    }

    val externalAudioSources = audioTracks.mapIndexedNotNull { index, audioTrack ->
        runCatching {
            val audioDataSourceFactory = createDataSourceFactory(
                context = context,
                link = link,
                globalExtraHeaders = audioTrack.headers.orEmpty(),
            )
            val audioMediaItem = MediaItem.Builder()
                .setUri(audioTrack.url)
                .setMimeType(MimeTypes.AUDIO_UNKNOWN)
                .setMediaId("external_audio_$index")
                .build()
            val audioMediaSource = DefaultMediaSourceFactory(audioDataSourceFactory, extractorFactory)
                .createMediaSource(audioMediaItem)
            audioMediaSource
        }.getOrNull()
    }

    val mediaSource = run {
        val allSources = ArrayList<MediaSource>(externalAudioSources.size + 2)
        allSources.add(videoMediaSource)
        subtitleMediaSource?.let(allSources::add)
        allSources.addAll(externalAudioSources)
        if (allSources.size == 1) {
            videoMediaSource
        } else {
            MergingMediaSource(*allSources.toTypedArray())
        }
    }

    val trackSelector = DefaultTrackSelector(context).apply {
        parameters = buildUponParameters()
            .setPreferredAudioLanguage(null)
            .build()
    }

    val exoPlayer = ExoPlayer.Builder(context)
        .setRenderersFactory(
            createTvPlayerRenderersFactory(
                context = context,
                subtitleSyncController = subtitleSyncController,
            )
        )
        .setTrackSelector(trackSelector)
        .setMediaSourceFactory(videoMediaSourceFactory)
        .build()
        .apply {
            setMediaSource(mediaSource, startPositionMs.coerceAtLeast(0L))
            prepare()
            playWhenReady = startPlayWhenReady
        }
    return TvPlayerInstance(
        player = exoPlayer,
        subtitleSyncController = subtitleSyncController,
    )
}

private fun createHttpDataSourceFactory(
    link: ExtractorLink,
    extraHeaders: Map<String, String> = emptyMap(),
): HttpDataSource.Factory {
    val provider = APIHolder.getApiFromNameNull(link.source)
    val interceptor = provider?.getVideoInterceptor(link)
    val client = if (interceptor == null) {
        app.baseClient
    } else {
        app.baseClient.newBuilder()
            .addInterceptor(interceptor)
            .build()
    }

    val userAgent = extraHeaders.entries.firstOrNull { (key, _) ->
        key.equals("User-Agent", ignoreCase = true)
    }?.value ?: link.headers.entries.firstOrNull { (key, _) ->
        key.equals("User-Agent", ignoreCase = true)
    }?.value ?: USER_AGENT

    val refererMap = if (link.referer.isBlank()) {
        emptyMap()
    } else {
        mapOf("referer" to link.referer)
    }

    val headers = mapOf(
        "accept" to "*/*",
        "sec-ch-ua" to "\"Chromium\";v=\"91\", \" Not;A Brand\";v=\"99\"",
        "sec-ch-ua-mobile" to "?0",
        "sec-fetch-user" to "?1",
        "sec-fetch-mode" to "navigate",
        "sec-fetch-dest" to "video",
    ) + refererMap + link.getAllHeaders() + extraHeaders

    return OkHttpDataSource.Factory(client)
        .setUserAgent(userAgent)
        .apply {
            setDefaultRequestProperties(headers)
        }
}

private fun createDataSourceFactory(
    context: Context,
    link: ExtractorLink,
    globalExtraHeaders: Map<String, String> = emptyMap(),
    perUriExtraHeaders: Map<String, Map<String, String>> = emptyMap(),
): DataSource.Factory {
    val defaultFactory = DefaultDataSource.Factory(
        context,
        createHttpDataSourceFactory(
            link = link,
            extraHeaders = globalExtraHeaders,
        )
    )
    if (perUriExtraHeaders.isEmpty()) {
        return defaultFactory
    }

    return ResolvingDataSource.Factory(defaultFactory) { dataSpec ->
        val extraHeaders = perUriExtraHeaders[dataSpec.uri.toString()]
        if (extraHeaders.isNullOrEmpty()) {
            dataSpec
        } else {
            dataSpec.withAdditionalHeaders(extraHeaders)
        }
    }
}

private fun seekPlayerBy(player: ExoPlayer, deltaMs: Long) {
    val current = player.currentPosition.coerceAtLeast(0L)
    seekPlayerTo(player = player, positionMs = current + deltaMs)
}

private fun seekPlayerTo(player: ExoPlayer, positionMs: Long) {
    val target = positionMs.coerceAtLeast(0L)
    val duration = player.duration
    val clamped = if (duration == C.TIME_UNSET || duration < 0L) {
        target
    } else {
        target.coerceAtMost(duration)
    }
    player.seekTo(clamped)
}

private suspend fun requestFocusWithRetry(
    focusRequester: FocusRequester,
): Boolean {
    repeat(FocusRequestRetryCount) {
        if (focusRequester.requestFocus()) {
            return true
        }
        delay(FocusRequestRetryDelayMs)
    }
    return false
}
