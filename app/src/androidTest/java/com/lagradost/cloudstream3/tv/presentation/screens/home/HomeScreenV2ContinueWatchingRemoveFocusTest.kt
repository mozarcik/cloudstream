package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.compat.home.ContinueWatchingImagePrefetcher
import com.lagradost.cloudstream3.tv.compat.home.ContinueWatchingRepository
import com.lagradost.cloudstream3.tv.compat.home.FeaturedItemCompat
import com.lagradost.cloudstream3.tv.compat.home.FeaturedRepository
import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import com.lagradost.cloudstream3.tv.compat.home.FeedRepository
import com.lagradost.cloudstream3.tv.compat.home.HomeSourceSelectionRepository
import com.lagradost.cloudstream3.tv.compat.home.MainPageRequest
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.compat.home.MediaListCompat
import com.lagradost.cloudstream3.tv.compat.home.SourcePreferencesRepository
import com.lagradost.cloudstream3.tv.compat.home.SourcePreferencesState
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository
import com.lagradost.cloudstream3.tv.compat.resume.ContinueWatchingState
import com.lagradost.cloudstream3.tv.compat.resume.ResumePlaybackContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val HomeContinueWatchingRemoveButtonTag = "home_continue_watching_remove_button"
private const val HomeContinueWatchingResumeButtonTag = "home_continue_watching_resume_button"
private const val HomeTestTopBarButtonTag = "home_test_top_bar_button"
private const val FirstContinueWatchingCardTag = "continue_watching_card_cw-1"
private const val SecondContinueWatchingCardTag = "continue_watching_card_cw-2"

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class HomeScreenV2ContinueWatchingRemoveFocusTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun removingFirstContinueWatchingItem_keepsFocusOnRemoveForNextItem() {
        val fakeApi = testSource(name = "Test source")
        val secondApi = testSource(name = "Alt source")
        val originalApis = APIHolder.apis
        val continueWatchingRepository = MutableContinueWatchingRepository(
            items = listOf(
                testContinueWatchingItem(id = "cw-1", parentId = 101, name = "First item"),
                testContinueWatchingItem(id = "cw-2", parentId = 202, name = "Second item"),
            )
        )

        try {
            APIHolder.apis = listOf(fakeApi, secondApi)
            SourceRepository.selectApi(fakeApi)

            composeRule.setContent {
                MaterialTheme {
                    HomeScreenContinueWatchingRemoveFocusHarness(
                        selectedSourceName = fakeApi.name,
                        continueWatchingRepository = continueWatchingRepository,
                    )
                }
            }

            waitUntilTextVisible("First item")
            waitUntilNodeVisible(SecondContinueWatchingCardTag)
            waitUntilNodeFocused(HomeContinueWatchingResumeButtonTag)

            moveFocusFromResumeToRemove()
            waitUntilNodeFocused(HomeContinueWatchingRemoveButtonTag)

            composeRule.onNodeWithTag(HomeContinueWatchingRemoveButtonTag).performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionCenter)
            }

            waitUntilTextMissing("First item")
            waitUntilTextVisible("Second item")
            waitUntilNodeFocused(HomeContinueWatchingRemoveButtonTag, timeoutMillis = 5_000L)

            composeRule.onNodeWithTag(HomeContinueWatchingRemoveButtonTag).assertIsFocused()
        } finally {
            APIHolder.apis = originalApis
        }
    }

    @Test
    fun removingSecondContinueWatchingItem_keepsFocusOnRemoveForPreviousItem() {
        val fakeApi = testSource(name = "Test source")
        val secondApi = testSource(name = "Alt source")
        val originalApis = APIHolder.apis
        val continueWatchingRepository = MutableContinueWatchingRepository(
            items = listOf(
                testContinueWatchingItem(id = "cw-1", parentId = 101, name = "First item"),
                testContinueWatchingItem(id = "cw-2", parentId = 202, name = "Second item"),
            )
        )

        try {
            APIHolder.apis = listOf(fakeApi, secondApi)
            SourceRepository.selectApi(fakeApi)

            composeRule.setContent {
                MaterialTheme {
                    HomeScreenContinueWatchingRemoveFocusHarness(
                        selectedSourceName = fakeApi.name,
                        continueWatchingRepository = continueWatchingRepository,
                    )
                }
            }

            waitUntilTextVisible("First item")
            waitUntilNodeVisible(SecondContinueWatchingCardTag)
            waitUntilNodeFocused(HomeContinueWatchingResumeButtonTag)

            composeRule.onNodeWithTag(HomeContinueWatchingResumeButtonTag).performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionDown)
                pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
                pressKey(androidx.compose.ui.input.key.Key.DirectionUp)
            }
            waitUntilNodeFocused(HomeContinueWatchingResumeButtonTag)

            moveFocusFromResumeToRemove()
            waitUntilNodeFocused(HomeContinueWatchingRemoveButtonTag)

            composeRule.onNodeWithTag(HomeContinueWatchingRemoveButtonTag).performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionCenter)
            }

            waitUntilTextVisible("First item")
            waitUntilTextMissing("Second item")
            waitUntilNodeFocused(HomeContinueWatchingRemoveButtonTag, timeoutMillis = 5_000L)

            composeRule.onNodeWithTag(HomeContinueWatchingRemoveButtonTag).assertIsFocused()
        } finally {
            APIHolder.apis = originalApis
        }
    }

    private fun moveFocusFromResumeToRemove() {
        composeRule.onNodeWithTag(HomeContinueWatchingResumeButtonTag).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
            pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
        }
    }

    private fun waitUntilNodeFocused(tag: String, timeoutMillis: Long = 2_000L) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule
                .onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .any { node ->
                    node.config.getOrElse(SemanticsProperties.Focused) { false }
                }
        }
    }

    private fun waitUntilNodeVisible(tag: String, timeoutMillis: Long = 5_000L) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule
                .onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private fun waitUntilTextVisible(text: String, timeoutMillis: Long = 5_000L) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule
                .onAllNodesWithText(text, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private fun waitUntilTextMissing(text: String, timeoutMillis: Long = 5_000L) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule
                .onAllNodesWithText(text, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
    }
}

