package com.lagradost.cloudstream3.tv.compat.resume

import android.util.Log
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKeys
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.utils.DOWNLOAD_HEADER_CACHE
import com.lagradost.cloudstream3.utils.downloader.DownloadObjects
import kotlinx.coroutines.CancellationException

private const val Tag = "MissingProviderFallback"
private const val MaxSearchPages = 2

internal data class MissingProviderReplacement(
    val api: MainAPI,
    val url: String,
    /** Parent id the entry was saved under; episode ids are derived from it. */
    val previousParentId: Int,
)

/**
 * A Continue watching entry whose provider was removed: look the title up in the currently selected
 * provider. Only an exact (normalized) name with a matching kind (series vs movie) is accepted; no
 * match means the caller keeps showing the "links error" instead of opening a wrong title.
 */
internal suspend fun findReplacementForMissingProvider(
    apiName: String,
    url: String,
): MissingProviderReplacement? {
    val header = findSavedHeader(apiName = apiName, url = url) ?: return null
    val wantedName = header.name.normalizeTitle()
    if (wantedName.isEmpty()) return null

    val api = SourceRepository.currentApiOrNull() ?: return null
    if (api.name == apiName) return null
    val repository = APIRepository(api)

    for (page in 1..MaxSearchPages) {
        val result = try {
            (repository.search(query = header.name, page = page) as? Resource.Success)?.value
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Log.w(Tag, "search failed api=${api.name} title=${header.name}", error)
            null
        } ?: return null

        val match = result.items.firstOrNull { candidate ->
            candidate.name.normalizeTitle() == wantedName && candidate.matchesKind(header.type)
        }
        if (match != null) {
            return MissingProviderReplacement(
                api = api,
                url = match.url,
                previousParentId = header.id,
            )
        }
        if (!result.hasNext) break
    }
    Log.i(Tag, "no replacement for '${header.name}' in ${api.name}")
    return null
}

private fun findSavedHeader(apiName: String, url: String): DownloadObjects.DownloadHeaderCached? {
    CloudStreamApp.context ?: return null
    return getKeys(DOWNLOAD_HEADER_CACHE).orEmpty().firstNotNullOfOrNull { key ->
        getKey<DownloadObjects.DownloadHeaderCached>(key)
            ?.takeIf { header -> header.apiName == apiName && header.url == url }
    }
}

private fun SearchResponse.matchesKind(savedType: TvType): Boolean {
    val candidateType = type ?: return true
    return candidateType.isEpisodeBased() == savedType.isEpisodeBased()
}

private fun String.normalizeTitle(): String {
    return trim().lowercase().replace(Regex("\\s+"), " ")
}
