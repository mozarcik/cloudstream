package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens

internal sealed interface TvPlayerControlsEvent {
    data object PlayPause : TvPlayerControlsEvent
    data object OpenSources : TvPlayerControlsEvent
    data object OpenVideoTracks : TvPlayerControlsEvent
    data object OpenSubtitles : TvPlayerControlsEvent
    data object OpenTracks : TvPlayerControlsEvent
    data object SyncSubtitles : TvPlayerControlsEvent
    data object ToggleResizeMode : TvPlayerControlsEvent
    data object Restart : TvPlayerControlsEvent
    data object NextEpisode : TvPlayerControlsEvent
    data object SeekBackward : TvPlayerControlsEvent
    data object SeekForward : TvPlayerControlsEvent
}

internal object PlayerControlsTokens {
    /** Subtitles are lifted by this much while the controls are visible (bottom padding + controls + timeline). */
    val SubtitleControlsVisibleOffset =
        TvDimens.PlayerControlsBottomPadding +
            TvDimens.PlayerOptionHeight +
            TvDimens.PlayerTimelineToControlsGap +
            TvDimens.PlayerTimelineHeight

    /** Seek hint ("+10 s") stays visible this long after the last key press. */
    const val SeekHintVisibleMs = 1_500L
}

internal object PlayerControlsTestTags {
    const val PlayPauseButton = "player_controls_play_pause"
    const val SourcesButton = "player_controls_sources"
    const val Timeline = "player_controls_timeline"
}

internal enum class PlayerControlFocusTarget {
    Restart,
    Video,
    Sources,
    PlayPause,
    NextEpisode,
    Subtitles,
    Sync,
    Audio,
    AspectRatio,
}
