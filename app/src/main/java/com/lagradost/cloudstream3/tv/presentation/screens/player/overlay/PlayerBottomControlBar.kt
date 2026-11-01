package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerResizeMode
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.subtitleValueLabel
import com.lagradost.cloudstream3.tv.presentation.screens.player.runtime.PlayerRuntimeTracksStateHolder
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerIcons
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens

/** What the controls row shows; hidden entries (null value or false flag) are not composed at all. */
@Immutable
internal data class PlayerBottomControlBarConfig(
    val showAudioTracksButton: Boolean,
    val showVideoTracksButton: Boolean,
    val showSyncButton: Boolean,
    val showNextEpisodeButton: Boolean,
    val videoValue: String? = null,
    val nextValue: String? = null,
    val subtitlesValue: String = "",
    val syncValue: String? = null,
    val audioValue: String? = null,
    val aspectValue: String = "",
)

/**
 * Controls row, fixed order (mockup): Play/Pause, Restart, Video*, Sources, Next*, Subtitles,
 * Sync*, Audio*, Aspect. Focus moves with the default left/right search because hidden
 * buttons are simply not composed.
 */
@Composable
internal fun PlayerBottomControlBar(
    isPlaying: Boolean,
    controlsEnabled: Boolean,
    controlsShown: Boolean,
    panelCloseCount: Int,
    config: PlayerBottomControlBarConfig,
    state: PlayerBottomControlBarState,
    timelineFocusRequester: FocusRequester,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlayerBottomControlRestoreFocusEffect(
        state = state,
        config = config,
        controlsEnabled = controlsEnabled,
        controlsShown = controlsShown,
        panelCloseCount = panelCloseCount,
    )

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    fun Modifier.control(target: PlayerControlFocusTarget): Modifier =
        focusRequester(state.requesterFor(target))

    Row(
        horizontalArrangement = Arrangement.spacedBy(TvDimens.PlayerOptionSpacing),
        verticalAlignment = Alignment.CenterVertically,
        // Up from any control goes to the timeline (mockup: the timeline sits above the row).
        // Explicit, because the default geometric search does not always reach it.
        // Buttons keep their natural width; when they do not fit the row scrolls (focus scrolls it).
        // The side padding comes after horizontalScroll: it is part of the scrolled content, so the
        // buttons run to the screen edge instead of being clipped at the padded edge.
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = TvDimens.PlayerSidePadding)
            .onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp && controlsEnabled) {
                timelineFocusRequester.requestFocus()
                true
            } else {
                false
            }
        },
    ) {
        PlayerOptionButton(
            icon = if (isPlaying) PlayerIcons.Pause else PlayerIcons.Play,
            label = stringResource(if (isPlaying) R.string.pause else R.string.home_play),
            enabled = controlsEnabled,
            minWidth = TvDimens.PlayerPlayPauseWidth,
            onClick = { onControlsEvent(TvPlayerControlsEvent.PlayPause) },
            onFocused = {
                state.lastFocusedControl = PlayerControlFocusTarget.PlayPause
                // Bring the leading padding back into view as well, not just the button.
                coroutineScope.launch { scrollState.animateScrollTo(0) }
            },
            // Fixed width: "Odtwórz" and "Wstrzymaj" must not shift the rest of the row.
            modifier = Modifier
                .padding(end = TvDimens.PlayerOptionSpacing)
                .width(TvDimens.PlayerPlayPauseWidth)
                .control(PlayerControlFocusTarget.PlayPause)
                .testTag(PlayerControlsTestTags.PlayPauseButton),
        )
        PlayerOptionButton(
            icon = PlayerIcons.Restart,
            label = stringResource(R.string.tv_player_control_restart),
            enabled = controlsEnabled,
            onClick = { onControlsEvent(TvPlayerControlsEvent.Restart) },
            onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.Restart },
            modifier = Modifier.control(PlayerControlFocusTarget.Restart),
        )
        if (config.showVideoTracksButton) {
            PlayerOptionButton(
                icon = PlayerIcons.Video,
                label = stringResource(R.string.tv_player_control_video),
                value = config.videoValue,
                enabled = controlsEnabled,
                onClick = { onControlsEvent(TvPlayerControlsEvent.OpenVideoTracks) },
                onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.Video },
                modifier = Modifier.control(PlayerControlFocusTarget.Video),
            )
        }
        PlayerOptionButton(
            icon = PlayerIcons.Sources,
            label = stringResource(R.string.sources),
            enabled = controlsEnabled,
            onClick = { onControlsEvent(TvPlayerControlsEvent.OpenSources) },
            onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.Sources },
            modifier = Modifier
                .control(PlayerControlFocusTarget.Sources)
                .testTag(PlayerControlsTestTags.SourcesButton),
        )
        if (config.showNextEpisodeButton) {
            PlayerOptionButton(
                icon = PlayerIcons.Next,
                label = stringResource(R.string.tv_player_control_next),
                value = config.nextValue,
                enabled = controlsEnabled,
                onClick = { onControlsEvent(TvPlayerControlsEvent.NextEpisode) },
                onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.NextEpisode },
                modifier = Modifier.control(PlayerControlFocusTarget.NextEpisode),
            )
        }
        PlayerOptionButton(
            icon = PlayerIcons.Subtitles,
            label = stringResource(R.string.player_subtitles_settings),
            value = config.subtitlesValue,
            enabled = controlsEnabled,
            onClick = { onControlsEvent(TvPlayerControlsEvent.OpenSubtitles) },
            onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.Subtitles },
            modifier = Modifier.control(PlayerControlFocusTarget.Subtitles),
        )
        if (config.showSyncButton) {
            PlayerOptionButton(
                icon = PlayerIcons.Sync,
                label = stringResource(R.string.tv_player_control_sync),
                value = config.syncValue,
                enabled = controlsEnabled,
                onClick = { onControlsEvent(TvPlayerControlsEvent.SyncSubtitles) },
                onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.Sync },
                modifier = Modifier.control(PlayerControlFocusTarget.Sync),
            )
        }
        if (config.showAudioTracksButton) {
            PlayerOptionButton(
                icon = PlayerIcons.Audio,
                label = stringResource(R.string.tv_player_control_audio),
                value = config.audioValue,
                enabled = controlsEnabled,
                onClick = { onControlsEvent(TvPlayerControlsEvent.OpenTracks) },
                onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.Audio },
                modifier = Modifier.control(PlayerControlFocusTarget.Audio),
            )
        }
        PlayerOptionButton(
            icon = PlayerIcons.Aspect,
            label = stringResource(R.string.tv_player_control_aspect),
            value = config.aspectValue,
            enabled = controlsEnabled,
            onClick = { onControlsEvent(TvPlayerControlsEvent.ToggleResizeMode) },
            onFocused = { state.lastFocusedControl = PlayerControlFocusTarget.AspectRatio },
            modifier = Modifier.control(PlayerControlFocusTarget.AspectRatio),
        )
    }
}

