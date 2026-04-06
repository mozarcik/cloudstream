package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.CloudStreamApp.Companion.removeKeys
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.compat.home.ContinueWatchingImagePrefetcher
import com.lagradost.cloudstream3.tv.compat.home.ContinueWatchingRepository
import com.lagradost.cloudstream3.tv.compat.home.ContinueWatchingRepositoryImpl
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
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.VideoWatchState
import com.lagradost.cloudstream3.utils.DOWNLOAD_HEADER_CACHE
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.RESULT_RESUME_WATCHING
import com.lagradost.cloudstream3.utils.VIDEO_POS_DUR
import com.lagradost.cloudstream3.utils.VideoDownloadHelper
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class HomeScreenV2ContinueWatchingRefreshTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun resumedHome_refreshesContinueWatchingContent() {
        val fakeApi = testSource(name = "Test source")
        val originalApis = APIHolder.apis
        val continueWatchingRepository = FakeContinueWatchingRepository(
            items = listOf(testContinueWatchingItem(name = "Old progress"))
        )

        try {
            APIHolder.apis = listOf(fakeApi)
            SourceRepository.selectApi(fakeApi)

            composeRule.setContent {
                MaterialTheme {
                    HomeScreenV2(
                        onMediaClick = {},
                        onContinueWatchingPlay = {},
                        onOpenFeedGrid = {},
                        onScroll = {},
                        topBarFocusRequester = FocusRequester(),
                        sourcesViewModelFactory = HomeSourcesViewModelFactory(
                            sourcePreferencesRepository = FakeSourcePreferencesRepository(),
                            homeSourceSelectionRepository = FakeHomeSourceSelectionRepository(
                                selectedSourceName = fakeApi.name
                            ),
                        ),
                        continueWatchingViewModelFactory = HomeContinueWatchingViewModelFactory(
                            continueWatchingRepository = continueWatchingRepository,
                            continueWatchingImagePrefetcher = ContinueWatchingImagePrefetcher(
                                composeRule.activity.applicationContext
                            ),
                        ),
                        featuredViewModelFactory = HomeFeaturedViewModelFactory(
                            featuredRepository = FakeFeaturedRepository()
                        ),
                        feedsViewModelFactory = HomeFeedsViewModelFactory(
                            feedRepository = FakeFeedRepository()
                        ),
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5_000L) {
                doesTextExist("Old progress")
            }
            composeRule.onNodeWithText("Old progress").assertIsDisplayed()

            continueWatchingRepository.items = listOf(
                testContinueWatchingItem(name = "Updated progress")
            )

            composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

            composeRule.waitUntil(timeoutMillis = 5_000L) {
                doesTextExist("Updated progress")
            }

            composeRule.onNodeWithText("Updated progress").assertIsDisplayed()
            composeRule.onAllNodesWithText("Old progress", useUnmergedTree = true).assertCountEquals(0)
        } finally {
            APIHolder.apis = originalApis
        }
    }

    @Test
    fun resumedHome_hidesZeroRemainingWhenNextEpisodeHasCompletedStoredProgress() {
        val fakeApi = testSource(name = "Test source")
        val originalApis = APIHolder.apis
        val parentId = 42
        val episodeDurationMs = 30L * 60L * 1000L
        val initialRemainingMs = 5L * 60L * 1000L
        val episodeFourId = 3_004
        val episodeFiveId = 3_005
        val seriesUrl = "https://example.com/show"
        val seriesName = "Sample Show"
        val remainingSuffix = composeRule.activity.getString(R.string.continue_watching_remaining_suffix)
        val initialMetadata = continueWatchingMetadata(
            season = 3,
            episode = 4,
            remainingMs = initialRemainingMs,
            remainingSuffix = remainingSuffix,
        )
        val staleZeroRemainingMetadata = continueWatchingMetadata(
            season = 3,
            episode = 5,
            remainingMs = 0L,
            remainingSuffix = remainingSuffix,
        )

        clearContinueWatchingStore()

        try {
            APIHolder.apis = listOf(fakeApi)
            SourceRepository.selectApi(fakeApi)
            seedContinueWatchingHeader(
                parentId = parentId,
                apiName = fakeApi.name,
                url = seriesUrl,
                name = seriesName,
            )

            DataStoreHelper.setViewPosAndResume(
                id = episodeFourId,
                position = episodeDurationMs - initialRemainingMs,
                duration = episodeDurationMs,
                currentEpisode = seriesEpisode(
                    id = episodeFourId,
                    parentId = parentId,
                    apiName = fakeApi.name,
                    season = 3,
                    episode = 4,
                ),
                nextEpisode = seriesEpisode(
                    id = episodeFiveId,
                    parentId = parentId,
                    apiName = fakeApi.name,
                    season = 3,
                    episode = 5,
                ),
            )

            composeRule.setContent {
                MaterialTheme {
                    HomeScreenV2(
                        onMediaClick = {},
                        onContinueWatchingPlay = {},
                        onOpenFeedGrid = {},
                        onScroll = {},
                        topBarFocusRequester = FocusRequester(),
                        sourcesViewModelFactory = HomeSourcesViewModelFactory(
                            sourcePreferencesRepository = FakeSourcePreferencesRepository(),
                            homeSourceSelectionRepository = FakeHomeSourceSelectionRepository(
                                selectedSourceName = fakeApi.name
                            ),
                        ),
                        continueWatchingViewModelFactory = HomeContinueWatchingViewModelFactory(
                            continueWatchingRepository = ContinueWatchingRepositoryImpl(),
                            continueWatchingImagePrefetcher = ContinueWatchingImagePrefetcher(
                                composeRule.activity.applicationContext
                            ),
                        ),
                        featuredViewModelFactory = HomeFeaturedViewModelFactory(
                            featuredRepository = FakeFeaturedRepository()
                        ),
                        feedsViewModelFactory = HomeFeedsViewModelFactory(
                            feedRepository = FakeFeedRepository()
                        ),
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5_000L) {
                doesTextExist(initialMetadata)
            }
            composeRule.onNodeWithText(initialMetadata).assertIsDisplayed()

            DataStoreHelper.setViewPos(
                id = episodeFiveId,
                pos = episodeDurationMs,
                dur = episodeDurationMs,
            )
            DataStoreHelper.setViewPosAndResume(
                id = episodeFourId,
                position = episodeDurationMs,
                duration = episodeDurationMs,
                currentEpisode = seriesEpisode(
                    id = episodeFourId,
                    parentId = parentId,
                    apiName = fakeApi.name,
                    season = 3,
                    episode = 4,
                ),
                nextEpisode = seriesEpisode(
                    id = episodeFiveId,
                    parentId = parentId,
                    apiName = fakeApi.name,
                    season = 3,
                    episode = 5,
                ),
            )

            composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

            composeRule.waitUntil(timeoutMillis = 5_000L) {
                doesTextExist("S3:E5") && !doesTextExist(staleZeroRemainingMetadata)
            }

            composeRule.onNodeWithText("S3:E5").assertIsDisplayed()
            composeRule
                .onAllNodesWithText(staleZeroRemainingMetadata, useUnmergedTree = true)
                .assertCountEquals(0)
        } finally {
            clearContinueWatchingStore()
            APIHolder.apis = originalApis
        }
    }

    @Test
    fun resumedHome_afterResumeActionAndMockPlayerBack_showsNextEpisodeInContinueWatching() {
        val fakeApi = testSource(name = "Test source")
        val originalApis = APIHolder.apis
        val parentId = 84
        val episodeDurationMs = 30L * 60L * 1000L
        val initialRemainingMs = 5L * 60L * 1000L
        val episodeFourId = 4_004
        val episodeFiveId = 4_005
        val seriesUrl = "https://example.com/show-resume"
        val seriesName = "Resume Show"
        val remainingSuffix = composeRule.activity.getString(R.string.continue_watching_remaining_suffix)
        val initialMetadata = continueWatchingMetadata(
            season = 3,
            episode = 4,
            remainingMs = initialRemainingMs,
            remainingSuffix = remainingSuffix,
        )

        clearContinueWatchingStore()

        try {
            APIHolder.apis = listOf(fakeApi)
            SourceRepository.selectApi(fakeApi)
            seedContinueWatchingHeader(
                parentId = parentId,
                apiName = fakeApi.name,
                url = seriesUrl,
                name = seriesName,
            )
            DataStoreHelper.setViewPosAndResume(
                id = episodeFourId,
                position = episodeDurationMs - initialRemainingMs,
                duration = episodeDurationMs,
                currentEpisode = seriesEpisode(
                    id = episodeFourId,
                    parentId = parentId,
                    apiName = fakeApi.name,
                    season = 3,
                    episode = 4,
                ),
                nextEpisode = seriesEpisode(
                    id = episodeFiveId,
                    parentId = parentId,
                    apiName = fakeApi.name,
                    season = 3,
                    episode = 5,
                ),
            )

            composeRule.setContent {
                MaterialTheme {
                    HomeScreenWithFakePlayerHarness(
                        selectedSourceName = fakeApi.name,
                        parentId = parentId,
                        apiName = fakeApi.name,
                        episodeFourId = episodeFourId,
                        episodeFiveId = episodeFiveId,
                        episodeDurationMs = episodeDurationMs,
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 5_000L) {
                doesTextExist(initialMetadata)
            }
            composeRule.onNodeWithText(initialMetadata).assertIsDisplayed()

            composeRule.onNodeWithTag("home_continue_watching_resume_button").assertIsFocused()
            composeRule.onNodeWithTag("home_continue_watching_resume_button").performKeyInput {
                pressKey(Key.DirectionCenter)
            }
            composeRule.waitUntil(timeoutMillis = 5_000L) {
                composeRule.onAllNodesWithTag("fake_player_screen")
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeRule.onNodeWithTag("fake_player_screen").assertIsDisplayed()
            composeRule.onNodeWithTag("fake_player_mark_95_button").performClick()
            composeRule.onNodeWithTag("fake_player_back_button").performClick()
            composeRule.waitUntil(timeoutMillis = 5_000L) {
                composeRule.onAllNodesWithTag("fake_player_screen")
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isEmpty()
            }

            composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

            composeRule.waitUntil(timeoutMillis = 5_000L) {
                doesTextExist("S3:E5")
            }

            composeRule.onNodeWithText("S3:E5").assertIsDisplayed()
            composeRule
                .onAllNodesWithText(initialMetadata, useUnmergedTree = true)
                .assertCountEquals(0)
        } finally {
            clearContinueWatchingStore()
            APIHolder.apis = originalApis
        }
    }

    private fun doesTextExist(text: String): Boolean {
        return composeRule
            .onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()
    }

    private fun clearContinueWatchingStore() {
        DataStoreHelper.deleteAllResumeStateIds()
        removeKeys("${DataStoreHelper.currentAccount}/$VIDEO_POS_DUR")
        removeKeys("${DataStoreHelper.currentAccount}/$RESULT_RESUME_WATCHING")
        removeKeys(DOWNLOAD_HEADER_CACHE)
    }

    private fun seedContinueWatchingHeader(
        parentId: Int,
        apiName: String,
        url: String,
        name: String,
    ) {
        setKey(
            DOWNLOAD_HEADER_CACHE,
            parentId.toString(),
            VideoDownloadHelper.DownloadHeaderCached(
                apiName = apiName,
                url = url,
                type = TvType.TvSeries,
                name = name,
                poster = "https://example.com/poster.jpg",
                backdrop = "https://example.com/backdrop.jpg",
                cacheTime = System.currentTimeMillis(),
                id = parentId,
            )
        )
    }

    private fun seriesEpisode(
        id: Int,
        parentId: Int,
        apiName: String,
        season: Int,
        episode: Int,
    ): ResultEpisode {
        return ResultEpisode(
            headerName = "Sample Show",
            name = "Episode $episode",
            poster = "https://example.com/poster.jpg",
            episode = episode,
            seasonIndex = season,
            season = season,
            data = "https://example.com/show/s${season}e${episode}",
            apiName = apiName,
            id = id,
            index = episode - 1,
            position = 0L,
            duration = 0L,
            score = null,
            description = null,
            isFiller = false,
            tvType = TvType.TvSeries,
            parentId = parentId,
            videoWatchState = VideoWatchState.None,
            totalEpisodeIndex = null,
            airDate = null,
            runTime = null,
            seasonData = null,
        )
    }

    private fun continueWatchingMetadata(
        season: Int,
        episode: Int,
        remainingMs: Long,
        remainingSuffix: String,
    ): String {
        val totalSeconds = remainingMs / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "S${season}:E${episode} • ${String.format("%d:%02d %s", minutes, seconds, remainingSuffix)}"
    }

    private fun testSource(name: String): MainAPI {
        return object : MainAPI() {
            override var name = name
            override val supportedTypes = emptySet<TvType>()
            override var lang = ""
            override val hasMainPage = true
        }
    }

    private fun testContinueWatchingItem(name: String): MediaItemCompat.Movie {
        return MediaItemCompat.Movie(
            id = "cw-1",
            url = "https://example.com/$name",
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
                    parentId = 1,
                    episodeId = null,
                    season = null,
                    episode = null,
                    isFromDownload = false,
                )
            )
        )
    }
}

@Composable
private fun HomeScreenWithFakePlayerHarness(
    selectedSourceName: String,
    parentId: Int,
    apiName: String,
    episodeFourId: Int,
    episodeFiveId: Int,
    episodeDurationMs: Long,
) {
    var isFakePlayerVisible by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreenV2(
            onMediaClick = {},
            onContinueWatchingPlay = {
                isFakePlayerVisible = true
            },
            onOpenFeedGrid = {},
            onScroll = {},
            topBarFocusRequester = FocusRequester(),
            sourcesViewModelFactory = HomeSourcesViewModelFactory(
                sourcePreferencesRepository = FakeSourcePreferencesRepository(),
                homeSourceSelectionRepository = FakeHomeSourceSelectionRepository(
                    selectedSourceName = selectedSourceName
                ),
            ),
            continueWatchingViewModelFactory = HomeContinueWatchingViewModelFactory(
                continueWatchingRepository = ContinueWatchingRepositoryImpl(),
                continueWatchingImagePrefetcher = ContinueWatchingImagePrefetcher(
                    composeRuleContext()
                ),
            ),
            featuredViewModelFactory = HomeFeaturedViewModelFactory(
                featuredRepository = FakeFeaturedRepository()
            ),
            feedsViewModelFactory = HomeFeedsViewModelFactory(
                feedRepository = FakeFeedRepository()
            ),
        )

        if (isFakePlayerVisible) {
            FakePlayerOverlay(
                parentId = parentId,
                apiName = apiName,
                episodeFourId = episodeFourId,
                episodeFiveId = episodeFiveId,
                episodeDurationMs = episodeDurationMs,
                onBack = {
                    isFakePlayerVisible = false
                }
            )
        }
    }
}

@Composable
private fun FakePlayerOverlay(
    parentId: Int,
    apiName: String,
    episodeFourId: Int,
    episodeFiveId: Int,
    episodeDurationMs: Long,
    onBack: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            .testTag("fake_player_screen")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Fake Player")
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                    DataStoreHelper.setViewPosAndResume(
                        id = episodeFourId,
                        position = episodeDurationMs * 95L / 100L,
                        duration = episodeDurationMs,
                        currentEpisode = testSeriesEpisode(
                            id = episodeFourId,
                            parentId = parentId,
                            apiName = apiName,
                            season = 3,
                            episode = 4,
                        ),
                        nextEpisode = testSeriesEpisode(
                            id = episodeFiveId,
                            parentId = parentId,
                            apiName = apiName,
                            season = 3,
                            episode = 5,
                        ),
                    )
                    }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .testTag("fake_player_mark_95_button")
            ) {
                Text(
                    text = "Mark 95%",
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .testTag("fake_player_back_button")
            ) {
                Text(
                    text = "Back",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun composeRuleContext() = androidx.compose.ui.platform.LocalContext.current

private fun testSeriesEpisode(
    id: Int,
    parentId: Int,
    apiName: String,
    season: Int,
    episode: Int,
): ResultEpisode {
    return ResultEpisode(
        headerName = "Sample Show",
        name = "Episode $episode",
        poster = "https://example.com/poster.jpg",
        episode = episode,
        seasonIndex = season,
        season = season,
        data = "https://example.com/show/s${season}e${episode}",
        apiName = apiName,
        id = id,
        index = episode - 1,
        position = 0L,
        duration = 0L,
        score = null,
        description = null,
        isFiller = false,
        tvType = TvType.TvSeries,
        parentId = parentId,
        videoWatchState = VideoWatchState.None,
        totalEpisodeIndex = null,
        airDate = null,
        runTime = null,
        seasonData = null,
    )
}

private class FakeContinueWatchingRepository(
    var items: List<MediaItemCompat>,
) : ContinueWatchingRepository {
    override suspend fun getItems(): Result<MediaListCompat> = Result.success(items)

    override suspend fun removeItem(parentId: Int): Result<Unit> = Result.success(Unit)
}

private class FakeSourcePreferencesRepository : SourcePreferencesRepository {
    private val preferencesState = MutableStateFlow(SourcePreferencesState())

    override val state: Flow<SourcePreferencesState> = preferencesState

    override suspend fun setPinned(sourceId: String, pinned: Boolean) = Unit

    override suspend fun incrementUsage(sourceId: String) = Unit
}

private class FakeHomeSourceSelectionRepository(
    private val selectedSourceName: String?,
) : HomeSourceSelectionRepository {
    override suspend fun getSelectedSourceName(): String? = selectedSourceName

    override suspend fun setSelectedSourceName(sourceName: String?) = Unit
}

private class FakeFeaturedRepository : FeaturedRepository {
    override suspend fun getItems(api: MainAPI): Result<List<FeaturedItemCompat>> {
        return Result.success(emptyList())
    }
}

private class FakeFeedRepository : FeedRepository {
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
