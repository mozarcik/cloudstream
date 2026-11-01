package com.lagradost.cloudstream3.tv.presentation.screens.home

import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import com.lagradost.cloudstream3.tv.presentation.screens.grid.TvGridFocusStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object HomeFeedGridSelectionStore {
    private val _selectedFeed = MutableStateFlow<FeedCategory?>(null)
    val selectedFeed: StateFlow<FeedCategory?> = _selectedFeed.asStateFlow()

    val focus = TvGridFocusStore(keyPrefix = "home_feed_grid")

    fun setSelectedFeed(feed: FeedCategory) {
        focus.reset()
        _selectedFeed.value = feed
    }
}
