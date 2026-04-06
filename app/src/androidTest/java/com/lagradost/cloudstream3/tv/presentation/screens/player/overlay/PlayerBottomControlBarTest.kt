package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
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

    @Test
    fun playPauseDoesNotShowTooltipButKeepsContentDescription() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pauseLabel = context.getString(R.string.pause)
        val sourcesLabel = context.getString(R.string.sources)

        composeRule.setContent {
            MaterialTheme {
                val playPauseFocusRequester = remember { FocusRequester() }
                PlayerBottomControlBar(
                    isPlaying = true,
                    controlsEnabled = true,
                    showAudioTracksButton = false,
                    showVideoTracksButton = false,
                    showSyncButton = false,
                    showNextEpisodeButton = false,
                    playPauseFocusRequester = playPauseFocusRequester,
                    onControlsEvent = {},
                )
            }
        }

        assertTrue(
            composeRule
                .onAllNodesWithTag(PlayerControlsTestTags.TooltipLane)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty(),
        )

        requestNodeFocus(PlayerControlsTestTags.PlayPauseButton)
        waitUntilNodeFocused(PlayerControlsTestTags.PlayPauseButton)
        waitUntilTextAbsent(pauseLabel)
        composeRule
            .onNodeWithTag(PlayerControlsTestTags.PlayPauseButton, useUnmergedTree = true)
            .assertContentDescriptionEquals(pauseLabel)

        requestNodeFocus(PlayerControlsTestTags.SourcesButton)
        waitUntilNodeFocused(PlayerControlsTestTags.SourcesButton)
        waitUntilTextPresent(sourcesLabel)

        requestNodeFocus(PlayerControlsTestTags.PlayPauseButton)
        waitUntilNodeFocused(PlayerControlsTestTags.PlayPauseButton)
        waitUntilTextAbsent(sourcesLabel)
        waitUntilTextAbsent(pauseLabel)
    }

    private fun requestNodeFocus(tag: String) {
        composeRule
            .onNodeWithTag(tag, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.waitForIdle()
    }

    private fun waitUntilNodeFocused(tag: String, timeoutMillis: Long = 2_000L) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule
                .onNodeWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNode()
                .config
                .getOrElse(SemanticsProperties.Focused) { false }
        }
    }

    private fun waitUntilTextPresent(text: String, timeoutMillis: Long = 2_000L) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule
                .onAllNodesWithText(text, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private fun waitUntilTextAbsent(text: String, timeoutMillis: Long = 2_000L) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule
                .onAllNodesWithText(text, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
    }
}
