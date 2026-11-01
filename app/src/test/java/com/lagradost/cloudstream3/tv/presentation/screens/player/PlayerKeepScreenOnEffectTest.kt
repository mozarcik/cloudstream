package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerKeepScreenOnEffectTest {
    @Test
    fun `should keep screen on while playing`() {
        assertTrue(
            shouldKeepPlaybackScreenOn(
                isPlaying = true,
                playerWantsToPlay = true,
                playbackState = Player.STATE_READY,
            )
        )
    }

    @Test
    fun `should keep screen on while buffering`() {
        assertTrue(
            shouldKeepPlaybackScreenOn(
                isPlaying = false,
                playerWantsToPlay = false,
                playbackState = Player.STATE_BUFFERING,
            )
        )
    }

    @Test
    fun `should allow screen off while paused`() {
        assertFalse(
            shouldKeepPlaybackScreenOn(
                isPlaying = false,
                playerWantsToPlay = false,
                playbackState = Player.STATE_READY,
            )
        )
    }
}
