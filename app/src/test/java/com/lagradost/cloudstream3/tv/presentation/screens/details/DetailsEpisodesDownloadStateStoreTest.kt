package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.utils.VideoDownloadManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailsEpisodesDownloadStateStoreTest {
    @Test
    fun `updateDetailsEpisodeDownloadStateByKey inserts pending state when episode was not hydrated yet`() {
        val nextStates = updateDetailsEpisodeDownloadStateByKey(
            currentStates = emptyMap(),
            episodeKey = "episode-key",
            fallbackEpisodeId = 42,
        ) { current ->
            current.withStatus(VideoDownloadManager.DownloadType.IsPending)
        }

        val state = nextStates.getValue("episode-key")
        assertEquals(42, state.episodeId)
        assertEquals(VideoDownloadManager.DownloadType.IsPending, state.status)
    }

    @Test
    fun `mergeHydratedEpisodeDownloadStates keeps optimistic pending when loaded snapshot is empty`() {
        val currentStates = mapOf(
            "episode-key" to DetailsDownloadButtonUiState(
                episodeId = 42,
                status = VideoDownloadManager.DownloadType.IsPending,
                progressFraction = 0f,
            )
        )
        val loadedStates = mapOf(
            "episode-key" to DetailsDownloadButtonUiState()
        )

        val mergedStates = mergeHydratedEpisodeDownloadStates(
            currentStates = currentStates,
            loadedStates = loadedStates,
        )

        assertEquals(currentStates.getValue("episode-key"), mergedStates.getValue("episode-key"))
    }

    @Test
    fun `mergeHydratedEpisodeDownloadStates applies loaded snapshot when it has real data`() {
        val currentStates = mapOf(
            "episode-key" to DetailsDownloadButtonUiState(
                episodeId = 42,
                status = VideoDownloadManager.DownloadType.IsPending,
                progressFraction = 0f,
            )
        )
        val loadedStates = mapOf(
            "episode-key" to DetailsDownloadButtonUiState(
                episodeId = 42,
                status = VideoDownloadManager.DownloadType.IsDownloading,
                progressFraction = 0.35f,
            )
        )

        val mergedStates = mergeHydratedEpisodeDownloadStates(
            currentStates = currentStates,
            loadedStates = loadedStates,
        )

        val mergedState = mergedStates.getValue("episode-key")
        assertEquals(VideoDownloadManager.DownloadType.IsDownloading, mergedState.status)
        assertEquals(0.35f, mergedState.progressFraction, 0f)
        assertTrue(mergedState.episodeId == 42)
    }
}
