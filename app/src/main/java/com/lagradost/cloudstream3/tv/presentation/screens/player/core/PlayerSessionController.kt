package com.lagradost.cloudstream3.tv.presentation.screens.player.core

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerSubtitleSyncController
import com.lagradost.cloudstream3.utils.ExtractorLink

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
        subtitleSyncController.setSubtitleDelayMs(
            player = player,
            newSubtitleDelayMs = subtitleDelayMs,
        )
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

    fun release() {
        subtitleSyncController.clearSubtitleView()
        subtitleSyncController.clearTextRenderer()
        player.release()
    }
}
