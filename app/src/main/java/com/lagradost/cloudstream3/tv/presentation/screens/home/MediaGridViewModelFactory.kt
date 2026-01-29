/*
 * CloudStream TV - Media Grid ViewModel Factory
 */

package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lagradost.cloudstream3.tv.compat.home.FeedRepository

class MediaGridViewModelFactory(
    private val feedRepository: FeedRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaGridViewModel::class.java)) {
            return MediaGridViewModel(feedRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
