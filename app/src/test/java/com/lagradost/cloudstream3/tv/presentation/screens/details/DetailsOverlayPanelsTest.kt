package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionUiState
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatActionOutcome
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatPanelItem
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatSelectionRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailsOverlayPanelsTest {
    private val skipLoadingItem = MovieDetailsCompatPanelItem(
        id = SkipDownloadLoadingActionId,
        label = "Pomin",
    )

    @Test
    fun `resolveDetailsDownloadPanelContent returns skip action while loading sources`() {
        val content = resolveDetailsDownloadPanelContent(
            state = DownloadMirrorSelectionUiState(
                isVisible = true,
                isLoading = true,
                loadedSourcesCount = 2,
            ),
            skipLoadingItem = skipLoadingItem,
        )

        assertEquals(listOf(skipLoadingItem), content.items)
        assertTrue(content.showItemsWhileLoading)
    }

    @Test
    fun `resolveDetailsDownloadPanelContent keeps skip action after skip until options exist`() {
        val content = resolveDetailsDownloadPanelContent(
            state = DownloadMirrorSelectionUiState(
                isVisible = true,
                isLoading = true,
                loadedSourcesCount = 3,
                isLoadingUiSkipped = true,
            ),
            skipLoadingItem = skipLoadingItem,
        )

        assertEquals(listOf(skipLoadingItem), content.items)
        assertTrue(content.showItemsWhileLoading)
    }

    @Test
    fun `resolveDetailsDownloadPanelContent shows selection options after skip when available`() {
        val options = listOf(MovieDetailsCompatPanelItem(id = 7, label = "Source"))
        val content = resolveDetailsDownloadPanelContent(
            state = DownloadMirrorSelectionUiState(
                isVisible = true,
                isLoading = true,
                loadedSourcesCount = 3,
                isLoadingUiSkipped = true,
                selectionRequest = createSelectionRequest(options),
            ),
            skipLoadingItem = skipLoadingItem,
        )

        assertEquals(options, content.items)
        assertTrue(content.showItemsWhileLoading)
    }

    @Test
    fun `resolveDetailsDownloadPanelContent shows selection options after loading`() {
        val options = listOf(MovieDetailsCompatPanelItem(id = 8, label = "Source"))
        val content = resolveDetailsDownloadPanelContent(
            state = DownloadMirrorSelectionUiState(
                isVisible = true,
                isLoading = false,
                selectionRequest = createSelectionRequest(options),
            ),
            skipLoadingItem = skipLoadingItem,
        )

        assertEquals(options, content.items)
        assertFalse(content.showItemsWhileLoading)
    }

    private fun createSelectionRequest(
        options: List<MovieDetailsCompatPanelItem>,
    ) = MovieDetailsCompatSelectionRequest(
        title = "Sources",
        options = options,
        targetEpisodeId = 123,
        onOptionSelected = { MovieDetailsCompatActionOutcome.Completed },
    )
}
