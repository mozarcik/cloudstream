package com.lagradost.cloudstream3.tv.compat

import com.lagradost.cloudstream3.utils.downloader.DownloadObjects
import com.lagradost.cloudstream3.utils.downloader.DownloadQueueManager
import android.content.Context
import android.widget.Toast
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.ui.result.ACTION_CHROME_CAST_EPISODE
import com.lagradost.cloudstream3.ui.result.ACTION_CHROME_CAST_MIRROR
import com.lagradost.cloudstream3.ui.result.ACTION_DOWNLOAD_EPISODE
import com.lagradost.cloudstream3.ui.result.ACTION_DOWNLOAD_EPISODE_SUBTITLE_MIRROR
import com.lagradost.cloudstream3.ui.result.ACTION_DOWNLOAD_MIRROR
import com.lagradost.cloudstream3.ui.result.ACTION_MARK_AS_WATCHED
import com.lagradost.cloudstream3.ui.result.ACTION_MARK_WATCHED_UP_TO_THIS_EPISODE
import com.lagradost.cloudstream3.ui.result.ACTION_PLAY_EPISODE_IN_PLAYER
import com.lagradost.cloudstream3.ui.result.ACTION_RELOAD_EPISODE
import com.lagradost.cloudstream3.ui.result.ResultViewModel2
import com.lagradost.cloudstream3.ui.result.getId

internal class MovieDetailsActionExecutor(
    private val watchStateSupport: MovieDetailsWatchStateSupport,
    private val downloadSupport: MovieDetailsDownloadSupport,
    private val playbackActionExecutor: MovieDetailsPlaybackActionExecutor,
) {
    suspend fun execute(
        actionId: Int,
        target: MovieDetailsActionTarget,
        context: Context?,
        onPlayInApp: (MovieDetailsActionTarget) -> Unit,
    ): MovieDetailsCompatActionOutcome {
        return when (actionId) {
            ACTION_PLAY_EPISODE_IN_PLAYER -> {
                onPlayInApp(target)
                MovieDetailsCompatActionOutcome.Completed
            }

            ACTION_DOWNLOAD_EPISODE -> {
                val loadResponse = target.loadResponse
                DownloadQueueManager.addToQueue(
                    DownloadObjects.DownloadQueueItem(
                        episode = target.episode,
                        isMovie = loadResponse.type in setOf(TvType.Movie, TvType.AnimeMovie),
                        resultName = loadResponse.name,
                        resultType = loadResponse.type,
                        resultPoster = loadResponse.posterUrl ?: loadResponse.backgroundPosterUrl,
                        apiName = loadResponse.apiName,
                        resultId = loadResponse.getId(),
                        resultUrl = loadResponse.url,
                        resultBackdrop = loadResponse.backgroundPosterUrl ?: loadResponse.posterUrl,
                    ).toWrapper()
                )
                MovieDetailsCompatActionOutcome.Completed
            }

            ACTION_DOWNLOAD_MIRROR -> downloadSupport.requestDownloadMirrorSelection(
                target = target,
                context = context,
            )

            ACTION_DOWNLOAD_EPISODE_SUBTITLE_MIRROR -> downloadSupport.requestSubtitleMirrorSelection(
                target = target,
                context = context,
            )

            ACTION_RELOAD_EPISODE -> {
                val hasLinks = playbackActionExecutor.reloadEpisodeLinks(target)
                showToast(if (hasLinks) R.string.links_reloaded_toast else R.string.no_links_found_toast)
                MovieDetailsCompatActionOutcome.Completed
            }

            ACTION_CHROME_CAST_EPISODE -> playbackActionExecutor.castEpisode(target)
            ACTION_CHROME_CAST_MIRROR -> playbackActionExecutor.castMirror(target, context)

            ACTION_MARK_AS_WATCHED -> {
                watchStateSupport.toggleWatched(target)
                MovieDetailsCompatActionOutcome.Completed
            }

            ACTION_MARK_WATCHED_UP_TO_THIS_EPISODE -> {
                watchStateSupport.toggleWatchedUpTo(target)
                MovieDetailsCompatActionOutcome.Completed
            }

            else -> playbackActionExecutor.executeExternalAction(
                actionId = actionId,
                target = target,
                context = context
            )
        }
    }

    fun showNoLinksToast() {
        showToast(R.string.no_links_found_toast)
    }

    private fun showToast(@androidx.annotation.StringRes textRes: Int) {
        CommonActivity.showToast(textRes, Toast.LENGTH_SHORT)
    }
}