/** Builds the values shown in the controls row (video height, next label, subtitle language, ...). */
@Composable
internal fun rememberPlayerBottomControlBarConfig(
    state: TvPlayerUiState.Ready,
    selectedSubtitle: SubtitleData?,
    hasSelectedSubtitle: Boolean,
    subtitleDelayMs: Long,
    runtimeTracksState: PlayerRuntimeTracksStateHolder,
    playerResizeMode: PlayerResizeMode,
): PlayerBottomControlBarConfig {
    val offLabel = stringResource(R.string.tv_player_value_off)
    // Mockup names (Fit / Stretch / Zoom), the same as the control pill; no toast on change.
    val aspectLabel = stringResource(
        when (playerResizeMode) {
            PlayerResizeMode.Fit -> R.string.tv_player_aspect_fit
            PlayerResizeMode.Fill -> R.string.tv_player_aspect_stretch
            PlayerResizeMode.Zoom -> R.string.tv_player_aspect_zoom
        },
    )
    val videoTrack = runtimeTracksState.videoTracks.firstOrNull { it.selectionId == runtimeTracksState.selectedVideoTrackId }
    val audioTrack = runtimeTracksState.audioTracks.firstOrNull { it.selectionId == runtimeTracksState.selectedAudioTrackId }
    val showVideo = runtimeTracksState.showRuntimeVideoTracksButton
    val showAudio = runtimeTracksState.showRuntimeAudioTracksButton
    val videoValue = videoTrack?.let { if (it.height > 0) "${it.height}p" else it.title }
    val audioValue = audioTrack?.title
    val subtitlesValue = if (hasSelectedSubtitle && selectedSubtitle != null) subtitleValueLabel(selectedSubtitle) else offLabel
    val syncValue = formatSubtitleDelayValue(subtitleDelayMs)
    return remember(
        state.hasNextEpisode, state.upNext?.label, hasSelectedSubtitle, showVideo, showAudio,
        videoValue, audioValue, subtitlesValue, syncValue, aspectLabel,
    ) {
        PlayerBottomControlBarConfig(
            showAudioTracksButton = showAudio,
            showVideoTracksButton = showVideo,
            showSyncButton = hasSelectedSubtitle,
            showNextEpisodeButton = state.hasNextEpisode,
            videoValue = videoValue,
            nextValue = state.upNext?.label,
            subtitlesValue = subtitlesValue,
            syncValue = syncValue,
            audioValue = audioValue,
            aspectValue = aspectLabel,
        )
    }
}

/** "+0.3 s", "0 s", "−2.9 s" (U+2212), the value of the Sync control and the sync panel. */
internal fun formatSubtitleDelayValue(delayMs: Long): String {
    if (delayMs == 0L) return "0 s"
    return String.format(java.util.Locale.US, "%+.1f s", delayMs / 1000f).replace('-', '\u2212')
}
