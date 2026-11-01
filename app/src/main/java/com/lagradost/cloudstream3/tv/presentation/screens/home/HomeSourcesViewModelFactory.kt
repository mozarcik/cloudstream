package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.tv.compat.home.HomeSourceSelectionRepository
import com.lagradost.cloudstream3.tv.compat.home.LegacyHomeSourceSelectionRepository
import com.lagradost.cloudstream3.tv.compat.home.SourcePreferencesDataStoreRepository
import com.lagradost.cloudstream3.tv.compat.home.SourcePreferencesRepository

class HomeSourcesViewModelFactory(
    private val sourcePreferencesRepository: SourcePreferencesRepository? = null,
    private val homeSourceSelectionRepository: HomeSourceSelectionRepository? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeSourcesViewModel::class.java)) {
            val applicationContext = CloudStreamApp.context
                ?: throw IllegalStateException("Application context is unavailable")

            return HomeSourcesViewModel(
                sourcePreferencesRepository = sourcePreferencesRepository
                    ?: SourcePreferencesDataStoreRepository(applicationContext),
                homeSourceSelectionRepository = homeSourceSelectionRepository
                    ?: LegacyHomeSourceSelectionRepository(),
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
