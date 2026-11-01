package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.media3.common.PlaybackException
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPlaybackErrorDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerErrorSupportTest {
    @Test
    fun `playbackErrorUiModel maps remote http failures to short UI message`() {
        val error = TvPlayerPlaybackErrorDetails(
            exoErrorCode = PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            exoErrorName = "ERROR_CODE_IO_BAD_HTTP_STATUS",
            httpCode = 403,
        )

        val uiModel = playbackErrorUiModel(error)

        assertEquals("Remote error", uiModel.message)
        assertTrue(uiModel.canRetry)
    }

    @Test
    fun `playbackErrorEventMessage includes code http status and first line only`() {
        val error = TvPlayerPlaybackErrorDetails(
            exoErrorCode = PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            exoErrorName = "ERROR_CODE_IO_BAD_HTTP_STATUS",
            httpCode = 403,
            message = "Forbidden stream\nsecond line should be ignored",
        )

        val eventMessage = playbackErrorEventMessage(error)

        assertTrue(eventMessage.contains("ERROR_CODE_IO_BAD_HTTP_STATUS"))
        assertTrue(
            eventMessage.contains("(${PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS})")
        )
        assertTrue(eventMessage.contains("http=403"))
        assertTrue(eventMessage.contains("message=Forbidden stream"))
        assertTrue(!eventMessage.contains("second line"))
    }
}
