package com.lagradost.cloudstream3.tv.compat

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.result.LinkLoadingResult
import com.lagradost.cloudstream3.ui.result.getId
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager
import com.lagradost.cloudstream3.utils.downloader.DownloadObjects
import com.lagradost.cloudstream3.utils.downloader.DownloadQueueManager
import com.lagradost.cloudstream3.utils.downloader.DownloadUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class MovieDetailsDownloadLauncher {
    private companion object {
        const val DebugTag = "MovieDownloadLaunch"
    }

    fun startMirrorDownload(
        target: MovieDetailsActionTarget,
        loaded: LinkLoadingResult,
        selectedIndex: Int,
        context: Context?,
    ) {
        val linkIndex = selectedIndex.coerceIn(loaded.links.indices)
        val downloadContext = context ?: CommonActivity.activity
        if (downloadContext == null) {
            Log.e(
                DebugTag,
                "startMirrorDownload aborted: context is null episodeId=${target.episode.id}"
            )
            showToast(R.string.download_failed)
            return
        }

        Log.d(
            DebugTag,
            "startMirrorDownload: episodeId=${target.episode.id} linkIndex=$linkIndex linkName=${loaded.links[linkIndex].name}"
        )
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
                links = listOf(loaded.links[linkIndex]),
                subs = loaded.subs,
                resultBackdrop = loadResponse.backgroundPosterUrl ?: loadResponse.posterUrl,
            ).toWrapper()
        )
        Log.d(
            DebugTag,
            "startMirrorDownload: request sent for episodeId=${target.episode.id}"
        )
        showToast(R.string.download_started)
    }

    suspend fun startSubtitleDownload(
        target: MovieDetailsActionTarget,
        subtitle: SubtitleData,
    ) {
        val context = CommonActivity.activity ?: return
        val loadResponse = target.loadResponse
        val isMovieType = loadResponse.type in setOf(TvType.Movie, TvType.AnimeMovie)
        val meta = VideoDownloadManager.getDownloadEpisodeMetadata(
            episode = target.episode,
            titleName = loadResponse.name,
            apiName = loadResponse.apiName,
            currentPoster = loadResponse.posterUrl,
            currentIsMovie = isMovieType,
            tvType = loadResponse.type,
        )
        withContext(Dispatchers.IO) {
            DownloadUtils.downloadSubtitle(context, subtitle, meta)
        }
        showToast(R.string.download_started)
    }

    private fun showToast(@androidx.annotation.StringRes textRes: Int) {
        CommonActivity.showToast(textRes, Toast.LENGTH_SHORT)
    }
}
