package com.lagradost.cloudstream3.tv.compat.home

import com.lagradost.cloudstream3.APIHolder.getApiFromNameNull
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.removeKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.utils.DOWNLOAD_HEADER_CACHE
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.VideoDownloadHelper

internal fun refreshContinueWatchingHeader(
    parentId: Int,
    apiName: String,
    url: String,
    name: String,
    type: TvType,
    posterUrl: String?,
    backdropUrl: String?,
) {
    val existingHeader = getKey<VideoDownloadHelper.DownloadHeaderCached>(
        DOWNLOAD_HEADER_CACHE,
        parentId.toString()
    )
    val normalizedPosterUrl = posterUrl?.takeIf { it.isNotBlank() }
    val normalizedBackdropUrl = backdropUrl?.takeIf { it.isNotBlank() }

    setKey(
        DOWNLOAD_HEADER_CACHE,
        parentId.toString(),
        VideoDownloadHelper.DownloadHeaderCached(
            apiName = apiName,
            url = url,
            type = type,
            name = name,
            poster = normalizedPosterUrl ?: existingHeader?.poster?.takeIf { it.isNotBlank() },
            backdrop = normalizedBackdropUrl,
            cacheTime = System.currentTimeMillis(),
            id = parentId,
        )
    )

    removeUnavailableContinueWatchingEntries(
        currentParentId = parentId,
        currentName = name,
        currentType = type,
    )
}

internal fun removeContinueWatchingEntry(parentId: Int?) {
    if (parentId == null) return

    DataStoreHelper.removeLastWatched(parentId)
    removeKey(DOWNLOAD_HEADER_CACHE, parentId.toString())
}

private fun removeUnavailableContinueWatchingEntries(
    currentParentId: Int,
    currentName: String,
    currentType: TvType,
) {
    val normalizedCurrentName = currentName.normalizeContinueWatchingName()
    if (normalizedCurrentName.isBlank()) return

    DataStoreHelper.getAllResumeStateIds().orEmpty().forEach { resumeParentId ->
        if (resumeParentId == currentParentId) return@forEach

        val resume = DataStoreHelper.getLastWatched(resumeParentId) ?: return@forEach
        val cachedHeader = getKey<VideoDownloadHelper.DownloadHeaderCached>(
            DOWNLOAD_HEADER_CACHE,
            resumeParentId.toString()
        ) ?: return@forEach

        // WHY: jeśli stare API już nie istnieje, zostawienie wpisu powoduje dublowanie albo
        // brak aktualizacji continue watching po wejściu w ten sam tytuł z nowego źródła.
        if (getApiFromNameNull(cachedHeader.apiName) != null) return@forEach
        if (cachedHeader.name.normalizeContinueWatchingName() != normalizedCurrentName) return@forEach
        if (cachedHeader.type.toContinueWatchingMatchKey() != currentType.toContinueWatchingMatchKey()) {
            return@forEach
        }

        DataStoreHelper.removeLastWatched(resume.parentId)
        removeKey(DOWNLOAD_HEADER_CACHE, resume.parentId.toString())
    }
}

private fun String.normalizeContinueWatchingName(): String {
    return trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")
}

private fun TvType.toContinueWatchingMatchKey(): String {
    return if (isEpisodeBased()) {
        "episode_based"
    } else {
        name
    }
}
