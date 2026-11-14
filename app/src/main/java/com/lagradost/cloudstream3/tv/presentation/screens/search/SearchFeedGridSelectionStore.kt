package com.lagradost.cloudstream3.tv.presentation.screens.search

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.compat.home.SearchGridPagingSource
import com.lagradost.cloudstream3.tv.presentation.screens.grid.TvGridFocusStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

object SearchFeedGridSelectionStore {
    private val _selectedSection = MutableStateFlow<SearchSectionUiState?>(null)
    val selectedSection: StateFlow<SearchSectionUiState?> = _selectedSection.asStateFlow()

    val focus = TvGridFocusStore()

    /** Query the opened section was searched with, shown next to the source name. */
    var query: String = ""
        private set

    /**
     * Results of the opened section, page by page. Cached for as long as the section stays selected,
     * so coming back from a title does not load the pages again.
     */
    var pagingData: Flow<PagingData<MediaItemCompat>> = flowOf(PagingData.empty())
        private set
    private var pagingScope: CoroutineScope? = null

    fun setQuery(value: String) {
        query = value
    }

    /** Call after [setQuery]: the pages are asked for the current query. */
    fun setSelectedSection(section: SearchSectionUiState) {
        focus.reset()
        _selectedSection.value = section
        pagingScope?.cancel()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main).also { pagingScope = it }
        val sectionQuery = query
        pagingData = Pager(
            config = PagingConfig(pageSize = 20, prefetchDistance = 14, enablePlaceholders = false),
            pagingSourceFactory = {
                SearchGridPagingSource(
                    apiName = section.id,
                    query = sectionQuery,
                    firstPage = section.items,
                    firstPageHasNext = section.hasNext,
                )
            },
        ).flow.cachedIn(scope)
    }
}
