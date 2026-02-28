package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.tv.compat.home.ContinueWatchingImagePrefetcher
import com.lagradost.cloudstream3.tv.compat.home.ContinueWatchingRepository
import com.lagradost.cloudstream3.tv.compat.home.ContinueWatchingRepositoryImpl

class HomeContinueWatchingViewModelFactory(
    private val continueWatchingRepository: ContinueWatchingRepository? = null,
    private val continueWatchingImagePrefetcher: ContinueWatchingImagePrefetcher? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeContinueWatchingViewModel::class.java)) {
            val applicationContext = CloudStreamApp.context
                ?: throw IllegalStateException("Application context is unavailable")

            return HomeContinueWatchingViewModel(
                continueWatchingRepository = continueWatchingRepository
                    ?: ContinueWatchingRepositoryImpl(),
                continueWatchingImagePrefetcher = continueWatchingImagePrefetcher
                    ?: ContinueWatchingImagePrefetcher(applicationContext),
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
