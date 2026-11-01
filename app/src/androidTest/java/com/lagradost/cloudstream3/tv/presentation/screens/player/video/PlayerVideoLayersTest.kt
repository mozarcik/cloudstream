package com.lagradost.cloudstream3.tv.presentation.screens.player.video

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerSubtitleSyncController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerVideoLayersTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun videoAndSubtitleLayersStayMountedWhenControlsAndResizeModeChange() {
        val controlsVisible = mutableStateOf(false)
        val resizeMode = mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT)

        composeRule.setContent {
            val context = LocalContext.current
            val player = remember { ExoPlayer.Builder(context).build() }
            val subtitleSyncController = remember { TvPlayerSubtitleSyncController(initialSubtitleDelayMs = 0L) }

            DisposableEffect(player) {
                onDispose {
                    subtitleSyncController.clearSubtitleView()
                    subtitleSyncController.clearTextRenderer()
                    player.release()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                TvPlayerVideoSurface(
                    player = player,
                    resizeMode = resizeMode.value,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(VIDEO_LAYER_TAG),
                )
                TvPlayerSubtitleLayer(
                    player = player,
                    subtitleSyncController = subtitleSyncController,
                    controlsVisible = controlsVisible.value,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(SUBTITLE_LAYER_TAG),
                )
            }
        }

        composeRule.onAllNodesWithTag(VIDEO_LAYER_TAG).assertCountEquals(1)
        composeRule.onAllNodesWithTag(SUBTITLE_LAYER_TAG).assertCountEquals(1)

        composeRule.runOnIdle {
            controlsVisible.value = true
            resizeMode.value = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }

        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(VIDEO_LAYER_TAG).assertCountEquals(1)
        composeRule.onAllNodesWithTag(SUBTITLE_LAYER_TAG).assertCountEquals(1)
    }

    private companion object {
        const val VIDEO_LAYER_TAG = "player_video_layer"
        const val SUBTITLE_LAYER_TAG = "player_subtitle_layer"
    }
}
