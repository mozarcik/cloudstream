package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DotSeparatedRow
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerMetadata
import com.lagradost.cloudstream3.utils.ExtractorLink

@Composable
internal fun PlayerOverlay(
    metadata: TvPlayerMetadata,
    link: ExtractorLink,
    isPlaying: Boolean,
    controlsEnabled: Boolean,
    showAudioTracksButton: Boolean,
    showVideoTracksButton: Boolean,
    showSyncButton: Boolean,
    showNextEpisodeButton: Boolean,
    playPauseFocusRequester: androidx.compose.ui.focus.FocusRequester,
    timelineFocusRequester: androidx.compose.ui.focus.FocusRequester,
    exoPlayer: ExoPlayer,
    onPlaybackProgress: (Long, Long) -> Unit,
    onControlsEvent: (TvPlayerControlsEvent) -> Unit,
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
            .padding(
                horizontal = PlayerControlsTokens.OverlayHorizontalPadding,
                vertical = PlayerControlsTokens.OverlayVerticalPadding,
            ),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom,
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

        Spacer(modifier = Modifier.height(PlayerControlsTokens.MetadataToTimelineSpacing))

        PlaybackTimelineSection(
            exoPlayer = exoPlayer,
            controlsEnabled = controlsEnabled,
            timelineFocusRequester = timelineFocusRequester,
            onPlaybackProgress = onPlaybackProgress,
            onControlsEvent = onControlsEvent,
        )

        Spacer(modifier = Modifier.height(PlayerControlsTokens.TimelineToControlsSpacing))

        PlayerBottomControlBar(
            isPlaying = isPlaying,
            controlsEnabled = controlsEnabled,
            showAudioTracksButton = showAudioTracksButton,
            showVideoTracksButton = showVideoTracksButton,
            showSyncButton = showSyncButton,
            showNextEpisodeButton = showNextEpisodeButton,
            playPauseFocusRequester = playPauseFocusRequester,
            onControlsEvent = onControlsEvent,
        )
    }
}

internal fun qualityLabel(quality: Int): String {
    return com.lagradost.cloudstream3.utils.Qualities.getStringByInt(quality)
}
