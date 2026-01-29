/*
 * CloudStream TV - Media Grid Component
 * Displays media items (movies/series) in a grid layout
 */

package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.MovieCard

private const val MEDIA_GRID_COLUMNS = 6

/**
 * Grid component displaying media items (movies/TV series)
 * Task 5.1: Paging support with LazyPagingItems
 * Task 5.2: Loading states with shimmer and progress indicators
 * 
 * @param pagingItems Paging items to display
 * @param onMediaClick Callback when media item is clicked
 * @param focusRequester Focus requester for first item
 * @param modifier Modifier for the grid container
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaGrid(
    pagingItems: LazyPagingItems<MediaItemCompat>,
    onMediaClick: (MediaItemCompat) -> Unit,
    onOpenFeedMenu: () -> Unit,
    focusRequester: FocusRequester,
    gridState: LazyGridState,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(MEDIA_GRID_COLUMNS),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        // Task 5.1: Paging items
        items(pagingItems.itemCount) { index ->
            val item = pagingItems[index]
            if (item != null) {
                var isFocused by remember { mutableStateOf(false) }
                
                MovieCard(
                    onClick = { onMediaClick(item) },
                    modifier = Modifier
                        .aspectRatio(2f / 3f)  // Portrait poster ratio
                        .onPreviewKeyEvent { keyEvent ->
                            if (
                                index % MEDIA_GRID_COLUMNS == 0 &&
                                keyEvent.key == Key.DirectionLeft &&
                                keyEvent.type == KeyEventType.KeyUp
                            ) {
                                onOpenFeedMenu()
                                true
                            } else {
                                false
                            }
                        }
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                        }
                        .border(
                            width = if (isFocused) 4.dp else 0.dp,
                            color = if (isFocused) Color.White else Color.Transparent
                        )
                        .let {
                            // Auto-focus first item
                            if (index == 0) it.focusRequester(focusRequester)
                            else it
                        }
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.posterUri)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // Task 5.2: Placeholder for loading items
                ShimmerCard(Modifier.aspectRatio(2f / 3f))
            }
        }
        
        // Task 5.2: Loading indicator when appending next page
        if (pagingItems.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(5) }) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ShimmerCard(Modifier.aspectRatio(2f / 3f))
                }
            }
        }
        
        // Task 5.3: Error state - just show shimmer for now
        if (pagingItems.loadState.append is LoadState.Error) {
            item(span = { GridItemSpan(5) }) {
                // Placeholder for error
                ShimmerCard(Modifier.aspectRatio(2f / 3f))
            }
        }
    }
}
