package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.subtitleProviders
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val OnlineSubtitlesSearchDebounceMs = 200L

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

        context.updateState(
            stateSnapshot.copy(
                status = TvPlayerOnlineSubtitlesStatus.Loading,
                errorMessage = null,
            )
        )
        context.refreshVisibleUi()

        val providers = subtitleProviders.toList()
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
            provider.idPrefix to when (val response = Resource.fromResult(provider.search(request))) {
                is Resource.Success -> response.value
                is Resource.Loading -> emptyList()
                is Resource.Failure -> {
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
