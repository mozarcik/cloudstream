package com.lagradost.cloudstream3.tv

internal const val TvExitConfirmationTimeoutMs = 2_000L

internal enum class TvExitRequestAction {
    ShowExitHint,
    ExitNow,
}

internal class TvExitRequestHandler(
    private val elapsedRealtime: () -> Long,
    private val confirmationTimeoutMs: Long = TvExitConfirmationTimeoutMs,
) {
    private var pendingExitRequestAtMs = NoPendingExitRequest

    fun onExitRequested(): TvExitRequestAction {
        val nowMs = elapsedRealtime()
        val previousRequestAtMs = pendingExitRequestAtMs

        return if (
            previousRequestAtMs != NoPendingExitRequest &&
            nowMs - previousRequestAtMs < confirmationTimeoutMs
        ) {
            pendingExitRequestAtMs = NoPendingExitRequest
            TvExitRequestAction.ExitNow
        } else {
            pendingExitRequestAtMs = nowMs
            TvExitRequestAction.ShowExitHint
        }
    }

    fun reset() {
        pendingExitRequestAtMs = NoPendingExitRequest
    }

    private companion object {
        const val NoPendingExitRequest = -1L
    }
}
