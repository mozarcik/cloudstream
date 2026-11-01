package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerBottomControlBarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Labels are inline in the pills (mockup), unavailable options are not composed. */
    @Test
    fun rowShowsLabelsAndHidesUnavailableOptions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pauseLabel = context.getString(R.string.pause)
        val sourcesLabel = context.getString(R.string.sources)
        val audioLabel = context.getString(R.string.tv_player_control_audio)
        val nextLabel = context.getString(R.string.tv_player_control_next)

        composeRule.setContent {
            MaterialTheme {
                val state = rememberPlayerBottomControlBarState(remember { FocusRequester() })
                PlayerBottomControlBar(
                    isPlaying = true,
                    controlsEnabled = true,
                    controlsShown = true,
                    panelCloseCount = 0,
                    config = PlayerBottomControlBarConfig(
                        showAudioTracksButton = false,
                        showVideoTracksButton = false,
                        showSyncButton = false,
                        showNextEpisodeButton = true,
                        nextValue = "S1:E2",
                        subtitlesValue = "Off",
                        aspectValue = "Fit",
                    ),
                    state = state,
                    timelineFocusRequester = remember { FocusRequester() },
                    onControlsEvent = {},
                )
            }
        }

        fun count(text: String) = composeRule
            .onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .size

        assertTrue(count(pauseLabel) == 1)
        assertTrue(count(sourcesLabel) == 1)
        assertTrue(count(nextLabel) == 1)
        assertTrue(count(audioLabel) == 0)
    }
}
