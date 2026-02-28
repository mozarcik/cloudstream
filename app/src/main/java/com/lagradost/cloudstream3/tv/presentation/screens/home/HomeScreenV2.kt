package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import com.lagradost.cloudstream3.tv.compat.home.FeedRepositoryImpl
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

@Composable
fun HomeScreenV2(
    onMediaClick: (MediaItemCompat) -> Unit,
    onContinueWatchingPlay: (MediaItemCompat) -> Unit,
    onOpenFeedGrid: (FeedCategory) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sourcesViewModel: HomeSourcesViewModel = viewModel(
        factory = remember { HomeSourcesViewModelFactory() }
    )
    val continueWatchingViewModel: HomeContinueWatchingViewModel = viewModel(
        factory = remember { HomeContinueWatchingViewModelFactory() }
    )
    val featuredViewModel: HomeFeaturedViewModel = viewModel(
        factory = remember { HomeFeaturedViewModelFactory() }
    )
    val feedsViewModel: HomeFeedsViewModel = viewModel(
        factory = remember { HomeFeedsViewModelFactory(FeedRepositoryImpl()) }
    )

    val sourcesUiState by sourcesViewModel.uiState.collectAsState()
    val continueWatchingUiState by continueWatchingViewModel.uiState.collectAsState()
    val featuredUiState by featuredViewModel.uiState.collectAsState()
    val feedsUiState by feedsViewModel.uiState.collectAsState()

    HomeScreenV2Content(
        sourcesUiState = sourcesUiState,
        continueWatchingUiState = continueWatchingUiState,
        featuredUiState = featuredUiState,
        feedsUiState = feedsUiState,
        onMediaClick = onMediaClick,
        onContinueWatchingPlay = onContinueWatchingPlay,
        onOpenFeedGrid = onOpenFeedGrid,
        onScroll = onScroll,
        onSourceSelected = sourcesViewModel::selectSource,
        onMorePanelOpenChange = sourcesViewModel::setMorePanelOpen,
        onTogglePin = sourcesViewModel::togglePinned,
        modifier = modifier,
    )
}
