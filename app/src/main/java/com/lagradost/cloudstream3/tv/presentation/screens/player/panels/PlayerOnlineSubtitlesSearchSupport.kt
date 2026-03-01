package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities
import kotlinx.collections.immutable.toPersistentList

internal fun mapOnlineSubtitleSearchResults(
    providerResults: List<Pair<String, List<AbstractSubtitleEntities.SubtitleEntity>>>,
    stringResolver: (Int, String) -> String,
): PlayerOnlineSubtitleSearchSnapshot {
    val merged = mergeOnlineSubtitleProviderResults(providerResults)
    val payloadsByResultId = LinkedHashMap<String, PlayerOnlineSubtitleResultPayload>(merged.size)
    val results = merged.mapIndexed { index, (providerIdPrefix, subtitle) ->
        val idSuffix =
            "${subtitle.idPrefix}_${subtitle.data}_${subtitle.name}_${subtitle.lang}_${subtitle.source}_$index"
        val resolvedProviderIdPrefix = subtitle.idPrefix
            .takeIf { prefix -> prefix.isNotBlank() }
            ?: providerIdPrefix
        val resultId = "subtitle_online_result_${idSuffix.hashCode()}"
        payloadsByResultId[resultId] = PlayerOnlineSubtitleResultPayload(
            idPrefix = subtitle.idPrefix,
            name = subtitle.name,
            lang = subtitle.lang,
            data = subtitle.data,
            type = subtitle.type,
            source = subtitle.source,
            epNumber = subtitle.epNumber,
            seasonNumber = subtitle.seasonNumber,
            year = subtitle.year,
            isHearingImpaired = subtitle.isHearingImpaired,
            headers = subtitle.headers.toMap(),
        )
        TvPlayerOnlineSubtitleResult(
            id = resultId,
            providerIdPrefix = resolvedProviderIdPrefix,
            title = subtitle.name.ifBlank {
                stringResolver(
                    com.lagradost.cloudstream3.R.string.no_subtitles_loaded,
                    "Unnamed subtitle",
                )
            },
            supportingTexts = formatOnlineSubtitleSupportingTexts(subtitle).toPersistentList(),
        )
    }
    return PlayerOnlineSubtitleSearchSnapshot(
        results = results.toPersistentList(),
        payloadsByResultId = payloadsByResultId,
    )
}

internal fun mergeOnlineSubtitleProviderResults(
    providerResults: List<Pair<String, List<AbstractSubtitleEntities.SubtitleEntity>>>,
): List<Pair<String, AbstractSubtitleEntities.SubtitleEntity>> {
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
    return merged
}

internal fun resolveOnlineSubtitleSearchStatus(
    mappedResults: PlayerOnlineSubtitleSearchSnapshot,
    failedProviders: Int,
): TvPlayerOnlineSubtitlesStatus {
    return when {
        mappedResults.results.isNotEmpty() -> TvPlayerOnlineSubtitlesStatus.Results
        failedProviders > 0 -> TvPlayerOnlineSubtitlesStatus.Error
        else -> TvPlayerOnlineSubtitlesStatus.Empty
    }
}
