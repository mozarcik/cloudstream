package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.tv.material3.MaterialTheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TrailerData
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DetailsLoadingPreview
import com.lagradost.cloudstream3.tv.presentation.screens.unavailable.UnavailableDetailsUiModel
import com.lagradost.cloudstream3.ui.WatchType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailsScreenRouteContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun doneState_rendersPlayButtonBeforeSecondaryContentFinishes() {
        composeRule.setContent {
            MaterialTheme {
                DetailsScreenRouteContent(
                    mode = DetailsScreenMode.TvSeries,
                    uiState = DetailsScreenUiState.Done(
                        details = fakeDetails(),
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

        composeRule.onNodeWithTag("details_play_button").assertIsDisplayed()
    }

    @Test
    fun doneState_requestsInitialFocusForPlayButton() {
        composeRule.setContent {
            MaterialTheme {
                DetailsScreenRouteContent(
                    mode = DetailsScreenMode.TvSeries,
                    uiState = DetailsScreenUiState.Done(
                        details = fakeDetails(),
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

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("details_play_button").assertIsFocused()
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
    return MovieDetails(
        id = "details-route-test",
        name = "Details",
        description = "Description",
        posterUri = "poster",
        seasonCount = 2,
        episodeCount = 8,
        currentSeason = 2,
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
    override var type: TvType = TvType.Movie
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
