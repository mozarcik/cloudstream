package com.lagradost.cloudstream3.tv.presentation.screens.settings.account

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenSubtitlesAccountScreenBackLogicTest {
    @Test
    fun resolveBackAction_returnsNoneForPreview() {
        val action = resolveOpenSubtitlesBackAction(
            isPreview = true,
            showLoginForm = true,
        )

        assertEquals(OpenSubtitlesBackAction.None, action)
    }

    @Test
    fun resolveBackAction_hidesLoginFormWhenActiveAndFormVisible() {
        val action = resolveOpenSubtitlesBackAction(
            isPreview = false,
            showLoginForm = true,
        )

        assertEquals(OpenSubtitlesBackAction.HideLoginForm, action)
    }

    @Test
    fun resolveBackAction_navigatesBackWhenActiveAndFormHidden() {
        val action = resolveOpenSubtitlesBackAction(
            isPreview = false,
            showLoginForm = false,
        )

        assertEquals(OpenSubtitlesBackAction.NavigateBack, action)
    }
}
