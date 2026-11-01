package com.lagradost.cloudstream3.tv.presentation.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.tv.compat.home.FeaturedRepository
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeFeaturedViewModel(
    private val featuredRepository: FeaturedRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeFeaturedUiState())
    val uiState = _uiState.asStateFlow()

    private var featuredLoadJob: Job? = null

    init {
        observeSelectedSource()
    }

    private fun observeSelectedSource() {
        viewModelScope.launch {
            SourceRepository.selectedApi.collect { selectedApi ->
                selectedApi?.let(::loadFeaturedForApi)
            }
        }
    }

    private fun loadFeaturedForApi(api: MainAPI) {
        featuredLoadJob?.cancel()
        featuredLoadJob = viewModelScope.launch {
            _uiState.value = HomeFeaturedUiState(state = HomeFeaturedLoadState.Loading)

            val nextState = featuredRepository.getItems(api)
                .fold(
                    onSuccess = { items ->
                        if (items.isEmpty()) {
                            HomeFeaturedLoadState.Empty
                        } else {
                            HomeFeaturedLoadState.Success(items.toPersistentList())
                        }
                    },
                    onFailure = { throwable ->
                        Log.e(TAG, "Failed to load featured items for ${api.name}", throwable)
                        HomeFeaturedLoadState.Error
                    }
                )

            _uiState.update { state -> state.copy(state = nextState) }
        }
    }

    private companion object {
        private const val TAG = "HomeFeaturedVM"
    }
}
