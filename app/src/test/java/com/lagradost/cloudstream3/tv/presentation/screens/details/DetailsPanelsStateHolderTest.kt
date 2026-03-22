package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatPanelItem
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatActionOutcome
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatSelectionRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailsPanelsStateHolderTest {
    @Test
    fun `navigateActionsBack pops nested selection before closing panel`() {
        val stateHolder = DetailsPanelsStateHolder()
        val root = createSelectionRequest(title = "Root")
        val nested = createSelectionRequest(title = "Nested")

        stateHolder.showRootActionSelection(root)
        stateHolder.showActionSelection(nested)

        stateHolder.navigateActionsBack()

        assertTrue(stateHolder.isActionsPanelVisible)
        assertEquals(root, stateHolder.currentActionSelection)

        stateHolder.navigateActionsBack()

        assertFalse(stateHolder.isActionsPanelVisible)
        assertNull(stateHolder.currentActionSelection)
    }

    @Test
    fun `openActionsPanel resets previous selection stack`() {
        val stateHolder = DetailsPanelsStateHolder()
        stateHolder.showRootActionSelection(createSelectionRequest(title = "Root"))
        stateHolder.showActionSelection(createSelectionRequest(title = "Nested"))

        stateHolder.openActionsPanel()

        assertTrue(stateHolder.isActionsPanelVisible)
        assertNull(stateHolder.currentActionSelection)
    }

    @Test
    fun `resetTransientState closes panels and clears loading flags`() {
        val stateHolder = DetailsPanelsStateHolder()
        stateHolder.openActionsPanel()
        stateHolder.openBookmarkPanel()
        stateHolder.updateActionInProgress(true)
        stateHolder.updatePanelLoading(true)
        stateHolder.showRootActionSelection(createSelectionRequest(title = "Root"))

        stateHolder.resetTransientState()

        assertFalse(stateHolder.isActionsPanelVisible)
        assertFalse(stateHolder.isBookmarkPanelVisible)
        assertFalse(stateHolder.isActionInProgress)
        assertFalse(stateHolder.isPanelLoading)
        assertNull(stateHolder.currentActionSelection)
    }

    private fun createSelectionRequest(
        title: String,
    ) = MovieDetailsCompatSelectionRequest(
        title = title,
        options = listOf(MovieDetailsCompatPanelItem(id = 1, label = "One")),
        onOptionSelected = { MovieDetailsCompatActionOutcome.Completed },
    )
}
