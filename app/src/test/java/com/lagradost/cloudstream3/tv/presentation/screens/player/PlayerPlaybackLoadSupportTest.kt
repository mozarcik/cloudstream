package com.lagradost.cloudstream3.tv.presentation.screens.player

import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.player.SubtitleOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPlaybackLoadSupportTest {
    @Test
    fun `restore request key prefers active request over consumed id`() {
        val key = playbackRestoreRequestKey(
            activeRequest = PlayerPlaybackRestoreRequest(
                requestId = 7L,
                positionMs = 12_345L,
                playWhenReady = true,
            ),
            lastConsumedRequestId = 6L,
        )

        assertEquals(7L, key)
    }

    @Test
    fun `restore request key falls back to last consumed id when no active request exists`() {
        val key = playbackRestoreRequestKey(
            activeRequest = null,
            lastConsumedRequestId = 8L,
        )

        assertEquals(8L, key)
    }

    @Test
    fun `runtime selection is attempted for user subtitle switch on same link`() {
        assertTrue(
            shouldAttemptRuntimeSubtitleSelection(
                isLinkChanged = false,
                isSubtitleChanged = true,
                selectedSubtitle = testSubtitle(origin = SubtitleOrigin.EMBEDDED_IN_VIDEO),
                subtitleSelectionSource = TvPlayerSubtitleSelectionSource.User,
                isCurrentSourceReady = false,
            )
        )
    }

    @Test
    fun `runtime selection is attempted for embedded auto selection before source ready`() {
        assertTrue(
            shouldAttemptRuntimeSubtitleSelection(
                isLinkChanged = false,
                isSubtitleChanged = true,
                selectedSubtitle = testSubtitle(origin = SubtitleOrigin.EMBEDDED_IN_VIDEO),
                subtitleSelectionSource = TvPlayerSubtitleSelectionSource.Auto,
                isCurrentSourceReady = false,
            )
        )
    }

    @Test
    fun `runtime selection is skipped when subtitle has not changed`() {
        assertFalse(
            shouldAttemptRuntimeSubtitleSelection(
                isLinkChanged = false,
                isSubtitleChanged = false,
                selectedSubtitle = testSubtitle(origin = SubtitleOrigin.EMBEDDED_IN_VIDEO),
                subtitleSelectionSource = TvPlayerSubtitleSelectionSource.User,
                isCurrentSourceReady = true,
            )
        )
    }

    @Test
    fun `subtitle encoding change reloads playback for external subtitle`() {
        assertTrue(
            shouldRestorePlaybackForSubtitleEncodingChange(
                selectedSubtitle = testSubtitle(origin = SubtitleOrigin.URL),
                currentEncodingValue = null,
                newEncodingValue = "Windows-1250",
            )
        )
    }

    @Test
    fun `subtitle encoding change does not reload playback for embedded subtitle`() {
        assertFalse(
            shouldRestorePlaybackForSubtitleEncodingChange(
                selectedSubtitle = testSubtitle(origin = SubtitleOrigin.EMBEDDED_IN_VIDEO),
                currentEncodingValue = null,
                newEncodingValue = "Windows-1250",
            )
        )
    }

    @Test
    fun `subtitle encoding change does not reload playback when encoding stays the same`() {
        assertFalse(
            shouldRestorePlaybackForSubtitleEncodingChange(
                selectedSubtitle = testSubtitle(origin = SubtitleOrigin.DOWNLOADED_FILE),
                currentEncodingValue = "UTF-8",
                newEncodingValue = "UTF-8",
            )
        )
    }

    @Test
    fun `subtitle encoding change does not reload playback without active subtitle`() {
        assertFalse(
            shouldRestorePlaybackForSubtitleEncodingChange(
                selectedSubtitle = null,
                currentEncodingValue = null,
                newEncodingValue = "Windows-1250",
            )
        )
    }

    private fun testSubtitle(origin: SubtitleOrigin): SubtitleData {
        return SubtitleData(
            originalName = "English",
            nameSuffix = "Forced",
            url = "embedded_en_forced",
            origin = origin,
            mimeType = "text/vtt",
            headers = emptyMap(),
            languageCode = "en",
        )
    }
}
