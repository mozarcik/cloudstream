package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.tv.material3.MaterialTheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatPanelItem
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieActionsSidePanel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DetailsDownloadPanelFocusTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun downloadPanel_keepsVisibleWhenLoadingItemsReplaceSkipAction() {
        var loading by mutableStateOf(true)
        var panelItems by mutableStateOf(
            listOf(
                MovieDetailsCompatPanelItem(
                    id = SkipDownloadLoadingActionId,
                    label = "Skip loading",
                )
            )
        )
        var closeRequests = 0

        composeRule.setContent {
            MaterialTheme {
                MovieActionsSidePanel(
                    visible = true,
                    loading = loading,
                    inProgress = false,
                    title = "Download",
                    items = panelItems,
                    onCloseRequested = { closeRequests += 1 },
                    onActionSelected = {},
                    panelTestTag = "download_panel_focus_test",
                    showItemsWhileLoading = true,
                    closeOnFocusExit = false,
                )
            }
        }

        composeRule.onNodeWithText("Skip loading").assertIsDisplayed()

        composeRule.runOnIdle {
            panelItems = listOf(
                MovieDetailsCompatPanelItem(
                    id = 7,
                    label = "Source 1",
                )
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Source 1").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("download_panel_focus_test").assertIsDisplayed()
        composeRule.onNodeWithText("Source 1").assertIsDisplayed()
        composeRule.onAllNodesWithText("Skip loading").assertCountEquals(0)
        composeRule.runOnIdle {
            assertEquals(0, closeRequests)
            loading = false
        }
    }
}
