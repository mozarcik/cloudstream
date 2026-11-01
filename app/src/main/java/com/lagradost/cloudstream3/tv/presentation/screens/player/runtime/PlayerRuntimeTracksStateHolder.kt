package com.lagradost.cloudstream3.tv.presentation.screens.player.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer

@Stable
internal class PlayerRuntimeTracksStateHolder(
    private val player: ExoPlayer,
) {
    var audioPanelVisible by mutableStateOf(false)
    var videoPanelVisible by mutableStateOf(false)
    var subtitleSyncPanelVisible by mutableStateOf(false)
    var audioTracks by mutableStateOf<List<PlayerAudioTrackOption>>(emptyList())
    var videoTracks by mutableStateOf<List<PlayerVideoTrackOption>>(emptyList())
    var selectedAudioTrackId by mutableStateOf<String?>(null)
    var selectedVideoTrackId by mutableStateOf<String?>(null)
    private var hasAppliedInitialAudioSelection by mutableStateOf(false)

    val showRuntimeAudioTracksButton: Boolean
        get() = audioTracks.size > 1

    val showRuntimeVideoTracksButton: Boolean
        get() = videoTracks.size > 1

    fun refresh(tracksSnapshot: Tracks = player.currentTracks): List<PlayerAudioTrackOption> {
        val extractedAudioTracks = extractRuntimeAudioTracks(tracksSnapshot)
        audioTracks = extractedAudioTracks
        selectedAudioTrackId = extractedAudioTracks.firstOrNull { track ->
            track.isSelected
        }?.selectionId

        val extractedVideoTracks = extractRuntimeVideoTracks(tracksSnapshot)
        videoTracks = extractedVideoTracks
        selectedVideoTrackId = extractedVideoTracks.firstOrNull { track ->
            track.isSelected
        }?.selectionId

        if (audioPanelVisible && extractedAudioTracks.size <= 1) {
            audioPanelVisible = false
        }
        if (videoPanelVisible && extractedVideoTracks.size <= 1) {
            videoPanelVisible = false
        }
        return extractedAudioTracks
    }

    fun applyInitialAudioSelectionIfNeeded(tracks: List<PlayerAudioTrackOption>) {
        if (hasAppliedInitialAudioSelection || tracks.isEmpty()) return
        hasAppliedInitialAudioSelection = true
        if (selectedAudioTrackId == null) {
            selectAudioTrack(tracks.first())
        }
    }

    fun selectAudioTrack(track: PlayerAudioTrackOption?) {
        applyRuntimeAudioTrackSelection(
            player = player,
            track = track,
        )
        selectedAudioTrackId = track?.selectionId
    }

    fun selectVideoTrack(track: PlayerVideoTrackOption?) {
        applyRuntimeVideoTrackSelection(
            player = player,
            track = track,
        )
        selectedVideoTrackId = track?.selectionId
    }
}

@Composable
internal fun rememberPlayerRuntimeTracksStateHolder(
    linkUrl: String,
    player: ExoPlayer,
): PlayerRuntimeTracksStateHolder {
    return remember(linkUrl, player) {
        PlayerRuntimeTracksStateHolder(player = player)
    }
}
