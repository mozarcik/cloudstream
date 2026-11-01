package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.tv.compat.home.FeaturedRepository
import com.lagradost.cloudstream3.tv.compat.home.FeaturedRepositoryImpl

class HomeFeaturedViewModelFactory(
    private val featuredRepository: FeaturedRepository? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeFeaturedViewModel::class.java)) {
            val applicationContext = CloudStreamApp.context
                ?: throw IllegalStateException("Application context is unavailable")

            return HomeFeaturedViewModel(
                featuredRepository = featuredRepository ?: FeaturedRepositoryImpl(applicationContext)
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
