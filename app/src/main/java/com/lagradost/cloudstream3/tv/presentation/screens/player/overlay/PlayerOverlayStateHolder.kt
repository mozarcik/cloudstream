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
    var showExtendedMetadata by mutableStateOf(true)
    var startupAutoHideArmed by mutableStateOf(true)
    var errorHandled by mutableStateOf(false)
    /** Accumulated seek while the controls are hidden ("+10 s ››"); 0 = hint hidden. */
    var seekHintDeltaMs by androidx.compose.runtime.mutableLongStateOf(0L)
    /** "Cancel" / "Watch credits" on the Up next card: hides it and cancels autoplay of this episode. */
    var upNextDismissed by mutableStateOf(false)
    /** The Up next card is on screen (reported by the card itself); counts as a panel for focus / keys. */
    var upNextVisible by mutableStateOf(false)
    /** Increments on every side-panel close: the control bar restores focus to the control that opened it. */
    var panelCloseCount by mutableIntStateOf(0)
    var sourceErrorDialogEffect by mutableStateOf<TvPlayerPanelEffect.OpenSourceErrorDialog?>(null)
    internal var metadataRevealGeneration by mutableIntStateOf(0)
        private set

    val showBufferingOverlay: Boolean
        get() = playerWantsToPlay &&
            !isPlaying &&
            (playerPlaybackState == Player.STATE_BUFFERING || playerPlaybackState == Player.STATE_IDLE)

    private fun advanceMetadataRevealGeneration(): Int {
        metadataRevealGeneration += 1
        return metadataRevealGeneration
    }

    fun hideExtendedMetadata(): Int {
        showExtendedMetadata = false
        return advanceMetadataRevealGeneration()
    }

    fun showSourceStartMetadata(): Int {
        showExtendedMetadata = true
        return advanceMetadataRevealGeneration()
    }

    fun resetForSourceChange() {
        controlsVisible = true
        startupAutoHideArmed = true
        errorHandled = false
        sourceErrorDialogEffect = null
        showSourceStartMetadata()
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
