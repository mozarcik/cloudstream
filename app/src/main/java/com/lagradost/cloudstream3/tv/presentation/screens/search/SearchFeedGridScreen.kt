package com.lagradost.cloudstream3.tv.presentation.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.screens.home.MediaGrid
import kotlinx.coroutines.flow.flowOf

@Composable
fun SearchFeedGridScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onBack: () -> Unit,
    onScroll: (Boolean) -> Unit,
) {
    val selectedSection by SearchFeedGridSelectionStore.selectedSection.collectAsState()

    if (selectedSection == null) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    val section = selectedSection ?: return
    val gridState = rememberLazyGridState()
    val pagingFlow = remember(section.id) {
        flowOf(PagingData.from(section.items))
    }
    val pagingItems = pagingFlow.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        onScroll(true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F16))
    ) {
        Text(
            text = section.title,
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
