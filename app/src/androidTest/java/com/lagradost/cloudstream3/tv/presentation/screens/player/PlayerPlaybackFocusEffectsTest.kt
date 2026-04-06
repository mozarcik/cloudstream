package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerOverlayStateHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerPlaybackFocusEffectsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun startupOverlayShowsExtendedMetadataImmediatelyOnDevice() {
        lateinit var overlayState: PlayerOverlayStateHolder

        composeRule.setContent {
            val playPauseFocusRequester = remember { FocusRequester() }
            val rootFocusRequester = remember { FocusRequester() }
            val controlsInteractionEvents = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
            overlayState = remember {
                PlayerOverlayStateHolder(
                    initialIsPlaying = true,
                    initialPlaybackState = Player.STATE_READY,
                    initialPlayWhenReady = true,
                )
            }

            Box {
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(playPauseFocusRequester)
                        .focusable(),
                )
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(rootFocusRequester)
                        .focusable(),
                )

                PlayerPlaybackFocusEffects(
                    overlayState = overlayState,
                    hasSidePanel = false,
                    controlsInteractionEvents = controlsInteractionEvents,
                    playPauseFocusRequester = playPauseFocusRequester,
                    rootFocusRequester = rootFocusRequester,
                )

                if (overlayState.showExtendedMetadata) {
                    Box(modifier = Modifier.testTag(ExtendedMetadataTag))
                }
            }
        }

        composeRule.runOnIdle {
            assertTrue(overlayState.controlsVisible)
            assertTrue(overlayState.startupAutoHideArmed)
            assertTrue(overlayState.showExtendedMetadata)
        }

        assertTrue(
            composeRule
                .onAllNodesWithTag(ExtendedMetadataTag)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty(),
        )
    }

    @Test
    fun pausedPlaybackRevealsExtendedMetadataAfterDelayOnDevice() {
        lateinit var overlayState: PlayerOverlayStateHolder

        composeRule.setContent {
            val playPauseFocusRequester = remember { FocusRequester() }
            val rootFocusRequester = remember { FocusRequester() }
            val controlsInteractionEvents = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
            overlayState = remember {
                PlayerOverlayStateHolder(
                    initialIsPlaying = false,
                    initialPlaybackState = Player.STATE_READY,
                    initialPlayWhenReady = false,
                ).apply {
                    startupAutoHideArmed = false
                    showExtendedMetadata = false
                }
            }

            Box {
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(playPauseFocusRequester)
                        .focusable(),
                )
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(rootFocusRequester)
                        .focusable(),
                )

                PlayerPlaybackFocusEffects(
                    overlayState = overlayState,
                    hasSidePanel = false,
                    controlsInteractionEvents = controlsInteractionEvents,
                    playPauseFocusRequester = playPauseFocusRequester,
                    rootFocusRequester = rootFocusRequester,
                )

                if (overlayState.showExtendedMetadata) {
                    Box(modifier = Modifier.testTag(ExtendedMetadataTag))
                }
            }
        }

        composeRule.runOnIdle {
            assertTrue(overlayState.controlsVisible)
            assertFalse(overlayState.showExtendedMetadata)
            assertFalse(overlayState.startupAutoHideArmed)
        }

        composeRule.waitUntil(timeoutMillis = 7_000L) {
            overlayState.showExtendedMetadata
        }

        assertTrue(
            composeRule
                .onAllNodesWithTag(ExtendedMetadataTag)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty(),
        )
    }

    private companion object {
        const val ExtendedMetadataTag = "player_extended_metadata"
    }
}
