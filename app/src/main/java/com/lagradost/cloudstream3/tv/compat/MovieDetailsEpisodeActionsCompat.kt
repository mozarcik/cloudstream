package com.lagradost.cloudstream3.tv.compat

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.CommonActivity.getCastSession
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.actions.VideoClickActionHolder
import com.lagradost.cloudstream3.getFolderPrefix
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.sortUrls
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.ui.player.LOADTYPE_ALL
import com.lagradost.cloudstream3.ui.player.LOADTYPE_CHROMECAST
import com.lagradost.cloudstream3.ui.player.LOADTYPE_INAPP
import com.lagradost.cloudstream3.ui.player.LOADTYPE_INAPP_DOWNLOAD
import com.lagradost.cloudstream3.ui.player.RepoLinkGenerator
import com.lagradost.cloudstream3.ui.result.ACTION_CHROME_CAST_EPISODE
import com.lagradost.cloudstream3.ui.result.ACTION_CHROME_CAST_MIRROR
import com.lagradost.cloudstream3.ui.result.ACTION_DOWNLOAD_EPISODE
import com.lagradost.cloudstream3.ui.result.ACTION_DOWNLOAD_EPISODE_SUBTITLE_MIRROR
import com.lagradost.cloudstream3.ui.result.ACTION_DOWNLOAD_MIRROR
import com.lagradost.cloudstream3.ui.result.ACTION_MARK_AS_WATCHED
import com.lagradost.cloudstream3.ui.result.ACTION_MARK_WATCHED_UP_TO_THIS_EPISODE
import com.lagradost.cloudstream3.ui.result.ACTION_PLAY_EPISODE_IN_PLAYER
import com.lagradost.cloudstream3.ui.result.ACTION_RELOAD_EPISODE
import com.lagradost.cloudstream3.ui.result.ExtractorSubtitleLink
import com.lagradost.cloudstream3.ui.result.LinkLoadingResult
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.ResultViewModel2
import com.lagradost.cloudstream3.ui.result.VideoWatchState
import com.lagradost.cloudstream3.ui.result.buildResultEpisode
import com.lagradost.cloudstream3.ui.result.getId
import com.lagradost.cloudstream3.ui.result.getRealPosition
import com.lagradost.cloudstream3.utils.AppContextUtils.isConnectedToChromecast
import com.lagradost.cloudstream3.utils.AppContextUtils.sortSubs
import com.lagradost.cloudstream3.utils.CastHelper.startCast
import com.lagradost.cloudstream3.utils.DataStore.getKey
import com.lagradost.cloudstream3.utils.DataStore.setKey
import com.lagradost.cloudstream3.utils.DataStoreHelper.getDub
import com.lagradost.cloudstream3.utils.DataStoreHelper.getVideoWatchState
import com.lagradost.cloudstream3.utils.DataStoreHelper.setVideoWatchState
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import com.lagradost.cloudstream3.utils.VideoDownloadManager.DownloadEpisodeMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class MovieDetailsCompatPanelItem(
    val id: Int,
    val label: String,
    @DrawableRes val iconRes: Int,
)

data class MovieDetailsCompatSelectionRequest(
    val title: String,
    val options: List<MovieDetailsCompatPanelItem>,
    val onOptionSelected: suspend (Int) -> MovieDetailsCompatActionOutcome,
)

data class MovieDetailsCompatDownloadSnapshot(
    val episodeId: Int,
    val status: VideoDownloadManager.DownloadType?,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val hasPendingRequest: Boolean,
)

sealed interface MovieDetailsCompatActionOutcome {
    data object Completed : MovieDetailsCompatActionOutcome
    data class OpenSelection(
        val request: MovieDetailsCompatSelectionRequest,
    ) : MovieDetailsCompatActionOutcome
}

