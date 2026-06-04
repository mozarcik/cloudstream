package com.lagradost.cloudstream3.tv.presentation.screens.player.core

import android.content.Context
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.player.CustomDecoder
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerSubtitleSyncController
import com.lagradost.cloudstream3.utils.ExtractorLink

private const val SubtitleRefreshSeekStepMs = 1L

internal fun resolveSubtitleRefreshSeekTarget(
    currentPositionMs: Long,
    durationMs: Long,
    preferredDeltaMs: Long,
): Long? {
    val normalizedPositionMs = currentPositionMs.coerceAtLeast(0L)
    val normalizedPreferredDeltaMs = preferredDeltaMs.takeIf { it != 0L } ?: SubtitleRefreshSeekStepMs
    val preferredTargetPositionMs = clampSubtitleRefreshTarget(
        positionMs = normalizedPositionMs + normalizedPreferredDeltaMs,
        durationMs = durationMs,
    )
    if (preferredTargetPositionMs != normalizedPositionMs) {
        return preferredTargetPositionMs
    }

    val fallbackTargetPositionMs = clampSubtitleRefreshTarget(
        positionMs = normalizedPositionMs - normalizedPreferredDeltaMs,
        durationMs = durationMs,
    )
    return fallbackTargetPositionMs.takeUnless { it == normalizedPositionMs }
}

private fun clampSubtitleRefreshTarget(
    positionMs: Long,
    durationMs: Long,
): Long {
    return if (durationMs == C.TIME_UNSET || durationMs < 0L) {
        positionMs.coerceAtLeast(0L)
    } else {
        positionMs.coerceIn(0L, durationMs)
    }
}

internal class PlayerSessionController(
    context: Context,
) {
    private val appContext = context.applicationContext
    val subtitleSyncController = TvPlayerSubtitleSyncController(
        initialSubtitleDelayMs = 0L,
    )

    private val trackSelector = DefaultTrackSelector(appContext).apply {
        parameters = buildUponParameters()
            .setPreferredAudioLanguage(null)
            .build()
    }
    private var nextSubtitleRefreshSeekDeltaMs = SubtitleRefreshSeekStepMs

    val player: ExoPlayer = ExoPlayer.Builder(appContext)
        .setRenderersFactory(
            createTvPlayerRenderersFactory(
                context = appContext,
                subtitleSyncController = subtitleSyncController,
            )
        )
        .setTrackSelector(trackSelector)
        .build()

    fun load(
        link: ExtractorLink,
        subtitle: SubtitleData?,
        audioTracks: List<com.lagradost.cloudstream3.AudioFile>,
        subtitleDelayMs: Long,
        startPositionMs: Long,
        startPlayWhenReady: Boolean,
    ) {
        trackSelector.parameters = trackSelector.buildUponParameters()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
        subtitleSyncController.setSubtitleDelayMs(subtitleDelayMs)
        CustomDecoder.updateForcedEncoding(appContext)
        val mediaSource = buildPlayerMediaSource(
            context = appContext,
            link = link,
            subtitle = subtitle,
            audioTracks = audioTracks,
        )
        player.setMediaSource(mediaSource, startPositionMs.coerceAtLeast(0L))
        player.prepare()
        player.playWhenReady = startPlayWhenReady
    }

    fun refreshSubtitleTrackAtCurrentPosition() {
        val currentPositionMs = player.currentPosition.coerceAtLeast(0L)
        if (!player.isCurrentMediaItemSeekable) {
            subtitleSyncDebugLog(
                "refreshSubtitleTrackAtCurrentPosition: media item is not seekable," +
                    " falling back to renderer reset at playerPosMs=$currentPositionMs",
            )
            subtitleSyncController.resetRendererPosition(player)
            return
        }

        val preferredDeltaMs = nextSubtitleRefreshSeekDeltaMs
        val targetPositionMs = resolveSubtitleRefreshSeekTarget(
            currentPositionMs = currentPositionMs,
            durationMs = player.duration,
            preferredDeltaMs = preferredDeltaMs,
        )
        if (targetPositionMs == null) {
            subtitleSyncDebugLog(
                "refreshSubtitleTrackAtCurrentPosition: no valid seek target," +
                    " falling back to renderer reset at playerPosMs=$currentPositionMs" +
                    " durationMs=${player.duration}",
            )
            subtitleSyncController.resetRendererPosition(player)
            return
        }

        nextSubtitleRefreshSeekDeltaMs = -preferredDeltaMs
        subtitleSyncDebugLog(
            "refreshSubtitleTrackAtCurrentPosition: seeking playerPosMs=$currentPositionMs" +
                " targetPosMs=$targetPositionMs" +
                " deltaMs=${targetPositionMs - currentPositionMs}",
        )
        player.seekTo(targetPositionMs)
    }

    fun release() {
        subtitleSyncController.clearSubtitleView()
        subtitleSyncController.clearTextRenderer()
        player.release()
    }

    fun pause() {
        player.pause()
    }
}
