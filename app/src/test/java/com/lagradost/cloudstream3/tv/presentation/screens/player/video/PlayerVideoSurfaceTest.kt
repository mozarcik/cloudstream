package com.lagradost.cloudstream3.tv.presentation.screens.player.video

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerVideoSurfaceTest {
    @Test
    fun `resolvedSubtitleTranslationY keeps base offset when controls are hidden`() {
        val translationY = resolvedSubtitleTranslationY(
            baseTranslationYPx = -24f,
            controlsVisibleOffsetPx = 106f,
            controlsVisible = false,
        )

        assertEquals(-24f, translationY, 0.001f)
    }

    @Test
    fun `resolvedSubtitleTranslationY lifts subtitles above controls when overlay is visible`() {
        val translationY = resolvedSubtitleTranslationY(
            baseTranslationYPx = -24f,
            controlsVisibleOffsetPx = 106f,
            controlsVisible = true,
        )

        assertEquals(-130f, translationY, 0.001f)
    }
}
