package com.lagradost.cloudstream3.tv.presentation.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.APIHolder.apis
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.tv.compat.home.SearchResponseMapper.toMediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedLoadState
import com.lagradost.cloudstream3.ui.APIRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

class SearchViewModel : ViewModel() {
    private var searchJob: Job? = null
    private var repositories = synchronized(apis) {
        apis.map { api ->
            APIRepository(api)
        }
    }

    private val _uiState = MutableStateFlow(SearchScreenUiState())
    val uiState = _uiState.asStateFlow()

    fun onQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(query = query)
        }
    }

    fun onSearchSubmitted() {
        val normalizedQuery = _uiState.value.query.trim()

        if (normalizedQuery.isBlank()) {
            searchJob?.cancel()
            _uiState.update { state ->
                state.copy(
                    submittedQuery = "",
                    isLoading = false,
                    hasSearched = false,
                    sections = persistentListOf(),
                )
            }
            return
        }

        val state = _uiState.value
        if (state.isLoading && state.submittedQuery == normalizedQuery) {
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            performSearch(normalizedQuery)
        }
    }

    private fun resolveRepositories(): List<APIRepository> {
        return synchronized(apis) {
            val currentRepoNames = repositories.map { it.name }
            val currentApiNames = apis.map { it.name }
            repositories = if (currentRepoNames == currentApiNames) {
                repositories
            } else {
                apis.map { api ->
                    APIRepository(api)
                }
            }

            repositories
        }
    }

    private suspend fun performSearch(query: String) {
        val availableRepositories = resolveRepositories()
        val initialSections = availableRepositories.map { repository ->
            SearchSectionUiState(
                id = repository.name,
                title = repository.name,
                state = HomeFeedLoadState.Loading,
            )
        }.toPersistentList()

        _uiState.update { state ->
            state.copy(
                submittedQuery = query,
                isLoading = initialSections.isNotEmpty(),
                hasSearched = true,
                sections = initialSections,
            )
        }

        if (initialSections.isEmpty()) {
            _uiState.update { state ->
                if (state.submittedQuery != query) {
                    state
                } else {
                    state.copy(isLoading = false)
                }
            }
            return
        }

        supervisorScope {
            availableRepositories.forEach { repository ->
                launch(Dispatchers.IO) {
                    try {
                        updateSectionState(
                            query = query,
                            sectionId = repository.name,
                            sectionState = resolveSectionState(
                                repository = repository,
                                query = query,
                            ),
                        )
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Throwable) {
                        updateSectionState(
                            query = query,
                            sectionId = repository.name,
                            sectionState = HomeFeedLoadState.Error,
                        )
                    }
                }
            }
        }

        _uiState.update { state ->
            if (state.submittedQuery != query) {
                state
            } else {
                state.copy(
                    isLoading = false,
                )
            }
        }
    }

    private suspend fun resolveSectionState(
        repository: APIRepository,
        query: String,
    ): HomeFeedLoadState {
        return when (val response = repository.search(query = query, page = 1)) {
            is Resource.Success -> HomeFeedLoadState.Success(
                response.value.items.map { item ->
                    item.toMediaItemCompat()
                }.toPersistentList()
            )

            is Resource.Failure -> HomeFeedLoadState.Error
            is Resource.Loading -> HomeFeedLoadState.Loading
        }
    }

    private fun updateSectionState(
        query: String,
        sectionId: String,
        sectionState: HomeFeedLoadState,
    ) {
        _uiState.update { state ->
            if (state.submittedQuery != query) {
                return@update state
            }

            val sectionIndex = state.sections.indexOfFirst { section ->
                section.id == sectionId
            }
            if (sectionIndex == -1) {
                return@update state
            }

            val currentSection = state.sections[sectionIndex]
            if (currentSection.state == sectionState) {
                return@update state
            }

            val updatedSections = state.sections.set(
                index = sectionIndex,
                element = currentSection.copy(state = sectionState)
            )

            state.copy(
                sections = updatedSections,
                isLoading = updatedSections.any { section ->
                    section.state is HomeFeedLoadState.Loading
                }
            )
        }
    }
}
