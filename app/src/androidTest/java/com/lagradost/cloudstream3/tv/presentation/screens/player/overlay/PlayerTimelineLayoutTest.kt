package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.activity.ComponentActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.tv.presentation.screens.player.createSilentWav
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The time labels take only the width of the longest time text; the bar gets the rest and touches no gap. */
@RunWith(AndroidJUnit4::class)
class PlayerTimelineLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun barStartsRightNextToTimeLabels() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val wav = createSilentWav(context, seconds = 120)

        composeRule.setContent {
            MaterialTheme {
                val player = remember {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(wav.toURI().toString()))
                        prepare()
                    }
                }
                DisposableEffect(player) { onDispose { player.release() } }
                PlaybackTimelineSection(
                    exoPlayer = player,
                    controlsEnabled = true,
                    timelineFocusRequester = remember { FocusRequester() },
                    downFocusRequester = remember { FocusRequester() },
                    seekStepMs = 10_000L,
                    accent = Color.White,
                    onPlaybackProgress = { _, _ -> },
                    onControlsEvent = {},
                )
            }
        }

        // Duration known: the remaining-time label shows "−2:00".
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithText("−2:00").fetchSemanticsNodes(false).isNotEmpty()
        }
        composeRule.waitForIdle()

        val density = context.resources.displayMetrics.density
        // The label nodes span the reserved width: measure where the glyphs really are inside them.
        fun glyphEdgeDp(text: String, right: Boolean): Float {
            val node = composeRule.onNodeWithText(text)
            val bounds = node.getBoundsInRoot()
            val layouts = mutableListOf<TextLayoutResult>()
            node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            val layout = layouts.first()
            return if (right) {
                bounds.left.value + layout.getLineRight(0) / density
            } else {
                bounds.left.value + layout.getLineLeft(0) / density
            }
        }
        val bar = composeRule.onNodeWithTag(PlayerControlsTestTags.Timeline).getBoundsInRoot()

        val leftGap = bar.left.value - glyphEdgeDp("0:00", right = true)
        val rightGap = glyphEdgeDp("−2:00", right = false) - bar.right.value
        assertTrue("Gap before the bar is $leftGap dp, expected <= 16 dp", leftGap <= 16f)
        assertTrue("Gap after the bar is $rightGap dp, expected <= 16 dp", rightGap <= 16f)
    }
}