class MovieDetailsEpisodeActionsCompat(
    private val sourceUrl: String,
    private val apiName: String,
    private val preferredSeason: Int? = null,
    private val preferredEpisode: Int? = null,
) {
    private companion object {
        const val DebugTag = "MovieActionsCompat"
    }

    private data class TargetEpisode(
        val data: String,
        val season: Int,
        val episode: Int,
        val index: Int,
        val id: Int,
    )

    private data class MovieActionTarget(
        val loadResponse: LoadResponse,
        val episode: ResultEpisode,
        val episodesBySeason: Map<Int, List<TargetEpisode>>,
    )

    private var cachedTarget: MovieActionTarget? = null

    suspend fun loadPanelActions(context: Context?): List<MovieDetailsCompatPanelItem> {
        val target = resolveTarget() ?: return emptyList()
        val actions = mutableListOf<MovieDetailsCompatPanelItem>()

        if (context?.isConnectedToChromecast() == true) {
            actions += item(
                id = ACTION_CHROME_CAST_EPISODE,
                labelRes = R.string.episode_action_chromecast_episode,
                iconRes = R.drawable.ic_baseline_tv_24,
                context = context
            )
            actions += item(
                id = ACTION_CHROME_CAST_MIRROR,
                labelRes = R.string.episode_action_chromecast_mirror,
                iconRes = R.drawable.ic_baseline_tv_24,
                context = context
            )
        }

        actions += item(
            id = ACTION_PLAY_EPISODE_IN_PLAYER,
            labelRes = R.string.episode_action_play_in_app,
            iconRes = R.drawable.ic_baseline_play_arrow_24,
            context = context
        )
        actions += item(
            id = ACTION_DOWNLOAD_EPISODE,
            labelRes = R.string.episode_action_auto_download,
            iconRes = R.drawable.baseline_downloading_24,
            context = context
        )
        actions += item(
            id = ACTION_DOWNLOAD_MIRROR,
            labelRes = R.string.episode_action_download_mirror,
            iconRes = R.drawable.baseline_downloading_24,
            context = context
        )
        actions += item(
            id = ACTION_DOWNLOAD_EPISODE_SUBTITLE_MIRROR,
            labelRes = R.string.episode_action_download_subtitle,
            iconRes = R.drawable.ic_baseline_subtitles_24,
            context = context
        )
        actions += item(
            id = ACTION_RELOAD_EPISODE,
            labelRes = R.string.episode_action_reload_links,
            iconRes = R.drawable.ic_refresh,
            context = context
        )

        val externalActions = VideoClickActionHolder.makeOptionMap(CommonActivity.activity, target.episode)
        actions += externalActions.map { (name, actionId) ->
            MovieDetailsCompatPanelItem(
                id = actionId,
                label = name.asStringNull(context) ?: name.toString(),
                iconRes = R.drawable.ic_baseline_open_in_new_24
            )
        }

        if (target.episode.tvType !in setOf(TvType.Movie, TvType.AnimeMovie)) {
            val isWatched = getVideoWatchState(target.episode.id) == VideoWatchState.Watched
            actions += item(
                id = ACTION_MARK_AS_WATCHED,
                labelRes = if (isWatched) R.string.action_remove_from_watched else R.string.action_mark_as_watched,
                iconRes = R.drawable.ic_baseline_check_24,
                context = context
            )
            actions += item(
                id = ACTION_MARK_WATCHED_UP_TO_THIS_EPISODE,
                labelRes = if (isWatched) {
                    R.string.action_remove_mark_watched_up_to_this_episode
                } else {
                    R.string.action_mark_watched_up_to_this_episode
                },
                iconRes = R.drawable.baseline_list_alt_24,
                context = context
            )
        }

        return actions
    }

    suspend fun execute(
        actionId: Int,
        context: Context?,
        onPlayInApp: () -> Unit,
    ): MovieDetailsCompatActionOutcome {
        val target = resolveTarget()
        if (target == null) {
            showToast(R.string.no_links_found_toast)
            return MovieDetailsCompatActionOutcome.Completed
        }

        return when (actionId) {
            ACTION_PLAY_EPISODE_IN_PLAYER -> {
                onPlayInApp()
                MovieDetailsCompatActionOutcome.Completed
            }

            ACTION_DOWNLOAD_EPISODE -> {
                ResultViewModel2.downloadEpisode(
                    activity = CommonActivity.activity,
                    episode = target.episode,
                    currentIsMovie = target.loadResponse.type in setOf(TvType.Movie, TvType.AnimeMovie),
                    currentHeaderName = target.loadResponse.name,
                    currentType = target.loadResponse.type,
                    currentPoster = target.loadResponse.posterUrl ?: target.loadResponse.backgroundPosterUrl,
                    apiName = target.loadResponse.apiName,
                    parentId = target.loadResponse.getId(),
                    url = target.loadResponse.url
                )
                MovieDetailsCompatActionOutcome.Completed
            }

            ACTION_DOWNLOAD_MIRROR -> downloadMirror(target, context)
            ACTION_DOWNLOAD_EPISODE_SUBTITLE_MIRROR -> downloadSubtitleMirror(target, context)

            ACTION_RELOAD_EPISODE -> {
                val links = loadLinks(
                    target = target,
                    sourceTypes = LOADTYPE_INAPP,
                    clearCache = true
                )
                showToast(if (links.links.isNotEmpty()) R.string.links_reloaded_toast else R.string.no_links_found_toast)
                MovieDetailsCompatActionOutcome.Completed
            }

            ACTION_CHROME_CAST_EPISODE -> castEpisode(target)
            ACTION_CHROME_CAST_MIRROR -> castMirror(target, context)

            ACTION_MARK_AS_WATCHED -> {
                val newState = if (getVideoWatchState(target.episode.id) == VideoWatchState.Watched) {
                    VideoWatchState.None
                } else {
                    VideoWatchState.Watched
                }
                setVideoWatchState(target.episode.id, newState)
                MovieDetailsCompatActionOutcome.Completed
            }

            ACTION_MARK_WATCHED_UP_TO_THIS_EPISODE -> {
                toggleWatchedUpTo(target)
                MovieDetailsCompatActionOutcome.Completed
            }

            else -> executeExternalAction(
                actionId = actionId,
                target = target,
                context = context
            )
        }
    }

    suspend fun getDownloadSnapshot(context: Context?): MovieDetailsCompatDownloadSnapshot? {
        val target = resolveTarget() ?: return null
        val episodeId = target.episode.id
        val key = episodeId.toString()
        val downloadedFileInfo = context?.let { ctx ->
            withContext(Dispatchers.IO) {
                VideoDownloadManager.getDownloadFileInfoAndUpdateSettings(ctx, episodeId)
            }
        }
        val hasPendingRequest = context?.let { ctx ->
            withContext(Dispatchers.IO) {
                val hasWorkerInfo = ctx.getKey<VideoDownloadManager.DownloadInfo>(
                    VideoDownloadManager.WORK_KEY_INFO,
                    key
                ) != null
                val hasWorkerPackage = ctx.getKey<VideoDownloadManager.DownloadResumePackage>(
                    VideoDownloadManager.WORK_KEY_PACKAGE,
                    key
                ) != null
                val hasResumePackage = VideoDownloadManager.getDownloadResumePackage(
                    ctx,
                    episodeId
                ) != null
                hasWorkerInfo || hasWorkerPackage || hasResumePackage
            }
        } ?: false

        return MovieDetailsCompatDownloadSnapshot(
            episodeId = episodeId,
            status = VideoDownloadManager.downloadStatus[episodeId],
            downloadedBytes = downloadedFileInfo?.fileLength ?: 0L,
            totalBytes = downloadedFileInfo?.totalBytes ?: 0L,
            hasPendingRequest = hasPendingRequest
        )
    }

    suspend fun requestDownloadMirrorSelection(
        context: Context?,
        onSourcesProgress: (Int) -> Unit = {},
        shouldSkipLoading: (() -> Boolean)? = null,
    ): MovieDetailsCompatActionOutcome {
        val target = resolveTarget()
        if (target == null) {
            showToast(R.string.no_links_found_toast)
            return MovieDetailsCompatActionOutcome.Completed
        }

        return requestDownloadMirrorSelection(
            target = target,
            context = context,
            onSourcesProgress = onSourcesProgress,
            shouldSkipLoading = shouldSkipLoading,
        )
    }

    private suspend fun downloadMirror(
        target: MovieActionTarget,
        context: Context?,
    ): MovieDetailsCompatActionOutcome {
        return requestDownloadMirrorSelection(
            target = target,
            context = context
        )
    }

    private suspend fun requestDownloadMirrorSelection(
        target: MovieActionTarget,
        context: Context?,
        onSourcesProgress: (Int) -> Unit = {},
        shouldSkipLoading: (() -> Boolean)? = null,
    ): MovieDetailsCompatActionOutcome {
        Log.d(
            DebugTag,
            "request download mirror selection start episodeId=${target.episode.id} name=${target.episode.name}"
        )
        onSourcesProgress(0)
        val loaded = loadLinks(
            target = target,
            sourceTypes = LOADTYPE_INAPP_DOWNLOAD,
            onLinksLoaded = onSourcesProgress,
            shouldSkipLoading = shouldSkipLoading
        )

        Log.d(
            DebugTag,
            "request download mirror selection loaded links=${loaded.links.size} subtitles=${loaded.subs.size}"
        )

        if (loaded.links.isEmpty()) {
            showToast(R.string.no_links_found_toast)
            return MovieDetailsCompatActionOutcome.Completed
        }

        val options = loaded.links.mapIndexed { index, link ->
            MovieDetailsCompatPanelItem(
                id = index,
                label = "${link.name} ${Qualities.getStringByInt(link.quality)}",
                iconRes = R.drawable.baseline_downloading_24
            )
        }

        val title = context?.getString(R.string.episode_action_download_mirror)
            ?: "Download mirror"

        return MovieDetailsCompatActionOutcome.OpenSelection(
            request = MovieDetailsCompatSelectionRequest(
                title = title,
                options = options,
                onOptionSelected = { selectedIndex ->
                    Log.d(
                        DebugTag,
                        "download mirror selected index=$selectedIndex episodeId=${target.episode.id}"
                    )
                    startMirrorDownload(
                        target = target,
                        loaded = loaded,
                        selectedIndex = selectedIndex,
                        context = context
                    )
                    MovieDetailsCompatActionOutcome.Completed
                }
            )
        )
    }

    private suspend fun downloadSubtitleMirror(
        target: MovieActionTarget,
        context: Context?,
    ): MovieDetailsCompatActionOutcome {
        val loaded = loadLinks(target = target)
        if (loaded.subs.isEmpty()) {
            showToast(R.string.no_links_found_toast)
            return MovieDetailsCompatActionOutcome.Completed
        }

        if (loaded.subs.size == 1) {
            startSubtitleDownload(target, loaded.subs.first())
            return MovieDetailsCompatActionOutcome.Completed
        }

        val options = loaded.subs.mapIndexed { index, subtitle ->
            MovieDetailsCompatPanelItem(
                id = index,
                label = subtitle.name,
                iconRes = R.drawable.ic_baseline_subtitles_24
            )
        }
        val title = context?.getString(R.string.episode_action_download_subtitle)
            ?: "Download subtitles"

        return MovieDetailsCompatActionOutcome.OpenSelection(
            request = MovieDetailsCompatSelectionRequest(
                title = title,
                options = options,
                onOptionSelected = { selectedIndex ->
                    val clampedIndex = selectedIndex.coerceIn(loaded.subs.indices)
                    startSubtitleDownload(target, loaded.subs[clampedIndex])
                    MovieDetailsCompatActionOutcome.Completed
                }
            )
        )
    }

    private suspend fun castEpisode(target: MovieActionTarget): MovieDetailsCompatActionOutcome {
        val loaded = loadLinks(
            target = target,
            sourceTypes = LOADTYPE_CHROMECAST,
            isCasting = true
        )
        if (loaded.links.isEmpty()) {
            showToast(R.string.no_links_found_toast)
            return MovieDetailsCompatActionOutcome.Completed
        }
        startChromecast(target = target, loaded = loaded, startIndex = 0)
        return MovieDetailsCompatActionOutcome.Completed
    }

    private suspend fun castMirror(
        target: MovieActionTarget,
        context: Context?,
    ): MovieDetailsCompatActionOutcome {
        val loaded = loadLinks(
            target = target,
            sourceTypes = LOADTYPE_CHROMECAST,
            isCasting = true
        )
        if (loaded.links.isEmpty()) {
            showToast(R.string.no_links_found_toast)
            return MovieDetailsCompatActionOutcome.Completed
        }

        if (loaded.links.size == 1) {
            startChromecast(target = target, loaded = loaded, startIndex = 0)
            return MovieDetailsCompatActionOutcome.Completed
        }

        val options = loaded.links.mapIndexed { index, link ->
            MovieDetailsCompatPanelItem(
                id = index,
                label = "${link.name} ${Qualities.getStringByInt(link.quality)}",
                iconRes = R.drawable.ic_baseline_tv_24
            )
        }
        val title = context?.getString(R.string.episode_action_chromecast_mirror)
            ?: "Chromecast mirror"

        return MovieDetailsCompatActionOutcome.OpenSelection(
            request = MovieDetailsCompatSelectionRequest(
                title = title,
                options = options,
                onOptionSelected = { selectedIndex ->
                    startChromecast(
                        target = target,
                        loaded = loaded,
                        startIndex = selectedIndex
                    )
                    MovieDetailsCompatActionOutcome.Completed
                }
            )
        )
    }

    private suspend fun executeExternalAction(
        actionId: Int,
        target: MovieActionTarget,
        context: Context?,
    ): MovieDetailsCompatActionOutcome {
        val action = VideoClickActionHolder.getActionById(actionId)
            ?: return MovieDetailsCompatActionOutcome.Completed

        CommonActivity.activity?.setKey("last_click_action", action.uniqueId())

        if (action.oneSource) {
            val loaded = loadLinks(
                target = target,
                sourceTypes = action.sourceTypes
            )
            if (loaded.links.isEmpty()) {
                showToast(R.string.no_links_found_toast)
                return MovieDetailsCompatActionOutcome.Completed
            }

            if (loaded.links.size == 1) {
                action.runActionSafe(CommonActivity.activity, target.episode, loaded, 0)
                return MovieDetailsCompatActionOutcome.Completed
            }

            val options = loaded.links.mapIndexed { index, link ->
                MovieDetailsCompatPanelItem(
                    id = index,
                    label = "${link.name} ${Qualities.getStringByInt(link.quality)}",
                    iconRes = R.drawable.ic_baseline_open_in_new_24
                )
            }

            return MovieDetailsCompatActionOutcome.OpenSelection(
                request = MovieDetailsCompatSelectionRequest(
                    title = action.name.asStringNull(context) ?: action.name.toString(),
                    options = options,
                    onOptionSelected = { selectedIndex ->
                        action.runActionSafe(
                            CommonActivity.activity,
                            target.episode,
                            loaded,
                            selectedIndex.coerceIn(loaded.links.indices)
                        )
                        MovieDetailsCompatActionOutcome.Completed
                    }
                )
            )
        }

        val loaded = loadLinks(
            target = target,
            sourceTypes = action.sourceTypes
        )
        if (loaded.links.isEmpty()) {
            showToast(R.string.no_links_found_toast)
            return MovieDetailsCompatActionOutcome.Completed
        }

        action.runActionSafe(CommonActivity.activity, target.episode, loaded, null)
        return MovieDetailsCompatActionOutcome.Completed
    }

    private suspend fun startChromecast(
        target: MovieActionTarget,
        loaded: LinkLoadingResult,
        startIndex: Int,
    ) {
        withContext(Dispatchers.Main) {
            val activity = CommonActivity.activity ?: return@withContext
            activity.getCastSession()?.startCast(
                apiName = target.loadResponse.apiName,
                isMovie = target.loadResponse.type in setOf(TvType.Movie, TvType.AnimeMovie),
                title = target.loadResponse.name,
                poster = target.loadResponse.posterUrl,
                currentEpisodeIndex = target.episode.index,
                episodes = listOf(target.episode),
                currentLinks = loaded.links,
                subtitles = loaded.subs,
                startIndex = startIndex.coerceIn(loaded.links.indices),
                startTime = target.episode.getRealPosition()
            )
        }
    }

    private fun startMirrorDownload(
        target: MovieActionTarget,
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
            subs = loaded.subs
        )
        Log.d(
            DebugTag,
            "startMirrorDownload: request sent for episodeId=${target.episode.id}"
        )
        showToast(R.string.download_started)
    }

    private suspend fun startSubtitleDownload(
        target: MovieActionTarget,
        subtitle: com.lagradost.cloudstream3.ui.player.SubtitleData,
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
                createNotificationCallback = {}
            )
        }
        showToast(R.string.download_started)
    }

    private suspend fun resolveTarget(): MovieActionTarget? {
        cachedTarget?.let { return it }

        val api = APIHolder.getApiFromNameNull(apiName) ?: return null
        val repository = APIRepository(api)
        val loadResponse = when (val response = repository.load(sourceUrl)) {
            is Resource.Success -> response.value
            else -> return null
        }

        val target = when (loadResponse) {
            is MovieLoadResponse -> buildMovieTarget(loadResponse)
            is TvSeriesLoadResponse -> buildSeriesTarget(loadResponse)
            is AnimeLoadResponse -> buildAnimeTarget(loadResponse)
            else -> null
        }

        cachedTarget = target
        return target
    }

    private fun buildMovieTarget(loadResponse: MovieLoadResponse): MovieActionTarget? {
        val data = loadResponse.dataUrl.takeIf { it.isNotBlank() } ?: return null
        val mainId = loadResponse.getId()
        val episode = TargetEpisode(
            data = data,
            season = 0,
            episode = 0,
            index = 0,
            id = mainId
        )
        return toMovieActionTarget(
            loadResponse = loadResponse,
            selectedEpisode = episode,
            allEpisodes = listOf(episode)
        )
    }

    private fun buildSeriesTarget(loadResponse: TvSeriesLoadResponse): MovieActionTarget? {
        val mainId = loadResponse.getId()
        val episodes = loadResponse.episodes
            .sortedBy { (it.season?.times(10_000) ?: 0) + (it.episode ?: 0) }
            .mapIndexed { index, episode ->
                val episodeIndex = episode.episode ?: (index + 1)
                TargetEpisode(
                    data = episode.data,
                    season = episode.season ?: 0,
                    episode = episodeIndex,
                    index = index,
                    id = mainId + (episode.season?.times(100_000) ?: 0) + episodeIndex + 1
                )
            }

        if (episodes.isEmpty()) {
            return null
        }

        return toMovieActionTarget(
            loadResponse = loadResponse,
            selectedEpisode = pickPreferredEpisode(episodes),
            allEpisodes = episodes
        )
    }

    private fun buildAnimeTarget(loadResponse: AnimeLoadResponse): MovieActionTarget? {
        val mainId = loadResponse.getId()
        val preferredDub = resolvePreferredDubStatus(loadResponse, mainId) ?: return null
        val dubEpisodes = loadResponse.episodes[preferredDub].orEmpty()
        if (dubEpisodes.isEmpty()) {
            return null
        }

        val episodes = dubEpisodes.mapIndexed { index, episode ->
            val episodeIndex = episode.episode ?: (index + 1)
            TargetEpisode(
                data = episode.data,
                season = episode.season ?: 0,
                episode = episodeIndex,
                index = index,
                id = mainId + episodeIndex + preferredDub.id * 1_000_000 + ((episode.season ?: 0) * 10_000)
            )
        }

        return toMovieActionTarget(
            loadResponse = loadResponse,
            selectedEpisode = pickPreferredEpisode(episodes),
            allEpisodes = episodes
        )
    }

    private fun resolvePreferredDubStatus(
        loadResponse: AnimeLoadResponse,
        mainId: Int,
    ): DubStatus? {
        val available = loadResponse.episodes.keys
        if (available.isEmpty()) return null

        val stored = getDub(mainId)
        if (stored != null && available.contains(stored)) {
            return stored
        }
        return when {
            available.contains(DubStatus.Dubbed) -> DubStatus.Dubbed
            available.contains(DubStatus.Subbed) -> DubStatus.Subbed
            available.contains(DubStatus.None) -> DubStatus.None
            else -> available.firstOrNull()
        }
    }

    private fun pickPreferredEpisode(episodes: List<TargetEpisode>): TargetEpisode {
        val orderedEpisodes = episodes.sortedWith(compareBy({ it.season }, { it.episode }))
        val exact = orderedEpisodes.firstOrNull { episode ->
            preferredSeason != null &&
                preferredEpisode != null &&
                episode.season == preferredSeason &&
                episode.episode == preferredEpisode
        }
        if (exact != null) {
            return exact
        }

        val matchingEpisode = orderedEpisodes.firstOrNull { episode ->
            preferredEpisode != null && episode.episode == preferredEpisode
        }
        if (matchingEpisode != null) {
            return matchingEpisode
        }

        val matchingSeason = orderedEpisodes.firstOrNull { episode ->
            preferredSeason != null && episode.season == preferredSeason
        }
        return matchingSeason ?: orderedEpisodes.first()
    }

    private fun toMovieActionTarget(
        loadResponse: LoadResponse,
        selectedEpisode: TargetEpisode,
        allEpisodes: List<TargetEpisode>,
    ): MovieActionTarget {
        val season = selectedEpisode.season.takeIf { it > 0 }
        val episode = buildResultEpisode(
            headerName = loadResponse.name,
            name = loadResponse.name,
            poster = loadResponse.posterUrl ?: loadResponse.backgroundPosterUrl,
            episode = selectedEpisode.episode,
            seasonIndex = season,
            season = season,
            data = selectedEpisode.data,
            apiName = loadResponse.apiName,
            id = selectedEpisode.id,
            index = selectedEpisode.index,
            description = loadResponse.plot,
            tvType = loadResponse.type,
            parentId = loadResponse.getId(),
        )

        val episodesBySeason = allEpisodes
            .groupBy { it.season }
            .mapValues { (_, episodesInSeason) -> episodesInSeason.sortedBy { it.episode } }

        return MovieActionTarget(
            loadResponse = loadResponse,
            episode = episode,
            episodesBySeason = episodesBySeason
        )
    }

    private fun toggleWatchedUpTo(target: MovieActionTarget) {
        val clickSeason = target.episode.season ?: 0
        val clickEpisode = target.episode.episode
        val watchState = if (getVideoWatchState(target.episode.id) == VideoWatchState.Watched) {
            VideoWatchState.None
        } else {
            VideoWatchState.Watched
        }

        target.episodesBySeason
            .toSortedMap()
            .forEach { (season, episodes) ->
                if (season > clickSeason) {
                    return@forEach
                }
                if (clickSeason != 0 && season == 0) {
                    return@forEach
                }
                val targetEpisodes = if (season == clickSeason) {
                    episodes.filter { it.episode <= clickEpisode }
                } else {
                    episodes
                }
                targetEpisodes.forEach { episode ->
                    setVideoWatchState(episode.id, watchState)
                }
            }
    }

    private suspend fun loadLinks(
        target: MovieActionTarget,
        sourceTypes: Set<ExtractorLinkType> = LOADTYPE_ALL,
        clearCache: Boolean = false,
        isCasting: Boolean = false,
        onLinksLoaded: ((Int) -> Unit)? = null,
        shouldSkipLoading: (() -> Boolean)? = null,
    ): LinkLoadingResult {
        val links = linkedSetOf<com.lagradost.cloudstream3.utils.ExtractorLink>()
        val subtitles = linkedSetOf<com.lagradost.cloudstream3.ui.player.SubtitleData>()

        coroutineScope {
            val loadJob = async {
                try {
                    RepoLinkGenerator(
                        episodes = listOf(target.episode),
                        page = target.loadResponse
                    ).generateLinks(
                        clearCache = clearCache,
                        sourceTypes = sourceTypes,
                        callback = { (link, _) ->
                            if (link != null && links.add(link)) {
                                onLinksLoaded?.invoke(links.size)
                                Log.d(
                                    DebugTag,
                                    "loadLinks source added count=${links.size} name=${link.name} quality=${link.quality} episodeId=${target.episode.id}"
                                )
                            }
                        },
                        subtitleCallback = { subtitle ->
                            subtitles += subtitle
                        },
                        isCasting = isCasting
                    )
                } catch (_: CancellationException) {
                    Log.d(DebugTag, "link loading cancelled for action")
                } catch (error: Throwable) {
                    Log.e(DebugTag, "failed loading links for action", error)
                }
            }

            if (shouldSkipLoading != null) {
                while (loadJob.isActive) {
                    if (shouldSkipLoading() && links.isNotEmpty()) {
                        Log.d(
                            DebugTag,
                            "loadLinks skip requested with alreadyLoaded=${links.size} episodeId=${target.episode.id}"
                        )
                        loadJob.cancel()
                        break
                    }
                    delay(80)
                }
            }

            try {
                loadJob.await()
            } catch (_: CancellationException) {
                // Use whatever links were loaded before cancellation.
            }
        }

        return LinkLoadingResult(
            links = sortUrls(links),
            subs = sortSubs(subtitles),
            syncData = HashMap(target.loadResponse.syncData)
        )
    }

    private fun createDownloadMeta(target: MovieActionTarget): DownloadEpisodeMetadata {
        val isMovieType = target.loadResponse.type in setOf(TvType.Movie, TvType.AnimeMovie)
        return DownloadEpisodeMetadata(
            id = target.episode.id,
            mainName = VideoDownloadManager.sanitizeFilename(target.loadResponse.name),
            sourceApiName = target.loadResponse.apiName,
            poster = target.episode.poster ?: target.loadResponse.posterUrl,
            name = target.episode.name,
            season = if (isMovieType) null else target.episode.season,
            episode = if (isMovieType) null else target.episode.episode,
            type = target.loadResponse.type
        )
    }

    private fun getDownloadFolder(type: TvType, title: String): String {
        return if (type.isEpisodeBased()) {
            "${type.getFolderPrefix()}/${VideoDownloadManager.sanitizeFilename(title)}"
        } else {
            type.getFolderPrefix()
        }
    }

    private fun item(
        id: Int,
        @StringRes labelRes: Int,
        @DrawableRes iconRes: Int,
        context: Context?,
    ): MovieDetailsCompatPanelItem {
        return MovieDetailsCompatPanelItem(
            id = id,
            label = context?.getString(labelRes) ?: labelRes.toString(),
            iconRes = iconRes
        )
    }

    private fun showToast(@StringRes textRes: Int) {
        CommonActivity.showToast(textRes, Toast.LENGTH_SHORT)
    }
}