@Composable
private fun HomeScreenContinueWatchingRemoveFocusHarness(
    selectedSourceName: String,
    continueWatchingRepository: ContinueWatchingRepository,
) {
    val topBarFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = {},
            modifier = Modifier
                .testTag(HomeTestTopBarButtonTag)
                .focusRequester(topBarFocusRequester)
        ) {
            Text(text = "Top bar")
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            HomeScreenV2(
                onMediaClick = {},
                onContinueWatchingPlay = {},
                onOpenFeedGrid = {},
                onScroll = {},
                topBarFocusRequester = topBarFocusRequester,
                sourcesViewModelFactory = HomeSourcesViewModelFactory(
                    sourcePreferencesRepository = RemoveFocusFakeSourcePreferencesRepository(),
                    homeSourceSelectionRepository = RemoveFocusFakeHomeSourceSelectionRepository(
                        selectedSourceName = selectedSourceName
                    ),
                ),
                continueWatchingViewModelFactory = HomeContinueWatchingViewModelFactory(
                    continueWatchingRepository = continueWatchingRepository,
                    continueWatchingImagePrefetcher = ContinueWatchingImagePrefetcher(context),
                ),
                featuredViewModelFactory = HomeFeaturedViewModelFactory(
                    featuredRepository = RemoveFocusFakeFeaturedRepository()
                ),
                feedsViewModelFactory = HomeFeedsViewModelFactory(
                    feedRepository = RemoveFocusFakeFeedRepository()
                ),
            )
        }
    }
}

private fun testSource(name: String): MainAPI {
    return object : MainAPI() {
        override var name = name
        override val supportedTypes = emptySet<TvType>()
        override var lang = ""
        override val hasMainPage = true
    }
}

private fun testContinueWatchingItem(
    id: String,
    parentId: Int,
    name: String,
): MediaItemCompat.Movie {
    return MediaItemCompat.Movie(
        id = id,
        url = "https://example.com/$id",
        apiName = "Local",
        name = name,
        posterUri = "",
        type = TvType.Movie,
        score = null,
        continueWatching = ContinueWatchingState(
            progress = 0.5f,
            remainingMs = 120_000L,
            hasBackdrop = false,
            resume = ResumePlaybackContext(
                parentId = parentId,
                episodeId = null,
                season = null,
                episode = null,
                isFromDownload = false,
            )
        )
    )
}

private class MutableContinueWatchingRepository(
    items: List<MediaItemCompat>,
    private val loadDelayMs: Long = 250L,
) : ContinueWatchingRepository {
    private var currentItems: List<MediaItemCompat> = items.toList()

    override suspend fun getItems(): Result<MediaListCompat> {
        delay(loadDelayMs)
        return Result.success(currentItems)
    }

    override suspend fun removeItem(parentId: Int): Result<Unit> {
        currentItems = currentItems.filterNot { item ->
            item.continueWatching?.parentId == parentId
        }
        return Result.success(Unit)
    }
}

private class RemoveFocusFakeSourcePreferencesRepository : SourcePreferencesRepository {
    private val preferencesState = MutableStateFlow(SourcePreferencesState())

    override val state: Flow<SourcePreferencesState> = preferencesState

    override suspend fun setPinned(sourceId: String, pinned: Boolean) = Unit

    override suspend fun incrementUsage(sourceId: String) = Unit
}

private class RemoveFocusFakeHomeSourceSelectionRepository(
    private val selectedSourceName: String?,
) : HomeSourceSelectionRepository {
    override suspend fun getSelectedSourceName(): String? = selectedSourceName

    override suspend fun setSelectedSourceName(sourceName: String?) = Unit
}

private class RemoveFocusFakeFeaturedRepository : FeaturedRepository {
    override suspend fun getItems(api: MainAPI): Result<List<FeaturedItemCompat>> {
        return Result.success(emptyList())
    }
}

private class RemoveFocusFakeFeedRepository : FeedRepository {
    override fun getFeedCategories(api: MainAPI): Flow<List<FeedCategory>> {
        return flowOf(
            listOf(
                FeedCategory(
                    id = "feed-1",
                    name = "Feed",
                    mainPageRequest = MainPageRequest(data = "feed", name = "Feed")
                )
            )
        )
    }

    override suspend fun getMediaForFeed(
        api: MainAPI,
        category: FeedCategory,
        page: Int,
    ): Result<MediaListCompat> {
        return Result.success(persistentListOf())
    }
}
