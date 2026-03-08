package com.lagradost.cloudstream3.tv.compat

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.player.LOADTYPE_ALL
import com.lagradost.cloudstream3.ui.player.LOADTYPE_INAPP_DOWNLOAD
import com.lagradost.cloudstream3.ui.result.LinkLoadingResult
import com.lagradost.cloudstream3.utils.ExtractorLinkType

internal class MovieDetailsDownloadSupport(
    private val linksLoader: MovieDetailsLinksLoader,
) {
    internal companion object {
        const val DebugTag = "MovieDownloadSupport"
    }

    private val snapshotResolver = MovieDetailsDownloadSnapshotResolver()
    private val downloadLauncher = MovieDetailsDownloadLauncher()

    suspend fun getDownloadSnapshot(
        context: Context?,
        target: MovieDetailsActionTarget,
    ): MovieDetailsCompatDownloadSnapshot {
        return snapshotResolver.resolve(context = context, target = target)
    }

    suspend fun requestDownloadMirrorSelection(
        target: MovieDetailsActionTarget,
        context: Context?,
        onSourcesProgress: (Int) -> Unit = {},
        onSelectionUpdated: ((MovieDetailsCompatSelectionRequest) -> Unit)? = null,
        shouldCancelLoading: (() -> Boolean)? = null,
    ): MovieDetailsCompatActionOutcome {
        Log.d(
            DebugTag,
            "request download mirror selection start episodeId=${target.episode.id} name=${target.episode.name}"
        )
        onSourcesProgress(0)
        val loaded = loadLinks(
            target = target,
            sourceTypes = LOADTYPE_INAPP_DOWNLOAD,
            onLinksBatchUpdated = { batch ->
                onSourcesProgress(batch.loadedCount)
                if (batch.links.isEmpty()) return@loadLinks

                onSelectionUpdated?.invoke(
                    buildMovieDetailsDownloadMirrorSelectionRequest(
                        target = target,
                        context = context,
                        loaded = LinkLoadingResult(
                            links = batch.links,
                            subs = batch.subtitles,
                            syncData = HashMap(target.loadResponse.syncData),
                        ),
                        downloadLauncher = downloadLauncher,
                    )
                )
            },
            shouldCancelLoading = shouldCancelLoading,
        )

        Log.d(
            DebugTag,
            "request download mirror selection loaded links=${loaded.links.size} subtitles=${loaded.subs.size}"
        )

        if (loaded.links.isEmpty()) {
            showToast(R.string.no_links_found_toast)
            return MovieDetailsCompatActionOutcome.Completed
        }

        return MovieDetailsCompatActionOutcome.OpenSelection(
            request = buildMovieDetailsDownloadMirrorSelectionRequest(
                target = target,
                context = context,
                loaded = loaded,
                downloadLauncher = downloadLauncher,
            )
        )
    }

    suspend fun requestSubtitleMirrorSelection(
        target: MovieDetailsActionTarget,
        context: Context?,
    ): MovieDetailsCompatActionOutcome {
        val loaded = loadLinks(target = target)
        if (loaded.subs.isEmpty()) {
            showToast(R.string.no_links_found_toast)
            return MovieDetailsCompatActionOutcome.Completed
        }

        if (loaded.subs.size == 1) {
            downloadLauncher.startSubtitleDownload(target, loaded.subs.first())
            return MovieDetailsCompatActionOutcome.Completed
        }

        return MovieDetailsCompatActionOutcome.OpenSelection(
            request = buildMovieDetailsSubtitleSelectionRequest(
                target = target,
                context = context,
                loaded = loaded,
                downloadLauncher = downloadLauncher,
            )
        )
    }

    suspend fun prefetchDownloadMirrorLinks(target: MovieDetailsActionTarget) {
        val loaded = loadLinks(
            target = target,
            sourceTypes = LOADTYPE_INAPP_DOWNLOAD,
        )
        Log.d(
            DebugTag,
            "prefetch download links done episodeId=${target.episode.id} links=${loaded.links.size}"
        )
    }

    private suspend fun loadLinks(
        target: MovieDetailsActionTarget,
        sourceTypes: Set<ExtractorLinkType> = LOADTYPE_ALL,
        clearCache: Boolean = false,
        onLinksBatchUpdated: ((MovieDetailsLinksBatchUpdate) -> Unit)? = null,
        shouldCancelLoading: (() -> Boolean)? = null,
    ): LinkLoadingResult {
        return linksLoader.load(
            target = target,
            sourceTypes = sourceTypes,
            clearCache = clearCache,
            onLinksBatchUpdated = onLinksBatchUpdated,
            shouldCancelLoading = shouldCancelLoading,
        )
    }

    private fun showToast(@androidx.annotation.StringRes textRes: Int) {
        CommonActivity.showToast(textRes, Toast.LENGTH_SHORT)
    }
}
