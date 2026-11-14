package com.lagradost.cloudstream3.tv.compat.home

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.lagradost.cloudstream3.APIHolder.apis
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.tv.compat.home.SearchResponseMapper.toMediaItemCompat
import com.lagradost.cloudstream3.ui.APIRepository

/**
 * Search results of one source, page by page. Page 1 is what the search screen already loaded
 * ([firstPage]); the next ones are asked from the source while it reports more ([firstPageHasNext],
 * then each page's own `hasNext`). A source that ignores the page number returns the same titles
 * again: a page with nothing new ends the list.
 */
class SearchGridPagingSource(
    private val apiName: String,
    private val query: String,
    private val firstPage: List<MediaItemCompat>,
    private val firstPageHasNext: Boolean,
) : PagingSource<Int, MediaItemCompat>() {
    private val seenUrls = HashSet<String>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItemCompat> {
        val page = params.key ?: 1
        if (page == 1) {
            firstPage.forEach { seenUrls.add(it.url) }
            return LoadResult.Page(
                data = firstPage,
                prevKey = null,
                nextKey = if (firstPageHasNext && firstPage.isNotEmpty()) 2 else null,
            )
        }

        val api = synchronized(apis) { apis.firstOrNull { it.name == apiName } }
            ?: return LoadResult.Page(emptyList(), prevKey = page - 1, nextKey = null)
        return when (val response = APIRepository(api).search(query = query, page = page)) {
            is Resource.Success -> {
                val newItems = response.value.items
                    .map { it.toMediaItemCompat() }
                    .filter { seenUrls.add(it.url) }
                LoadResult.Page(
                    data = newItems,
                    prevKey = page - 1,
                    nextKey = if (response.value.hasNext && newItems.isNotEmpty()) page + 1 else null,
                )
            }

            is Resource.Failure -> LoadResult.Error(Exception(response.errorString))
            is Resource.Loading -> LoadResult.Error(IllegalStateException("search page $page did not finish"))
        }
    }

    /** A refresh starts again from the page that is already in memory. */
    override fun getRefreshKey(state: PagingState<Int, MediaItemCompat>): Int? = null
}
