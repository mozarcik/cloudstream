package com.lagradost.cloudstream3.tv

import org.junit.Assert.assertEquals
import org.junit.Test

class TvExitRequestHandlerTest {
    private var nowMs = 0L

    private val handler = TvExitRequestHandler(
        elapsedRealtime = { nowMs },
    )

    @Test
    fun `first exit request shows hint`() {
        val action = handler.onExitRequested()

        assertEquals(TvExitRequestAction.ShowExitHint, action)
    }

    @Test
    fun `second exit request inside timeout exits`() {
        handler.onExitRequested()
        nowMs = 1_999L

        val action = handler.onExitRequested()

        assertEquals(TvExitRequestAction.ExitNow, action)
    }

    @Test
    fun `second exit request after timeout shows hint again`() {
        handler.onExitRequested()
        nowMs = TvExitConfirmationTimeoutMs

        val action = handler.onExitRequested()

        assertEquals(TvExitRequestAction.ShowExitHint, action)
    }

    @Test
    fun `reset clears pending exit request`() {
        handler.onExitRequested()
        nowMs = 1_000L
        handler.reset()

        val action = handler.onExitRequested()

        assertEquals(TvExitRequestAction.ShowExitHint, action)
    }
}
