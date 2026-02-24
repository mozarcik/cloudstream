package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.os.SystemClock
import android.util.Log
import androidx.media3.common.text.Cue
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer.STATE_ENABLED
import androidx.media3.exoplayer.Renderer.STATE_STARTED
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.ui.SubtitleView
import com.lagradost.cloudstream3.ui.player.CustomDecoder
import com.lagradost.cloudstream3.ui.player.CustomSubtitleDecoderFactory
import com.lagradost.cloudstream3.ui.player.SubtitleCue
import java.lang.ref.WeakReference

internal data class TvPlayerInstance(
    val player: ExoPlayer,
    val subtitleSyncController: TvPlayerSubtitleSyncController,
)

/**
 * Legacy-compatible subtitle sync controller:
 * - cue list comes from CustomSubtitleDecoderFactory (unshifted timestamps),
 * - delay shown to user is inverse of renderer offset.
 */
internal class TvPlayerSubtitleSyncController(
    initialSubtitleDelayMs: Long,
) {
    private companion object {
        const val SubtitleSyncDebugTag = "TvSubtitleSync"
        const val EmptyCuesLogIntervalMs = 3_000L
        const val PeriodicCuesLogIntervalMs = 5_000L
    }

    private val decoderFactory = CustomSubtitleDecoderFactory()
    private var currentTextRenderer: TextRenderer? = null
    private var subtitleViewRef: WeakReference<SubtitleView>? = null
    private var subtitleDelayMs: Long = initialSubtitleDelayMs
    private var rendererOffsetMs: Long = -initialSubtitleDelayMs
    private var lastLoggedCueCount = Int.MIN_VALUE
    private var lastEmptyCuesLogTimestampMs = 0L
    private var lastPeriodicCuesLogTimestampMs = 0L

    init {
        CustomDecoder.subtitleOffset = rendererOffsetMs
        debugLog(
            "init: subtitleDelayMs=$subtitleDelayMs rendererOffsetMs=$rendererOffsetMs",
        )
    }

    fun subtitleDecoderFactory(): CustomSubtitleDecoderFactory = decoderFactory

    fun subtitleDelayMs(): Long = subtitleDelayMs

    fun rendererOffsetMs(): Long = rendererOffsetMs

    fun subtitleCues(): List<SubtitleCue> {
        val rawCues = decoderFactory.getSubtitleCues()
        val cues = rawCues.orEmpty()
        val nowMs = SystemClock.elapsedRealtime()

        when {
            rawCues == null -> {
                if (nowMs - lastEmptyCuesLogTimestampMs >= EmptyCuesLogIntervalMs) {
                    lastEmptyCuesLogTimestampMs = nowMs
                    debugLog(
                        "subtitleCues: decoder returned null (rendererAttached=${currentTextRenderer != null}," +
                            " rendererState=${currentTextRenderer?.state?.let(::rendererStateLabel) ?: "null"})"
                    )
                }
            }
            cues.size != lastLoggedCueCount -> {
                lastLoggedCueCount = cues.size
                lastPeriodicCuesLogTimestampMs = nowMs
                debugLog(
                    "subtitleCues: count changed -> ${cues.size}," +
                        " firstStartMs=${cues.firstOrNull()?.startTimeMs}," +
                        " lastStartMs=${cues.lastOrNull()?.startTimeMs}"
                )
            }
            nowMs - lastPeriodicCuesLogTimestampMs >= PeriodicCuesLogIntervalMs -> {
                lastPeriodicCuesLogTimestampMs = nowMs
                debugLog(
                    "subtitleCues: stable count=${cues.size}," +
                        " rendererState=${currentTextRenderer?.state?.let(::rendererStateLabel) ?: "null"}"
                )
            }
        }
        return cues
    }

    fun attachTextRenderer(renderer: TextRenderer) {
        currentTextRenderer = renderer
        CustomDecoder.subtitleOffset = rendererOffsetMs
        debugLog(
            "attachTextRenderer: state=${rendererStateLabel(renderer.state)} rendererOffsetMs=$rendererOffsetMs",
        )
    }

    fun clearTextRenderer() {
        debugLog(
            "clearTextRenderer: previousState=${currentTextRenderer?.state?.let(::rendererStateLabel) ?: "null"}",
        )
        currentTextRenderer = null
    }

    fun attachSubtitleView(subtitleView: SubtitleView?) {
        subtitleViewRef = subtitleView?.let(::WeakReference)
        debugLog(
            "attachSubtitleView: attached=${subtitleView != null}",
        )
    }

    fun clearSubtitleView() {
        subtitleViewRef = null
        debugLog("clearSubtitleView")
    }

    fun dispatchStyledCues(cues: List<Cue>): Boolean {
        val subtitleView = subtitleViewRef?.get() ?: return false
        // WHY: parity z legacy (CS3IPlayer) - ustawiamy cue bezpośrednio na SubtitleView.
        subtitleView.setCues(cues)
        return true
    }

    fun logDebugSnapshot(
        player: ExoPlayer,
        reason: String,
        subtitleId: String?,
        subtitleLabel: String?,
        subtitleMimeType: String?,
    ) {
        val cues = decoderFactory.getSubtitleCues()
        debugLog(
            "snapshot[$reason]: playerPosMs=${player.currentPosition.coerceAtLeast(0L)} playWhenReady=${player.playWhenReady}" +
                " subtitleId=${subtitleId ?: "null"} subtitleLabel=${subtitleLabel ?: "null"}" +
                " mime=${subtitleMimeType ?: "null"} delayMs=$subtitleDelayMs rendererOffsetMs=$rendererOffsetMs" +
                " rendererAttached=${currentTextRenderer != null}" +
                " subtitleViewAttached=${subtitleViewRef?.get() != null}" +
                " rendererState=${currentTextRenderer?.state?.let(::rendererStateLabel) ?: "null"}" +
                " cues=${cues?.size ?: "null"}"
        )
    }

    fun setSubtitleDelayMs(
        player: ExoPlayer,
        newSubtitleDelayMs: Long,
    ) {
        if (subtitleDelayMs == newSubtitleDelayMs) return
        val previousDelayMs = subtitleDelayMs
        val previousRendererOffsetMs = rendererOffsetMs
        subtitleDelayMs = newSubtitleDelayMs
        rendererOffsetMs = -newSubtitleDelayMs
        CustomDecoder.subtitleOffset = rendererOffsetMs
        debugLog(
            "setSubtitleDelayMs: delay $previousDelayMs -> $subtitleDelayMs," +
                " rendererOffset $previousRendererOffsetMs -> $rendererOffsetMs," +
                " playerPosMs=${player.currentPosition.coerceAtLeast(0L)}"
        )

        val textRenderer = currentTextRenderer ?: return
        if (textRenderer.state == STATE_ENABLED || textRenderer.state == STATE_STARTED) {
            textRenderer.resetPosition(player.currentPosition.coerceAtLeast(0L))
            debugLog(
                "setSubtitleDelayMs: resetPosition called, rendererState=${rendererStateLabel(textRenderer.state)}",
            )
        } else {
            debugLog(
                "setSubtitleDelayMs: skip resetPosition, rendererState=${rendererStateLabel(textRenderer.state)}",
            )
        }
    }

    private fun rendererStateLabel(state: Int): String {
        return when (state) {
            STATE_STARTED -> "STARTED"
            STATE_ENABLED -> "ENABLED"
            else -> state.toString()
        }
    }

    private fun debugLog(message: String) {
        Log.i(SubtitleSyncDebugTag, message)
    }
}
