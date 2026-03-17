package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.pressKey
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionUiState
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatActionOutcome
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatPanelItem
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatSelectionRequest
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsDownloadActionState
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsQuickAction
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.EpisodeCard
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import kotlinx.coroutines.delay
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DetailsEpisodeDownloadFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun seasonOneEpisodeThree_downloadFlowUpdatesEpisodeButtonProgress() {
        val loadingText = composeRule.activity.getString(R.string.loading)
        val downloadingLabel = composeRule.activity.getString(R.string.downloading)
        val expectedProgressLabel = "$downloadingLabel (35%)"
        val sourceLabel = "Source 1"
        val sourceActionTag = "movie_action_1"
        val debugStateTag = "details_episode_download_debug"
        val downloadTriggerTag = "episode_3_download_trigger"
        val episodeThreeActionTag = "episode_episode-3_action_download"

        composeRule.setContent {
            MaterialTheme {
                DetailsEpisodeDownloadFlowHarness(
                    sourceLabel = sourceLabel,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(
                episodeThreeActionTag,
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(downloadTriggerTag).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.textValueForNode(debugStateTag).contains("selected=episode-3")
        }
        assertTrue(composeRule.textValueForNode(debugStateTag).contains("visible=true"))

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(DetailsScreenMode.TvSeries.downloadPanelTestTag)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(DetailsScreenMode.TvSeries.downloadPanelTestTag).assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(loadingText).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(loadingText).assertIsDisplayed()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(sourceLabel).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(sourceActionTag).performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(DetailsScreenMode.TvSeries.downloadPanelTestTag)
                .fetchSemanticsNodes().isEmpty()
        }
        assertTrue(composeRule.textValueForNode(debugStateTag).contains("selected=episode-3"))
        assertTrue(composeRule.textValueForNode(debugStateTag).contains("visible=false"))
        assertTrue(composeRule.textValueForNode(debugStateTag).contains("progress=0.35"))

        composeRule.onAllNodesWithTag(DetailsScreenMode.TvSeries.downloadPanelTestTag).assertCountEquals(0)
        composeRule.onNodeWithTag(
            episodeThreeActionTag,
            useUnmergedTree = true,
        ).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                expectedProgressLabel,
            )
        )
    }
}

