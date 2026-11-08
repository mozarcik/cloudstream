package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import com.lagradost.cloudstream3.tv.compat.home.MainPageRequest
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.compat.resume.ContinueWatchingState
import com.lagradost.cloudstream3.tv.compat.resume.ResumePlaybackContext
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class HomeScreenV2ContentNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun hiddenSourcesRow_continueWatchingCardsReachFirstFeedAndBack() {
        val onlySource = testSource(name = "Local")

        composeRule.setContent {
            MaterialTheme {
                HomeScreenV2Content(
                    sourcesUiState = HomeSourcesUiState(
                        selectedSource = onlySource,
                        allSources = persistentListOf(onlySource),
                        quickSources = persistentListOf(onlySource),
                    ),
                    continueWatchingUiState = HomeContinueWatchingUiState(
                        state = HomeFeedLoadState.Success(
                            persistentListOf(testContinueWatchingItem())
                        )
                    ),
                    feedsUiState = HomeFeedsUiState(
                        feedSections = persistentListOf(
                            HomeFeedSectionUiState(
                                feed = FeedCategory(
                                    id = "feed-1",
                                    name = "Trending",
                                    mainPageRequest = MainPageRequest(
                                        data = "feed-data",
                                        name = "Trending"
                                    )
                                ),
                                state = HomeFeedLoadState.Success(
                                    persistentListOf(testFeedItem())
                                )
                            )
                        ),
                        isFeedListLoading = false
                    ),
                    onMediaClick = {},
                    onContinueWatchingPlay = {},
                    onOpenFeedGrid = { _ -> },
                    onSourceSelected = { _ -> },
                    onMorePanelOpenChange = {},
                    onTogglePin = { _ -> },
                    onRefreshFeed = { _ -> },
                    onRemoveContinueWatching = { _ -> },
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag("sources_row").assertCountEquals(0)
        composeRule.onNodeWithTag("home_continue_watching_resume_button").assertIsFocused()

        composeRule.onNodeWithTag("home_continue_watching_resume_button").performKeyInput {
            pressKey(Key.DirectionDown)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("continue_watching_card_cw-1").assertIsFocused()

        composeRule.onNodeWithTag("continue_watching_card_cw-1").performKeyInput {
            pressKey(Key.DirectionDown)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home:feed:feed-1:slot:0", useUnmergedTree = true).assertIsFocused()

        composeRule.onNodeWithTag("home:feed:feed-1:slot:0", useUnmergedTree = true).performKeyInput {
            pressKey(Key.DirectionUp)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("continue_watching_card_cw-1").assertIsFocused()
    }

    private fun testSource(
        name: String,
    ): MainAPI {
        return object : MainAPI() {
            override var name = name
            override val supportedTypes = emptySet<TvType>()
            override var lang = ""
            override val hasMainPage = true
        }
    }

    private fun testContinueWatchingItem(): MediaItemCompat.Movie {
        return MediaItemCompat.Movie(
            id = "cw-1",
            url = "https://example.com/cw-1",
            apiName = "Local",
            name = "Continue Watching",
            posterUri = "",
            type = TvType.Movie,
            score = null,
            continueWatching = ContinueWatchingState(
                progress = 0.5f,
                remainingMs = 120_000L,
                hasBackdrop = false,
                resume = ResumePlaybackContext(
                    parentId = 1,
                    episodeId = null,
                    season = null,
                    episode = null,
                    isFromDownload = false
                )
            )
        )
    }


    private fun testFeedItem(): MediaItemCompat.Movie {
        return MediaItemCompat.Movie(
            id = "feed-item-1",
            url = "https://example.com/feed-item-1",
            apiName = "Local",
            name = "Feed Item",
            posterUri = "",
            type = TvType.Movie,
            score = null,
        )
    }
}
