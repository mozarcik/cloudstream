package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.activity.ComponentActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.platform.LocalContext
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerMetadata
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerOverlayContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun hiddenExtendedMetadataKeepsOnlyTimelineAndControlsVisible() {
        val metadata = testMetadata()

        composeRule.setContent {
            MaterialTheme {
                val context = LocalContext.current
                val player = remember { ExoPlayer.Builder(context).build() }
                val playPauseFocusRequester = remember { FocusRequester() }
                val timelineFocusRequester = remember { FocusRequester() }
                DisposableEffect(player) {
                    onDispose { player.release() }
                }
                PlayerOverlay(
                    metadata = metadata,
                    link = testLink(),
                    isPlaying = true,
                    controlsEnabled = true,
                    showExtendedMetadata = false,
                    showAudioTracksButton = false,
                    showVideoTracksButton = false,
                    showSyncButton = false,
                    showNextEpisodeButton = false,
                    playPauseFocusRequester = playPauseFocusRequester,
                    timelineFocusRequester = timelineFocusRequester,
                    exoPlayer = player,
                    onPlaybackProgress = { _, _ -> },
                    onControlsEvent = {},
                )
            }
        }

        assertTrue(
            composeRule
                .onAllNodesWithText(metadata.title)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty(),
        )
        assertTrue(
            composeRule
                .onAllNodesWithTag(PlayerControlsTestTags.Timeline)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty(),
        )
        assertTrue(
            composeRule
                .onAllNodesWithTag(PlayerControlsTestTags.PlayPauseButton, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty(),
        )
    }

    @Test
    fun visibleExtendedMetadataShowsTitleAndPlaybackControls() {
        val metadata = testMetadata()

        composeRule.setContent {
            MaterialTheme {
                val context = LocalContext.current
                val player = remember { ExoPlayer.Builder(context).build() }
                val playPauseFocusRequester = remember { FocusRequester() }
                val timelineFocusRequester = remember { FocusRequester() }
                DisposableEffect(player) {
                    onDispose { player.release() }
                }
                PlayerOverlay(
                    metadata = metadata,
                    link = testLink(),
                    isPlaying = false,
                    controlsEnabled = true,
                    showExtendedMetadata = true,
                    showAudioTracksButton = false,
                    showVideoTracksButton = false,
                    showSyncButton = false,
                    showNextEpisodeButton = false,
                    playPauseFocusRequester = playPauseFocusRequester,
                    timelineFocusRequester = timelineFocusRequester,
                    exoPlayer = player,
                    onPlaybackProgress = { _, _ -> },
                    onControlsEvent = {},
                )
            }
        }

        assertTrue(
            composeRule
                .onAllNodesWithText(metadata.title)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty(),
        )
        assertTrue(
            composeRule
                .onAllNodesWithTag(PlayerControlsTestTags.Timeline)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty(),
        )
        assertTrue(
            composeRule
                .onAllNodesWithTag(PlayerControlsTestTags.PlayPauseButton, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty(),
        )
    }

    private fun testMetadata(): TvPlayerMetadata {
        return TvPlayerMetadata(
            title = "Overlay Test Title",
            subtitle = "2026 . Test API",
            backdropUri = null,
            year = 2026,
            apiName = "Test API",
            season = 1,
            episode = 2,
            episodeTitle = "Episode Name",
            isEpisodeBased = true,
        )
    }

    @Suppress("DEPRECATION")
    private fun testLink(): ExtractorLink {
        return ExtractorLink(
            source = "TestSource",
            name = "TestName",
            url = "https://example.com/video.mp4",
            referer = "https://example.com",
            quality = 1080,
            type = ExtractorLinkType.VIDEO,
        )
    }
}