@Composable
private fun DetailsEpisodeDownloadFlowHarness(
    sourceLabel: String,
) {
    val logTag = "DetailsEpisodeDownloadFlowTest"
    val episodes = remember { fakeSeasonOneEpisodes() }
    val details = remember { fakeDownloadFlowDetails(episodes) }
    val panelsStateHolder = remember { DetailsPanelsStateHolder() }
    var selectedEpisodeId by remember { mutableStateOf<String?>(null) }
    var downloadMirrorState by remember { mutableStateOf(DownloadMirrorSelectionUiState()) }
    var episodeDownloadStates by remember { mutableStateOf<Map<String, DetailsDownloadButtonUiState>>(emptyMap()) }
    val episodeThree = remember(episodes) {
        episodes.first { episode -> episode.episodeNumber == 3 }
    }
    val startEpisodeDownload: (TvEpisode) -> Unit = { episode ->
        Log.d(
            logTag,
            "download flow trigger episodeId=${episode.id} season=${episode.seasonNumber} episode=${episode.episodeNumber}"
        )
        selectedEpisodeId = episode.id
        downloadMirrorState = DownloadMirrorSelectionUiState(
            isVisible = true,
            isLoading = true,
        )
    }

    LaunchedEffect(downloadMirrorState.isVisible, downloadMirrorState.isLoading, selectedEpisodeId) {
        if (!downloadMirrorState.isVisible || !downloadMirrorState.isLoading) return@LaunchedEffect

        val selectedEpisode = episodes.firstOrNull { episode -> episode.id == selectedEpisodeId } ?: return@LaunchedEffect
        Log.d(
            logTag,
            "loading sources for episodeId=${selectedEpisode.id} season=${selectedEpisode.seasonNumber} episode=${selectedEpisode.episodeNumber}"
        )
        delay(1_000)
        downloadMirrorState = downloadMirrorState.copy(
            isLoading = false,
            loadedSourcesCount = 1,
            selectionRequest = MovieDetailsCompatSelectionRequest(
                title = "Download mirror",
                options = listOf(
                    MovieDetailsCompatPanelItem(
                        id = 1,
                        label = sourceLabel,
                    )
                ),
                targetEpisodeId = 10_000 + (selectedEpisode.episodeNumber ?: 0),
                targetSeasonNumber = selectedEpisode.seasonNumber,
                targetEpisodeNumber = selectedEpisode.episodeNumber,
                onOptionSelected = { MovieDetailsCompatActionOutcome.Completed },
            ),
        )
        Log.d(logTag, "sources ready for episodeId=${selectedEpisode.id}")
    }

    LaunchedEffect(downloadMirrorState) {
        Log.d(
            logTag,
            "panel visible=${downloadMirrorState.isVisible} loading=${downloadMirrorState.isLoading} " +
                "loaded=${downloadMirrorState.loadedSourcesCount} hasSelection=${downloadMirrorState.selectionRequest != null}"
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                items = episodes,
                key = { episode -> episode.id }
            ) { episode ->
                EpisodeCard(
                    episode = episode,
                    fallbackDescription = details.description,
                    onEpisodeSelected = {},
                    onEpisodeQuickActionClick = { clickedEpisode, action ->
                        Log.d(
                            logTag,
                            "quick action clicked episodeId=${clickedEpisode.id} action=$action"
                        )
                        if (action == MovieDetailsQuickAction.Download) {
                            startEpisodeDownload(clickedEpisode)
                        }
                    },
                    downloadActionState = episodeDownloadStates[episode.id]
                        ?.toMovieDetailsDownloadActionState()
                        ?: MovieDetailsDownloadActionState.Idle,
                    forceActionsVisible = true,
                )
            }
        }

        Box(
            modifier = Modifier
                .testTag("episode_3_download_trigger")
                .clickable { startEpisodeDownload(episodeThree) }
        ) {
            Text(text = "Trigger episode 3 download")
        }

        val selectedDownloadState = selectedEpisodeId?.let(episodeDownloadStates::get)
        Text(
            text = "selected=$selectedEpisodeId visible=${downloadMirrorState.isVisible} " +
                "loading=${downloadMirrorState.isLoading} progress=${selectedDownloadState?.progressFraction ?: 0f}",
            modifier = Modifier.testTag("details_episode_download_debug")
        )

        DetailsOverlayPanels(
            mode = DetailsScreenMode.TvSeries,
            details = details,
            panelsStateHolder = panelsStateHolder,
            downloadMirrorState = downloadMirrorState,
            onActionSelected = {},
            onDownloadActionSelected = { actionId ->
                val selectedEpisode = episodes.firstOrNull { episode -> episode.id == selectedEpisodeId }
                if (selectedEpisode != null && actionId == 1) {
                    Log.d(
                        logTag,
                        "source selected actionId=$actionId episodeId=${selectedEpisode.id}"
                    )
                    episodeDownloadStates = episodeDownloadStates + (
                        selectedEpisode.id to DetailsDownloadButtonUiState(
                            episodeId = 10_000 + (selectedEpisode.episodeNumber ?: 0),
                            status = VideoDownloadManager.DownloadType.IsDownloading,
                            progressFraction = 0.35f,
                        )
                    )
                    downloadMirrorState = DownloadMirrorSelectionUiState()
                }
            },
            onCloseActionsPanel = {},
            onCloseDownloadPanel = {
                Log.d(logTag, "download panel closed")
                downloadMirrorState = DownloadMirrorSelectionUiState()
            },
            onSkipDownloadLoading = {},
            onCloseBookmarkPanel = {},
            onBookmarkSelected = {},
        )
    }
}

private fun fakeSeasonOneEpisodes(): List<TvEpisode> {
    return (1..3).map { episodeNumber ->
        TvEpisode(
            id = "episode-$episodeNumber",
            data = "episode-data-$episodeNumber",
            seasonNumber = 1,
            episodeNumber = episodeNumber,
            title = "Episode $episodeNumber",
            description = "Description $episodeNumber",
            durationMinutes = 45,
            ratingText = "8.$episodeNumber",
            releaseDateMillis = 1_700_000_000_000L + (episodeNumber * 60_000L),
            posterUri = "poster-$episodeNumber",
        )
    }
}

private fun fakeDownloadFlowDetails(
    episodes: List<TvEpisode>,
): MovieDetails {
    return MovieDetails(
        id = "details-download-flow-test",
        name = "Details Download Flow",
        description = "Test description",
        posterUri = "poster",
        backdropUri = "backdrop",
        seasons = listOf(
            TvSeason(
                id = "season-1",
                seasonNumber = 1,
                displaySeasonNumber = 1,
                title = "Season 1",
                episodes = episodes,
            )
        ),
    )
}

private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.textValueForNode(tag: String): String {
    return onNodeWithTag(tag)
        .fetchSemanticsNode()
        .config[SemanticsProperties.Text]
        .joinToString(separator = "") { it.text }
}
