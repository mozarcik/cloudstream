package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lagradost.cloudstream3.tv.presentation.screens.player.core.PlayerSessionController
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerOverlayStateHolder
import com.lagradost.cloudstream3.tv.presentation.screens.player.runtime.PlayerRuntimeTracksStateHolder
import com.lagradost.cloudstream3.ui.player.SubtitleData
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression: a subtitle change that reloads the media (external subtitles are part of the MediaSource)
 * restarted from the resume point the episode was opened with - sometimes 30-40 min back - instead of
 * from where the viewer was, whenever no restore request had been captured (auto-selected subtitles etc.).
 */
@RunWith(AndroidJUnit4::class)
class PlayerSubtitleChangeKeepsPositionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var session: PlayerSessionController? = null

    @After
    fun tearDown() {
        composeRule.runOnUiThread { session?.release() }
    }

    @Test
    fun subtitleChangeWithoutRestoreRequestContinuesFromCurrentPosition() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val wav = createSilentWav(context, seconds = 120)
        val subtitle = testSubtitle(createVttFile(context))
        val resumePositionMs = 5_000L
        val watchedPositionMs = 60_000L
        lateinit var controller: PlayerSessionController
        lateinit var overlayState: PlayerOverlayStateHolder
        lateinit var runtimeTracksState: PlayerRuntimeTracksStateHolder
        var selectedSubtitle by mutableStateOf<SubtitleData?>(null)
        composeRule.runOnUiThread {
            controller = PlayerSessionController(context).also { session = it }
            overlayState = PlayerOverlayStateHolder(
                initialIsPlaying = false,
                initialPlaybackState = Player.STATE_IDLE,
                initialPlayWhenReady = true,
            )
            runtimeTracksState = PlayerRuntimeTracksStateHolder(player = controller.player)
        }

        composeRule.setContent {
            val state = remember {
                TvPlayerUiState.Ready(
                    metadata = TvPlayerMetadata(title = "Position test", subtitle = "", backdropUri = null),
                    link = testFileLink(wav),
                    resumePositionMs = resumePositionMs,
                )
            }
            PlayerPlaybackLoadEffect(
                state = state,
                selectedSubtitle = selectedSubtitle,
                selectedSubtitleId = selectedSubtitle?.getId(),
                isCurrentSourceReady = true,
                subtitleSelectionSource = TvPlayerSubtitleSelectionSource.Auto,
                initialSubtitleDelayMs = 0L,
                playbackRestoreRequest = null,
                playbackRestoreRequestKey = null,
                defaultPlayerPositionMs = state.resumePositionMs,
                defaultPlayerPlayWhenReady = true,
                playerSessionController = controller,
                overlayState = overlayState,
                runtimeTracksState = runtimeTracksState,
                onPlaybackRestoreConsumed = {},
            )
        }

        fun positionMs(): Long {
            var value = 0L
            composeRule.runOnUiThread { value = controller.player.currentPosition }
            return value
        }

        // Opened at the resume point, then the viewer watched on.
        composeRule.waitUntil(timeoutMillis = 20_000) {
            var ready = false
            composeRule.runOnUiThread { ready = controller.player.playbackState == Player.STATE_READY }
            ready
        }
        composeRule.runOnUiThread { controller.player.seekTo(watchedPositionMs) }
        composeRule.waitUntil(timeoutMillis = 10_000) { positionMs() >= watchedPositionMs }

        // The subtitle appears (auto selection / loaded from file) - the media is rebuilt.
        selectedSubtitle = subtitle
        composeRule.waitForIdle()
        Thread.sleep(2_000)

        val after = positionMs()
        assertTrue(
            "Position after the subtitle change was $after ms, expected >= $watchedPositionMs ms " +
                "(resume point was $resumePositionMs ms)",
            after >= watchedPositionMs,
        )
    }
}
