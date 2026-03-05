package com.lagradost.cloudstream3.tv.presentation.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.tv.compat.home.ContinueWatchingImagePrefetcher
import com.lagradost.cloudstream3.tv.compat.home.ContinueWatchingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.toPersistentList

class HomeContinueWatchingViewModel(
    private val continueWatchingRepository: ContinueWatchingRepository,
    private val continueWatchingImagePrefetcher: ContinueWatchingImagePrefetcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeContinueWatchingUiState())
    val uiState = _uiState.asStateFlow()

    private var continueWatchingLoadJob: Job? = null

    init {
        loadContinueWatching()
    }

    fun removeItem(parentId: Int?) {
        if (parentId == null) return

        viewModelScope.launch {
            continueWatchingRepository.removeItem(parentId)
                .fold(
                    onSuccess = {
                        loadContinueWatching(forceReload = true)
                    },
                    onFailure = { throwable ->
                        Log.e(TAG, "Failed to remove continue watching item", throwable)
                    }
                )
        }
    }

    private fun loadContinueWatching(forceReload: Boolean = false) {
        val currentState = _uiState.value.state
        if (!forceReload && currentState !is HomeFeedLoadState.Loading) {
            return
        }

        continueWatchingLoadJob?.cancel()
        continueWatchingLoadJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(state = HomeFeedLoadState.Loading)
            }

            val nextState = continueWatchingRepository
                .getItems()
                .fold(
                    onSuccess = { items ->
                        val resolvedItems = items.take(HOME_FEED_PRELOAD_SIZE).toPersistentList()
                        continueWatchingImagePrefetcher.prefetch(resolvedItems)
                        HomeFeedLoadState.Success(resolvedItems)
                    },
                    onFailure = { throwable ->
                        Log.e(TAG, "Failed to load continue watching", throwable)
                        HomeFeedLoadState.Error
                    }
                )

            _uiState.update { state ->
                state.copy(state = nextState)
            }
        }
    }

    private companion object {
        private const val TAG = "HomeContinueWatchingVM"
    }
}
