package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.lagradost.cloudstream3.tv.compat.home.FeedRepositoryImpl
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository
import com.lagradost.cloudstream3.tv.presentation.screens.grid.TvShowMoreGridScreen

/** Home feed "Show more": the shared grid fed by a Paging3 source (infinite scroll). */
@Composable
fun HomeFeedGridScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onBack: () -> Unit,
    restoreFocusToken: Int = 0,
    mediaGridViewModel: MediaGridViewModel = viewModel(
        factory = MediaGridViewModelFactory(FeedRepositoryImpl())
    ),
) {
    val selectedFeed by HomeFeedGridSelectionStore.selectedFeed.collectAsState()
    val feed = selectedFeed
    if (feed == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val sourceName by SourceRepository.selectedApi.collectAsState(initial = null)
    val pagingItems = mediaGridViewModel.pagingData.collectAsLazyPagingItems()
    LaunchedEffect(feed.id) { mediaGridViewModel.selectFeed(feed) }

    TvShowMoreGridScreen(
        title = feed.name,
        subtitle = sourceName?.name,
        pagingItems = pagingItems,
        focusStore = HomeFeedGridSelectionStore.focus,
        onMediaClick = onMediaClick,
        screenKey = feed.id,
        restoreFocusToken = restoreFocusToken,
    )
}
