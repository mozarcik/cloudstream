package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatPanelItem
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatSelectionRequest
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatActionOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailsPanelsStateHolderTest {
    @Test
    fun `navigateActionsBack clears selection before closing panel`() {
        val stateHolder = DetailsPanelsStateHolder()
        stateHolder.showActionSelection(
            MovieDetailsCompatSelectionRequest(
                title = "Sources",
                options = listOf(MovieDetailsCompatPanelItem(id = 1, label = "One")),
                onOptionSelected = { MovieDetailsCompatActionOutcome.Completed },
            )
        )

        stateHolder.navigateActionsBack()

        assertTrue(stateHolder.isActionsPanelVisible)
        assertNull(stateHolder.panelSelection)

        stateHolder.navigateActionsBack()

        assertFalse(stateHolder.isActionsPanelVisible)
    }

    @Test
    fun `openActionsPanel resets previous selection and items`() {
        val stateHolder = DetailsPanelsStateHolder()
        stateHolder.updatePanelItems(listOf(MovieDetailsCompatPanelItem(id = 1, label = "Old")))
        stateHolder.showActionSelection(
            MovieDetailsCompatSelectionRequest(
                title = "Old selection",
                options = emptyList(),
                onOptionSelected = { MovieDetailsCompatActionOutcome.Completed },
            )
        )

        stateHolder.openActionsPanel()

        assertTrue(stateHolder.isActionsPanelVisible)
        assertNull(stateHolder.panelSelection)
        assertEquals(emptyList<MovieDetailsCompatPanelItem>(), stateHolder.panelItems)
    }

    @Test
    fun `resetTransientState closes panels and clears loading flags`() {
        val stateHolder = DetailsPanelsStateHolder()
        stateHolder.openActionsPanel()
        stateHolder.openBookmarkPanel()
        stateHolder.updateActionInProgress(true)
        stateHolder.updatePanelLoading(true)
        stateHolder.updatePanelItems(listOf(MovieDetailsCompatPanelItem(id = 7, label = "Item")))

        stateHolder.resetTransientState()

        assertFalse(stateHolder.isActionsPanelVisible)
        assertFalse(stateHolder.isBookmarkPanelVisible)
        assertFalse(stateHolder.isActionInProgress)
        assertFalse(stateHolder.isPanelLoading)
        assertEquals(emptyList<MovieDetailsCompatPanelItem>(), stateHolder.panelItems)
    }
}
