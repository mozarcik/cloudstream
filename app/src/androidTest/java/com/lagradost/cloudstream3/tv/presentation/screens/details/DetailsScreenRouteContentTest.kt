package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.tv.material3.MaterialTheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.ShowStatus
import com.lagradost.cloudstream3.TrailerData
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DetailsLoadingPreview
import com.lagradost.cloudstream3.ui.WatchType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Focus moves into the collapsing page asynchronously; the emulator needs more than 2 s. */
private const val FocusTimeoutMs = 6_000L

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DetailsScreenRouteContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun doneState_rendersPlayButtonBeforeSecondaryContentFinishes() {
        setDoneStateContent()

        composeRule.onNodeWithTag("details_play_button").assertIsDisplayed()
    }

    @Test
    fun doneState_requestsInitialFocusForPlayButton() {
        setDoneStateContent()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("details_play_button").assertIsFocused()
    }

    @Test
    fun doneState_rendersLegacyHeroMetadataAndOriginalTitle() {
        setDoneStateContent()

        val expectedRating = "8.4"
        val expectedOriginalTitle = composeRule.activity.getString(
            R.string.details_original_title_format,
            "Legacy Details"
        )
        val expectedStatus = composeRule.activity.getString(R.string.status_ongoing)

        composeRule.onNodeWithTag("details_title_text").assertIsDisplayed()
        // The provider name sits in the upper-cased eyebrow ("TV SERIES • TEST API").
        composeRule.onNodeWithText("Test API", substring = true, ignoreCase = true).assertIsDisplayed()
        composeRule.onNodeWithText(expectedRating).assertIsDisplayed()
        composeRule.onNodeWithText(expectedStatus).assertIsDisplayed()
        composeRule.onNodeWithText("TV-14").assertIsDisplayed()
        composeRule.onNodeWithText(expectedOriginalTitle).assertIsDisplayed()
        composeRule.onNodeWithText("Action, Drama").assertIsDisplayed()
    }

    @Test
    fun doneState_showsTextTitleEvenWhenLogoIsAvailable() {
        setDoneStateContent(
            details = fakeDetails().copy(logoUri = "https://example.com/logo.png")
        )

        composeRule.onNodeWithTag("details_title_text").assertIsDisplayed()
        composeRule.onAllNodesWithTag("details_title_logo").assertCountEquals(0)
    }

    @Test
    fun doneState_heroShowsRuntimeAndGenresInsteadOfDescription() {
        setDoneStateContent(details = fakeDetails().copy(duration = "165min"))

        composeRule.onNodeWithTag("details_genres").assertIsDisplayed()
        composeRule.onNodeWithText("2h 45min").assertIsDisplayed()
        // The story lives in the Details tab only; the hero no longer repeats it.
        composeRule.onAllNodesWithTag("details_description").assertCountEquals(0)
        composeRule.onAllNodesWithText(fakeDetails().description).assertCountEquals(0)
    }

    @Test
    fun doneState_downFromPlayFocusesEpisodesTab() {
        setDoneStateContent()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("details_play_button").assertIsFocused()

        composeRule.onNodeWithTag("details_play_button").performKeyInput {
            pressKey(Key.DirectionDown)
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("details_tab_Episodes").assertIsFocused()
    }

    @Test
    fun doneState_detailsTabFollowsEpisodesTab() {
        setDoneStateContent()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("details_play_button").performKeyInput {
            pressKey(Key.DirectionDown)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("details_tab_Episodes").performKeyInput {
            pressKey(Key.DirectionRight)
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("details_tab_Info").assertIsFocused()
    }

    @Test
    fun doneState_downFromEpisodesTabFocusesFirstEpisode() {
        setDoneStateContent()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("details_play_button").performKeyInput {
            pressKey(Key.DirectionDown)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("details_tab_Episodes").performKeyInput {
            pressKey(Key.DirectionDown)
        }

        composeRule.waitUntil(timeoutMillis = FocusTimeoutMs) {
            isNodeFocused("episode_card_episode-1")
        }
    }

    @Test
    fun doneState_upToEpisodeOne_heroPlayDoesNotStealFocusWithoutExtraInput() {
        setDoneStateContent(details = fakeDetailsWithEpisodes(episodeCount = 10))

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("details_play_button").assertIsFocused()

        composeRule.onNodeWithTag("details_play_button").performKeyInput {
            pressKey(Key.DirectionDown)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("details_tab_Episodes").assertIsFocused()

        composeRule.onNodeWithTag("details_tab_Episodes").performKeyInput {
            pressKey(Key.DirectionDown)
        }
        composeRule.waitUntil(timeoutMillis = FocusTimeoutMs) {
            isNodeFocused("episode_card_episode-1")
        }

        repeat(7) { index ->
            composeRule.onNodeWithTag("episode_card_episode-${index + 1}").performKeyInput {
                pressKey(Key.DirectionDown)
            }
            composeRule.waitUntil(timeoutMillis = FocusTimeoutMs) {
                isNodeFocused("episode_card_episode-${index + 2}")
            }
        }


        repeat(6) { offset ->
            val sourceEpisode = 8 - offset
            val targetEpisode = sourceEpisode - 1
            composeRule.onNodeWithTag("episode_card_episode-$sourceEpisode").performKeyInput {
                pressKey(Key.DirectionUp)
            }
            composeRule.waitUntil(timeoutMillis = FocusTimeoutMs) {
                isNodeFocused("episode_card_episode-$targetEpisode")
            }
        }

        assertTrue(isNodeFocused("episode_card_episode-2"))

        composeRule.mainClock.autoAdvance = false
        try {
            var episodeOneFocusedAtLeastOnce = false
            var heroPlayFocusedAfterEpisodeOne = false
            val focusTrace = StringBuilder()

            composeRule.onNodeWithTag("episode_card_episode-2").performKeyInput {
                pressKey(Key.DirectionUp)
            }

            repeat(120) { frame ->
                composeRule.mainClock.advanceTimeByFrame()
                composeRule.waitForIdle()

                val episodeOneFocused = isNodeFocused("episode_card_episode-1")
                val heroPlayFocused = isNodeFocused("details_play_button")
                focusTrace.append(
                    "frame=$frame episode1=$episodeOneFocused hero=$heroPlayFocused\n"
                )

                if (episodeOneFocused) {
                    episodeOneFocusedAtLeastOnce = true
                } else if (episodeOneFocusedAtLeastOnce && heroPlayFocused) {
                    heroPlayFocusedAfterEpisodeOne = true
                }
            }

            assertTrue(focusTrace.toString(), episodeOneFocusedAtLeastOnce)
            assertFalse(focusTrace.toString(), heroPlayFocusedAfterEpisodeOne)
            assertTrue(focusTrace.toString(), isNodeFocused("episode_card_episode-1"))
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun loadingState_rendersPreviewTitlePlaceholder() {
        composeRule.setContent {
            MaterialTheme {
                DetailsScreenLoadingStateContent(
                    mode = DetailsScreenMode.Media,
                    state = DetailsScreenUiState.Loading(
                        preview = DetailsLoadingPreview(
                            title = "Preview title",
                            posterUri = "poster",
                        )
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Preview title").assertIsDisplayed()
    }
}

private fun fakeDetails(): MovieDetails {
    val episode = TvEpisode(
        id = "episode-1",
        data = "episode-data-1",
        seasonNumber = 1,
        episodeNumber = 1,
        title = "Pilot",
        description = "Episode description",
        durationMinutes = 45,
        ratingText = "8.1",
        releaseDateMillis = 1_700_000_000_000L,
        posterUri = "episode-poster",
    )
    val season = TvSeason(
        id = "season-1",
        seasonNumber = 1,
        displaySeasonNumber = 1,
        title = "Season 1",
        episodes = listOf(episode),
    )

    return MovieDetails(
        id = "details-route-test",
        name = "Details",
        originalTitle = "Legacy Details",
        description = "Description ".repeat(24),
        posterUri = "poster",
        seasons = listOf(season),
        seasonCount = 1,
        episodeCount = 1,
        currentSeason = 1,
        currentEpisode = 1,
        providerName = "Test API",
        type = TvType.TvSeries,
        score = Score.from(84, 100),
        showStatus = ShowStatus.Ongoing,
        pgRating = "TV-14",
        releaseDate = "2024",
        categories = listOf("Action", "Drama"),
        duration = "45min",
    )
}

private fun fakeDetailsWithEpisodes(episodeCount: Int): MovieDetails {
    val season = TvSeason(
        id = "season-1",
        seasonNumber = 1,
        displaySeasonNumber = 1,
        title = "Season 1",
        episodes = (1..episodeCount).map { episodeNumber ->
            TvEpisode(
                id = "episode-$episodeNumber",
                data = "episode-data-$episodeNumber",
                seasonNumber = 1,
                episodeNumber = episodeNumber,
                title = "Episode $episodeNumber",
                description = "Episode description $episodeNumber",
                durationMinutes = 44 + episodeNumber,
                ratingText = "8.$episodeNumber",
                releaseDateMillis = 1_700_000_000_000L + (episodeNumber * 86_400_000L),
                posterUri = "episode-poster-$episodeNumber",
            )
        },
    )

    return fakeDetails().copy(
        seasons = listOf(season),
        episodeCount = episodeCount,
    )
}

private class FakeDetailsRouteLoadResponse : LoadResponse {
    override var name: String = "Fake title"
    override var url: String = "fake-url"
    override var apiName: String = "fake-api"
    override var type: TvType = TvType.TvSeries
    override var posterUrl: String? = "poster"
    override var year: Int? = 2024
    override var plot: String? = "plot"
    override var score: Score? = null
    override var tags: List<String>? = emptyList()
    override var duration: Int? = null
    override var trailers: MutableList<TrailerData> = mutableListOf()
    override var recommendations: List<SearchResponse>? = emptyList()
    override var actors: List<ActorData>? = emptyList()
    override var comingSoon: Boolean = false
    override var syncData: MutableMap<String, String> = mutableMapOf()
    override var posterHeaders: Map<String, String>? = emptyMap()
    override var backgroundPosterUrl: String? = null
    override var logoUrl: String? = null
    override var contentRating: String? = null
    override var uniqueUrl: String = "unique-url"
}

private fun DetailsScreenRouteContentTest.setDoneStateContent(
    details: MovieDetails = fakeDetails(),
) {
    composeRule.setContent {
        MaterialTheme {
            DetailsScreenContent(
                mode = DetailsScreenMode.TvSeries,
                details = details,
                actionsCompat = MovieDetailsEpisodeActionsCompat(
                    loadResponse = FakeDetailsRouteLoadResponse()
                ),
                isSecondaryContentLoading = true,
                goToPlayer = {},
                onBackPressed = {},
                onManualSearchRequested = {},
                refreshScreenWithNewItem = {},
                onFavoriteClick = {},
                onBookmarkClick = {},
            )
        }
    }
}

/** An episode card is a focus group: the focused node is its inner clickable, so descendants count too. */
private fun DetailsScreenRouteContentTest.isNodeFocused(tag: String): Boolean {
    return composeRule
        .onAllNodes(
            hasTestTag(tag) and (isFocused() or hasAnyDescendant(isFocused())),
            useUnmergedTree = true,
        )
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .isNotEmpty()
}

private fun DetailsScreenRouteContentTest.doesNodeExist(tag: String): Boolean {
    return composeRule
        .onAllNodes(hasTestTag(tag), useUnmergedTree = true)
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .isNotEmpty()
}
