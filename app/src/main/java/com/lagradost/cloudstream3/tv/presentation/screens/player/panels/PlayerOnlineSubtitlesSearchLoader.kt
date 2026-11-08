package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities.SubtitleEntity
import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities.SubtitleSearch
import com.lagradost.cloudstream3.syncproviders.SubtitleRepo
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.subtitleProviders
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val OnlineSubtitlesSearchDebounceMs = 200L
private const val SubsLogTag = "TvSubsSearch"

internal class PlayerOnlineSubtitlesSearchLoader(
    private val context: PlayerOnlineSubtitlesLoadContext,
) {
    private var searchDebounceJob: Job? = null
    private var searchJob: Job? = null

    fun reset() {
        searchDebounceJob?.cancel()
        searchDebounceJob = null
        searchJob?.cancel()
        searchJob = null
    }

    fun onQueryUpdated(
        query: String,
        shouldSearchImmediately: Boolean,
    ) {
        val normalizedQuery = query.trim()
        val currentState = context.state()
        if (normalizedQuery == currentState.query) return

        context.updateState(
            currentState.copy(
                query = normalizedQuery,
                status = if (normalizedQuery.isBlank()) {
                    TvPlayerOnlineSubtitlesStatus.Idle
                } else {
                    TvPlayerOnlineSubtitlesStatus.Loading
                },
                errorMessage = null,
                results = if (normalizedQuery.isBlank()) {
                    kotlinx.collections.immutable.persistentListOf()
                } else {
                    currentState.results
                },
            )
        )
        if (shouldSearchImmediately) {
            scheduleSearch(immediate = false)
        }
        context.refreshVisibleUi()
    }

    fun scheduleSearch(immediate: Boolean) {
        searchDebounceJob?.cancel()
        searchDebounceJob = null

        val query = context.state().query
        if (query.isBlank()) {
            searchJob?.cancel()
            searchJob = null
            context.updateState(
                context.state().copy(
                    status = TvPlayerOnlineSubtitlesStatus.Idle,
                    results = kotlinx.collections.immutable.persistentListOf(),
                    errorMessage = null,
                )
            )
            context.clearResultPayloads()
            context.refreshVisibleUi()
            return
        }

        val launchSearch: () -> Unit = {
            searchJob?.cancel()
            searchJob = context.coroutineScope.launch {
                performSearch()
            }
        }

        if (immediate) {
            launchSearch()
            return
        }

        searchDebounceJob = context.coroutineScope.launch {
            delay(OnlineSubtitlesSearchDebounceMs)
            searchDebounceJob = null
            launchSearch()
        }
    }

    private suspend fun performSearch() {
        val stateSnapshot = context.state()
        val querySnapshot = stateSnapshot.query
        val languageTagSnapshot = stateSnapshot.selectedLanguageTag
        if (querySnapshot.isBlank()) {
            return
        }

        val request = context.createSearchRequest(querySnapshot, languageTagSnapshot)
        Log.i(SubsLogTag, "search start: query='${request.query}' lang=${request.lang} imdb=${request.imdbId} tmdb=${request.tmdbId} mal=${request.malId} anilist=${request.aniListId} season=${request.seasonNumber} ep=${request.epNumber} year=${request.year}")

        context.updateState(
            stateSnapshot.copy(
                status = TvPlayerOnlineSubtitlesStatus.Loading,
                errorMessage = null,
            )
        )
        context.refreshVisibleUi()

        val providers = subtitleProviders.toList()
        Log.i(SubsLogTag, "providers (${providers.size}): ${providers.joinToString { it.idPrefix }}")
        if (providers.isEmpty()) {
            context.updateState(
                context.state().copy(
                    status = TvPlayerOnlineSubtitlesStatus.Error,
                    results = kotlinx.collections.immutable.persistentListOf(),
                    errorMessage = onlineSubtitlesSearchFailedMessage(context.stringResolver),
                )
            )
            context.clearResultPayloads()
            context.refreshVisibleUi()
            return
        }

        var failedProviders = 0
        val providerResults = providers.amap { provider ->
            val startedAt = System.currentTimeMillis()
            val result = provider.searchWithYearFallback(request)
            val tookMs = System.currentTimeMillis() - startedAt
            provider.idPrefix to when (val response = Resource.fromResult(result)) {
                is Resource.Success -> {
                    Log.i(
                        SubsLogTag,
                        "[${provider.idPrefix}] ${response.value.size} results in ${tookMs}ms: " +
                            response.value.take(10).joinToString { "'${it.name}'/${it.lang}/s${it.seasonNumber}e${it.epNumber}" }
                    )
                    response.value
                }
                is Resource.Loading -> emptyList()
                is Resource.Failure -> {
                    Log.w(SubsLogTag, "[${provider.idPrefix}] FAILED in ${tookMs}ms: ${response.errorString}", result.exceptionOrNull())
                    failedProviders += 1
                    emptyList()
                }
            }
        }

        val refreshedState = context.state()
        val queryChanged = querySnapshot != refreshedState.query ||
            !languageTagSnapshot.equals(refreshedState.selectedLanguageTag, ignoreCase = true)
        if (queryChanged) {
            return
        }

        Log.i(SubsLogTag, "search done: merged=${providerResults.sumOf { it.second.size }} failedProviders=$failedProviders")
        val searchSnapshot = mapOnlineSubtitleSearchResults(
            providerResults = providerResults,
            stringResolver = context.stringResolver,
        )
        val nextStatus = resolveOnlineSubtitleSearchStatus(
            mappedResults = searchSnapshot,
            failedProviders = failedProviders,
        )
        context.updateResultPayloads(searchSnapshot.payloadsByResultId)
        context.updateState(
            refreshedState.copy(
                status = nextStatus,
                results = if (nextStatus == TvPlayerOnlineSubtitlesStatus.Results) {
                    searchSnapshot.results
                } else {
                    kotlinx.collections.immutable.persistentListOf()
                },
                errorMessage = when (nextStatus) {
                    TvPlayerOnlineSubtitlesStatus.Error -> {
                        onlineSubtitlesSearchFailedMessage(context.stringResolver)
                    }
                    else -> null
                },
            )
        )
        context.refreshVisibleUi()
    }
}

/**
 * Searches with the request as is; when that finds nothing and a year was given, retries without it
 * (the year of a title often differs between databases, e.g. 2016 vs 2017).
 */
internal suspend fun SubtitleRepo.searchWithYearFallback(
    request: SubtitleSearch,
): Result<List<SubtitleEntity>> {
    val first = search(request)
    if (request.year == null || !first.getOrNull().isNullOrEmpty()) return first
    Log.i(SubsLogTag, "[$idPrefix] no results with year=${request.year}, retrying without year")
    return search(request.copy(year = null))
}
