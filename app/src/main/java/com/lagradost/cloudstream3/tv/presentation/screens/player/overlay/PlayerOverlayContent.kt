package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerMetadata
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import com.lagradost.cloudstream3.utils.ExtractorLink

/** "A • B • C" as one text node, bullets in the separator color of the mockups. */
internal fun bulletJoined(texts: List<String>): AnnotatedString = buildAnnotatedString {
    texts.forEachIndexed { index, text ->
        if (index > 0) {
            withStyle(SpanStyle(color = TvColors.Disabled)) { append(" • ") }
        }
        append(text)
    }
}

/** Accent of the player: cool for series, warm for movies (mockups PlayerControls / PlayerControlsMovie). */
internal fun playerAccent(metadata: TvPlayerMetadata): Color =
    if (metadata.isEpisodeBased) TvColors.FeaturedEyebrow else TvColors.Accent

internal fun playerEpisodeLabel(metadata: TvPlayerMetadata): String? = when {
    metadata.season != null && metadata.episode != null -> "S${metadata.season}:E${metadata.episode}"
    metadata.episode != null -> "E${metadata.episode}"
    else -> null
}

@Composable
internal fun PlayerOverlay(
    metadata: TvPlayerMetadata,
    link: ExtractorLink,
    isPlaying: Boolean,
    controlsEnabled: Boolean,
    controlsShown: Boolean,
    panelCloseCount: Int,
    barConfig: PlayerBottomControlBarConfig,
    seekStepMs: Long,
    playPauseFocusRequester: FocusRequester,
    timelineFocusRequester: FocusRequester,
    exoPlayer: ExoPlayer,
    onPlaybackProgress: (Long, Long) -> Unit,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
) {
    val gradient = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.55f to Color.Black.copy(alpha = 0.8f),
            1f to Color.Black.copy(alpha = 0.92f),
        )
    }
    val episodeLine = if (metadata.isEpisodeBased) {
        bulletJoined(
            listOfNotNull(
                playerEpisodeLabel(metadata),
                metadata.episodeTitle?.takeIf { it.isNotBlank() && !it.equals(metadata.title, ignoreCase = true) },
            ),
        ).text.takeIf { it.isNotBlank() }
    } else {
        null
    }
    val infoLine = bulletJoined(
        listOfNotNull(
            link.name.takeIf { it.isNotBlank() },
            qualityLabel(link.quality).takeIf { it.isNotBlank() },
        ),
    ).text.takeIf { it.isNotBlank() }
    val barState = rememberPlayerBottomControlBarState(playPauseFocusRequester)

    Box(modifier = Modifier.fillMaxSize()) {
    PlayerTopMask(modifier = Modifier.align(Alignment.TopStart))
    PlayerTitleBlock(
        title = metadata.title,
        episodeLine = episodeLine,
        infoLine = infoLine,
        modifier = Modifier.align(Alignment.TopStart).padding(start = TvDimens.PlayerSidePadding),
    )

    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .height(TvDimens.PlayerGradientHeight)
            .background(gradient)
            // No side padding here: the controls row scrolls edge to edge and pads its own content.
            .padding(bottom = TvDimens.PlayerControlsBottomPadding),
    ) {
        PlaybackTimelineSection(
            exoPlayer = exoPlayer,
            controlsEnabled = controlsEnabled,
            timelineFocusRequester = timelineFocusRequester,
            downFocusRequester = barState.resolveFocusRequester(barState.lastFocusedControl, barConfig),
            seekStepMs = seekStepMs,
            accent = playerAccent(metadata),
            onPlaybackProgress = onPlaybackProgress,
            onControlsEvent = onControlsEvent,
        )

        Spacer(modifier = Modifier.height(TvDimens.PlayerTimelineToControlsGap))

        PlayerBottomControlBar(
            isPlaying = isPlaying,
            controlsEnabled = controlsEnabled,
            controlsShown = controlsShown,
            panelCloseCount = panelCloseCount,
            config = barConfig,
            state = barState,
            timelineFocusRequester = timelineFocusRequester,
            onControlsEvent = onControlsEvent,
        )
    }
    }
}

internal fun qualityLabel(quality: Int): String {
    return com.lagradost.cloudstream3.utils.Qualities.getStringByInt(quality)
}
