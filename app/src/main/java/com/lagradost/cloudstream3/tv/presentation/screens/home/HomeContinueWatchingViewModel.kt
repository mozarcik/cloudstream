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
    private var nextRemoveActionToken = 0

    init {
        loadContinueWatching()
    }

    fun refresh() {
        loadContinueWatching(forceReload = true)
    }

    fun removeItem(parentId: Int?) {
        if (parentId == null) return
        val removeActionToken = nextRemoveActionToken()

        viewModelScope.launch {
            continueWatchingRepository.removeItem(parentId)
                .fold(
                    onSuccess = {
                        loadContinueWatching(forceReload = true) {
                            publishRemoveResult(
                                token = removeActionToken,
                                wasSuccessful = true,
                            )
                        }
                    },
                    onFailure = { throwable ->
                        Log.e(TAG, "Failed to remove continue watching item", throwable)
                        publishRemoveResult(
                            token = removeActionToken,
                            wasSuccessful = false,
                        )
                    }
                )
        }
    }

    private fun loadContinueWatching(
        forceReload: Boolean = false,
        onLoaded: (() -> Unit)? = null,
    ) {
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
            onLoaded?.invoke()
        }
    }

    private fun publishRemoveResult(
        token: Int,
        wasSuccessful: Boolean,
    ) {
        _uiState.update { state ->
            state.copy(
                lastRemoveActionToken = token,
                lastRemoveSucceeded = wasSuccessful,
            )
        }
    }

    private fun nextRemoveActionToken(): Int {
        nextRemoveActionToken += 1
        return nextRemoveActionToken
    }

    private companion object {
        private const val TAG = "HomeContinueWatchingVM"
    }
}
