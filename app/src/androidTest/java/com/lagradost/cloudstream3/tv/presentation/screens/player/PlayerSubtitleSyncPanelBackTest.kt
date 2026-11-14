package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.screens.player.core.PlayerSessionController
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerNoticeStack
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelEffect
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsUiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression: Back must close the subtitle sync panel. On the Chromecast the Back key down was swallowed
 * together with the focus while the panel was open, so no BackHandler fired and the panel stayed.
 * Back is sent as a real key event through the activity window (as the remote does).
 */
@RunWith(AndroidJUnit4::class)
class PlayerSubtitleSyncPanelBackTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var session: PlayerSessionController? = null

    @After
    fun tearDown() {
        composeRule.runOnUiThread { session?.release() }
    }

    @Test
    fun backKeyClosesSubtitleSyncPanel() {
        val panelTitle = openSyncPanel()

        pressBack()

        waitForCount(panelTitle, expected = 0)
    }

    /** The "Custom value" dialog is a separate window: the first Back closes it, the second the panel. */
    @Test
    fun backKeyClosesCustomValueDialogFirstThenSyncPanel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val panelTitle = openSyncPanel()
        val dialogTitle = context.getString(R.string.subtitle_offset_title)

        composeRule.onAllNodesWithText(
            context.getString(R.string.tv_player_sync_custom_value),
            useUnmergedTree = true,
        )[0].performClick()
        waitForCount(dialogTitle, expected = 1)
        Thread.sleep(1_000)

        pressBack()
        waitForCount(dialogTitle, expected = 0)
        assertTrue("The panel stays after the dialog is closed", count(panelTitle) > 0)

        pressBack()
        waitForCount(panelTitle, expected = 0)
    }

    private fun count(text: String) = composeRule
        .onAllNodesWithText(text, useUnmergedTree = true)
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .size

    private fun waitForCount(text: String, expected: Int) {
        composeRule.waitUntil(timeoutMillis = 5_000) { count(text) == expected }
        assertTrue("\"$text\" nodes: expected $expected, was ${count(text)}", count(text) == expected)
    }

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    }

    /** Plays a local file with a subtitle, opens the sync panel from the controls; returns the panel title. */
    private fun openSyncPanel(): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val syncLabel = context.getString(R.string.tv_player_control_sync)
        val panelTitle = context.getString(R.string.subtitle_offset)
        val subtitle = testSubtitle(createVttFile(context))
        val wav = createSilentWav(context, seconds = 120)
        var controller: PlayerSessionController? = null
        composeRule.runOnUiThread { controller = PlayerSessionController(context).also { session = it } }

        composeRule.setContent {
            MaterialTheme {
                PlayerPlaybackScreen(
                    state = TvPlayerUiState.Ready(
                        metadata = TvPlayerMetadata(title = "Back test", subtitle = "", backdropUri = null),
                        link = testFileLink(wav),
                    ),
                    catalogState = PlayerCatalogUiState(
                        subtitles = persistentListOf(subtitle),
                        selectedSubtitle = subtitle,
                        selectedSubtitleId = subtitle.getId(),
                        selectedSubtitleIndex = 0,
                        subtitleSelectionSource = TvPlayerSubtitleSelectionSource.User,
                        isCurrentSourceReady = true,
                    ),
                    panelsState = TvPlayerPanelsUiState(),
                    seekPreferencesState = TvPlayerSeekPreferencesState(10_000L, 10_000L),
                    actions = noOpActions(),
                    panelEffects = MutableSharedFlow<TvPlayerPanelEffect>(),
                    notices = PlayerNoticeStack(),
                    playerSessionController = controller!!,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 20_000) { count(syncLabel) > 0 }
        composeRule.onAllNodesWithText(syncLabel, useUnmergedTree = true)[0].performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) { count(panelTitle) > 0 }
        // Let the panel take its initial focus, like a user pressing a D-pad key after it opened.
        Thread.sleep(1_000)
        composeRule.waitForIdle()
        return panelTitle
    }

    private fun noOpActions() = PlayerScreenActions(
        onBackPressed = {},
        onSkipLoading = {},
        onRetry = {},
        onPlaybackReady = {},
        onPlaybackEnded = { _, _ -> },
        onPlaybackProgress = { _, _ -> },
        onPlaybackStopped = { _, _ -> },
        onPlayNextEpisode = { _, _ -> },
        onRetrySource = {},
        onOpenPanel = {},
        onClosePanel = {},
        onDisableSubtitlesFromPlaybackError = {},
        onPanelItemAction = {},
        onPlaybackError = {},
        onOpenSubtitleFilePicker = {},
        onSubtitlesSidePanelBackPressed = { false },
        onEmbeddedSubtitlesChanged = {},
    )
}
