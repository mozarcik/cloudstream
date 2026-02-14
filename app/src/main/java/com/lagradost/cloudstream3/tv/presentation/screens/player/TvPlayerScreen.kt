package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.content.Context
import android.os.SystemClock
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.tv.presentation.common.MenuListSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DotSeparatedRow
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.utils.DrmExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.coroutines.delay
import java.util.Locale

object TvPlayerScreen {
    const val UrlBundleKey = "url"
    const val ApiNameBundleKey = "apiName"
    const val EpisodeDataBundleKey = "episodeData"
}

@Composable
fun TvPlayerScreen(
    onBackPressed: () -> Unit,
    tvPlayerScreenViewModel: TvPlayerScreenViewModel,
) {
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
                onBackPressed = onBackPressed,
                onPlaybackError = tvPlayerScreenViewModel::onPlaybackError,
                onSourceSelected = tvPlayerScreenViewModel::selectSource,
                onPlaybackProgress = tvPlayerScreenViewModel::onPlaybackProgress,
                onPlaybackStopped = tvPlayerScreenViewModel::onPlaybackStopped,
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

            if (state.canSkip) {
                Button(
                    onClick = onSkipLoading,
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

private enum class PlayerSidePanel {
    None,
    Sources,
    Subtitles,
    Tracks,
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
    onBackPressed: () -> Unit,
    onPlaybackError: () -> Unit,
    onSourceSelected: (Int) -> Unit,
    onPlaybackProgress: (Long, Long) -> Unit,
    onPlaybackStopped: (Long, Long) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
    }
    val seekWhenControlsHiddenMs = remember(preferences, context) {
        preferences.getInt(context.getString(R.string.android_tv_interface_off_seek_key), 10)
            .toLong() * 1000L
    }
    val seekWhenControlsVisibleMs = remember(preferences, context) {
        preferences.getInt(context.getString(R.string.android_tv_interface_on_seek_key), 30)
            .toLong() * 1000L
    }

    var selectedSubtitleIndex by remember(state.link.url) { mutableStateOf(-1) }
    var selectedAudioTrackIndex by remember(state.link.url) { mutableStateOf(-1) }
    var playerResizeMode by remember { mutableStateOf(PlayerResizeMode.Fit) }
    var sidePanel by remember { mutableStateOf(PlayerSidePanel.None) }
    var pendingSeekPositionMs by remember { mutableStateOf<Long?>(null) }
    var pendingPlayWhenReady by remember { mutableStateOf<Boolean?>(null) }

    val selectedSubtitle = state.subtitles.getOrNull(selectedSubtitleIndex)
    val selectedAudioTrack = state.link.audioTracks.getOrNull(selectedAudioTrackIndex)
    val expandedSubtitleGroups = remember(state.link.url) { mutableStateMapOf<String, Boolean>() }

    val exoPlayer = remember(
        state.link.url,
        selectedSubtitle?.getId(),
        selectedAudioTrack?.url,
        selectedAudioTrack?.headers,
    ) {
        createPlayer(
            context = context,
            link = state.link,
            subtitle = selectedSubtitle,
            audioTrack = selectedAudioTrack,
        )
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var playerPlaybackState by remember { mutableStateOf(exoPlayer.playbackState) }
    var playerWantsToPlay by remember { mutableStateOf(exoPlayer.playWhenReady) }
    var controlsInteractionTick by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var errorHandled by remember(state.link.url) { mutableStateOf(false) }
    var hasAppliedInitialResume by remember(state.episodeId) { mutableStateOf(false) }

    val rootFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val timelineFocusRequester = remember { FocusRequester() }

    val seekBy: (Long) -> Unit = { deltaMs ->
        seekPlayerBy(exoPlayer, deltaMs)
    }
    val registerControlsInteraction: () -> Unit = {
        controlsInteractionTick = SystemClock.elapsedRealtime()
    }
    val toggleResizeMode: () -> Unit = {
        registerControlsInteraction()
        playerResizeMode = playerResizeMode.next()
        Toast.makeText(
            context,
            context.getString(playerResizeMode.labelResId),
            Toast.LENGTH_SHORT,
        ).show()
    }
    val hasSidePanel = sidePanel != PlayerSidePanel.None

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                playerPlaybackState = playbackState
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                playerWantsToPlay = playWhenReady
            }

            override fun onPlayerError(error: PlaybackException) {
                if (errorHandled) return
                controlsVisible = true
                val currentPosition = exoPlayer.currentPosition
                val currentPlayWhenReady = exoPlayer.playWhenReady

                // If subtitle loading/parsing fails, keep current source and fall back to "no subtitles".
                if (selectedSubtitleIndex >= 0) {
                    pendingSeekPositionMs = currentPosition
                    pendingPlayWhenReady = currentPlayWhenReady
                    selectedSubtitleIndex = -1
                    Toast.makeText(context, context.getString(R.string.no_subtitles), Toast.LENGTH_SHORT).show()
                    return
                }

                errorHandled = true
                pendingSeekPositionMs = currentPosition
                pendingPlayWhenReady = currentPlayWhenReady
                onPlaybackError()
            }
        }

        exoPlayer.addListener(listener)
        onDispose {
            val rawDuration = exoPlayer.duration
            val finalDurationMs = if (rawDuration == C.TIME_UNSET || rawDuration < 0L) 0L else rawDuration
            onPlaybackStopped(exoPlayer.currentPosition.coerceAtLeast(0L), finalDurationMs)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(state.link.url, state.link.extractorData, state.link.source) {
        runCatching {
            APIHolder.getApiFromNameNull(state.link.source)?.extractorVerifierJob(state.link.extractorData)
        }
    }

    LaunchedEffect(exoPlayer) {
        if (!hasAppliedInitialResume && state.resumePositionMs > 0L && pendingSeekPositionMs == null) {
            seekPlayerTo(exoPlayer, state.resumePositionMs)
            hasAppliedInitialResume = true
        }

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

        while (true) {
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            val rawDuration = exoPlayer.duration
            durationMs = if (rawDuration == C.TIME_UNSET || rawDuration < 0L) 0L else rawDuration
            onPlaybackProgress(positionMs, durationMs)
            delay(250L)
        }
    }

    LaunchedEffect(controlsVisible, sidePanel) {
        if (controlsVisible && sidePanel == PlayerSidePanel.None) {
            playPauseFocusRequester.requestFocus()
        } else if (!controlsVisible) {
            rootFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(controlsVisible, playerWantsToPlay, sidePanel, controlsInteractionTick) {
        if (controlsVisible && playerWantsToPlay && sidePanel == PlayerSidePanel.None) {
            delay(4_500L)
            controlsVisible = false
        }
    }

    LaunchedEffect(playerWantsToPlay) {
        if (!playerWantsToPlay) {
            controlsVisible = true
        }
    }

    BackHandler {
        if (sidePanel != PlayerSidePanel.None) {
            sidePanel = PlayerSidePanel.None
        } else if (controlsVisible && playerWantsToPlay) {
            controlsVisible = false
        } else {
            onBackPressed()
        }
    }

    val progressFraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val showBufferingOverlay = playerWantsToPlay &&
        (playerPlaybackState == Player.STATE_BUFFERING || playerPlaybackState == Player.STATE_IDLE)

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
                            seekBy(-seekWhenControlsHiddenMs)
                            true
                        } else {
                            false
                        }
                    }

                    Key.DirectionRight -> {
                        if (!controlsVisible) {
                            seekBy(seekWhenControlsHiddenMs)
                            true
                        } else {
                            false
                        }
                    }

                    else -> false
                }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    player = exoPlayer
                    useController = false
                    setShutterBackgroundColor("#000000".toColorInt())
                    setBackgroundColor("#000000".toColorInt())
                    resizeMode = playerResizeMode.resizeMode
                }
            },
            update = { view ->
                view.player = exoPlayer
                view.resizeMode = playerResizeMode.resizeMode
            },
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 170)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 220),
                    initialOffsetY = { fullHeight -> fullHeight / 3 }
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
                slideOutVertically(
                    animationSpec = tween(durationMillis = 180),
                    targetOffsetY = { fullHeight -> fullHeight / 3 }
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.82f),
                            )
                        )
                    )
            ) {
                PlayerOverlay(
                    metadata = state.metadata,
                    link = state.link,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    progressFraction = progressFraction,
                    isPlaying = isPlaying,
                    showTracksButton = state.link.audioTracks.isNotEmpty(),
                    showNextEpisodeButton = state.metadata.isEpisodeBased,
                    playPauseFocusRequester = playPauseFocusRequester,
                    timelineFocusRequester = timelineFocusRequester,
                    onPlayPause = {
                        registerControlsInteraction()
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.playWhenReady = true
                            exoPlayer.play()
                        }
                    },
                    onOpenSources = {
                        registerControlsInteraction()
                        sidePanel = PlayerSidePanel.Sources
                    },
                    onOpenSubtitles = {
                        registerControlsInteraction()
                        sidePanel = PlayerSidePanel.Subtitles
                    },
                    onOpenTracks = {
                        registerControlsInteraction()
                        sidePanel = PlayerSidePanel.Tracks
                    },
                    onSyncSubtitles = {
                        registerControlsInteraction()
                    },
                    onToggleResize = toggleResizeMode,
                    onRestart = {
                        registerControlsInteraction()
                        exoPlayer.seekTo(0L)
                        exoPlayer.playWhenReady = true
                        exoPlayer.play()
                    },
                    onNextEpisode = {
                        registerControlsInteraction()
                    },
                    onSeekBackward = {
                        registerControlsInteraction()
                        seekBy(-seekWhenControlsVisibleMs)
                    },
                    onSeekForward = {
                        registerControlsInteraction()
                        seekBy(seekWhenControlsVisibleMs)
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = showBufferingOverlay,
            enter = fadeIn(animationSpec = tween(durationMillis = 150)),
            exit = fadeOut(animationSpec = tween(durationMillis = 150)),
        ) {
            BufferingOverlay()
        }

        val unknownQualityLabel = stringResource(R.string.tv_grid_rating_unknown_short)
        val sourceMenuItems = buildList {
            state.sources
                .withIndex()
                .groupBy { indexedLink ->
                    sourceQualityHeaderLabel(
                        link = indexedLink.value,
                        unknownQualityLabel = unknownQualityLabel,
                    )
                }
                .forEach { (qualityHeader, indexedLinks) ->
                    add(
                        SidePanelMenuItem(
                            id = "source_header_${qualityHeader.replace(" ", "_")}",
                            title = qualityHeader,
                            isSectionHeader = true,
                            enabled = false,
                            onClick = { },
                        )
                    )

                    indexedLinks.forEach { indexedLink ->
                        val index = indexedLink.index
                        val link = indexedLink.value
                        val sourceDetailsTexts = formatSourceDetailsTexts(
                            link = link,
                            unknownQualityLabel = unknownQualityLabel,
                        )
                        add(
                            SidePanelMenuItem(
                                id = "source_$index",
                                title = link.name.ifBlank { formatSourceMenuLabel(link) },
                                titleMaxLines = 2,
                                selected = index == state.currentSourceIndex,
                                supportingContent = sourceDetailsTexts.takeIf { it.isNotEmpty() }?.let { details ->
                                    {
                                        DotSeparatedRow(
                                            texts = details,
                                            textStyle = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                            ),
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                },
                                onClick = {
                                    registerControlsInteraction()
                                    pendingSeekPositionMs = exoPlayer.currentPosition
                                    pendingPlayWhenReady = exoPlayer.playWhenReady
                                    sidePanel = PlayerSidePanel.None
                                    if (index != state.currentSourceIndex) {
                                        onSourceSelected(index)
                                    }
                                }
                            )
                        )
                    }
                }
        }
        MenuListSidePanel(
            visible = sidePanel == PlayerSidePanel.Sources,
            onCloseRequested = { sidePanel = PlayerSidePanel.None },
            title = stringResource(R.string.sources),
            items = sourceMenuItems,
            showSelectionRadio = true,
            initialFocusedItemId = sourceMenuItems.firstOrNull { item ->
                item.id == "source_${state.currentSourceIndex}" && !item.isSectionHeader
            }?.id ?: sourceMenuItems.firstOrNull { !it.isSectionHeader }?.id,
        )

        val selectedSubtitleGroupKey = selectedSubtitle?.let(::subtitleLanguageKey)
        val subtitleLanguageGroups = remember(state.subtitles, selectedSubtitleIndex) {
            buildSubtitleLanguageGroups(
                subtitles = state.subtitles,
                selectedSubtitleIndex = selectedSubtitleIndex,
            )
        }
        val subtitleMenuItems = buildList {
            add(
                SidePanelMenuItem(
                    id = "subtitle_none",
                    title = stringResource(R.string.no_subtitles),
                    selected = selectedSubtitleIndex == -1,
                    onClick = {
                        registerControlsInteraction()
                        pendingSeekPositionMs = exoPlayer.currentPosition
                        pendingPlayWhenReady = exoPlayer.playWhenReady
                        selectedSubtitleIndex = -1
                        sidePanel = PlayerSidePanel.None
                    }
                )
            )

            subtitleLanguageGroups.forEach { group ->
                val groupItemId = subtitleGroupItemId(group.key)
                val headerId = "subtitle_lang_$groupItemId"
                val isExpanded = expandedSubtitleGroups[group.key] ?: (group.key == selectedSubtitleGroupKey)

                add(
                    SidePanelMenuItem(
                        id = headerId,
                        title = group.displayName,
                        titleTextStyle = MaterialTheme.typography.titleMedium,
                        selected = !isExpanded && group.items.any { indexedSubtitle ->
                            indexedSubtitle.index == selectedSubtitleIndex
                        },
                        trailingContent = {
                            Icon(
                                imageVector = if (isExpanded) {
                                    Icons.Default.KeyboardArrowDown
                                } else {
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                                },
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            registerControlsInteraction()
                            expandedSubtitleGroups[group.key] = !isExpanded
                        },
                        onKeyUp = { keyCode ->
                            when (keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                                android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                                    if (!isExpanded) {
                                        registerControlsInteraction()
                                        expandedSubtitleGroups[group.key] = true
                                        true
                                    } else {
                                        false
                                    }
                                }

                                android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                                android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                                    if (isExpanded) {
                                        registerControlsInteraction()
                                        expandedSubtitleGroups[group.key] = false
                                        true
                                    } else {
                                        false
                                    }
                                }

                                else -> false
                            }
                        },
                    )
                )

                group.items.forEachIndexed { itemIndex, indexedSubtitle ->
                    val index = indexedSubtitle.index
                    val subtitle = indexedSubtitle.value
                    val subtitleDetailsTexts = formatSubtitleDetailsTexts(subtitle)
                    val groupItemShape = when {
                        group.items.size == 1 -> RoundedCornerShape(10.dp)
                        itemIndex == 0 -> RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                        itemIndex == group.items.lastIndex -> RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                    add(
                        SidePanelMenuItem(
                            id = "subtitle_$index",
                            title = subtitle.name,
                            titleMaxLines = 2,
                            titleTextStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
                            selected = selectedSubtitleIndex == index,
                            isVisible = isExpanded,
                            animateVisibility = true,
                            itemBackgroundColor = Color(0xFF0A0A0B),
                            itemBackgroundShape = groupItemShape,
                            focusedItemShape = RoundedCornerShape(10.dp),
                            supportingContent = subtitleDetailsTexts.takeIf { it.isNotEmpty() }?.let { details ->
                                {
                                    DotSeparatedRow(
                                        texts = details,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Normal,
                                        ),
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = selectedSubtitleIndex == index,
                                    onClick = { },
                                )
                            },
                            onClick = {
                                registerControlsInteraction()
                                pendingSeekPositionMs = exoPlayer.currentPosition
                                pendingPlayWhenReady = exoPlayer.playWhenReady
                                selectedSubtitleIndex = index
                                sidePanel = PlayerSidePanel.None
                            }
                        )
                    )
                }
            }
        }
        val initialSubtitleFocusedItemId = when {
            selectedSubtitleIndex == -1 -> "subtitle_none"
            selectedSubtitleGroupKey == null -> subtitleMenuItems.firstOrNull { it.isVisible && !it.isSectionHeader }?.id
            else -> {
                val selectedGroupExpanded = expandedSubtitleGroups[selectedSubtitleGroupKey]
                    ?: true
                if (selectedGroupExpanded) {
                    "subtitle_$selectedSubtitleIndex"
                } else {
                    "subtitle_lang_${subtitleGroupItemId(selectedSubtitleGroupKey)}"
                }
            }
        }
        MenuListSidePanel(
            visible = sidePanel == PlayerSidePanel.Subtitles,
            onCloseRequested = { sidePanel = PlayerSidePanel.None },
            title = stringResource(R.string.player_subtitles_settings),
            items = subtitleMenuItems,
            showSelectionRadio = false,
            initialFocusedItemId = initialSubtitleFocusedItemId
                ?: subtitleMenuItems.firstOrNull { it.isVisible && !it.isSectionHeader }?.id,
        )

        val trackMenuItems = buildList {
            add(
                SidePanelMenuItem(
                    id = "track_default",
                    title = stringResource(R.string.action_default),
                    selected = selectedAudioTrackIndex == -1,
                    onClick = {
                        registerControlsInteraction()
                        pendingSeekPositionMs = exoPlayer.currentPosition
                        pendingPlayWhenReady = exoPlayer.playWhenReady
                        selectedAudioTrackIndex = -1
                        sidePanel = PlayerSidePanel.None
                    }
                )
            )

            state.link.audioTracks.forEachIndexed { index, audioTrack ->
                add(
                    SidePanelMenuItem(
                        id = "track_$index",
                        title = formatAudioTrackLabel(index = index, track = audioTrack),
                        selected = selectedAudioTrackIndex == index,
                        onClick = {
                            registerControlsInteraction()
                            pendingSeekPositionMs = exoPlayer.currentPosition
                            pendingPlayWhenReady = exoPlayer.playWhenReady
                            selectedAudioTrackIndex = index
                            sidePanel = PlayerSidePanel.None
                        }
                    )
                )
            }
        }
        MenuListSidePanel(
            visible = sidePanel == PlayerSidePanel.Tracks,
            onCloseRequested = { sidePanel = PlayerSidePanel.None },
            title = stringResource(R.string.audio_tracks),
            items = trackMenuItems,
            showSelectionRadio = true,
            initialFocusedItemId = trackMenuItems.firstOrNull { it.selected }?.id
                ?: trackMenuItems.firstOrNull()?.id,
        )
    }
}

