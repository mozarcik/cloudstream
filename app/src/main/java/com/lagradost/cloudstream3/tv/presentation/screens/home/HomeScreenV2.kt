package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    topBarFocusRequester: FocusRequester,
    restoreFocusToken: Int = 0,
    sourcesViewModelFactory: HomeSourcesViewModelFactory = HomeSourcesViewModelFactory(),
    continueWatchingViewModelFactory: HomeContinueWatchingViewModelFactory =
        HomeContinueWatchingViewModelFactory(),
    featuredViewModelFactory: HomeFeaturedViewModelFactory = HomeFeaturedViewModelFactory(),
    feedsViewModelFactory: HomeFeedsViewModelFactory =
        HomeFeedsViewModelFactory(FeedRepositoryImpl()),
    modifier: Modifier = Modifier,
) {
    val sourcesViewModel: HomeSourcesViewModel = viewModel(
        factory = remember(sourcesViewModelFactory) { sourcesViewModelFactory }
    )
    val continueWatchingViewModel: HomeContinueWatchingViewModel = viewModel(
        factory = remember(continueWatchingViewModelFactory) { continueWatchingViewModelFactory }
    )
    val featuredViewModel: HomeFeaturedViewModel = viewModel(
        factory = remember(featuredViewModelFactory) { featuredViewModelFactory }
    )
    val feedsViewModel: HomeFeedsViewModel = viewModel(
        factory = remember(feedsViewModelFactory) { feedsViewModelFactory }
    )

    val sourcesUiState by sourcesViewModel.uiState.collectAsState()
    val continueWatchingUiState by continueWatchingViewModel.uiState.collectAsState()
    val featuredUiState by featuredViewModel.uiState.collectAsState()
    val feedsUiState by feedsViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var shouldRefreshContinueWatchingOnResume by remember {
        mutableStateOf(false)
    }

    DisposableEffect(lifecycleOwner, continueWatchingViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    shouldRefreshContinueWatchingOnResume = true
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (!shouldRefreshContinueWatchingOnResume) {
                        return@LifecycleEventObserver
                    }

                    shouldRefreshContinueWatchingOnResume = false
                    continueWatchingViewModel.refresh()
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    HomeScreenV2Content(
        sourcesUiState = sourcesUiState,
        continueWatchingUiState = continueWatchingUiState,
        featuredUiState = featuredUiState,
        feedsUiState = feedsUiState,
        onMediaClick = onMediaClick,
        onContinueWatchingPlay = onContinueWatchingPlay,
        onOpenFeedGrid = onOpenFeedGrid,
        onScroll = onScroll,
        topBarFocusRequester = topBarFocusRequester,
        restoreFocusToken = restoreFocusToken,
        onSourceSelected = sourcesViewModel::selectSource,
        onMorePanelOpenChange = sourcesViewModel::setMorePanelOpen,
        onTogglePin = sourcesViewModel::togglePinned,
        onRemoveContinueWatching = { item ->
            continueWatchingViewModel.removeItem(item.continueWatching?.parentId)
        },
        modifier = modifier,
    )
}
