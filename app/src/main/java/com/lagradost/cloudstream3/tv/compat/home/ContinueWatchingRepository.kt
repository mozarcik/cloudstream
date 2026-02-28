package com.lagradost.cloudstream3.tv.compat.home

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lagradost.cloudstream3.ui.home.HomeViewModel
import com.lagradost.cloudstream3.tv.compat.home.SearchResponseMapper.toMediaItemCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ContinueWatchingRepository {
    suspend fun getItems(): Result<MediaListCompat>
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
}

class ContinueWatchingImagePrefetcher(
    private val context: Context,
) {
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
    }

    private fun buildPrefetchUrls(items: List<MediaItemCompat>): List<String> {
        val primaryItem = items.firstOrNull()

        return buildList {
            primaryItem?.backdropUri
                ?.takeIf { url -> url.isNotBlank() }
                ?.let(::add)
            primaryItem?.posterUri
                ?.takeIf { url -> url.isNotBlank() }
                ?.let(::add)

            items.take(CONTINUE_WATCHING_PREFETCH_CARD_COUNT).forEach { item ->
                item.posterUri.takeIf { url -> url.isNotBlank() }?.let(::add)
            }
        }.distinct()
    }

    private companion object {
        private const val CONTINUE_WATCHING_PREFETCH_CARD_COUNT = 5
    }
}
