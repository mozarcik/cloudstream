package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities.SubtitleSearch
import com.lagradost.cloudstream3.ui.player.SubtitleData
import kotlinx.coroutines.CoroutineScope

internal class PlayerOnlineSubtitlesLoader(
    coroutineScope: CoroutineScope,
    stringResolver: (Int, String) -> String,
    createSearchRequest: (String, String?) -> SubtitleSearch,
    stateProvider: () -> TvPlayerOnlineSubtitlesState,
    stateUpdater: (TvPlayerOnlineSubtitlesState) -> Unit,
    onVisibleUiRefreshRequested: () -> Unit,
    onSubtitlesDownloaded: (List<SubtitleData>) -> Unit,
) {
    private val context = PlayerOnlineSubtitlesLoadContext(
        coroutineScope = coroutineScope,
        stringResolver = stringResolver,
        createSearchRequest = createSearchRequest,
        stateProvider = stateProvider,
        stateUpdater = stateUpdater,
        onVisibleUiRefreshRequested = onVisibleUiRefreshRequested,
        onSubtitlesDownloaded = onSubtitlesDownloaded,
    )
    private val searchLoader = PlayerOnlineSubtitlesSearchLoader(context)
    private val downloadLoader = PlayerOnlineSubtitlesDownloadLoader(context)

    fun reset() {
        searchLoader.reset()
        downloadLoader.reset()
        context.clearResultPayloads()
    }

    fun onQueryUpdated(
        query: String,
        shouldSearchImmediately: Boolean,
    ) {
        searchLoader.onQueryUpdated(
            query = query,
            shouldSearchImmediately = shouldSearchImmediately,
        )
    }

    fun scheduleSearch(immediate: Boolean) {
        searchLoader.scheduleSearch(immediate = immediate)
    }

    fun selectOnlineSubtitleResult(resultId: String) {
        downloadLoader.selectOnlineSubtitleResult(resultId)
    }

    fun loadFirstAvailableSubtitle(defaultQuery: String) {
        downloadLoader.loadFirstAvailableSubtitle(defaultQuery)
    }
}
