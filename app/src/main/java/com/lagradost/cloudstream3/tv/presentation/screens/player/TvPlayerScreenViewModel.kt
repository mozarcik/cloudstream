package com.lagradost.cloudstream3.tv.presentation.screens.player

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
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.result.buildResultEpisode
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DebugTag = "TvPlayerVM"

data class TvPlayerMetadata(
    val title: String,
    val subtitle: String,
    val backdropUri: String?,
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
    private var loadingJob: Job? = null
    private var hasFinalized = false
    private var metadata: TvPlayerMetadata = TvPlayerMetadata.Empty

    init {
        loadSources()
    }

    fun retry() {
        loadingJob?.cancel()
        loadedLinksByUrl.clear()
        hasFinalized = false
        metadata = TvPlayerMetadata.Empty
        loadSources()
    }

    fun skipLoading() {
        loadingJob?.cancel()
        finalizeLoading(forceError = true)
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
                            postLoadingState()
                        }
                    },
                    subtitleCallback = {}
                )
            } catch (_: CancellationException) {
                Log.d(DebugTag, "source loading cancelled")
            } catch (t: Throwable) {
                logError(t)
            }

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
                }

                is AnimeLoadResponse -> {
                    val first = loadResponse.episodes.values
                        .flatten()
                        .sortedWith(compareBy({ it.season ?: Int.MAX_VALUE }, { it.episode ?: Int.MAX_VALUE }))
                        .firstOrNull()
                    resolvedData = first?.data
                    resolvedSeason = first?.season
                    resolvedEpisode = first?.episode ?: 1
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
                }

                is AnimeLoadResponse -> {
                    val matchingEpisode = loadResponse.episodes.values
                        .flatten()
                        .firstOrNull { episode ->
                            episode.data == resolvedData
                        }
                    resolvedSeason = matchingEpisode?.season
                    resolvedEpisode = matchingEpisode?.episode ?: 1
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
            name = resolvedTitle,
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
            metadata = resolveMetadata(loadResponse, apiName)
        )
    }

    private fun resolveMetadata(loadResponse: LoadResponse?, apiName: String): TvPlayerMetadata {
        if (loadResponse == null) {
            return TvPlayerMetadata(
                title = apiName,
                subtitle = "",
                backdropUri = null
            )
        }

        val subtitleChunks = buildList {
            when (loadResponse) {
                is MovieLoadResponse -> loadResponse.year?.let { year -> add(year.toString()) }
                is TvSeriesLoadResponse -> loadResponse.year?.let { year -> add(year.toString()) }
                is AnimeLoadResponse -> loadResponse.year?.let { year -> add(year.toString()) }
                else -> Unit
            }
            add(apiName)
        }

        return TvPlayerMetadata(
            title = loadResponse.name,
            subtitle = subtitleChunks.joinToString(separator = " • "),
            backdropUri = loadResponse.backgroundPosterUrl ?: loadResponse.posterUrl
        )
    }

    private fun postLoadingState() {
        val loadedSources = synchronized(loadedLinksByUrl) { loadedLinksByUrl.size }
        _uiState.value = TvPlayerUiState.LoadingSources(
            metadata = metadata,
            loadedSources = loadedSources,
            canSkip = true,
        )
    }

    private fun finalizeLoading(forceError: Boolean) {
        if (hasFinalized) return

        val bestLink = synchronized(loadedLinksByUrl) {
            loadedLinksByUrl.values.maxByOrNull { link ->
                val quality = if (link.quality > 0) link.quality else 0
                val hlsBonus = if (link.isM3u8) 10_000 else 0
                quality + hlsBonus
            }
        }

        if (bestLink != null) {
            hasFinalized = true
            val sourceCount = synchronized(loadedLinksByUrl) { loadedLinksByUrl.size }
            _uiState.value = TvPlayerUiState.Ready(
                metadata = metadata,
                link = bestLink,
                sourceCount = sourceCount,
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
}
