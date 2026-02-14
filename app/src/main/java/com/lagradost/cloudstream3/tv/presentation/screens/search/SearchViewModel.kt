package com.lagradost.cloudstream3.tv.presentation.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.APIHolder.apis
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.tv.compat.home.SearchResponseMapper.toMediaItemCompat
import com.lagradost.cloudstream3.ui.APIRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

private const val SEARCH_DEBOUNCE_MS = 350L

class SearchViewModel : ViewModel() {
    private val queryFlow = MutableStateFlow("")
    private var repositories = synchronized(apis) {
        apis.map { api ->
            APIRepository(api)
        }
    }

    private val _uiState = MutableStateFlow(SearchScreenUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeQuery()
    }

    fun onQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(query = query)
        }
        queryFlow.value = query
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        viewModelScope.launch {
            queryFlow
                .map { it.trim() }
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { normalizedQuery ->
                    if (normalizedQuery.isBlank()) {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                hasSearched = false,
                                sections = emptyList(),
                            )
                        }
                        return@collectLatest
                    }

                    performSearch(normalizedQuery)
                }
        }
    }

    private suspend fun performSearch(query: String) {
        repositories = synchronized(apis) {
            val currentRepoNames = repositories.map { it.name }
            val currentApiNames = apis.map { it.name }
            if (currentRepoNames == currentApiNames) {
                repositories
            } else {
                apis.map { api ->
                    APIRepository(api)
                }
            }
        }

        _uiState.update { state ->
            state.copy(
                isLoading = true,
                hasSearched = true,
                sections = emptyList(),
            )
        }

        val sections = supervisorScope {
            repositories.map { repository ->
                async(Dispatchers.IO) {
                    val search = repository.search(query = query, page = 1)
                    val searchItems = (search as? Resource.Success)?.value?.items.orEmpty()
                    if (searchItems.isEmpty()) {
                        return@async null
                    }

                    SearchSectionUiState(
                        id = repository.name,
                        title = repository.name,
                        items = searchItems.map { response ->
                            response.toMediaItemCompat()
                        }
                    )
                }
            }.awaitAll().filterNotNull()
        }

        _uiState.update { state ->
            if (state.query.trim() != query) {
                state
            } else {
                state.copy(
                    isLoading = false,
                    hasSearched = true,
                    sections = sections,
                )
            }
        }
    }
}
