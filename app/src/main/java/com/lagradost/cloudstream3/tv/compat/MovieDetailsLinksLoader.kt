package com.lagradost.cloudstream3.tv.compat

import android.util.Log
import com.lagradost.cloudstream3.sortUrls
import com.lagradost.cloudstream3.tv.util.tvTraceAsyncSection
import com.lagradost.cloudstream3.ui.player.RepoLinkGenerator
import com.lagradost.cloudstream3.ui.result.LinkLoadingResult
import com.lagradost.cloudstream3.utils.AppContextUtils.sortSubs
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

internal data class MovieDetailsLinksBatchUpdate(
    val loadedCount: Int,
    val links: List<com.lagradost.cloudstream3.utils.ExtractorLink>,
    val subtitles: List<com.lagradost.cloudstream3.ui.player.SubtitleData>,
    val isCompleted: Boolean,
)

internal class MovieDetailsLinksLoader {
    private companion object {
        const val DebugTag = "MovieLinksLoader"
        const val LinkLoadingPollIntervalMs = 80L
    }

    private val cache = MovieDetailsLinksCache()

    suspend fun load(
        target: MovieDetailsActionTarget,
        sourceTypes: Set<ExtractorLinkType>,
        clearCache: Boolean = false,
        isCasting: Boolean = false,
        onLinksLoaded: ((Int) -> Unit)? = null,
        onLinksBatchUpdated: ((MovieDetailsLinksBatchUpdate) -> Unit)? = null,
        shouldCancelLoading: (() -> Boolean)? = null,
    ): LinkLoadingResult {
        return tvTraceAsyncSection(
            sectionName = "details_action_links",
            cookie = target.episode.id,
        ) {
            val cacheKey = MovieDetailsLinksCache.Key(
                episodeId = target.episode.id,
                sourceTypes = sourceTypes.toSet(),
                isCasting = isCasting,
            )

            if (clearCache) {
                cache.remove(cacheKey)
            }

            if (!clearCache) {
                val cachedResult = cache.get(cacheKey)
                if (cachedResult != null) {
                    onLinksLoaded?.invoke(cachedResult.links.size)
                    MovieDetailsLinksBatchPublisher(
                        callback = onLinksBatchUpdated,
                    ).emitCompleted(cachedResult)
                    return@tvTraceAsyncSection cachedResult
                }
            }

            val links = linkedSetOf<com.lagradost.cloudstream3.utils.ExtractorLink>()
            val subtitles = linkedSetOf<com.lagradost.cloudstream3.ui.player.SubtitleData>()
            var cancelledByExternalRequest = false
            var completedWithoutCancellation = false
            val batchPublisher = MovieDetailsLinksBatchPublisher(
                callback = onLinksBatchUpdated,
            )

            coroutineScope {
                val loadJob = async {
                    try {
                        RepoLinkGenerator(
                            episodes = listOf(target.episode),
                            page = target.loadResponse,
                        ).generateLinks(
                            clearCache = clearCache,
                            sourceTypes = sourceTypes,
                            callback = { (link, _) ->
                                if (link != null && links.add(link)) {
                                    onLinksLoaded?.invoke(links.size)
                                    batchPublisher.emit(
                                        links = links,
                                        subtitles = subtitles,
                                        isCompleted = false,
                                    )
                                    Log.d(
                                        DebugTag,
                                        "loadLinks source added count=${links.size} name=${link.name} quality=${link.quality} episodeId=${target.episode.id}"
                                    )
                                }
                            },
                            subtitleCallback = { subtitle ->
                                subtitles += subtitle
                            },
                            isCasting = isCasting,
                        )
                        completedWithoutCancellation = true
                    } catch (_: CancellationException) {
                        Log.d(DebugTag, "link loading cancelled for action")
                    } catch (error: Throwable) {
                        Log.e(DebugTag, "failed loading links for action", error)
                    }
                }

                if (shouldCancelLoading != null) {
                    while (loadJob.isActive) {
                        if (shouldCancelLoading()) {
                            cancelledByExternalRequest = true
                            Log.d(
                                DebugTag,
                                "loadLinks cancelled by external request episodeId=${target.episode.id} loaded=${links.size}"
                            )
                            loadJob.cancel()
                            break
                        }
                        delay(LinkLoadingPollIntervalMs)
                    }
                }

                try {
                    loadJob.await()
                } catch (_: CancellationException) {
                    // Keep partial results if loading was interrupted.
                }
            }

            batchPublisher.emit(
                links = links,
                subtitles = subtitles,
                isCompleted = true,
            )

            val result = LinkLoadingResult(
                links = sortUrls(links),
                subs = sortSubs(subtitles),
                syncData = HashMap(target.loadResponse.syncData),
            )
            if (!cancelledByExternalRequest && completedWithoutCancellation) {
                cache.put(cacheKey, result)
            }
            result
        }
    }
}
