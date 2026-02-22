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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

private const val MEDIA_GRID_COLUMNS = 6
private const val GRID_PLACEHOLDER_KEY_BASE = Long.MIN_VALUE

enum class MediaGridDuplicatesMode {
    Keep,
    Remove,
}

private data class MediaGridIdentity(
    val apiName: String,
    val id: String,
    val url: String,
)

private data class StaticGridEntry(
    val item: MediaItemCompat,
    val key: Long,
)

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
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        items(
            count = pagingItems.itemCount,
            key = { index ->
                pagingItems.peek(index)?.let { item ->
                    mediaGridPagingItemKey(item = item, index = index)
                } ?: (GRID_PLACEHOLDER_KEY_BASE + index)
            },
            contentType = { "poster_item" }
        ) { index ->
            val item = pagingItems[index]
            if (item != null) {
                FeedPosterCard(
                    item = item,
                    onClick = {
                        onMediaClick(item)
                    },
                    modifier = Modifier.fillMaxWidth()
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

@Composable
fun MediaGridStatic(
    items: List<MediaItemCompat>,
    onMediaClick: (MediaItemCompat) -> Unit,
    gridState: LazyGridState,
    duplicatesMode: MediaGridDuplicatesMode = MediaGridDuplicatesMode.Keep,
    modifier: Modifier = Modifier
) {
    val displayItems = remember(items, duplicatesMode) {
        buildStaticGridEntries(
            sourceItems = items,
            duplicatesMode = duplicatesMode
        )
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(MEDIA_GRID_COLUMNS),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(
            count = displayItems.size,
            key = { index ->
                displayItems[index].key
            },
            contentType = { "poster_item" }
        ) { index ->
            val item = displayItems[index].item
            FeedPosterCard(
                item = item,
                onClick = {
                    onMediaClick(item)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun mediaGridPagingItemKey(
    item: MediaItemCompat,
    index: Int,
): Long {
    // WHY: paging może zawierać duplikaty tej samej pozycji; index jest tie-breakerem.
    return (31L * item.gridKeySeed) + index.toLong()
}

private fun mediaGridStaticItemKey(
    item: MediaItemCompat,
    occurrence: Int,
): Long {
    return (31L * item.gridKeySeed) + occurrence.toLong()
}

private fun buildStaticGridEntries(
    sourceItems: List<MediaItemCompat>,
    duplicatesMode: MediaGridDuplicatesMode,
): List<StaticGridEntry> {
    val entries = ArrayList<StaticGridEntry>(sourceItems.size)
    val identityOccurrences = LinkedHashMap<MediaGridIdentity, Int>(sourceItems.size, 0.75f)
    val dedupeSet = LinkedHashSet<MediaGridIdentity>(sourceItems.size, 0.75f)

    sourceItems.forEach { item ->
        val identity = item.toGridIdentity()
        if (duplicatesMode == MediaGridDuplicatesMode.Remove && !dedupeSet.add(identity)) {
            return@forEach
        }

        val occurrence = identityOccurrences[identity] ?: 0
        identityOccurrences[identity] = occurrence + 1
        entries.add(
            StaticGridEntry(
                item = item,
                key = mediaGridStaticItemKey(
                    item = item,
                    occurrence = occurrence
                )
            )
        )
    }

    return entries
}

private fun MediaItemCompat.toGridIdentity(): MediaGridIdentity {
    return MediaGridIdentity(
        apiName = apiName,
        id = id,
        url = url
    )
}
