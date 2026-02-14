package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.tv.compat.home.FeedRepository
import com.lagradost.cloudstream3.tv.compat.home.SourcePreferencesDataStoreRepository
import com.lagradost.cloudstream3.tv.compat.home.SourcePreferencesRepository

class HomeScreenV2ViewModelFactory(
    private val feedRepository: FeedRepository,
    private val sourcePreferencesRepository: SourcePreferencesRepository? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeScreenV2ViewModel::class.java)) {
            val preferencesRepository = sourcePreferencesRepository
                ?: SourcePreferencesDataStoreRepository(
                    context = CloudStreamApp.context
                        ?: throw IllegalStateException("Application context is unavailable")
                )

            return HomeScreenV2ViewModel(
                feedRepository = feedRepository,
                sourcePreferencesRepository = preferencesRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
