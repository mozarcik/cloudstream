package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSubtitleEncodingControllerTest {
    @Test
    fun `encoding panel focuses currently selected option`() {
        val panelItems = buildSubtitleEncodingPanelItems(
            options = encodingOptions(),
            selectedValue = "Windows-1250",
        )

        val selectedItem = panelItems.items.first { item -> item.selected }
        assertEquals(selectedItem.id, panelItems.initialFocusedItemId)
        assertEquals("Eastern European (Windows-1250)", selectedItem.title)
    }

    @Test
    fun `encoding controller returns focus to encoding row after back`() {
        val controller = PlayerSubtitleEncodingController()

        assertTrue(controller.openSelection())
        val selectionContent = controller.buildPanelContent(
            options = encodingOptions(),
            selectedValue = "UTF-8",
        )
        assertEquals(TvPlayerSubtitlePanelScreen.EncodingSelection, selectionContent.screen)
        assertEquals(
            selectionContent.items.first { item -> item.selected }.id,
            selectionContent.initialFocusedItemId,
        )

        assertTrue(controller.navigateBack())
        val returnContent = controller.buildPanelContent(
            options = encodingOptions(),
            selectedValue = "UTF-8",
        )
        assertEquals(TvPlayerSubtitlePanelScreen.Main, returnContent.screen)
        assertEquals(
            TvPlayerSubtitlePanelNavigationDirection.Backward,
            returnContent.direction,
        )
        assertEquals(SubtitleEncodingItemId, returnContent.overrideMainInitialFocusedItemId)

        val consumedContent = controller.buildPanelContent(
            options = encodingOptions(),
            selectedValue = "UTF-8",
        )
        assertEquals(TvPlayerSubtitlePanelScreen.Main, consumedContent.screen)
        assertEquals(
            TvPlayerSubtitlePanelNavigationDirection.Forward,
            consumedContent.direction,
        )
        assertNull(consumedContent.overrideMainInitialFocusedItemId)
    }

    @Test
    fun `main subtitle panel contains encoding entry with current label`() {
        val panelItems = buildSubtitlePanelItems(
            subtitles = emptyList(),
            selectedSubtitleIndex = -1,
            subtitleEncodingLabel = "Automatic",
            preferredSubtitleLanguageKey = "",
            preferredSubtitleBaseLanguageKey = "",
            showOnlineSubtitleActions = false,
            showFirstAvailableSubtitleAction = false,
        )

        val encodingItem = panelItems.items.first { item ->
            item.id == SubtitleEncodingItemId
        }
        assertEquals("Automatic", encodingItem.supportingTexts.single())
        assertTrue(encodingItem.showChevron)
    }

    private fun encodingOptions(): List<TvPlayerSubtitleEncodingOption> {
        return listOf(
            TvPlayerSubtitleEncodingOption(
                value = null,
                label = "Automatic",
            ),
            TvPlayerSubtitleEncodingOption(
                value = "UTF-8",
                label = "Universal (UTF-8)",
            ),
            TvPlayerSubtitleEncodingOption(
                value = "Windows-1250",
                label = "Eastern European (Windows-1250)",
            ),
        )
    }
}
