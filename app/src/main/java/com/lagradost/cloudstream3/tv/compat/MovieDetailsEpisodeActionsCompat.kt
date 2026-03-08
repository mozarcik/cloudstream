package com.lagradost.cloudstream3.tv.compat

import android.content.Context
import com.lagradost.cloudstream3.LoadResponse

class MovieDetailsEpisodeActionsCompat(
    private val loadResponse: LoadResponse,
    private val preferredSeason: Int? = null,
    private val preferredEpisode: Int? = null,
) {
    private val targetResolver = MovieDetailsActionTargetResolver(loadResponse, preferredSeason, preferredEpisode)
    private val linksLoader = MovieDetailsLinksLoader()
    private val watchStateSupport = MovieDetailsWatchStateSupport()
    private val panelActionsFactory = MovieDetailsPanelActionsFactory(watchStateSupport)
    private val downloadSupport = MovieDetailsDownloadSupport(linksLoader)
    private val playbackActionExecutor = MovieDetailsPlaybackActionExecutor(linksLoader)
    private val actionExecutor = MovieDetailsActionExecutor(
        watchStateSupport = watchStateSupport,
        downloadSupport = downloadSupport,
        playbackActionExecutor = playbackActionExecutor,
    )
    private val targetStore = MovieDetailsActionTargetStore()
    private val downloadFacade = MovieDetailsEpisodeDownloadFacade(downloadSupport, ::resolveTarget, actionExecutor::showNoLinksToast)

    suspend fun loadPanelActions(context: Context?): List<MovieDetailsCompatPanelItem> {
        val target = resolveTarget() ?: return emptyList()
        return panelActionsFactory.build(context = context, target = target)
    }

    suspend fun execute(
        actionId: Int,
        context: Context?,
        onPlayInApp: () -> Unit,
    ): MovieDetailsCompatActionOutcome = executeInternal(
            actionId = actionId,
            context = context,
            preferredSeason = preferredSeason,
            preferredEpisode = preferredEpisode,
            onPlayInApp = { onPlayInApp() },
        )

    suspend fun executeForEpisode(
        actionId: Int,
        context: Context?,
        preferredSeason: Int?,
        preferredEpisode: Int?,
        onPlayInApp: (String?) -> Unit,
    ): MovieDetailsCompatActionOutcome = executeInternal(
            actionId = actionId,
            context = context,
            preferredSeason = preferredSeason,
            preferredEpisode = preferredEpisode,
            onPlayInApp = { target ->
                onPlayInApp(target.episode.data)
            },
        )

    suspend fun isEpisodeWatched(
        preferredSeason: Int?,
        preferredEpisode: Int?,
    ): Boolean? = resolveTarget(preferredSeason, preferredEpisode)?.let(watchStateSupport::isEpisodeWatched)

    suspend fun getDownloadSnapshot(context: Context?): MovieDetailsCompatDownloadSnapshot? {
        return downloadFacade.getDownloadSnapshot(
            context = context,
            preferredSeason = preferredSeason,
            preferredEpisode = preferredEpisode,
        )
    }

    suspend fun getDownloadSnapshotForEpisode(
        context: Context?,
        preferredSeason: Int?,
        preferredEpisode: Int?,
    ): MovieDetailsCompatDownloadSnapshot? {
        return downloadFacade.getDownloadSnapshot(
            context = context,
            preferredSeason = preferredSeason,
            preferredEpisode = preferredEpisode,
        )
    }

    suspend fun requestDownloadMirrorSelection(
        context: Context?,
        preferredSeason: Int? = this.preferredSeason,
        preferredEpisode: Int? = this.preferredEpisode,
        onSourcesProgress: (Int) -> Unit = {},
        onSelectionUpdated: ((MovieDetailsCompatSelectionRequest) -> Unit)? = null,
        shouldCancelLoading: (() -> Boolean)? = null,
    ): MovieDetailsCompatActionOutcome {
        return downloadFacade.requestDownloadMirrorSelection(
            context = context,
            preferredSeason = preferredSeason,
            preferredEpisode = preferredEpisode,
            onSourcesProgress = onSourcesProgress,
            onSelectionUpdated = onSelectionUpdated,
            shouldCancelLoading = shouldCancelLoading,
        )
    }

    suspend fun prefetchDownloadMirrorLinks() {
        downloadFacade.prefetchDownloadMirrorLinks(
            preferredSeason = preferredSeason,
            preferredEpisode = preferredEpisode,
        )
    }

    private suspend fun executeInternal(
        actionId: Int,
        context: Context?,
        preferredSeason: Int?,
        preferredEpisode: Int?,
        onPlayInApp: (MovieDetailsActionTarget) -> Unit,
    ): MovieDetailsCompatActionOutcome {
        val resolvedTarget = resolveTarget(
            preferredSeason = preferredSeason,
            preferredEpisode = preferredEpisode,
        ) ?: run {
            actionExecutor.showNoLinksToast()
            return MovieDetailsCompatActionOutcome.Completed
        }

        return actionExecutor.execute(
            actionId = actionId,
            target = resolvedTarget,
            context = context,
            onPlayInApp = onPlayInApp,
        )
    }

    private suspend fun resolveTarget(
        preferredSeason: Int? = this.preferredSeason,
        preferredEpisode: Int? = this.preferredEpisode,
    ): MovieDetailsActionTarget? {
        return targetStore.resolve(
            targetResolver = targetResolver,
            preferredSeason = preferredSeason,
            preferredEpisode = preferredEpisode,
        )
    }
}
