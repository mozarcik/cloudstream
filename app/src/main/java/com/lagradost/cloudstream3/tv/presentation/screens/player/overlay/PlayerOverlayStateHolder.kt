package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelEffect

@Stable
internal class PlayerOverlayStateHolder(
    initialIsPlaying: Boolean,
    initialPlaybackState: Int,
    initialPlayWhenReady: Boolean,
) {
    var controlsVisible by mutableStateOf(true)
    var isPlaying by mutableStateOf(initialIsPlaying)
    var playerPlaybackState by mutableIntStateOf(initialPlaybackState)
    var playerWantsToPlay by mutableStateOf(initialPlayWhenReady)
    var errorHandled by mutableStateOf(false)
    var sourceErrorDialogEffect by mutableStateOf<TvPlayerPanelEffect.OpenSourceErrorDialog?>(null)

    val showBufferingOverlay: Boolean
        get() = playerWantsToPlay &&
            !isPlaying &&
            (playerPlaybackState == Player.STATE_BUFFERING || playerPlaybackState == Player.STATE_IDLE)

    fun resetForSourceChange() {
        errorHandled = false
        sourceErrorDialogEffect = null
    }

    fun syncFromPlayer(player: Player) {
        isPlaying = player.isPlaying
        playerPlaybackState = player.playbackState
        playerWantsToPlay = player.playWhenReady
    }
}

@Composable
internal fun rememberPlayerOverlayStateHolder(
    linkUrl: String,
    initialIsPlaying: Boolean,
    initialPlaybackState: Int,
    initialPlayWhenReady: Boolean,
): PlayerOverlayStateHolder {
    return remember(linkUrl) {
        PlayerOverlayStateHolder(
            initialIsPlaying = initialIsPlaying,
            initialPlaybackState = initialPlaybackState,
            initialPlayWhenReady = initialPlayWhenReady,
        )
    }
}
