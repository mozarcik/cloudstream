package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lagradost.cloudstream3.tv.compat.home.FeedRepository

class HomeFeedsViewModelFactory(
    private val feedRepository: FeedRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeFeedsViewModel::class.java)) {
            return HomeFeedsViewModel(
                feedRepository = feedRepository
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
