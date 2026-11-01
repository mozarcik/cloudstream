package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.media3.common.Player
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerOverlayStateHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPlaybackContentTest {
    @Test
    fun `hidden controls confirm pauses playback and consumes matching key up`() {
        val overlayState = overlayState().apply {
            controlsVisible = false
            playerWantsToPlay = true
        }
        val previewKeyState = PlayerPreviewKeyState()
        var pauseCalls = 0

        val keyDownHandled = handlePlayerPreviewKeyEvent(
            eventType = KeyEventType.KeyDown,
            keyCode = AndroidKeyEvent.KEYCODE_DPAD_CENTER,
            downTime = 100L,
            previewKeyState = previewKeyState,
            hasSidePanel = false,
            controlsVisible = overlayState.controlsVisible,
            overlayState = overlayState,
            rootFocusRequester = FocusRequester(),
            playerWantsToPlay = overlayState.playerWantsToPlay,
            showBufferingOverlay = false,
            registerControlsInteraction = {},
            onSeekBackward = {},
            onSeekForward = {},
            onPauseForControls = { pauseCalls += 1 },
        )

        assertTrue(keyDownHandled)
        assertTrue(overlayState.controlsVisible)
        assertEquals(1, pauseCalls)

        overlayState.playerWantsToPlay = false

        val keyUpHandled = handlePlayerPreviewKeyEvent(
            eventType = KeyEventType.KeyUp,
            keyCode = AndroidKeyEvent.KEYCODE_DPAD_CENTER,
            downTime = 100L,
            previewKeyState = previewKeyState,
            hasSidePanel = false,
            controlsVisible = overlayState.controlsVisible,
            overlayState = overlayState,
            rootFocusRequester = FocusRequester(),
            playerWantsToPlay = overlayState.playerWantsToPlay,
            showBufferingOverlay = false,
            registerControlsInteraction = {},
            onSeekBackward = {},
            onSeekForward = {},
            onPauseForControls = { pauseCalls += 1 },
        )

        assertTrue(keyUpHandled)
        assertEquals(1, pauseCalls)
    }

    @Test
    fun `visible controls confirm stays available for focused control`() {
        val overlayState = overlayState().apply {
            controlsVisible = true
            playerWantsToPlay = true
        }
        val previewKeyState = PlayerPreviewKeyState()
        var pauseCalls = 0

        val handled = handlePlayerPreviewKeyEvent(
            eventType = KeyEventType.KeyDown,
            keyCode = AndroidKeyEvent.KEYCODE_DPAD_CENTER,
            downTime = 100L,
            previewKeyState = previewKeyState,
            hasSidePanel = false,
            controlsVisible = overlayState.controlsVisible,
            overlayState = overlayState,
            rootFocusRequester = FocusRequester(),
            playerWantsToPlay = overlayState.playerWantsToPlay,
            showBufferingOverlay = false,
            registerControlsInteraction = {},
            onSeekBackward = {},
            onSeekForward = {},
            onPauseForControls = { pauseCalls += 1 },
        )

        assertFalse(handled)
        assertEquals(0, pauseCalls)
    }

    private fun overlayState(): PlayerOverlayStateHolder {
        return PlayerOverlayStateHolder(
            initialIsPlaying = true,
            initialPlaybackState = Player.STATE_READY,
            initialPlayWhenReady = true,
        )
    }
}
