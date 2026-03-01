package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.subtitles.SubtitleResource.SingleSubtitleResource
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.subtitleProviders
import com.lagradost.cloudstream3.ui.player.PlayerSubtitleHelper.Companion.toSubtitleMimeType
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesFragment.Companion.getAutoSelectLanguageTagIETF
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class PlayerOnlineSubtitlesDownloadLoader(
    private val context: PlayerOnlineSubtitlesLoadContext,
) {
    private var loadJob: Job? = null
    private var firstAvailableSubtitleJob: Job? = null

    fun reset() {
        loadJob?.cancel()
        loadJob = null
        firstAvailableSubtitleJob?.cancel()
        firstAvailableSubtitleJob = null
    }

    fun selectOnlineSubtitleResult(resultId: String) {
        val currentState = context.state()
        val selectedResult = currentState.results.firstOrNull { result ->
            result.id == resultId
        } ?: return
        val payload = context.resultPayload(resultId) ?: run {
            context.updateState(
                currentState.copy(
                    status = TvPlayerOnlineSubtitlesStatus.Results,
                    errorMessage = onlineSubtitlesSearchFailedMessage(context.stringResolver),
                )
            )
            context.refreshVisibleUi()
            return
        }
        val providerIdPrefix = selectedResult.providerIdPrefix
        val provider = subtitleProviders.firstOrNull { candidate ->
            candidate.idPrefix == providerIdPrefix
        } ?: run {
            context.updateState(
                currentState.copy(
                    status = TvPlayerOnlineSubtitlesStatus.Results,
                    errorMessage = onlineSubtitlesSearchFailedMessage(context.stringResolver),
                )
            )
            context.refreshVisibleUi()
            return
        }

        loadJob?.cancel()
        loadJob = context.coroutineScope.launch {
            val subtitleEntry = payload.toSubtitleEntity()
            when (val resource = Resource.fromResult(provider.resource(subtitleEntry))) {
                is Resource.Success -> {
                    val downloadedSubtitles = mapDownloadedSubtitles(
                        subtitleEntry = subtitleEntry,
                        downloadedEntries = resource.value.getSubtitles(),
                    )
                    if (downloadedSubtitles.isEmpty()) {
                        context.updateState(
                            context.state().copy(
                                status = TvPlayerOnlineSubtitlesStatus.Results,
                                errorMessage = onlineSubtitlesNoSubtitlesLoadedMessage(context.stringResolver),
                            )
                        )
                        context.refreshVisibleUi()
                        return@launch
                    }

                    context.deliverDownloadedSubtitles(downloadedSubtitles)
                }

                is Resource.Failure -> {
                    context.updateState(
                        context.state().copy(
                            status = TvPlayerOnlineSubtitlesStatus.Results,
                            errorMessage = resource.errorString,
                        )
                    )
                    context.refreshVisibleUi()
                }

                is Resource.Loading -> Unit
            }
        }
    }

    fun loadFirstAvailableSubtitle(defaultQuery: String) {
        val query = defaultQuery.trim()
        if (query.isBlank()) {
            return
        }

        val providers = subtitleProviders.toList()
        if (providers.isEmpty()) {
            return
        }

        val request = context.createSearchRequest(
            query,
            getAutoSelectLanguageTagIETF().trim(),
        )

        firstAvailableSubtitleJob?.cancel()
        loadJob?.cancel()
        loadJob = null
        firstAvailableSubtitleJob = context.coroutineScope.launch {
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

                    val downloadedSubtitles = mapDownloadedSubtitles(
                        subtitleEntry = subtitleEntry,
                        downloadedEntries = subtitleResource.value.getSubtitles(),
                    )
                    if (downloadedSubtitles.isEmpty()) continue

                    context.deliverDownloadedSubtitles(downloadedSubtitles)
                    return@launch
                }
            }
        }
    }

    private fun mapDownloadedSubtitles(
        subtitleEntry: com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities.SubtitleEntity,
        downloadedEntries: List<SingleSubtitleResource>,
    ): List<SubtitleData> {
        return downloadedEntries.map { downloaded ->
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
    }
}
