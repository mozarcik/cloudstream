package com.lagradost.cloudstream3.tv.compat

import android.content.Context

internal class MovieDetailsEpisodeDownloadFacade(
    private val downloadSupport: MovieDetailsDownloadSupport,
    private val resolveTarget: suspend (Int?, Int?) -> MovieDetailsActionTarget?,
    private val onMissingTarget: () -> Unit,
) {
    suspend fun getDownloadSnapshot(
        context: Context?,
        preferredSeason: Int?,
        preferredEpisode: Int?,
    ): MovieDetailsCompatDownloadSnapshot? {
        val target = resolveTarget(preferredSeason, preferredEpisode) ?: return null
        return downloadSupport.getDownloadSnapshot(
            context = context,
            target = target,
        )
    }

    suspend fun requestDownloadMirrorSelection(
        context: Context?,
        preferredSeason: Int?,
        preferredEpisode: Int?,
        onSourcesProgress: (Int) -> Unit = {},
        onSelectionUpdated: ((MovieDetailsCompatSelectionRequest) -> Unit)? = null,
        shouldCancelLoading: (() -> Boolean)? = null,
    ): MovieDetailsCompatActionOutcome {
        val target = resolveTarget(preferredSeason, preferredEpisode)
        if (target == null) {
            onMissingTarget()
            return MovieDetailsCompatActionOutcome.Completed
        }

        return downloadSupport.requestDownloadMirrorSelection(
            target = target,
            context = context,
            onSourcesProgress = onSourcesProgress,
            onSelectionUpdated = onSelectionUpdated,
            shouldCancelLoading = shouldCancelLoading,
        )
    }

    suspend fun prefetchDownloadMirrorLinks(
        preferredSeason: Int?,
        preferredEpisode: Int?,
    ) {
        val target = resolveTarget(preferredSeason, preferredEpisode) ?: return
        downloadSupport.prefetchDownloadMirrorLinks(target)
    }
}
