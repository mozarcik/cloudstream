package com.lagradost.cloudstream3.tv.presentation.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import com.lagradost.cloudstream3.tv.compat.home.FeedRepository
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.toPersistentList

class HomeFeedsViewModel(
    private val feedRepository: FeedRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeFeedsUiState())
    val uiState = _uiState.asStateFlow()

    private var homeLoadJob: Job? = null

    init {
        observeSelectedSource()
    }

    private fun observeSelectedSource() {
        viewModelScope.launch {
            SourceRepository.selectedApi.collect { selectedApi ->
                selectedApi?.let { api ->
                    loadHomeForApi(api)
                }
            }
        }
    }

    private fun loadHomeForApi(api: MainAPI) {
        homeLoadJob?.cancel()
        homeLoadJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isFeedListLoading = true,
                    feedSections = state.feedSections.map { section ->
                        section.copy(state = HomeFeedLoadState.Loading)
                    }.toPersistentList()
                )
            }

            val categories = runCatching {
                feedRepository.getFeedCategories(api).first()
            }.onFailure { throwable ->
                Log.e(TAG, "Failed to load feed categories for ${api.name}", throwable)
            }.getOrDefault(emptyList())

            val feedSections = categories
                .map { category ->
                    HomeFeedSectionUiState(
                        feed = category,
                        state = HomeFeedLoadState.Loading
                    )
                }.toPersistentList()

            _uiState.update { state ->
                state.copy(
                    feedSections = feedSections,
                    isFeedListLoading = false
                )
            }

            feedSections.forEach { section ->
                launch {
                    loadFeedSection(api, section.feed)
                }
            }
        }
    }

    private suspend fun loadFeedSection(
        api: MainAPI,
        feed: FeedCategory,
    ) {
        val sectionState = feedRepository
            .getMediaForFeed(api, feed, page = 1)
            .fold(
                onSuccess = { items ->
                    HomeFeedLoadState.Success(items.take(HOME_FEED_PRELOAD_SIZE).toPersistentList())
                },
                onFailure = { throwable ->
                    Log.e(TAG, "Failed to load feed section '${feed.name}'", throwable)
                    HomeFeedLoadState.Error
                }
            )

        _uiState.update { state ->
            val sectionIndex = state.feedSections.indexOfFirst { section ->
                section.feed.id == feed.id
            }
            if (sectionIndex == -1) {
                return@update state
            }

            val currentSection = state.feedSections[sectionIndex]
            if (currentSection.state == sectionState) {
                return@update state
            }

            state.copy(
                feedSections = state.feedSections.set(
                    index = sectionIndex,
                    element = currentSection.copy(state = sectionState)
                )
            )
        }
    }

    private companion object {
        private const val TAG = "HomeFeedsVM"
    }
}
