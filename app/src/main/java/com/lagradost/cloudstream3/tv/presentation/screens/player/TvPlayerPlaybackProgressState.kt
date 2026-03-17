package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.os.SystemClock
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.isLiveStream
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.utils.DataStoreHelper.getViewPos
import com.lagradost.cloudstream3.utils.DataStoreHelper.setViewPosAndResume

private const val DefaultPlaybackProgressPersistIntervalMs = 10_000L

internal class TvPlayerPlaybackProgressState(
    private val persistIntervalMs: Long = DefaultPlaybackProgressPersistIntervalMs,
) {
    private var currentEpisode: ResultEpisode? = null
    private var nextEpisode: ResultEpisode? = null
    private var currentResumePositionMs: Long = 0L
    private var lastPlaybackProgressPersistAtElapsedMs: Long = 0L
    private var persistenceEnabled: Boolean = true

    val resumePositionMs: Long
        get() = currentResumePositionMs

    fun reset() {
        currentEpisode = null
        nextEpisode = null
        currentResumePositionMs = 0L
        lastPlaybackProgressPersistAtElapsedMs = 0L
        persistenceEnabled = true
    }

    fun onEpisodeChanged(
        episode: ResultEpisode?,
        nextEpisode: ResultEpisode?,
    ) {
        currentEpisode = episode
        this.nextEpisode = nextEpisode
        currentResumePositionMs = episode?.let { getResumePosition(it.id) } ?: 0L
        lastPlaybackProgressPersistAtElapsedMs = 0L
    }

    fun setPersistenceEnabled(enabled: Boolean) {
        persistenceEnabled = enabled
    }

    fun onPlaybackProgress(positionMs: Long, durationMs: Long) {
        persistPlaybackProgress(
            positionMs = positionMs,
            durationMs = durationMs,
            force = false,
        )
    }

    fun onPlaybackStopped(positionMs: Long, durationMs: Long) {
        persistPlaybackProgress(
            positionMs = positionMs,
            durationMs = durationMs,
            force = true,
        )
    }

    private fun getResumePosition(episodeId: Int): Long {
        val posDur = getViewPos(episodeId) ?: return 0L
        if (posDur.duration == 0L) return 0L
        if (posDur.position * 100L / posDur.duration > 95L) return 0L
        return posDur.position
    }

    private fun persistPlaybackProgress(
        positionMs: Long,
        durationMs: Long,
        force: Boolean,
    ) {
        if (!persistenceEnabled) return
        val episode = currentEpisode ?: return
        if (episode.tvType.isLiveStream() || episode.tvType == TvType.NSFW) return
        if (durationMs <= 0L) return

        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastPlaybackProgressPersistAtElapsedMs < persistIntervalMs) {
            return
        }

        lastPlaybackProgressPersistAtElapsedMs = now
        setViewPosAndResume(
            id = episode.id,
            position = positionMs.coerceAtLeast(0L),
            duration = durationMs,
            currentEpisode = episode,
            nextEpisode = nextEpisode,
        )
    }
}
