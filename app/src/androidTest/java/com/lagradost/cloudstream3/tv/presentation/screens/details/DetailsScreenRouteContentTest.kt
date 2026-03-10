package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import com.lagradost.cloudstream3.tv.presentation.screens.unavailable.UnavailableDetailsUiModel
import com.lagradost.cloudstream3.ui.WatchType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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

        val expectedRating = composeRule.activity.getString(R.string.rating_format, "8.4")
        val expectedOriginalTitle = composeRule.activity.getString(
            R.string.details_original_title_format,
            "Legacy Details"
        )
        val expectedStatus = composeRule.activity.getString(R.string.status_ongoing)

        composeRule.onNodeWithTag("details_title_text").assertIsDisplayed()
        composeRule.onNodeWithText("Test API").assertIsDisplayed()
        composeRule.onNodeWithText(expectedRating).assertIsDisplayed()
        composeRule.onNodeWithText(expectedStatus).assertIsDisplayed()
        composeRule.onNodeWithText("TV-14").assertIsDisplayed()
        composeRule.onNodeWithText(expectedOriginalTitle).assertIsDisplayed()
        composeRule.onNodeWithText("Action, Drama").assertIsDisplayed()
    }

    @Test
    fun doneState_descriptionClickTogglesExpandedState() {
        setDoneStateContent()

        val descriptionNode = composeRule.onNodeWithTag("details_description")
        val playButtonNode = composeRule.onNodeWithTag("details_play_button")

        descriptionNode.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "collapsed"
            )
        )

        playButtonNode.performKeyInput {
            pressKey(Key.DirectionUp)
        }

        composeRule.waitForIdle()
        descriptionNode.assertIsFocused()
        descriptionNode.performKeyInput {
            pressKey(Key.DirectionCenter)
        }

        composeRule.waitForIdle()
        descriptionNode.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "expanded"
            )
        )

        descriptionNode.performKeyInput {
            pressKey(Key.DirectionCenter)
        }

        composeRule.waitForIdle()
        descriptionNode.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "collapsed"
            )
        )
    }

    @Test
    fun doneState_upFromPlayFocusesDescription() {
        setDoneStateContent()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("details_play_button").assertIsFocused()

        composeRule.onNodeWithTag("details_play_button").performKeyInput {
            pressKey(Key.DirectionUp)
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("details_description").assertIsFocused()
    }

    @Test
    fun doneState_downFromPlayFocusesSeasonSelector() {
        setDoneStateContent()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("details_play_button").assertIsFocused()

        composeRule.onNodeWithTag("details_play_button").performKeyInput {
            pressKey(Key.DirectionDown)
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("details_season_tab_season-1").assertIsFocused()
    }

    @Test
    fun loadingState_rendersPreviewTitlePlaceholder() {
        composeRule.setContent {
            MaterialTheme {
                DetailsScreenRouteContent(
                    mode = DetailsScreenMode.Media,
                    uiState = DetailsScreenUiState.Loading(
                        preview = DetailsLoadingPreview(
                            title = "Preview title",
                            posterUri = "poster",
                        )
                    ),
                    actionsCompat = null,
                    shouldShowUnavailableState = false,
                    unavailableDetails = fakeUnavailableDetails(),
                    canRemoveFromLibrary = false,
                    goToPlayer = {},
                    onBackPressed = {},
                    onManualSearchRequested = {},
                    refreshScreenWithNewItem = {},
                    onFavoriteClick = {},
                    onBookmarkClick = {},
                    onRemoveUnavailable = {},
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

private fun fakeUnavailableDetails(): UnavailableDetailsUiModel {
    return UnavailableDetailsUiModel(
        title = "Unavailable",
        posterUrl = null,
        backdropUrl = null,
        description = null,
        type = TvType.Movie,
        year = null,
        providerName = "Provider",
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
            DetailsScreenRouteContent(
                mode = DetailsScreenMode.TvSeries,
                uiState = DetailsScreenUiState.Done(
                    details = details,
                    isSecondaryContentLoading = true,
                ),
                actionsCompat = MovieDetailsEpisodeActionsCompat(
                    loadResponse = FakeDetailsRouteLoadResponse()
                ),
                shouldShowUnavailableState = false,
                unavailableDetails = fakeUnavailableDetails(),
                canRemoveFromLibrary = false,
                goToPlayer = {},
                onBackPressed = {},
                onManualSearchRequested = {},
                refreshScreenWithNewItem = {},
                onFavoriteClick = {},
                onBookmarkClick = {},
                onRemoveUnavailable = {},
            )
        }
    }
}
