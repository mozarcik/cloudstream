package com.lagradost.cloudstream3.tv.compat.downloads

import android.content.Context
import android.net.Uri
import android.util.Log
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getActivity
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKeys
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.player.DownloadFileGenerator
import com.lagradost.cloudstream3.ui.player.ExtractorUri
import com.lagradost.cloudstream3.ui.player.GeneratorPlayer
import com.lagradost.cloudstream3.utils.DOWNLOAD_EPISODE_CACHE
import com.lagradost.cloudstream3.utils.DOWNLOAD_HEADER_CACHE
import com.lagradost.cloudstream3.utils.UIHelper.navigate
import com.lagradost.cloudstream3.utils.VideoDownloadHelper
import com.lagradost.cloudstream3.utils.VideoDownloadManager

private const val DebugTag = "TvDownloadPlayback"

internal fun playDownloadedEpisode(
    context: Context,
    episodeId: Int,
    fallbackEpisode: VideoDownloadHelper.DownloadEpisodeCached? = null,
): Boolean {
    Log.d(
        DebugTag,
        "playDownloadedEpisode start episodeId=$episodeId fallbackId=${fallbackEpisode?.id}"
    )
    val activity = context.getActivity() ?: CommonActivity.activity ?: return false
    val cachedEpisodes = getKeys(DOWNLOAD_EPISODE_CACHE)
        ?.mapNotNull { key ->
            getKey<VideoDownloadHelper.DownloadEpisodeCached>(key)
        }
        .orEmpty()
    Log.d(DebugTag, "cachedEpisodes count=${cachedEpisodes.size}")

    val selectedEpisode = cachedEpisodes.firstOrNull { episode ->
        episode.id == episodeId
    } ?: fallbackEpisode ?: run {
        Log.e(DebugTag, "selectedEpisode not found for episodeId=$episodeId")
        return false
    }
    Log.d(
        DebugTag,
        "selectedEpisode id=${selectedEpisode.id} parentId=${selectedEpisode.parentId} season=${selectedEpisode.season} episode=${selectedEpisode.episode}"
    )

    val header = getKey<VideoDownloadHelper.DownloadHeaderCached>(
        DOWNLOAD_HEADER_CACHE,
        selectedEpisode.parentId.toString()
    )
    Log.d(DebugTag, "header found=${header != null} for parentId=${selectedEpisode.parentId}")

    val parentEpisodes = (cachedEpisodes + selectedEpisode)
        .asSequence()
        .filter { episode -> episode.parentId == selectedEpisode.parentId }
        .distinctBy { episode -> episode.id }
        .sortedWith(
            compareBy<VideoDownloadHelper.DownloadEpisodeCached> { episode -> episode.season ?: 0 }
                .thenBy { episode -> episode.episode }
        )
        .toList()
    Log.d(DebugTag, "parentEpisodes count=${parentEpisodes.size}")

    val extractorItems = mutableListOf<ExtractorUri>()
    parentEpisodes.forEach { episode ->
        val downloadInfo = getKey<VideoDownloadManager.DownloadedFileInfo>(
            VideoDownloadManager.KEY_DOWNLOAD_INFO,
            episode.id.toString()
        ) ?: run {
            Log.d(
                DebugTag,
                "downloadInfo missing for episodeId=${episode.id} (skipping in playlist)"
            )
            return@forEach
        }

        extractorItems += ExtractorUri(
            // Resolve actual URI lazily in DownloadFileGenerator (legacy path).
            uri = Uri.EMPTY,
            id = episode.id,
            parentId = episode.parentId,
            name = episode.name ?: activity.getString(R.string.downloaded_file),
            season = episode.season,
            episode = episode.episode,
            headerName = header?.name,
            tvType = header?.type,
            basePath = downloadInfo.basePath,
            displayName = downloadInfo.displayName,
            relativePath = downloadInfo.relativePath,
        )
    }
    Log.d(DebugTag, "extractorItems from cache count=${extractorItems.size}")

    if (extractorItems.isEmpty()) {
        val singleFile = VideoDownloadManager.getDownloadFileInfoAndUpdateSettings(
            activity,
            selectedEpisode.id
        ) ?: run {
            Log.e(
                DebugTag,
                "singleFile missing for episodeId=${selectedEpisode.id}; cannot start playback"
            )
            return false
        }

        extractorItems += ExtractorUri(
            uri = singleFile.path,
            id = selectedEpisode.id,
            parentId = selectedEpisode.parentId,
            name = selectedEpisode.name ?: activity.getString(R.string.downloaded_file),
            season = selectedEpisode.season,
            episode = selectedEpisode.episode,
            headerName = header?.name,
            tvType = header?.type,
        )
        Log.d(
            DebugTag,
            "fallback single file used for episodeId=${selectedEpisode.id} path=${singleFile.path}"
        )
    }

    val currentIndex = extractorItems.indexOfFirst { item -> item.id == selectedEpisode.id }
        .takeIf { index -> index >= 0 } ?: 0
    Log.d(
        DebugTag,
        "starting player items=${extractorItems.size} currentIndex=$currentIndex selectedEpisodeId=${selectedEpisode.id}"
    )

    val player = GeneratorPlayer.newInstance(
        DownloadFileGenerator(extractorItems).apply {
            goto(currentIndex)
        }
    )
    runCatching {
        activity.navigate(R.id.global_to_navigation_player, player)
    }.onFailure { error ->
        Log.e(DebugTag, "navigation to player failed for episodeId=${selectedEpisode.id}", error)
        return false
    }
    Log.d(DebugTag, "navigation requested for episodeId=${selectedEpisode.id}")
    return true
}
