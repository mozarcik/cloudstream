package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.media3.common.Player
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerOverlayStateHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPlaybackEffectsTest {
    @Test
    fun `shouldRevealExtendedMetadata returns true only when controls are visible and playback is paused`() {
        val overlayState = overlayState().apply {
            controlsVisible = true
            playerWantsToPlay = false
        }

        assertTrue(shouldRevealExtendedMetadata(overlayState, hasSidePanel = false))
        assertFalse(shouldRevealExtendedMetadata(overlayState, hasSidePanel = true))

        overlayState.playerWantsToPlay = true
        assertFalse(shouldRevealExtendedMetadata(overlayState, hasSidePanel = false))

        overlayState.playerWantsToPlay = false
        overlayState.controlsVisible = false
        assertFalse(shouldRevealExtendedMetadata(overlayState, hasSidePanel = false))
    }

    @Test
    fun `shouldShowSourceStartMetadata returns true only while startup overlay is armed and visible`() {
        val overlayState = overlayState().apply {
            controlsVisible = true
            startupAutoHideArmed = true
            playerWantsToPlay = true
        }

        assertTrue(shouldShowSourceStartMetadata(overlayState, hasSidePanel = false))
        assertFalse(shouldShowSourceStartMetadata(overlayState, hasSidePanel = true))

        overlayState.startupAutoHideArmed = false
        assertFalse(shouldShowSourceStartMetadata(overlayState, hasSidePanel = false))

        overlayState.startupAutoHideArmed = true
        overlayState.controlsVisible = false
        assertFalse(shouldShowSourceStartMetadata(overlayState, hasSidePanel = false))
    }

    @Test
    fun `hideExtendedMetadata clears flag and increments reveal generation`() {
        val overlayState = overlayState().apply {
            showExtendedMetadata = true
        }

        val firstGeneration = overlayState.hideExtendedMetadata()
        val secondGeneration = overlayState.hideExtendedMetadata()

        assertFalse(overlayState.showExtendedMetadata)
        assertEquals(firstGeneration + 1, secondGeneration)
        assertEquals(secondGeneration, overlayState.metadataRevealGeneration)
    }

    @Test
    fun `resetForSourceChange shows metadata immediately and rearms startup overlay`() {
        val overlayState = overlayState().apply {
            controlsVisible = false
            showExtendedMetadata = false
            startupAutoHideArmed = false
            hideExtendedMetadata()
        }

        overlayState.resetForSourceChange()

        assertTrue(overlayState.controlsVisible)
        assertTrue(overlayState.showExtendedMetadata)
        assertTrue(overlayState.startupAutoHideArmed)
        assertEquals(2, overlayState.metadataRevealGeneration)
    }

    private fun overlayState(): PlayerOverlayStateHolder {
        return PlayerOverlayStateHolder(
            initialIsPlaying = true,
            initialPlaybackState = Player.STATE_READY,
            initialPlayWhenReady = true,
        )
    }
}