@Composable
private fun BufferingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AndroidView(
                factory = { progressContext ->
                    ProgressBar(progressContext).apply {
                        isIndeterminate = true
                    }
                },
                modifier = Modifier.size(56.dp),
            )

            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun PlayerOverlay(
    metadata: TvPlayerMetadata,
    link: ExtractorLink,
    positionMs: Long,
    durationMs: Long,
    progressFraction: Float,
    isPlaying: Boolean,
    showTracksButton: Boolean,
    showNextEpisodeButton: Boolean,
    playPauseFocusRequester: FocusRequester,
    timelineFocusRequester: FocusRequester,
    onPlayPause: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenTracks: () -> Unit,
    onSyncSubtitles: () -> Unit,
    onToggleResize: () -> Unit,
    onRestart: () -> Unit,
    onNextEpisode: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
) {
    val episodeLabel = when {
        metadata.season != null && metadata.episode != null -> {
            "S${metadata.season}:E${metadata.episode}"
        }
        metadata.episode != null -> {
            "E${metadata.episode}"
        }
        else -> null
    }
    val episodeTitle = metadata.episodeTitle
        ?.takeIf { it.isNotBlank() && !it.equals(metadata.title, ignoreCase = true) }
    val episodeTexts = if (metadata.isEpisodeBased) {
        listOfNotNull(
            episodeLabel,
            episodeTitle,
        )
    } else {
        emptyList()
    }
    val infoTexts = listOfNotNull(
        metadata.year?.toString(),
        metadata.apiName.takeIf { it.isNotBlank() },
        qualityLabel(link.quality).takeIf { it.isNotBlank() },
        link.source.takeIf { it.isNotBlank() },
        link.type.name.takeIf { it.isNotBlank() },
        link.name.takeIf { it.isNotBlank() },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (metadata.title.isNotBlank()) {
            Text(
                text = metadata.title,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (episodeTexts.isNotEmpty()) {
            DotSeparatedRow(
                texts = episodeTexts,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                ),
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        if (infoTexts.isNotEmpty()) {
            DotSeparatedRow(
                texts = infoTexts,
                textStyle = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                ),
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPlaybackTime(positionMs),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.width(14.dp))
            PlaybackTimeline(
                progressFraction = progressFraction,
                focusRequester = timelineFocusRequester,
                onSeekBackward = onSeekBackward,
                onSeekForward = onSeekForward,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = formatPlaybackTime(durationMs),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            PlayerPrimaryControlButton(
                icon = if (isPlaying) {
                    Icons.Default.Pause
                } else {
                    Icons.Default.PlayArrow
                },
                onClick = onPlayPause,
                modifier = Modifier
                    .align(Alignment.Center)
                    .focusRequester(playPauseFocusRequester),
                buttonSize = 64.dp,
                iconSize = 28.dp,
            )

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerSecondaryControlButton(
                    icon = Icons.Default.Source,
                    onClick = onOpenSources,
                )
                PlayerSecondaryControlButton(
                    icon = Icons.Default.Subtitles,
                    onClick = onOpenSubtitles,
                )
                if (showTracksButton) {
                    PlayerSecondaryControlButton(
                        icon = Icons.Default.Audiotrack,
                        onClick = onOpenTracks,
                    )
                }
                PlayerSecondaryControlButton(
                    icon = Icons.Default.Sync,
                    onClick = onSyncSubtitles,
                )
                PlayerSecondaryControlButton(
                    icon = Icons.Default.AspectRatio,
                    onClick = onToggleResize,
                )
                PlayerSecondaryControlButton(
                    icon = Icons.Default.Replay,
                    onClick = onRestart,
                )
                if (showNextEpisodeButton) {
                    PlayerSecondaryControlButton(
                        icon = Icons.Default.SkipNext,
                        onClick = onNextEpisode,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerPrimaryControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 48.dp,
    iconSize: Dp = 20.dp,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White,
            contentColor = Color.Black,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun PlayerSecondaryControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(42.dp),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.2f),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(width = 2.dp, color = Color.White),
                shape = CircleShape,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun PlaybackTimeline(
    progressFraction: Float,
    focusRequester: FocusRequester,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeProgress = progressFraction.coerceIn(0f, 1f)
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(24.dp)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onSeekBackward()
                        true
                    }

                    Key.DirectionRight -> {
                        onSeekForward()
                        true
                    }

                    else -> false
                }
            }
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .focusable()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (isFocused) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.25f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(safeProgress)
                .height(8.dp)
                .background(Color.White)
        )
    }
}

private fun createPlayer(
    context: Context,
    link: ExtractorLink,
    subtitle: com.lagradost.cloudstream3.ui.player.SubtitleData?,
    audioTrack: com.lagradost.cloudstream3.AudioFile?,
): ExoPlayer {
    val subtitleConfiguration = subtitle?.let { subtitleData ->
        MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitleData.getFixedUrl()))
            .setMimeType(subtitleData.mimeType)
            .setLabel(subtitleData.name)
            .setLanguage(subtitleData.languageCode)
            .setId(subtitleData.getId())
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
    }
    val subtitleHeadersByUri = subtitle?.let { subtitleData ->
        mapOf(subtitleData.getFixedUrl() to subtitleData.headers)
    }.orEmpty()
    val videoDataSourceFactory = createDataSourceFactory(
        context = context,
        link = link,
        perUriExtraHeaders = subtitleHeadersByUri,
    )
    val videoMediaSourceFactory = DefaultMediaSourceFactory(videoDataSourceFactory)

    val videoMediaItem = MediaItem.Builder()
        .setUri(link.url)
        .apply {
            if (subtitleConfiguration != null) {
                setSubtitleConfigurations(listOf(subtitleConfiguration))
            }
        }
        .build()
    val videoMediaSource = videoMediaSourceFactory.createMediaSource(videoMediaItem)

    val mediaSource = if (audioTrack == null) {
        videoMediaSource
    } else {
        val audioDataSourceFactory = createDataSourceFactory(
            context = context,
            link = link,
            globalExtraHeaders = audioTrack.headers.orEmpty(),
        )
        val audioMediaSource = DefaultMediaSourceFactory(audioDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(audioTrack.url))
        MergingMediaSource(videoMediaSource, audioMediaSource)
    }

    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(videoMediaSourceFactory)
        .build()
        .apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
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

private fun qualityLabel(quality: Int): String {
    return com.lagradost.cloudstream3.utils.Qualities.getStringByInt(quality)
}

private fun formatSourceMenuLabel(link: ExtractorLink): String {
    val quality = qualityLabel(link.quality)
    return if (quality.isBlank()) {
        link.name
    } else {
        "${link.name} $quality"
    }
}

private fun sourceQualityHeaderLabel(
    link: ExtractorLink,
    unknownQualityLabel: String,
): String {
    return qualityLabel(link.quality).ifBlank { unknownQualityLabel }
}

private fun formatSourceDetailsTexts(
    link: ExtractorLink,
    unknownQualityLabel: String,
): List<String> {
    val quality = qualityLabel(link.quality).ifBlank { unknownQualityLabel }
    val host = extractHostFromUrl(link.url)
    return buildList {
        add(quality)
        link.source.takeIf { it.isNotBlank() }?.let(::add)
        add(link.type.name)
        host?.let(::add)
        if (link is DrmExtractorLink) {
            add("DRM")
        }
        if (link.audioTracks.isNotEmpty()) {
            add("A${link.audioTracks.size}")
        }
    }.distinct()
}

private data class SubtitleLanguageGroup(
    val key: String,
    val displayName: String,
    val items: List<IndexedValue<SubtitleData>>,
)

private fun buildSubtitleLanguageGroups(
    subtitles: List<SubtitleData>,
    selectedSubtitleIndex: Int,
): List<SubtitleLanguageGroup> {
    val indexedSubtitles = subtitles.withIndex().toList()
    val selectedGroupKey = indexedSubtitles.getOrNull(selectedSubtitleIndex)?.value?.let(::subtitleLanguageKey)
    val grouped = indexedSubtitles.groupBy { indexedSubtitle ->
        subtitleLanguageKey(indexedSubtitle.value)
    }

    return grouped.map { (groupKey, groupItems) ->
        SubtitleLanguageGroup(
            key = groupKey,
            displayName = subtitleLanguageDisplayName(groupKey),
            items = groupItems.sortedWith(
                compareBy<IndexedValue<SubtitleData>> { indexedSubtitle ->
                    indexedSubtitle.value.nameSuffix.toIntOrNull() ?: 0
                }.thenBy { indexedSubtitle ->
                    indexedSubtitle.value.name
                }
            ),
        )
    }.sortedWith(
        compareByDescending<SubtitleLanguageGroup> { group ->
            group.key == selectedGroupKey
        }.thenBy { group ->
            subtitleLanguagePreferredRank(group.key)
        }.thenBy { group ->
            group.displayName.lowercase(Locale.getDefault())
        }
    )
}

private fun subtitleGroupItemId(languageKey: String): String {
    return languageKey.replace(Regex("[^A-Za-z0-9_]"), "_")
}

private fun subtitleLanguagePreferredRank(languageKey: String): Int {
    return when (languageKey.substringBefore('-')) {
        "pl" -> 0
        "en" -> 1
        else -> 2
    }
}

private fun subtitleLanguageKey(subtitle: SubtitleData): String {
    val rawCode = subtitle.languageCode
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: subtitle.getIETF_tag()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        ?: "und"

    return rawCode.lowercase(Locale.ROOT)
}

private fun subtitleLanguageDisplayName(languageKey: String): String {
    if (languageKey == "und") return "Unknown"

    val baseLanguage = languageKey.substringBefore('-')
    val displayLocale = Locale.getDefault()
    val localizedName = Locale.forLanguageTag(baseLanguage).getDisplayLanguage(displayLocale)
        .takeIf { it.isNotBlank() && !it.equals(baseLanguage, ignoreCase = true) }
        ?: languageKey

    return localizedName.replaceFirstChar { firstChar ->
        if (firstChar.isLowerCase()) {
            firstChar.titlecase(displayLocale)
        } else {
            firstChar.toString()
        }
    }
}

private fun formatSubtitleDetailsTexts(subtitle: SubtitleData): List<String> {
    val format = formatSubtitleMimeType(subtitle.mimeType)
    val host = extractHostFromUrl(subtitle.getFixedUrl())
    return buildList {
        subtitle.getIETF_tag()?.takeIf { it.isNotBlank() }?.let(::add)
        format?.let(::add)
        host?.let(::add)
    }.distinct()
}

private fun formatSubtitleMimeType(mimeType: String?): String? {
    if (mimeType.isNullOrBlank()) return null
    return when {
        mimeType.contains("subrip", ignoreCase = true) -> "SRT"
        mimeType.contains("vtt", ignoreCase = true) -> "VTT"
        mimeType.contains("ttml", ignoreCase = true) -> "TTML"
        mimeType.contains("/") -> mimeType.substringAfterLast("/").uppercase()
        else -> mimeType.uppercase()
    }
}

private fun extractHostFromUrl(url: String): String? {
    return runCatching {
        android.net.Uri.parse(url).host
    }.getOrNull()
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }
}

private fun formatAudioTrackLabel(
    index: Int,
    track: com.lagradost.cloudstream3.AudioFile,
): String {
    val host = runCatching {
        android.net.Uri.parse(track.url).host
    }.getOrNull()
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }

    return if (host == null) {
        "Track ${index + 1}"
    } else {
        "Track ${index + 1} ($host)"
    }
}

private fun formatPlaybackTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
