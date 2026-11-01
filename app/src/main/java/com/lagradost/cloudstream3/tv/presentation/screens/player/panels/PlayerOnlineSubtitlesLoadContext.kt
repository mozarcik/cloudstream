package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities.SubtitleSearch
import com.lagradost.cloudstream3.ui.player.SubtitleData
import kotlinx.coroutines.CoroutineScope

internal class PlayerOnlineSubtitlesLoadContext(
    internal val coroutineScope: CoroutineScope,
    internal val stringResolver: (Int, String) -> String,
    internal val createSearchRequest: (String, String?) -> SubtitleSearch,
    private val stateProvider: () -> TvPlayerOnlineSubtitlesState,
    private val stateUpdater: (TvPlayerOnlineSubtitlesState) -> Unit,
    private val onVisibleUiRefreshRequested: () -> Unit,
    private val onSubtitlesDownloaded: (List<SubtitleData>) -> Unit,
) {
    private var resultPayloadsById: Map<String, PlayerOnlineSubtitleResultPayload> = emptyMap()

    internal fun state(): TvPlayerOnlineSubtitlesState = stateProvider()

    internal fun updateState(nextState: TvPlayerOnlineSubtitlesState) {
        stateUpdater(nextState)
    }

    internal fun updateResultPayloads(payloadsByResultId: Map<String, PlayerOnlineSubtitleResultPayload>) {
        resultPayloadsById = payloadsByResultId
    }

    internal fun resultPayload(resultId: String): PlayerOnlineSubtitleResultPayload? {
        return resultPayloadsById[resultId]
    }

    internal fun clearResultPayloads() {
        resultPayloadsById = emptyMap()
    }

    internal fun refreshVisibleUi() {
        onVisibleUiRefreshRequested()
    }

    internal fun deliverDownloadedSubtitles(downloadedSubtitles: List<SubtitleData>) {
        onSubtitlesDownloaded(downloadedSubtitles)
    }
}
