/*
 * CloudStream TV - Media Grid Component
 * Displays media items (movies/series) in a grid layout
 */

package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

private const val MEDIA_GRID_COLUMNS = 6

@Composable
fun MediaGrid(
    pagingItems: LazyPagingItems<MediaItemCompat>,
    onMediaClick: (MediaItemCompat) -> Unit,
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
        items(
            count = pagingItems.itemCount,
            key = { index ->
                pagingItems[index]?.let { item ->
                    "${item.id}_${item.apiName}_${item.url}_$index"
                } ?: "grid_placeholder_$index"
            }
        ) { index ->
            val item = pagingItems[index]
            if (item != null) {
                FeedPosterCard(
                    item = item,
                    onClick = {
                        onMediaClick(item)
                    },
                    modifier = Modifier.aspectRatio(2f / 3f)
                )
            } else {
                ShimmerCard(Modifier.aspectRatio(2f / 3f))
            }
        }

        if (pagingItems.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(MEDIA_GRID_COLUMNS) }) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ShimmerCard(Modifier.aspectRatio(2f / 3f))
                }
            }
        }

        if (pagingItems.loadState.append is LoadState.Error) {
            item(span = { GridItemSpan(MEDIA_GRID_COLUMNS) }) {
                ShimmerCard(Modifier.aspectRatio(2f / 3f))
            }
        }
    }
}
