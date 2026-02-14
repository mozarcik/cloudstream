package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.compat.home.FeedRepositoryImpl
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

@Composable
fun HomeFeedGridScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onBack: () -> Unit,
    onScroll: (Boolean) -> Unit,
    mediaGridViewModel: MediaGridViewModel = viewModel(
        factory = MediaGridViewModelFactory(FeedRepositoryImpl())
    ),
) {
    val selectedFeed by HomeFeedGridSelectionStore.selectedFeed.collectAsState()
    val pagingItems = mediaGridViewModel.pagingData.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()

    if (selectedFeed == null) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    LaunchedEffect(Unit) {
        onScroll(true)
    }

    LaunchedEffect(selectedFeed?.id) {
        selectedFeed?.let { feed ->
            mediaGridViewModel.selectFeed(feed)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F16))
    ) {
        Text(
            text = selectedFeed?.name.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 12.dp, end = 24.dp)
        )

        MediaGrid(
            pagingItems = pagingItems,
            onMediaClick = onMediaClick,
            gridState = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp)
        )
    }
}
