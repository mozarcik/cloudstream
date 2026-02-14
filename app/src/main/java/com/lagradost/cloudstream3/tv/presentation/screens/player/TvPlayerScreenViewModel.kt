package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.ui.player.LOADTYPE_INAPP
import com.lagradost.cloudstream3.ui.player.RepoLinkGenerator
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.buildResultEpisode
import com.lagradost.cloudstream3.isLiveStream
import com.lagradost.cloudstream3.utils.AppContextUtils.sortSubs
import com.lagradost.cloudstream3.utils.DataStoreHelper.getViewPos
import com.lagradost.cloudstream3.utils.DataStoreHelper.setViewPosAndResume
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DebugTag = "TvPlayerVM"
private const val ReadyRefreshBatchSize = 10
private const val ReadyRefreshMinIntervalMs = 500L
private const val PlaybackProgressPersistIntervalMs = 1_000L

data class TvPlayerMetadata(
    val title: String,
    val subtitle: String,
    val backdropUri: String?,
    val year: Int? = null,
    val apiName: String = "",
    val season: Int? = null,
    val episode: Int? = null,
    val episodeTitle: String? = null,
    val isEpisodeBased: Boolean = false,
) {
    companion object {
        val Empty = TvPlayerMetadata(
            title = "",
            subtitle = "",
            backdropUri = null
        )
    }
}

sealed interface TvPlayerUiState {
    data class LoadingSources(
        val metadata: TvPlayerMetadata,
        val loadedSources: Int,
        val canSkip: Boolean,
    ) : TvPlayerUiState

    data class Ready(
        val metadata: TvPlayerMetadata,
        val link: ExtractorLink,
        val sourceCount: Int,
        val sources: List<ExtractorLink> = emptyList(),
        val currentSourceIndex: Int = -1,
        val subtitles: List<SubtitleData> = emptyList(),
        val episodeId: Int = -1,
        val resumePositionMs: Long = 0L,
    ) : TvPlayerUiState

    data class Error(
        val metadata: TvPlayerMetadata,
        val messageResId: Int,
    ) : TvPlayerUiState
}

class TvPlayerScreenViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private data class PlaybackTarget(
        val episode: ResultEpisode,
        val page: LoadResponse?,
        val metadata: TvPlayerMetadata,
    )

    private val _uiState = MutableStateFlow<TvPlayerUiState>(
        TvPlayerUiState.LoadingSources(
            metadata = TvPlayerMetadata.Empty,
            loadedSources = 0,
            canSkip = false,
        )
    )
    val uiState: StateFlow<TvPlayerUiState> = _uiState.asStateFlow()

    private val loadedLinksByUrl = linkedMapOf<String, ExtractorLink>()
    private val loadedSubtitlesById = linkedMapOf<String, SubtitleData>()
    private var loadingJob: Job? = null
    private var hasFinalized = false
    private var metadata: TvPlayerMetadata = TvPlayerMetadata.Empty
    private var orderedLinks: List<ExtractorLink> = emptyList()
    private var orderedSubtitles: List<SubtitleData> = emptyList()
    private var currentLinkIndex: Int = -1
    private var pendingReadyRefreshChanges: Int = 0
    private var lastReadyRefreshAtElapsedMs: Long = 0L
    private var currentEpisode: ResultEpisode? = null
    private var currentResumePositionMs: Long = 0L
    private var lastPlaybackProgressPersistAtElapsedMs: Long = 0L

    init {
        loadSources()
    }

    fun retry() {
        loadingJob?.cancel()
        loadedLinksByUrl.clear()
        loadedSubtitlesById.clear()
        hasFinalized = false
        metadata = TvPlayerMetadata.Empty
        orderedLinks = emptyList()
        orderedSubtitles = emptyList()
        currentLinkIndex = -1
        pendingReadyRefreshChanges = 0
        lastReadyRefreshAtElapsedMs = 0L
        currentEpisode = null
        currentResumePositionMs = 0L
        lastPlaybackProgressPersistAtElapsedMs = 0L
        loadSources()
    }

    fun skipLoading() {
        val hasLoadedSources = synchronized(loadedLinksByUrl) { loadedLinksByUrl.isNotEmpty() }
        if (!hasLoadedSources) {
            return
        }
        if (!hasFinalized) {
            finalizeLoading(forceError = true)
        }
        loadingJob?.cancel()
        loadingJob = null
        pendingReadyRefreshChanges = 0
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

    fun selectSource(index: Int) {
        if (!hasFinalized) return
        val selectedLink = orderedLinks.getOrNull(index) ?: return

        currentLinkIndex = index
        postReadyState(
            link = selectedLink,
            currentIndex = index,
        )
    }

    fun onPlaybackError() {
        if (!hasFinalized) return

        val nextIndex = currentLinkIndex + 1
        val nextLink = orderedLinks.getOrNull(nextIndex)
        if (nextLink != null) {
            currentLinkIndex = nextIndex
            postReadyState(
                link = nextLink,
                currentIndex = nextIndex,
            )
            return
        }

        _uiState.value = TvPlayerUiState.Error(
            metadata = metadata,
            messageResId = R.string.no_links_found_toast,
        )
    }

    private fun loadSources() {
        val url = savedStateHandle.get<String>(TvPlayerScreen.UrlBundleKey).orEmpty()
        val apiName = savedStateHandle.get<String>(TvPlayerScreen.ApiNameBundleKey).orEmpty()
        val episodeData = savedStateHandle.get<String>(TvPlayerScreen.EpisodeDataBundleKey)
            ?.takeIf { it.isNotBlank() }

        if (url.isBlank() || apiName.isBlank()) {
            Log.e(DebugTag, "missing args: url=$url apiName=$apiName")
            _uiState.value = TvPlayerUiState.Error(
                metadata = TvPlayerMetadata.Empty,
                messageResId = R.string.error_loading_links_toast
            )
            return
        }

        loadingJob = viewModelScope.launch {
            val api = APIHolder.getApiFromNameNull(apiName)
            if (api == null) {
                Log.e(DebugTag, "provider not found api=$apiName")
                _uiState.value = TvPlayerUiState.Error(
                    metadata = TvPlayerMetadata(
                        title = apiName,
                        subtitle = "",
                        backdropUri = null
                    ),
                    messageResId = R.string.error_loading_links_toast
                )
                return@launch
            }

            val repository = APIRepository(api)
            val target = resolvePlaybackTarget(
                repository = repository,
                url = url,
                apiName = apiName,
                directEpisodeData = episodeData,
            )

            if (target == null) {
                _uiState.value = TvPlayerUiState.Error(
                    metadata = TvPlayerMetadata(
                        title = apiName,
                        subtitle = "",
                        backdropUri = null
                    ),
                    messageResId = R.string.no_links_found_toast
                )
                return@launch
            }

            metadata = target.metadata
            currentEpisode = target.episode
            currentResumePositionMs = getResumePosition(target.episode.id)
            lastPlaybackProgressPersistAtElapsedMs = 0L
            postLoadingState()

            val generator = RepoLinkGenerator(
                episodes = listOf(target.episode),
                page = target.page,
            )

            try {
                generator.generateLinks(
                    clearCache = false,
                    sourceTypes = LOADTYPE_INAPP,
                    callback = { (link, _) ->
                        if (link == null || link.url.isBlank()) return@generateLinks
                        val inserted = synchronized(loadedLinksByUrl) {
                            loadedLinksByUrl.putIfAbsent(link.url, link) == null
                        }
                        if (inserted) {
                            if (hasFinalized) {
                                onBackgroundDataInsertedAfterFinalize()
                            } else {
                                postLoadingState()
                            }
                        }
                    },
                    subtitleCallback = { subtitle ->
                        val inserted = synchronized(loadedSubtitlesById) {
                            loadedSubtitlesById.putIfAbsent(subtitle.getId(), subtitle) == null
                        }
                        if (inserted && hasFinalized) {
                            onBackgroundDataInsertedAfterFinalize()
                        }
                    }
                )
            } catch (_: CancellationException) {
                Log.d(DebugTag, "source loading cancelled")
            } catch (t: Throwable) {
                logError(t)
            }

            flushPendingReadyRefresh(force = true)
            finalizeLoading(forceError = true)
        }
    }

    private suspend fun resolvePlaybackTarget(
        repository: APIRepository,
        url: String,
        apiName: String,
        directEpisodeData: String?,
    ): PlaybackTarget? {
        var loadResponse: LoadResponse? = null
        var resolvedData = directEpisodeData
        var resolvedSeason: Int? = null
        var resolvedEpisode = 0
        var resolvedEpisodeTitle: String? = null

        if (resolvedData.isNullOrBlank()) {
            loadResponse = (repository.load(url) as? Resource.Success)?.value
            if (loadResponse == null) {
                return null
            }

            when (loadResponse) {
                is MovieLoadResponse -> {
                    resolvedData = loadResponse.dataUrl
                }

                is TvSeriesLoadResponse -> {
                    val first = loadResponse.episodes
                        .sortedWith(compareBy({ it.season ?: Int.MAX_VALUE }, { it.episode ?: Int.MAX_VALUE }))
                        .firstOrNull()
                    resolvedData = first?.data
                    resolvedSeason = first?.season
                    resolvedEpisode = first?.episode ?: 1
                    resolvedEpisodeTitle = first?.name
                }

                is AnimeLoadResponse -> {
                    val first = loadResponse.episodes.values
                        .flatten()
                        .sortedWith(compareBy({ it.season ?: Int.MAX_VALUE }, { it.episode ?: Int.MAX_VALUE }))
                        .firstOrNull()
                    resolvedData = first?.data
                    resolvedSeason = first?.season
                    resolvedEpisode = first?.episode ?: 1
                    resolvedEpisodeTitle = first?.name
                }

                else -> {
                    resolvedData = null
                }
            }
        } else {
            loadResponse = (repository.load(url) as? Resource.Success)?.value
            when (loadResponse) {
                is TvSeriesLoadResponse -> {
                    val matchingEpisode = loadResponse.episodes.firstOrNull { episode ->
                        episode.data == resolvedData
                    }
                    resolvedSeason = matchingEpisode?.season
                    resolvedEpisode = matchingEpisode?.episode ?: 1
                    resolvedEpisodeTitle = matchingEpisode?.name
                }

                is AnimeLoadResponse -> {
                    val matchingEpisode = loadResponse.episodes.values
                        .flatten()
                        .firstOrNull { episode ->
                            episode.data == resolvedData
                        }
                    resolvedSeason = matchingEpisode?.season
                    resolvedEpisode = matchingEpisode?.episode ?: 1
                    resolvedEpisodeTitle = matchingEpisode?.name
                }

                else -> Unit
            }
        }

        if (resolvedData.isNullOrBlank()) {
            return null
        }

        val resolvedType = loadResponse?.type ?: TvType.TvSeries
        val resolvedTitle = loadResponse?.name ?: apiName
        val parentId = url.hashCode()
        val episodeId = "$apiName|$resolvedData".hashCode()

        val episode = buildResultEpisode(
            headerName = resolvedTitle,
            name = resolvedEpisodeTitle ?: resolvedTitle,
            poster = loadResponse?.posterUrl ?: loadResponse?.backgroundPosterUrl,
            episode = resolvedEpisode,
            seasonIndex = resolvedSeason,
            season = resolvedSeason,
            data = resolvedData,
            apiName = apiName,
            id = episodeId,
            index = 0,
            tvType = resolvedType,
            parentId = parentId,
        )

        return PlaybackTarget(
            episode = episode,
            page = loadResponse,
            metadata = resolveMetadata(
                loadResponse = loadResponse,
                apiName = apiName,
                resolvedSeason = resolvedSeason,
                resolvedEpisode = resolvedEpisode.takeIf { it > 0 },
                resolvedEpisodeTitle = resolvedEpisodeTitle,
            )
        )
    }

    private fun resolveMetadata(
        loadResponse: LoadResponse?,
        apiName: String,
        resolvedSeason: Int?,
        resolvedEpisode: Int?,
        resolvedEpisodeTitle: String?,
    ): TvPlayerMetadata {
        if (loadResponse == null) {
            return TvPlayerMetadata(
                title = apiName,
                subtitle = "",
                backdropUri = null,
                apiName = apiName,
            )
        }

        val year = when (loadResponse) {
            is MovieLoadResponse -> loadResponse.year
            is TvSeriesLoadResponse -> loadResponse.year
            is AnimeLoadResponse -> loadResponse.year
            else -> null
        }
        val subtitleChunks = listOfNotNull(year?.toString(), apiName)
        val isEpisodeBased = loadResponse is TvSeriesLoadResponse || loadResponse is AnimeLoadResponse

        return TvPlayerMetadata(
            title = loadResponse.name,
            subtitle = subtitleChunks.joinToString(separator = " . "),
            backdropUri = loadResponse.backgroundPosterUrl ?: loadResponse.posterUrl,
            year = year,
            apiName = apiName,
            season = resolvedSeason,
            episode = resolvedEpisode,
            episodeTitle = resolvedEpisodeTitle,
            isEpisodeBased = isEpisodeBased,
        )
    }

    private fun postLoadingState() {
        if (hasFinalized) return
        val loadedSources = synchronized(loadedLinksByUrl) { loadedLinksByUrl.size }
        _uiState.value = TvPlayerUiState.LoadingSources(
            metadata = metadata,
            loadedSources = loadedSources,
            canSkip = loadedSources > 0,
        )
    }

    private fun onBackgroundDataInsertedAfterFinalize() {
        if (!hasFinalized) return

        pendingReadyRefreshChanges += 1
        val now = SystemClock.elapsedRealtime()
        val elapsedSinceLastRefresh = now - lastReadyRefreshAtElapsedMs

        if (pendingReadyRefreshChanges >= ReadyRefreshBatchSize ||
            elapsedSinceLastRefresh >= ReadyRefreshMinIntervalMs
        ) {
            flushPendingReadyRefresh(force = false)
        }
    }

    private fun flushPendingReadyRefresh(force: Boolean) {
        if (!hasFinalized) return
        if (!force && pendingReadyRefreshChanges <= 0) return

        pendingReadyRefreshChanges = 0
        refreshReadyStateFromLoadedData()
    }

    private fun refreshReadyStateFromLoadedData() {
        if (!hasFinalized) return
        val currentUrl = orderedLinks.getOrNull(currentLinkIndex)?.url
        val candidateLinks = synchronized(loadedLinksByUrl) {
            loadedLinksByUrl.values
                .sortedWith(
                    compareByDescending<ExtractorLink> { link ->
                        if (link.quality > 0) link.quality else 0
                    }.thenBy { link ->
                        link.isM3u8
                    }
                )
        }
        if (candidateLinks.isEmpty()) return

        orderedLinks = candidateLinks
        orderedSubtitles = synchronized(loadedSubtitlesById) {
            sortSubs(loadedSubtitlesById.values.toSet())
        }

        val resolvedCurrentIndex = currentUrl
            ?.let { url -> orderedLinks.indexOfFirst { link -> link.url == url } }
            ?.takeIf { it >= 0 }
            ?: currentLinkIndex.takeIf { it in orderedLinks.indices }
            ?: 0

        currentLinkIndex = resolvedCurrentIndex
        postReadyState(
            link = orderedLinks[resolvedCurrentIndex],
            currentIndex = resolvedCurrentIndex,
        )
    }

    private fun finalizeLoading(forceError: Boolean) {
        if (hasFinalized) return

        val candidateLinks = synchronized(loadedLinksByUrl) {
            loadedLinksByUrl.values
                .sortedWith(
                    compareByDescending<ExtractorLink> { link ->
                        if (link.quality > 0) link.quality else 0
                    }.thenBy { link ->
                        link.isM3u8
                    }
                )
        }

        if (candidateLinks.isNotEmpty()) {
            orderedLinks = candidateLinks
            orderedSubtitles = synchronized(loadedSubtitlesById) {
                sortSubs(loadedSubtitlesById.values.toSet())
            }
            currentLinkIndex = 0
            hasFinalized = true
            postReadyState(
                link = candidateLinks.first(),
                currentIndex = 0,
            )
            return
        }

        if (forceError) {
            hasFinalized = true
            _uiState.value = TvPlayerUiState.Error(
                metadata = metadata,
                messageResId = R.string.no_links_found_toast,
            )
        }
    }

    private fun postReadyState(
        link: ExtractorLink,
        currentIndex: Int,
    ) {
        lastReadyRefreshAtElapsedMs = SystemClock.elapsedRealtime()
        val episodeId = currentEpisode?.id ?: -1
        _uiState.value = TvPlayerUiState.Ready(
            metadata = metadata,
            link = link,
            sourceCount = orderedLinks.size,
            sources = orderedLinks,
            currentSourceIndex = currentIndex,
            subtitles = orderedSubtitles,
            episodeId = episodeId,
            resumePositionMs = currentResumePositionMs,
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
        val episode = currentEpisode ?: return
        if (episode.tvType.isLiveStream() || episode.tvType == TvType.NSFW) return
        if (durationMs <= 0L) return

        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastPlaybackProgressPersistAtElapsedMs < PlaybackProgressPersistIntervalMs) {
            return
        }

        lastPlaybackProgressPersistAtElapsedMs = now
        setViewPosAndResume(
            id = episode.id,
            position = positionMs.coerceAtLeast(0L),
            duration = durationMs,
            currentEpisode = episode,
            nextEpisode = null,
        )
    }
}
