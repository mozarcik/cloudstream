package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

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
    val OverlayHorizontalPadding = 48.dp
    val OverlayVerticalPadding = 20.dp
    val MetadataToTimelineSpacing = 16.dp
    val TimelineToControlsSpacing = 18.dp

    val PlayButtonSize = 64.dp
    val PlayIconSize = 28.dp
    const val PlayFocusScale = 1.05f

    val SecondaryButtonSize = 42.dp
    val SecondaryIconSize = 17.dp
    const val SecondaryFocusScale = 1.05f
    const val SecondaryContainerAlpha = 0.2f

    val ButtonsSpacing = 8.dp

    val TooltipShape = RoundedCornerShape(12.dp)
    val TooltipHorizontalPadding = 16.dp
    val TooltipVerticalPadding = 6.dp
    val TooltipTonalElevation = 5.dp
    const val TooltipAlpha = 0.95f
    val TooltipVerticalOffset = 16.dp
    val TooltipMaxWidth = 220.dp
    const val TooltipFadeInMs = 150
    const val TooltipFadeOutMs = 0

    val TimelineContainerHeight = 24.dp
    val TimelineInactiveTrackHeight = 6.dp
    val TimelineFocusedTrackHeight = 8.dp
    val TimelineTrackPadding = 8.dp
    const val TimelineFocusAnimationMs = 120
}

internal data class PlayerControlTooltipState(
    val text: String,
    val anchorCenterXPx: Float,
    val anchorTopYPx: Float,
)

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
