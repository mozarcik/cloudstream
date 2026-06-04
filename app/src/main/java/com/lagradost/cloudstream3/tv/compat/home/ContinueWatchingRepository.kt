package com.lagradost.cloudstream3.tv.compat.home

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lagradost.cloudstream3.ui.home.HomeViewModel
import com.lagradost.cloudstream3.tv.util.warmArtworkSeedColor
import com.lagradost.cloudstream3.tv.compat.home.SearchResponseMapper.toMediaItemCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface ContinueWatchingRepository {
    suspend fun getItems(): Result<MediaListCompat>
    suspend fun removeItem(parentId: Int): Result<Unit>
}

class ContinueWatchingRepositoryImpl : ContinueWatchingRepository {
    override suspend fun getItems(): Result<MediaListCompat> = withContext(Dispatchers.IO) {
        runCatching {
            HomeViewModel.getResumeWatching()
                .orEmpty()
                .map { resumeItem -> resumeItem.toMediaItemCompat() }
                .distinctBy { mediaItem -> "${mediaItem.apiName}|${mediaItem.url}" }
        }
    }

    override suspend fun removeItem(parentId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            removeContinueWatchingEntry(parentId)
        }
    }
}

class ContinueWatchingImagePrefetcher(
    private val context: Context,
) {
    private val colorWarmupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun prefetch(items: List<MediaItemCompat>) {
        if (items.isEmpty()) return

        val imageLoader = SingletonImageLoader.get(context)
        buildPrefetchUrls(items).forEach { imageUrl ->
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(false)
                .build()

            imageLoader.enqueue(request)
        }

        colorWarmupScope.launch {
            buildColorWarmupUrls(items).forEach { imageUrl ->
                warmArtworkSeedColor(
                    context = context,
                    artworkUrl = imageUrl,
                )
            }
        }
    }

    private fun buildPrefetchUrls(items: List<MediaItemCompat>): List<String> {
        val primaryItem = items.firstOrNull()

        return buildList {
            primaryItem?.backdropUri
                ?.takeIf { url -> url.isNotBlank() }
                ?.let(::add)
            primaryItem?.preferredBackdropUriOrNull()?.let(::add)

            items.take(CONTINUE_WATCHING_PREFETCH_CARD_COUNT).forEach { item ->
                item.preferredBackdropUriOrNull()?.let(::add)
            }
        }.distinct()
    }

    private fun buildColorWarmupUrls(items: List<MediaItemCompat>): List<String> {
        return buildList {
            items.take(CONTINUE_WATCHING_PREFETCH_CARD_COUNT).forEach { item ->
                item.preferredBackdropUriOrNull()?.let(::add)
            }
        }.distinct()
    }

    private companion object {
        private const val CONTINUE_WATCHING_PREFETCH_CARD_COUNT = 5
    }
}
