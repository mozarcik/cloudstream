package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.getAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.getImdbId
import com.lagradost.cloudstream3.LoadResponse.Companion.getMalId
import com.lagradost.cloudstream3.LoadResponse.Companion.getTMDbId
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities
import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities.SubtitleSearch
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.subtitleProviders
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelInlineTextField
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.ui.player.LOADTYPE_INAPP
import com.lagradost.cloudstream3.ui.player.PlayerSubtitleHelper.Companion.toSubtitleMimeType
import com.lagradost.cloudstream3.ui.player.RepoLinkGenerator
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.player.SubtitleOrigin
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesFragment.Companion.getAutoSelectLanguageTagIETF
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.buildResultEpisode
import com.lagradost.cloudstream3.ui.result.getId
import com.lagradost.cloudstream3.utils.AppContextUtils.sortSubs
import com.lagradost.cloudstream3.utils.DataStoreHelper.getDub
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.SubtitleHelper.fromTagToLanguageName
import com.lagradost.cloudstream3.utils.SubtitleHelper.languages
import com.lagradost.safefile.SafeFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private const val DebugTag = "TvPlayerVM"
private const val ReadyRefreshBatchSize = 20
private const val ReadyRefreshDebounceMs = 900L
private const val OnlineSubtitleSearchDebounceMs = 200L

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
        val selectedSubtitleIndex: Int = -1,
        val selectedAudioTrackIndex: Int = -1,
        val panels: TvPlayerPanelsUiState = TvPlayerPanelsUiState(),
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

    private data class SubtitlePanelNavigationState(
        val screen: TvPlayerSubtitlePanelScreen = TvPlayerSubtitlePanelScreen.Main,
        val direction: TvPlayerSubtitlePanelNavigationDirection = TvPlayerSubtitlePanelNavigationDirection.Forward,
        val focusInternetEntryWhenMainVisible: Boolean = false,
        val focusOnlineSearchItemId: String? = null,
    )

    private val _uiState = MutableStateFlow<TvPlayerUiState>(
        TvPlayerUiState.LoadingSources(
            metadata = TvPlayerMetadata.Empty,
            loadedSources = 0,
            canSkip = false,
        )
    )
    val uiState: StateFlow<TvPlayerUiState> = _uiState.asStateFlow()

    private val _panelEffects = MutableSharedFlow<TvPlayerPanelEffect>(
        replay = 0,
        extraBufferCapacity = 8,
    )
    val panelEffects: SharedFlow<TvPlayerPanelEffect> = _panelEffects.asSharedFlow()

    private val loadedLinksByUrl = linkedMapOf<String, ExtractorLink>()
    private val loadedSubtitlesById = linkedMapOf<String, SubtitleData>()
    private val sourceStatesByUrl = linkedMapOf<String, TvPlayerSourceState>()
    private var loadingJob: Job? = null
    private var onlineSubtitleSearchDebounceJob: Job? = null
    private var onlineSubtitleSearchJob: Job? = null
    private var onlineSubtitleLoadJob: Job? = null
    private var firstAvailableSubtitleJob: Job? = null
    private var hasFinalized = false
    private var metadata: TvPlayerMetadata = TvPlayerMetadata.Empty
    private var currentLoadResponse: LoadResponse? = null
    private var orderedLinks: List<ExtractorLink> = emptyList()
    private var orderedSubtitles: List<SubtitleData> = emptyList()
    private var currentLinkIndex: Int = -1
    private var pendingReadyRefreshChanges: Int = 0
    private var pendingReadyRefreshJob: Job? = null
    private var currentEpisode: ResultEpisode? = null
    private val playbackProgressState = TvPlayerPlaybackProgressState()
    private val panelsState = TvPlayerPanelsStateHolder()
    private var subtitlePanelNavigationState = SubtitlePanelNavigationState()
    private var onlineSubtitlesState = TvPlayerOnlineSubtitlesState(
        selectedLanguageTag = getAutoSelectLanguageTagIETF().trim().ifBlank { "en" },
        selectedLanguageLabel = "",
    )
    private val subtitleLanguageOptions = buildSubtitleLanguageOptions()

    init {
        loadSources()
    }

    fun retry() {
        loadingJob?.cancel()
        onlineSubtitleSearchDebounceJob?.cancel()
        onlineSubtitleSearchDebounceJob = null
        onlineSubtitleSearchJob?.cancel()
        onlineSubtitleSearchJob = null
        onlineSubtitleLoadJob?.cancel()
        onlineSubtitleLoadJob = null
        firstAvailableSubtitleJob?.cancel()
        firstAvailableSubtitleJob = null
        loadedLinksByUrl.clear()
        loadedSubtitlesById.clear()
        sourceStatesByUrl.clear()
        hasFinalized = false
        metadata = TvPlayerMetadata.Empty
        currentLoadResponse = null
        orderedLinks = emptyList()
        orderedSubtitles = emptyList()
        currentLinkIndex = -1
        pendingReadyRefreshChanges = 0
        pendingReadyRefreshJob?.cancel()
        pendingReadyRefreshJob = null
        currentEpisode = null
        subtitlePanelNavigationState = SubtitlePanelNavigationState()
        onlineSubtitlesState = createDefaultOnlineSubtitlesState(query = "")
        playbackProgressState.reset()
        panelsState.reset()
        loadSources()
    }

    fun skipLoading() {
        val hasLoadedSources = synchronized(loadedLinksByUrl) { loadedLinksByUrl.isNotEmpty() }
        if (!hasLoadedSources) {
            return
        }
        finalizeLoading(forceError = true)
    }

    fun onPlaybackProgress(positionMs: Long, durationMs: Long) {
        playbackProgressState.onPlaybackProgress(
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    fun onPlaybackStopped(positionMs: Long, durationMs: Long) {
        playbackProgressState.onPlaybackStopped(
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    fun selectSource(
        index: Int,
        forceReloadCurrent: Boolean = false,
    ) {
        if (!hasFinalized) return
        val selectedLink = orderedLinks.getOrNull(index) ?: return

        if (!forceReloadCurrent && index == currentLinkIndex) {
            closePanel()
            return
        }

        updateSourceState(
            url = selectedLink.url,
            state = TvPlayerSourceState(status = TvPlayerSourceStatus.Loading),
        )
        panelsState.onSourceChanged(selectedLink)
        subtitlePanelNavigationState = SubtitlePanelNavigationState()
        currentLinkIndex = index
        postReadyState(
            link = selectedLink,
            currentIndex = index,
        )
    }

    fun retrySource(index: Int) {
        if (!hasFinalized) return
        selectSource(
            index = index,
            forceReloadCurrent = true,
        )
    }

    fun openPanel(panel: TvPlayerSidePanel) {
        if (!hasFinalized) return
        if (panelsState.openPanel(panel)) {
            if (panel == TvPlayerSidePanel.Subtitles) {
                subtitlePanelNavigationState = SubtitlePanelNavigationState(
                    screen = TvPlayerSubtitlePanelScreen.Main,
                    direction = TvPlayerSubtitlePanelNavigationDirection.Forward,
                    focusInternetEntryWhenMainVisible = false,
                )
                ensureOnlineSubtitlesQueryInitialized()
                postReadyStateForCurrentLink()
            } else {
                updateReadyStateActivePanel(panel)
            }
        }
    }

    fun closePanel() {
        if (!hasFinalized) return
        val wasSubtitlesPanelOpen = (_uiState.value as? TvPlayerUiState.Ready)
            ?.panels
            ?.activePanel == TvPlayerSidePanel.Subtitles
        if (panelsState.closePanel()) {
            subtitlePanelNavigationState = SubtitlePanelNavigationState()
            if (wasSubtitlesPanelOpen) {
                postReadyStateForCurrentLink()
            } else {
                updateReadyStateActivePanel(TvPlayerSidePanel.None)
            }
            if (pendingReadyRefreshChanges > 0) {
                flushPendingReadyRefresh(force = false)
            }
        }
    }

    fun disableSubtitlesFromPlaybackError() {
        if (!hasFinalized) return
        if (panelsState.disableSubtitlesFromPlaybackError()) {
            subtitlePanelNavigationState = SubtitlePanelNavigationState()
            postReadyStateForCurrentLink()
        }
    }

    fun onPanelItemAction(action: TvPlayerPanelItemAction) {
        if (!hasFinalized) return

        when (action) {
            TvPlayerPanelItemAction.LoadSubtitleFromFile -> {
                _panelEffects.tryEmit(TvPlayerPanelEffect.OpenSubtitleFilePicker)
                return
            }
            TvPlayerPanelItemAction.OpenOnlineSubtitles -> {
                openOnlineSubtitlesPanel()
                return
            }
            TvPlayerPanelItemAction.LoadFirstAvailableSubtitle -> {
                closePanel()
                loadFirstAvailableSubtitle()
                return
            }
            TvPlayerPanelItemAction.BackFromOnlineSubtitles -> {
                navigateBackFromOnlineSubtitles()
                return
            }
            TvPlayerPanelItemAction.EditOnlineSubtitlesQuery -> {
                return
            }
            is TvPlayerPanelItemAction.UpdateOnlineSubtitlesQuery -> {
                onOnlineSubtitlesQueryUpdated(action.query)
                return
            }
            TvPlayerPanelItemAction.SelectOnlineSubtitlesLanguage -> {
                openOnlineSubtitlesLanguagePanel()
                return
            }
            is TvPlayerPanelItemAction.SelectOnlineSubtitlesLanguageOption -> {
                onOnlineSubtitlesLanguageUpdated(action.languageTag)
                navigateBackFromOnlineSubtitles()
                return
            }
            TvPlayerPanelItemAction.RetryOnlineSubtitlesSearch -> {
                scheduleOnlineSubtitlesSearch(immediate = true)
                return
            }
            is TvPlayerPanelItemAction.SelectOnlineSubtitleResult -> {
                selectOnlineSubtitleResult(action.resultId)
                return
            }
            is TvPlayerPanelItemAction.InspectSourceError -> {
                openSourceErrorDialog(action.index)
                return
            }
            else -> Unit
        }

        val outcome = panelsState.onPanelItemAction(
            action = action,
            currentLink = orderedLinks.getOrNull(currentLinkIndex),
            subtitles = orderedSubtitles,
        )
        val selectedSourceIndex = outcome.selectedSourceIndex
        if (selectedSourceIndex != null) {
            selectSource(selectedSourceIndex)
            return
        }

        when (action) {
            TvPlayerPanelItemAction.DisableSubtitles,
            is TvPlayerPanelItemAction.SelectSubtitle,
            TvPlayerPanelItemAction.SelectDefaultTrack,
            is TvPlayerPanelItemAction.SelectTrack -> {
                subtitlePanelNavigationState = SubtitlePanelNavigationState()
            }
            else -> Unit
        }

        if (outcome.stateChanged) {
            postReadyStateForCurrentLink()
        }
    }

    fun onSubtitleFileSelected(uri: Uri?) {
        if (!hasFinalized) return
        val subtitleUri = uri ?: return

        val context = CloudStreamApp.context ?: return
        val subtitleName = runCatching {
            SafeFile.fromUri(context, subtitleUri)?.name()
        }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: subtitleUri.lastPathSegment
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
            ?: subtitleUri.toString()

        val subtitle = SubtitleData(
            originalName = subtitleName,
            nameSuffix = "",
            url = subtitleUri.toString(),
            origin = SubtitleOrigin.DOWNLOADED_FILE,
            mimeType = subtitleName.toSubtitleMimeType(),
            headers = emptyMap(),
            languageCode = null,
        )

        synchronized(loadedSubtitlesById) {
            loadedSubtitlesById.putIfAbsent(subtitle.getId(), subtitle)
        }
        orderedSubtitles = synchronized(loadedSubtitlesById) {
            sortSubs(loadedSubtitlesById.values.toSet())
        }

        panelsState.selectSubtitleById(subtitle.getId())
        subtitlePanelNavigationState = SubtitlePanelNavigationState()
        postReadyStateForCurrentLink()
    }

    fun onOnlineSubtitlesQueryUpdated(query: String) {
        if (!hasFinalized) return
        val normalizedQuery = query.trim()
        if (normalizedQuery == onlineSubtitlesState.query) return

        onlineSubtitlesState = onlineSubtitlesState.copy(
            query = normalizedQuery,
            status = when {
                normalizedQuery.isBlank() -> TvPlayerOnlineSubtitlesStatus.Idle
                else -> TvPlayerOnlineSubtitlesStatus.Loading
            },
            errorMessage = null,
            results = if (normalizedQuery.isBlank()) emptyList() else onlineSubtitlesState.results,
        )
        if (subtitlePanelNavigationState.screen == TvPlayerSubtitlePanelScreen.OnlineSearch) {
            scheduleOnlineSubtitlesSearch(immediate = false)
        }
        refreshReadyStateIfSubtitlesPanelVisible()
    }

    fun onOnlineSubtitlesLanguageUpdated(languageTag: String) {
        if (!hasFinalized) return
        val normalizedTag = languageTag.trim().ifBlank { getAutoSelectLanguageTagIETF() }
        if (normalizedTag.equals(onlineSubtitlesState.selectedLanguageTag, ignoreCase = true)) {
            return
        }

        onlineSubtitlesState = onlineSubtitlesState.copy(
            selectedLanguageTag = normalizedTag,
            selectedLanguageLabel = subtitleLanguageLabel(normalizedTag),
            status = when {
                onlineSubtitlesState.query.isBlank() -> TvPlayerOnlineSubtitlesStatus.Idle
                else -> TvPlayerOnlineSubtitlesStatus.Loading
            },
            errorMessage = null,
            results = if (onlineSubtitlesState.query.isBlank()) emptyList() else onlineSubtitlesState.results,
        )
        if (subtitlePanelNavigationState.screen == TvPlayerSubtitlePanelScreen.OnlineSearch ||
            subtitlePanelNavigationState.screen == TvPlayerSubtitlePanelScreen.OnlineLanguageSelection
        ) {
            scheduleOnlineSubtitlesSearch(immediate = true)
        }
        refreshReadyStateIfSubtitlesPanelVisible()
    }

    fun onSubtitlesSidePanelBackPressed(): Boolean {
        if (!hasFinalized) return false
        if ((_uiState.value as? TvPlayerUiState.Ready)?.panels?.activePanel != TvPlayerSidePanel.Subtitles) {
            return false
        }
        return navigateBackFromOnlineSubtitles()
    }

    fun onPlaybackReady() {
        if (!hasFinalized) return
        val currentLink = orderedLinks.getOrNull(currentLinkIndex) ?: return
        val stateUpdated = updateSourceState(
            url = currentLink.url,
            state = TvPlayerSourceState(status = TvPlayerSourceStatus.Success),
        )
        if (stateUpdated) {
            postReadyStateForCurrentLink()
        }
    }

    fun onPlaybackError(error: TvPlayerPlaybackErrorDetails?) {
        if (!hasFinalized) return

        val currentLink = orderedLinks.getOrNull(currentLinkIndex)
        if (currentLink != null) {
            updateSourceState(
                url = currentLink.url,
                state = TvPlayerSourceState(
                    status = TvPlayerSourceStatus.Error,
                    httpCode = error?.httpCode,
                ),
            )
        }

        val nextIndex = currentLinkIndex + 1
        val nextLink = orderedLinks.getOrNull(nextIndex)
        if (nextLink != null) {
            updateSourceState(
                url = nextLink.url,
                state = TvPlayerSourceState(status = TvPlayerSourceStatus.Loading),
            )
            panelsState.onSourceChanged(nextLink)
            subtitlePanelNavigationState = SubtitlePanelNavigationState()
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
            val target = withContext(Dispatchers.IO) {
                resolvePlaybackTarget(
                    repository = repository,
                    url = url,
                    apiName = apiName,
                    directEpisodeData = episodeData,
                )
            }

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
            currentLoadResponse = target.page
            onlineSubtitlesState = createDefaultOnlineSubtitlesState(
                query = defaultOnlineSubtitlesQuery(),
            )
            playbackProgressState.onEpisodeChanged(target.episode)
            postLoadingState()

            val generator = RepoLinkGenerator(
                episodes = listOf(target.episode),
                page = target.page,
            )

            try {
                withContext(Dispatchers.IO) {
                    val loadingScope = this
                    generator.generateLinks(
                        clearCache = false,
                        sourceTypes = LOADTYPE_INAPP,
                        callback = { (link, _) ->
                            if (link == null || link.url.isBlank()) return@generateLinks
                            val inserted = synchronized(loadedLinksByUrl) {
                                loadedLinksByUrl.putIfAbsent(link.url, link) == null
                            }
                            synchronized(sourceStatesByUrl) {
                                sourceStatesByUrl.putIfAbsent(
                                    link.url,
                                    TvPlayerSourceState(status = TvPlayerSourceStatus.Loading),
                                )
                            }
                            if (inserted) {
                                loadingScope.launch(Dispatchers.Main.immediate) {
                                    if (hasFinalized) {
                                        onBackgroundDataInsertedAfterFinalize()
                                    } else {
                                        postLoadingState()
                                    }
                                }
                            }
                        },
                        subtitleCallback = { subtitle ->
                            val inserted = synchronized(loadedSubtitlesById) {
                                loadedSubtitlesById.putIfAbsent(subtitle.getId(), subtitle) == null
                            }
                            if (inserted) {
                                loadingScope.launch(Dispatchers.Main.immediate) {
                                    if (hasFinalized) {
                                        onBackgroundDataInsertedAfterFinalize()
                                    }
                                }
                            }
                        }
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
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
        var resolvedAnimeDubStatus: DubStatus? = null

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
                    val mainId = loadResponse.getId()
                    val preferredDubStatus = resolvePreferredAnimeDubStatus(loadResponse, mainId)
                    val preferredDubEpisodes = preferredDubStatus
                        ?.let { dubStatus -> loadResponse.episodes[dubStatus].orEmpty() }
                        .orEmpty()

                    val first = if (preferredDubEpisodes.isNotEmpty()) {
                        resolvedAnimeDubStatus = preferredDubStatus
                        preferredDubEpisodes
                    } else {
                        val fallbackEntry = loadResponse.episodes.entries
                            .firstOrNull { entry -> entry.value.isNotEmpty() }
                        resolvedAnimeDubStatus = fallbackEntry?.key
                        fallbackEntry?.value.orEmpty()
                    }
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
                    val matchingEntry = loadResponse.episodes.entries
                        .firstOrNull { entry ->
                            entry.value.any { episode ->
                                episode.data == resolvedData
                            }
                        }
                    val matchingEpisode = matchingEntry
                        ?.value
                        ?.firstOrNull { episode ->
                            episode.data == resolvedData
                        }
                    resolvedAnimeDubStatus = matchingEntry?.key
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
        val parentId = loadResponse?.getId() ?: url.hashCode()
        val episodeId = when (val response = loadResponse) {
            is MovieLoadResponse -> parentId

            is TvSeriesLoadResponse -> {
                val matchingEpisode = response.episodes.firstOrNull { episode ->
                    episode.data == resolvedData
                }
                val episodeNumber = matchingEpisode?.episode ?: resolvedEpisode.takeIf { it > 0 } ?: 1
                val seasonNumber = matchingEpisode?.season ?: resolvedSeason
                parentId + (seasonNumber?.times(100_000) ?: 0) + episodeNumber + 1
            }

            is AnimeLoadResponse -> {
                val matchingEntry = response.episodes.entries.firstOrNull { entry ->
                    entry.value.any { episode ->
                        episode.data == resolvedData
                    }
                }
                val matchingEpisode = matchingEntry
                    ?.value
                    ?.firstOrNull { episode ->
                        episode.data == resolvedData
                    }
                val dubStatus = matchingEntry?.key
                    ?: resolvedAnimeDubStatus
                    ?: resolvePreferredAnimeDubStatus(response, parentId)
                val episodeNumber = matchingEpisode?.episode ?: resolvedEpisode.takeIf { it > 0 } ?: 1
                val seasonNumber = matchingEpisode?.season ?: resolvedSeason
                parentId + episodeNumber + ((dubStatus?.id ?: 0) * 1_000_000) + (seasonNumber?.times(10_000) ?: 0)
            }

            else -> "$apiName|$resolvedData".hashCode()
        }

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

    private fun resolvePreferredAnimeDubStatus(
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

        if (isSelectionPanelOpen()) {
            return
        }

        if (pendingReadyRefreshChanges >= ReadyRefreshBatchSize) {
            flushPendingReadyRefresh(force = false)
            return
        }

        schedulePendingReadyRefresh()
    }

    private fun flushPendingReadyRefresh(force: Boolean) {
        if (!hasFinalized) return
        if (!force && pendingReadyRefreshChanges <= 0) return

        pendingReadyRefreshJob?.cancel()
        pendingReadyRefreshJob = null
        pendingReadyRefreshChanges = 0
        refreshReadyStateFromLoadedData()
    }

    private fun schedulePendingReadyRefresh() {
        if (pendingReadyRefreshJob?.isActive == true) {
            return
        }

        pendingReadyRefreshJob = viewModelScope.launch {
            delay(ReadyRefreshDebounceMs)
            pendingReadyRefreshJob = null
            if (!hasFinalized || isSelectionPanelOpen()) {
                return@launch
            }
            flushPendingReadyRefresh(force = false)
        }
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

        ensureSourceStateEntries(candidateLinks)
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
            ensureSourceStateEntries(candidateLinks)
            orderedLinks = candidateLinks
            orderedSubtitles = synchronized(loadedSubtitlesById) {
                sortSubs(loadedSubtitlesById.values.toSet())
            }
            updateSourceState(
                url = candidateLinks.first().url,
                state = TvPlayerSourceState(status = TvPlayerSourceStatus.Loading),
            )
            panelsState.onSourceChanged(candidateLinks.first())
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
        val sourceStatesSnapshot = synchronized(sourceStatesByUrl) {
            sourceStatesByUrl.toMap()
        }
        val isCurrentSourceReady = sourceStatesSnapshot[link.url]?.status == TvPlayerSourceStatus.Success
        if (isCurrentSourceReady) {
            panelsState.applyPreferredSubtitleAutoSelection(orderedSubtitles)
        }
        val panelSelection = panelsState.selection(
            currentLink = link,
            subtitles = orderedSubtitles,
        )
        // WHY: player powinien wystartować najpierw na samym źródle.
        // Dopiero po sukcesie odtwarzania udostępniamy/subskrybujemy napisy w UI i playerze.
        val subtitlesForUi = if (isCurrentSourceReady) orderedSubtitles else emptyList()
        val selectedSubtitleIndexForUi = if (isCurrentSourceReady) {
            panelSelection.selectedSubtitleIndex
        } else {
            -1
        }
        val (subtitleOnlineItems, baseSubtitleOnlineInitialFocusedItemId) = when (
            subtitlePanelNavigationState.screen
        ) {
            TvPlayerSubtitlePanelScreen.OnlineLanguageSelection -> buildOnlineSubtitlesLanguagePanelItems()
            TvPlayerSubtitlePanelScreen.OnlineSearch,
            TvPlayerSubtitlePanelScreen.Main -> buildOnlineSubtitlesPanelItems()
        }
        val subtitleOnlineInitialFocusedItemId = subtitlePanelNavigationState.focusOnlineSearchItemId
            ?.takeIf { requestedId ->
                subtitlePanelNavigationState.screen == TvPlayerSubtitlePanelScreen.OnlineSearch &&
                    subtitleOnlineItems.any { item -> item.id == requestedId && item.enabled }
            } ?: baseSubtitleOnlineInitialFocusedItemId
        val hasOnlineSubtitleProviders = subtitleProviders.isNotEmpty()
        val canLoadFirstAvailableSubtitle = hasOnlineSubtitleProviders && defaultOnlineSubtitlesQuery().isNotBlank()
        val basePanelsState = panelsState.buildPanelsUiState(
            orderedLinks = orderedLinks,
            currentSourceIndex = currentIndex,
            sourceStates = sourceStatesSnapshot,
            currentLink = link,
            subtitles = subtitlesForUi,
            showOnlineSubtitleActions = hasOnlineSubtitleProviders,
            showFirstAvailableSubtitleAction = canLoadFirstAvailableSubtitle,
        )
        val subtitleInitialFocusedItemId = if (
            subtitlePanelNavigationState.screen == TvPlayerSubtitlePanelScreen.Main &&
            subtitlePanelNavigationState.focusInternetEntryWhenMainVisible
        ) {
            SubtitleLoadFromInternetItemId
        } else {
            basePanelsState.subtitleInitialFocusedItemId
        }
        val panelsUiState = basePanelsState.copy(
            subtitlePanelScreen = subtitlePanelNavigationState.screen,
            subtitlePanelNavigationDirection = subtitlePanelNavigationState.direction,
            subtitleOnlineItems = subtitleOnlineItems,
            subtitleInitialFocusedItemId = subtitleInitialFocusedItemId,
            subtitleOnlineInitialFocusedItemId = subtitleOnlineInitialFocusedItemId,
        )
        if (subtitlePanelNavigationState.focusInternetEntryWhenMainVisible) {
            subtitlePanelNavigationState = subtitlePanelNavigationState.copy(
                focusInternetEntryWhenMainVisible = false,
            )
        }
        if (subtitlePanelNavigationState.focusOnlineSearchItemId != null) {
            subtitlePanelNavigationState = subtitlePanelNavigationState.copy(
                focusOnlineSearchItemId = null,
            )
        }
        val episodeId = currentEpisode?.id ?: -1
        _uiState.value = TvPlayerUiState.Ready(
            metadata = metadata,
            link = link,
            sourceCount = orderedLinks.size,
            sources = orderedLinks,
            currentSourceIndex = currentIndex,
            subtitles = subtitlesForUi,
            selectedSubtitleIndex = selectedSubtitleIndexForUi,
            selectedAudioTrackIndex = panelSelection.selectedAudioTrackIndex,
            panels = panelsUiState,
            episodeId = episodeId,
            resumePositionMs = playbackProgressState.resumePositionMs,
        )
    }

    private fun postReadyStateForCurrentLink() {
        if (!hasFinalized) return
        val link = orderedLinks.getOrNull(currentLinkIndex) ?: return
        postReadyState(
            link = link,
            currentIndex = currentLinkIndex,
        )
    }

    private fun openOnlineSubtitlesPanel() {
        ensureOnlineSubtitlesQueryInitialized()
        subtitlePanelNavigationState = SubtitlePanelNavigationState(
            screen = TvPlayerSubtitlePanelScreen.OnlineSearch,
            direction = TvPlayerSubtitlePanelNavigationDirection.Forward,
            focusInternetEntryWhenMainVisible = false,
            focusOnlineSearchItemId = null,
        )
        if (onlineSubtitlesState.query.isBlank()) {
            onlineSubtitlesState = onlineSubtitlesState.copy(
                status = TvPlayerOnlineSubtitlesStatus.Idle,
                results = emptyList(),
                errorMessage = null,
            )
        } else {
            scheduleOnlineSubtitlesSearch(immediate = true)
        }
        postReadyStateForCurrentLink()
    }

    private fun openOnlineSubtitlesLanguagePanel() {
        if (subtitlePanelNavigationState.screen != TvPlayerSubtitlePanelScreen.OnlineSearch) {
            return
        }
        subtitlePanelNavigationState = SubtitlePanelNavigationState(
            screen = TvPlayerSubtitlePanelScreen.OnlineLanguageSelection,
            direction = TvPlayerSubtitlePanelNavigationDirection.Forward,
            focusInternetEntryWhenMainVisible = false,
            focusOnlineSearchItemId = null,
        )
        postReadyStateForCurrentLink()
    }

    private fun navigateBackFromOnlineSubtitles(): Boolean {
        when (subtitlePanelNavigationState.screen) {
            TvPlayerSubtitlePanelScreen.Main -> return false
            TvPlayerSubtitlePanelScreen.OnlineLanguageSelection -> {
                subtitlePanelNavigationState = SubtitlePanelNavigationState(
                    screen = TvPlayerSubtitlePanelScreen.OnlineSearch,
                    direction = TvPlayerSubtitlePanelNavigationDirection.Backward,
                    focusInternetEntryWhenMainVisible = false,
                    focusOnlineSearchItemId = SubtitleOnlineLanguageItemId,
                )
                postReadyStateForCurrentLink()
                return true
            }
            TvPlayerSubtitlePanelScreen.OnlineSearch -> {
                subtitlePanelNavigationState = SubtitlePanelNavigationState(
                    screen = TvPlayerSubtitlePanelScreen.Main,
                    direction = TvPlayerSubtitlePanelNavigationDirection.Backward,
                    focusInternetEntryWhenMainVisible = true,
                    focusOnlineSearchItemId = null,
                )
                postReadyStateForCurrentLink()
                return true
            }
        }
    }

    private fun scheduleOnlineSubtitlesSearch(immediate: Boolean) {
        onlineSubtitleSearchDebounceJob?.cancel()
        onlineSubtitleSearchDebounceJob = null

        val query = onlineSubtitlesState.query
        if (query.isBlank()) {
            onlineSubtitleSearchJob?.cancel()
            onlineSubtitleSearchJob = null
            onlineSubtitlesState = onlineSubtitlesState.copy(
                status = TvPlayerOnlineSubtitlesStatus.Idle,
                results = emptyList(),
                errorMessage = null,
            )
            refreshReadyStateIfSubtitlesPanelVisible()
            return
        }

        val launchSearch: () -> Unit = {
            onlineSubtitleSearchJob?.cancel()
            onlineSubtitleSearchJob = viewModelScope.launch {
                performOnlineSubtitlesSearch()
            }
        }

        if (immediate) {
            launchSearch()
        } else {
            onlineSubtitleSearchDebounceJob = viewModelScope.launch {
                delay(OnlineSubtitleSearchDebounceMs)
                onlineSubtitleSearchDebounceJob = null
                launchSearch()
            }
        }
    }

    private fun createSubtitleSearchRequest(
        query: String,
        languageTag: String?,
    ): SubtitleSearch {
        return SubtitleSearch(
            query = query,
            lang = languageTag?.ifBlank { null },
            imdbId = currentLoadResponse?.getImdbId(),
            tmdbId = currentLoadResponse?.getTMDbId()?.toIntOrNull(),
            malId = currentLoadResponse?.getMalId()?.toIntOrNull(),
            aniListId = currentLoadResponse?.getAniListId()?.toIntOrNull(),
            epNumber = currentEpisode?.episode,
            seasonNumber = currentEpisode?.season,
            year = metadata.year,
        )
    }

    private suspend fun performOnlineSubtitlesSearch() {
        val querySnapshot = onlineSubtitlesState.query
        val languageTagSnapshot = onlineSubtitlesState.selectedLanguageTag
        if (querySnapshot.isBlank()) {
            return
        }

        // Legacy behavior parity:
        // - keep plain title in query
        // - pass SxxExx through epNumber/seasonNumber request fields
        val request = createSubtitleSearchRequest(
            query = querySnapshot,
            languageTag = languageTagSnapshot,
        )

        onlineSubtitlesState = onlineSubtitlesState.copy(
            status = TvPlayerOnlineSubtitlesStatus.Loading,
            errorMessage = null,
        )
        refreshReadyStateIfSubtitlesPanelVisible()

        val providers = subtitleProviders.toList()
        if (providers.isEmpty()) {
            onlineSubtitlesState = onlineSubtitlesState.copy(
                status = TvPlayerOnlineSubtitlesStatus.Error,
                results = emptyList(),
                errorMessage = stringFromAppContext(
                    resId = R.string.tv_search_failed_to_load,
                    fallback = "Couldn't load results from this source",
                ),
            )
            refreshReadyStateIfSubtitlesPanelVisible()
            return
        }

        var failedProviders = 0
        val providerResults = providers.amap { provider ->
            provider.idPrefix to when (val response = Resource.fromResult(provider.search(request))) {
                is Resource.Success -> response.value
                is Resource.Loading -> emptyList()
                is Resource.Failure -> {
                    failedProviders += 1
                    emptyList()
                }
            }
        }

        val queryChanged = querySnapshot != onlineSubtitlesState.query ||
            !languageTagSnapshot.equals(onlineSubtitlesState.selectedLanguageTag, ignoreCase = true)
        if (queryChanged) {
            return
        }

        val maxSize = providerResults.maxOfOrNull { (_, result) ->
            result.size
        } ?: 0
        val merged = ArrayList<Pair<String, AbstractSubtitleEntities.SubtitleEntity>>()
        for (resultIndex in 0 until maxSize) {
            for (providerIndex in providerResults.indices) {
                val providerIdPrefix = providerResults[providerIndex].first
                providerResults[providerIndex].second.getOrNull(resultIndex)?.let { subtitle ->
                    merged += providerIdPrefix to subtitle
                }
            }
        }

        val mappedResults = merged.mapIndexed { index, (providerIdPrefix, subtitle) ->
            val idSuffix = "${subtitle.idPrefix}_${subtitle.data}_${subtitle.name}_${subtitle.lang}_${subtitle.source}_$index"
            val resolvedProviderIdPrefix = subtitle.idPrefix
                .takeIf { prefix ->
                    prefix.isNotBlank()
                } ?: providerIdPrefix
            TvPlayerOnlineSubtitleResult(
                id = "subtitle_online_result_${idSuffix.hashCode()}",
                providerIdPrefix = resolvedProviderIdPrefix,
                subtitle = subtitle,
                title = subtitle.name.ifBlank {
                    stringFromAppContext(
                        resId = R.string.no_subtitles_loaded,
                        fallback = "Unnamed subtitle",
                    )
                },
                supportingTexts = formatOnlineSubtitleSupportingTexts(subtitle),
            )
        }

        onlineSubtitlesState = when {
            mappedResults.isNotEmpty() -> {
                onlineSubtitlesState.copy(
                    status = TvPlayerOnlineSubtitlesStatus.Results,
                    results = mappedResults,
                    errorMessage = null,
                )
            }

            failedProviders > 0 -> {
                onlineSubtitlesState.copy(
                    status = TvPlayerOnlineSubtitlesStatus.Error,
                    results = emptyList(),
                    errorMessage = stringFromAppContext(
                        resId = R.string.tv_search_failed_to_load,
                        fallback = "Couldn't load results from this source",
                    ),
                )
            }

            else -> {
                onlineSubtitlesState.copy(
                    status = TvPlayerOnlineSubtitlesStatus.Empty,
                    results = emptyList(),
                    errorMessage = null,
                )
            }
        }
        refreshReadyStateIfSubtitlesPanelVisible()
    }

    private fun selectOnlineSubtitleResult(resultId: String) {
        val selectedResult = onlineSubtitlesState.results.firstOrNull { result ->
            result.id == resultId
        } ?: return
        val providerIdPrefix = selectedResult.subtitle.idPrefix
            .takeIf { idPrefix ->
                idPrefix.isNotBlank()
            } ?: selectedResult.providerIdPrefix
        val provider = subtitleProviders.firstOrNull { candidate ->
            candidate.idPrefix == providerIdPrefix
        } ?: run {
            onlineSubtitlesState = onlineSubtitlesState.copy(
                status = TvPlayerOnlineSubtitlesStatus.Results,
                errorMessage = stringFromAppContext(
                    resId = R.string.tv_search_failed_to_load,
                    fallback = "Couldn't load results from this source",
                ),
            )
            refreshReadyStateIfSubtitlesPanelVisible()
            return
        }

        onlineSubtitleLoadJob?.cancel()
        onlineSubtitleLoadJob = viewModelScope.launch {
            when (val resource = Resource.fromResult(provider.resource(selectedResult.subtitle))) {
                is Resource.Success -> {
                    val downloadedSubtitles = resource.value.getSubtitles().map { downloaded ->
                        SubtitleData(
                            originalName = downloaded.name ?: onlineSubtitleDisplayName(
                                entry = selectedResult.subtitle,
                                withLanguage = true,
                            ),
                            nameSuffix = "",
                            url = downloaded.url,
                            origin = downloaded.origin,
                            mimeType = downloaded.url.toSubtitleMimeType(),
                            headers = selectedResult.subtitle.headers,
                            languageCode = selectedResult.subtitle.lang,
                        )
                    }
                    if (downloadedSubtitles.isEmpty()) {
                        onlineSubtitlesState = onlineSubtitlesState.copy(
                            status = TvPlayerOnlineSubtitlesStatus.Results,
                            errorMessage = stringFromAppContext(
                                resId = R.string.no_subtitles_loaded,
                                fallback = "No subtitles loaded yet",
                            ),
                        )
                        refreshReadyStateIfSubtitlesPanelVisible()
                        return@launch
                    }

                    synchronized(loadedSubtitlesById) {
                        downloadedSubtitles.forEach { subtitle ->
                            loadedSubtitlesById[subtitle.getId()] = subtitle
                        }
                    }
                    orderedSubtitles = synchronized(loadedSubtitlesById) {
                        sortSubs(loadedSubtitlesById.values.toSet())
                    }

                    val selectedSubtitleId = downloadedSubtitles.firstNotNullOfOrNull { subtitle ->
                        orderedSubtitles.firstOrNull { existing ->
                            existing.getId() == subtitle.getId()
                        }?.getId()
                    } ?: downloadedSubtitles.first().getId()
                    panelsState.selectSubtitleById(selectedSubtitleId)
                    subtitlePanelNavigationState = SubtitlePanelNavigationState()
                    postReadyStateForCurrentLink()
                }

                is Resource.Failure -> {
                    onlineSubtitlesState = onlineSubtitlesState.copy(
                        status = TvPlayerOnlineSubtitlesStatus.Results,
                        errorMessage = resource.errorString,
                    )
                    refreshReadyStateIfSubtitlesPanelVisible()
                }

                is Resource.Loading -> Unit
            }
        }
    }

    private fun loadFirstAvailableSubtitle() {
        val query = defaultOnlineSubtitlesQuery().trim()
        if (query.isBlank()) {
            return
        }

        val providers = subtitleProviders.toList()
        if (providers.isEmpty()) {
            return
        }

        val request = createSubtitleSearchRequest(
            query = query,
            languageTag = getAutoSelectLanguageTagIETF().trim(),
        )

        firstAvailableSubtitleJob?.cancel()
        onlineSubtitleLoadJob?.cancel()
        onlineSubtitleLoadJob = null
        firstAvailableSubtitleJob = viewModelScope.launch {
            val providerResults = providers.amap { provider ->
                provider to when (val result = Resource.fromResult(provider.search(request))) {
                    is Resource.Success -> result.value
                    is Resource.Loading -> emptyList()
                    is Resource.Failure -> emptyList()
                }
            }

            val maxSize = providerResults.maxOfOrNull { (_, subtitles) ->
                subtitles.size
            } ?: 0

            for (resultIndex in 0 until maxSize) {
                for ((provider, subtitles) in providerResults) {
                    val subtitleEntry = subtitles.getOrNull(resultIndex) ?: continue
                    val subtitleResource = Resource.fromResult(provider.resource(subtitleEntry))
                    if (subtitleResource !is Resource.Success) continue

                    val downloadedSubtitles = subtitleResource.value.getSubtitles().map { downloaded ->
                        SubtitleData(
                            originalName = downloaded.name ?: onlineSubtitleDisplayName(
                                entry = subtitleEntry,
                                withLanguage = true,
                            ),
                            nameSuffix = "",
                            url = downloaded.url,
                            origin = downloaded.origin,
                            mimeType = downloaded.url.toSubtitleMimeType(),
                            headers = subtitleEntry.headers,
                            languageCode = subtitleEntry.lang,
                        )
                    }
                    if (downloadedSubtitles.isEmpty()) continue

                    var insertedAny = false
                    synchronized(loadedSubtitlesById) {
                        downloadedSubtitles.forEach { subtitle ->
                            if (loadedSubtitlesById.putIfAbsent(subtitle.getId(), subtitle) == null) {
                                insertedAny = true
                            }
                        }
                    }
                    if (!insertedAny) continue

                    orderedSubtitles = synchronized(loadedSubtitlesById) {
                        sortSubs(loadedSubtitlesById.values.toSet())
                    }
                    panelsState.selectSubtitleById(downloadedSubtitles.first().getId())
                    subtitlePanelNavigationState = SubtitlePanelNavigationState()
                    postReadyStateForCurrentLink()
                    return@launch
                }
            }
        }
    }

    private fun buildOnlineSubtitlesPanelItems(): Pair<List<SidePanelMenuItem>, String?> {
        val queryPlaceholder = stringFromAppContext(
            resId = R.string.search_hint,
            fallback = "Search…",
        )

        val items = buildList {
            add(
                SidePanelMenuItem(
                    id = SubtitleOnlineQueryItemId,
                    title = stringFromAppContext(
                        resId = R.string.search,
                        fallback = "Search",
                    ),
                    inlineTextField = SidePanelInlineTextField(
                        value = onlineSubtitlesState.query,
                        placeholder = queryPlaceholder,
                        valueChangeToken = TvPlayerPanelItemAction.EditOnlineSubtitlesQuery,
                    ),
                )
            )
            add(
                SidePanelMenuItem(
                    id = SubtitleOnlineLanguageItemId,
                    title = stringFromAppContext(
                        resId = R.string.subs_subtitle_languages,
                        fallback = "Subtitle language",
                    ),
                    supportingTexts = listOf(onlineSubtitlesState.selectedLanguageLabel),
                    actionToken = TvPlayerPanelItemAction.SelectOnlineSubtitlesLanguage,
                )
            )

            when (onlineSubtitlesState.status) {
                TvPlayerOnlineSubtitlesStatus.Idle -> Unit
                TvPlayerOnlineSubtitlesStatus.Loading -> {
                    add(
                        SidePanelMenuItem(
                            id = SubtitleOnlineLoadingItemId,
                            title = stringFromAppContext(
                                resId = R.string.loading,
                                fallback = "Loading…",
                            ),
                            enabled = true,
                        )
                    )
                }
                TvPlayerOnlineSubtitlesStatus.Empty -> {
                    add(
                        SidePanelMenuItem(
                            id = SubtitleOnlineEmptyItemId,
                            title = stringFromAppContext(
                                resId = R.string.tv_feed_empty,
                                fallback = "No items in this list",
                            ),
                            enabled = false,
                        )
                    )
                }
                TvPlayerOnlineSubtitlesStatus.Error -> {
                    add(
                        SidePanelMenuItem(
                            id = SubtitleOnlineErrorItemId,
                            title = onlineSubtitlesState.errorMessage
                                ?.takeIf { message ->
                                    message.isNotBlank()
                                } ?: stringFromAppContext(
                                resId = R.string.tv_search_failed_to_load,
                                fallback = "Couldn't load results from this source",
                            ),
                            enabled = false,
                        )
                    )
                    add(
                        SidePanelMenuItem(
                            id = SubtitleOnlineRetryItemId,
                            title = stringFromAppContext(
                                resId = R.string.tv_player_retry,
                                fallback = "Try again",
                            ),
                            actionToken = TvPlayerPanelItemAction.RetryOnlineSubtitlesSearch,
                        )
                    )
                }
                TvPlayerOnlineSubtitlesStatus.Results -> {
                    onlineSubtitlesState.results.forEach { result ->
                        add(
                            SidePanelMenuItem(
                                id = result.id,
                                title = result.title,
                                titleMaxLines = 1,
                                supportingTexts = result.supportingTexts,
                                actionToken = TvPlayerPanelItemAction.SelectOnlineSubtitleResult(
                                    resultId = result.id,
                                ),
                            )
                        )
                    }
                }
            }
        }

        val initialFocus = when {
            onlineSubtitlesState.query.isBlank() -> SubtitleOnlineQueryItemId
            onlineSubtitlesState.status == TvPlayerOnlineSubtitlesStatus.Results &&
                onlineSubtitlesState.results.isNotEmpty() -> onlineSubtitlesState.results.first().id
            onlineSubtitlesState.status == TvPlayerOnlineSubtitlesStatus.Loading -> SubtitleOnlineLoadingItemId
            onlineSubtitlesState.status == TvPlayerOnlineSubtitlesStatus.Error -> SubtitleOnlineRetryItemId
            else -> SubtitleOnlineQueryItemId
        }
        return items to initialFocus
    }

    private fun buildOnlineSubtitlesLanguagePanelItems(): Pair<List<SidePanelMenuItem>, String?> {
        val languageItems = subtitleLanguageOptions.map { option ->
            val selected = option.tag.equals(onlineSubtitlesState.selectedLanguageTag, ignoreCase = true)
            SidePanelMenuItem(
                id = "$SubtitleOnlineLanguageOptionItemPrefix${option.tag}",
                title = option.label,
                selected = selected,
                showTrailingRadio = true,
                actionToken = TvPlayerPanelItemAction.SelectOnlineSubtitlesLanguageOption(
                    languageTag = option.tag,
                ),
            )
        }
        val initialFocus = languageItems.firstOrNull { item ->
            item.selected
        }?.id ?: languageItems.firstOrNull()?.id
        return languageItems to initialFocus
    }

    private fun ensureOnlineSubtitlesQueryInitialized() {
        if (onlineSubtitlesState.query.isNotBlank()) return
        onlineSubtitlesState = onlineSubtitlesState.copy(
            query = defaultOnlineSubtitlesQuery(),
        )
    }

    private fun defaultOnlineSubtitlesQuery(): String {
        return metadata.title.takeIf { title ->
            title.isNotBlank()
        } ?: currentEpisode?.headerName?.takeIf { headerName ->
            headerName.isNotBlank()
        } ?: ""
    }

    private fun createDefaultOnlineSubtitlesState(
        query: String,
    ): TvPlayerOnlineSubtitlesState {
        val languageTag = getAutoSelectLanguageTagIETF().trim().ifBlank { "en" }
        return TvPlayerOnlineSubtitlesState(
            query = query,
            selectedLanguageTag = languageTag,
            selectedLanguageLabel = subtitleLanguageLabel(languageTag),
            status = if (query.isBlank()) {
                TvPlayerOnlineSubtitlesStatus.Idle
            } else {
                TvPlayerOnlineSubtitlesStatus.Loading
            },
        )
    }

    private fun buildSubtitleLanguageOptions(): List<TvPlayerSubtitleLanguageOption> {
        return languages
            .map { language ->
                TvPlayerSubtitleLanguageOption(
                    tag = language.IETF_tag,
                    label = language.nameNextToFlagEmoji(),
                )
            }
            .sortedBy { option ->
                option.label.substringAfter('\u00a0').lowercase(Locale.ROOT)
            }
    }

    private fun subtitleLanguageLabel(languageTag: String): String {
        val fromOptions = subtitleLanguageOptions.firstOrNull { option ->
            option.tag.equals(languageTag, ignoreCase = true)
        }?.label
        if (fromOptions != null) {
            return fromOptions
        }

        val fallbackLanguageName = fromTagToLanguageName(languageTag)
            ?.replaceFirstChar { firstChar ->
                if (firstChar.isLowerCase()) firstChar.titlecase(Locale.getDefault()) else firstChar.toString()
            }
        return fallbackLanguageName ?: languageTag
    }

    private fun onlineSubtitleDisplayName(
        entry: AbstractSubtitleEntities.SubtitleEntity,
        withLanguage: Boolean,
    ): String {
        if (!withLanguage || entry.lang.isBlank()) {
            return entry.name
        }
        val localizedLanguage = fromTagToLanguageName(entry.lang.trim()) ?: entry.lang
        return "$localizedLanguage ${entry.name}"
    }

    private fun formatOnlineSubtitleSupportingTexts(
        subtitle: AbstractSubtitleEntities.SubtitleEntity,
    ): List<String> {
        val languageName = fromTagToLanguageName(subtitle.lang)?.takeIf { localized ->
            localized.isNotBlank()
        } ?: subtitle.lang.takeIf { language ->
            language.isNotBlank()
        }
        val formatHint = subtitleTypeHint(subtitle)
        return buildList {
            subtitle.source.takeIf { source -> source.isNotBlank() }?.let(::add)
            languageName?.let(::add)
            formatHint?.let(::add)
        }.distinct()
    }

    private fun subtitleTypeHint(subtitle: AbstractSubtitleEntities.SubtitleEntity): String? {
        fun extractType(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val sanitized = raw.substringBefore('?').substringBefore('#')
            val extension = sanitized.substringAfterLast('.', "")
            if (extension.isBlank()) return null
            return extension.lowercase(Locale.ROOT)
                .takeIf { value ->
                    value in setOf("srt", "vtt", "ass", "ssa", "sub", "ttml", "smi")
                }?.uppercase(Locale.ROOT)
        }

        return extractType(subtitle.name) ?: extractType(subtitle.data)
    }

    private fun openSourceErrorDialog(index: Int) {
        val link = orderedLinks.getOrNull(index) ?: return
        val sourceState = synchronized(sourceStatesByUrl) {
            sourceStatesByUrl[link.url]
        }
        if (sourceState?.status != TvPlayerSourceStatus.Error) {
            selectSource(index = index)
            return
        }

        _panelEffects.tryEmit(
            TvPlayerPanelEffect.OpenSourceErrorDialog(
                dialog = TvPlayerSourceErrorDialog(
                    sourceIndex = index,
                    sourceLabel = sourceDisplayLabel(link = link, index = index),
                    message = buildSourceErrorMessage(httpCode = sourceState.httpCode),
                ),
            )
        )
    }

    private fun buildSourceErrorMessage(httpCode: Int?): String {
        return if (httpCode != null) {
            val template = stringFromAppContext(
                resId = R.string.tv_player_source_error_dialog_http_code,
                fallback = "This source couldn't be loaded (HTTP %1\$d).",
            )
            runCatching {
                template.format(httpCode)
            }.getOrElse {
                "This source couldn't be loaded (HTTP $httpCode)."
            }
        } else {
            stringFromAppContext(
                resId = R.string.tv_player_source_error_dialog_unavailable,
                fallback = "This source is currently unavailable.",
            )
        }
    }

    private fun sourceDisplayLabel(
        link: ExtractorLink,
        index: Int,
    ): String {
        return link.name
            .takeIf { name -> name.isNotBlank() }
            ?: link.source
                .takeIf { source -> source.isNotBlank() }
            ?: "Source ${index + 1}"
    }

    private fun ensureSourceStateEntries(links: List<ExtractorLink>) {
        synchronized(sourceStatesByUrl) {
            links.forEach { link ->
                sourceStatesByUrl.putIfAbsent(
                    link.url,
                    TvPlayerSourceState(status = TvPlayerSourceStatus.Loading),
                )
            }
        }
    }

    private fun updateSourceState(
        url: String,
        state: TvPlayerSourceState,
    ): Boolean {
        return synchronized(sourceStatesByUrl) {
            val currentState = sourceStatesByUrl[url]
            if (currentState == state) {
                false
            } else {
                sourceStatesByUrl[url] = state
                true
            }
        }
    }

    private fun refreshReadyStateIfSubtitlesPanelVisible() {
        val readyState = _uiState.value as? TvPlayerUiState.Ready ?: return
        if (readyState.panels.activePanel != TvPlayerSidePanel.Subtitles) {
            return
        }
        postReadyStateForCurrentLink()
    }

    private fun stringFromAppContext(
        resId: Int,
        fallback: String,
    ): String {
        return CloudStreamApp.context?.getString(resId) ?: fallback
    }

    private fun updateReadyStateActivePanel(panel: TvPlayerSidePanel) {
        val currentState = _uiState.value as? TvPlayerUiState.Ready ?: return
        if (currentState.panels.activePanel == panel) return

        _uiState.value = currentState.copy(
            panels = currentState.panels.copy(activePanel = panel),
        )
    }

    private fun isSelectionPanelOpen(): Boolean {
        val readyState = _uiState.value as? TvPlayerUiState.Ready ?: return false
        return readyState.panels.activePanel != TvPlayerSidePanel.None
    }

}
