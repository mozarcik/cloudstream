package com.lagradost.cloudstream3.tv.presentation.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.HaloHost
import com.lagradost.cloudstream3.tv.presentation.screens.home.MediaGridStatic

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

    LaunchedEffect(Unit) {
        onScroll(true)
    }

    HaloHost(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 12.dp, end = 24.dp)
            )

            MediaGridStatic(
                items = section.items,
                onMediaClick = onMediaClick,
                gridState = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp)
            )
        }
    }
}
