package com.lagradost.cloudstream3.tv.compat.home

import android.content.Context
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.utils.AppContextUtils.filterHomePageListByFilmQuality
import com.lagradost.cloudstream3.utils.AppContextUtils.filterSearchResultByFilmQuality
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

interface FeaturedRepository {
    suspend fun getItems(api: MainAPI): Result<List<FeaturedItemCompat>>
}

class FeaturedRepositoryImpl(
    private val applicationContext: Context,
) : FeaturedRepository {

    override suspend fun getItems(api: MainAPI): Result<List<FeaturedItemCompat>> =
        withContext(Dispatchers.IO) {
            try {
                if (!api.hasMainPage || api.mainPage.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val repository = APIRepository(api)
                val homePageResource = repository.getMainPage(page = FEATURED_INITIAL_PAGE, nameIndex = null)

                val homePageResponses = when (homePageResource) {
                    is Resource.Success -> homePageResource.value
                    is Resource.Failure -> error(homePageResource.errorString)
                    is Resource.Loading -> emptyList()
                }

                val featuredCandidates = buildFeaturedCandidates(homePageResponses)
                if (featuredCandidates.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                Result.success(
                    loadFeaturedItems(
                    repository = repository,
                    candidates = featuredCandidates.take(FEATURED_ITEM_COUNT)
                )
                )
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    throw throwable
                }
                Result.failure(throwable)
            }
        }

    private suspend fun loadFeaturedItems(
        repository: APIRepository,
        candidates: List<SearchResponse>,
    ): List<FeaturedItemCompat> = coroutineScope {
        candidates.map { candidate ->
            async {
                val loadResult = repository.load(candidate.url)
                val loadResponse = (loadResult as? Resource.Success)?.value ?: return@async null
                loadResponse.toFeaturedItemCompat()
            }
        }.awaitAll().filterNotNull()
    }

    private fun buildFeaturedCandidates(homePageResponses: List<com.lagradost.cloudstream3.HomePageResponse?>): List<SearchResponse> {
        val filteredLists = homePageResponses
            .asSequence()
            .filterNotNull()
            .flatMap { homePageResponse -> homePageResponse.items.asSequence() }
            .map { homePageList -> applicationContext.filterHomePageListByFilmQuality(homePageList) }
            .filter { homePageList -> homePageList.list.isNotEmpty() }
            .toList()

        if (filteredLists.isEmpty()) {
            return emptyList()
        }

        val distinctCandidates = filteredLists
            .shuffled()
            .flatMap { homePageList -> homePageList.list }
            .distinctBy { searchResponse -> searchResponse.url }

        if (distinctCandidates.isEmpty()) {
            return emptyList()
        }

        return applicationContext.filterSearchResultByFilmQuality(distinctCandidates.shuffled())
    }

    private companion object {
        private const val FEATURED_INITIAL_PAGE = 1
        private const val FEATURED_ITEM_COUNT = 5
    }
}
