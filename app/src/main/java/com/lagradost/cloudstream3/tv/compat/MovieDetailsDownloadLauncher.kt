package com.lagradost.cloudstream3.tv.compat

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.getFolderPrefix
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.result.ExtractorSubtitleLink
import com.lagradost.cloudstream3.ui.result.LinkLoadingResult
import com.lagradost.cloudstream3.ui.result.ResultViewModel2
import com.lagradost.cloudstream3.ui.result.getId
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import com.lagradost.cloudstream3.utils.VideoDownloadManager.DownloadEpisodeMetadata
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
        ResultViewModel2.startDownload(
            context = downloadContext,
            episode = target.episode,
            currentIsMovie = target.loadResponse.type in setOf(TvType.Movie, TvType.AnimeMovie),
            currentHeaderName = target.loadResponse.name,
            currentType = target.loadResponse.type,
            currentPoster = target.loadResponse.posterUrl ?: target.loadResponse.backgroundPosterUrl,
            currentBackdrop = target.loadResponse.backgroundPosterUrl,
            apiName = target.loadResponse.apiName,
            parentId = target.loadResponse.getId(),
            url = target.loadResponse.url,
            links = listOf(loaded.links[linkIndex]),
            subs = loaded.subs,
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
        val meta = createDownloadMeta(target)
        val fileName = VideoDownloadManager.getFileName(context, meta)
        val folder = getDownloadFolder(target.loadResponse.type, target.loadResponse.name)

        withContext(Dispatchers.IO) {
            VideoDownloadManager.downloadThing(
                context = context,
                link = ExtractorSubtitleLink(subtitle.name, subtitle.url, "", subtitle.headers),
                name = "$fileName ${subtitle.name}",
                folder = folder,
                extension = if (subtitle.url.contains(".srt")) "srt" else "vtt",
                tryResume = false,
                parentId = null,
                createNotificationCallback = {},
            )
        }
        showToast(R.string.download_started)
    }

    private fun createDownloadMeta(target: MovieDetailsActionTarget): DownloadEpisodeMetadata {
        val isMovieType = target.loadResponse.type in setOf(TvType.Movie, TvType.AnimeMovie)
        return DownloadEpisodeMetadata(
            id = target.episode.id,
            mainName = VideoDownloadManager.sanitizeFilename(target.loadResponse.name),
            sourceApiName = target.loadResponse.apiName,
            poster = target.episode.poster ?: target.loadResponse.posterUrl,
            name = target.episode.name,
            season = if (isMovieType) null else target.episode.season,
            episode = if (isMovieType) null else target.episode.episode,
            type = target.loadResponse.type,
        )
    }

    private fun getDownloadFolder(type: TvType, title: String): String {
        return if (type.isEpisodeBased()) {
            "${type.getFolderPrefix()}/${VideoDownloadManager.sanitizeFilename(title)}"
        } else {
            type.getFolderPrefix()
        }
    }

    private fun showToast(@androidx.annotation.StringRes textRes: Int) {
        CommonActivity.showToast(textRes, Toast.LENGTH_SHORT)
    }
}
