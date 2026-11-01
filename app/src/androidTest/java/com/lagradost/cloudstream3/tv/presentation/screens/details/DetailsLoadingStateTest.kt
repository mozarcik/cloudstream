package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DetailsLoadingPreview
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** D19 (loading state): the placeholder keeps the final hero's title slot so the swap does not shift anything. */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DetailsLoadingStateTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setLoading(preview: DetailsLoadingPreview, mode: DetailsScreenMode = DetailsScreenMode.Media) {
        composeRule.setContent {
            MaterialTheme {
                DetailsScreenLoadingStateContent(mode = mode, state = DetailsScreenUiState.Loading(preview))
            }
        }
    }

    /** Layout (clipped) height of the title slot; the unclipped bounds would include the overflowing text. */
    private fun titleSlotHeightDp(): Float {
        val node = composeRule.onNodeWithTag("details_loading_title").fetchSemanticsNode()
        return node.boundsInRoot.height / composeRule.density.density
    }

    @Test
    fun loading_withPreviewTitle_showsTitleInOneLineSlot() {
        setLoading(DetailsLoadingPreview(title = "Short"))

        composeRule.onNodeWithText("Short").assertIsDisplayed()
        assertEquals(TvDimens.DetailsTitleLineHeight.value, titleSlotHeightDp(), 0.5f)
    }

    @Test
    fun loading_withLongTitle_usesTwoLineSlot() {
        setLoading(DetailsLoadingPreview(title = "A very long title ".repeat(8)))

        assertEquals((TvDimens.DetailsTitleLineHeight * 2).value, titleSlotHeightDp(), 0.5f)
    }

    @Test
    fun loading_withoutPreviewInMediaMode_showsNoPlaceholderTitle() {
        setLoading(DetailsLoadingPreview())

        composeRule.onAllNodesWithTag("details_loading_title").assertCountEquals(0)
    }

    @Test
    fun loading_withoutPreviewInMovieMode_fallsBackToTypeLabel() {
        setLoading(DetailsLoadingPreview(), mode = DetailsScreenMode.Movie)

        composeRule.onNodeWithTag("details_loading_title").assertIsDisplayed()
    }
}
