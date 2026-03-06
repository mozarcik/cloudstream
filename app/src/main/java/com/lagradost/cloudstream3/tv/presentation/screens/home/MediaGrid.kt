/*
 * CloudStream TV - Media Grid Component
 * Displays media items (movies/series) in a grid layout
 */

package com.lagradost.cloudstream3.tv.presentation.screens.home

import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.focus.rememberFocusRequesterMap

private const val MEDIA_GRID_COLUMNS = 6
private const val GRID_PLACEHOLDER_KEY_BASE = Long.MIN_VALUE
private const val HomeGridFocusDebugTag = "TvHomeFocus"

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
    firstItemFocusRequester: FocusRequester? = null,
    focusKeyPrefix: String? = null,
    pendingRestoreFocusTargetId: String? = null,
    pendingRestoreFocusIndex: Int? = null,
    restoreFocusToken: Int = 0,
    onItemFocused: ((MediaItemCompat, Int) -> Unit)? = null,
    onRestoreFocusConsumed: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    fun focusTargetIdFor(item: MediaItemCompat): String? {
        return focusKeyPrefix?.let { prefix ->
            "$prefix:item:${item.apiName}|${item.id}|${item.url}"
        }
    }

    val loadedFocusTargetIds = remember(pagingItems.itemSnapshotList.items, focusKeyPrefix) {
        pagingItems.itemSnapshotList.items.mapNotNull(::focusTargetIdFor)
    }
    val itemFocusRequesters = rememberFocusRequesterMap(loadedFocusTargetIds)
    val firstItemTargetId = remember(pagingItems.itemSnapshotList.items, focusKeyPrefix) {
        pagingItems.itemSnapshotList.items.firstOrNull()?.let(::focusTargetIdFor)
    }

    fun focusRequesterFor(index: Int, item: MediaItemCompat): FocusRequester? {
        val targetId = focusTargetIdFor(item)
        return when {
            index == 0 && firstItemFocusRequester != null -> firstItemFocusRequester
            targetId != null -> itemFocusRequesters[targetId]
            else -> null
        }
    }

    val restoreFocusIndex = remember(
        pagingItems.itemSnapshotList.items,
        pagingItems.itemCount,
        pendingRestoreFocusTargetId,
        pendingRestoreFocusIndex,
        restoreFocusToken,
        focusKeyPrefix
    ) {
        if (restoreFocusToken <= 0 || pendingRestoreFocusTargetId == null) {
            null
        } else if (pendingRestoreFocusIndex != null && pendingRestoreFocusIndex < pagingItems.itemCount) {
            pendingRestoreFocusIndex
        } else {
            (0 until pagingItems.itemCount).firstOrNull { index ->
                pagingItems.peek(index)?.let(::focusTargetIdFor) == pendingRestoreFocusTargetId
            }
        }
    }

    LaunchedEffect(restoreFocusIndex, pendingRestoreFocusTargetId, restoreFocusToken) {
        val targetIndex = restoreFocusIndex ?: return@LaunchedEffect
        if (restoreFocusToken <= 0) return@LaunchedEffect
        Log.d(
            HomeGridFocusDebugTag,
            "grid restore scroll token=$restoreFocusToken target=$pendingRestoreFocusTargetId index=$targetIndex"
        )
        gridState.scrollToItem(targetIndex)
    }

    FocusRequestEffect(
        requester = when {
            pendingRestoreFocusTargetId == firstItemTargetId && firstItemFocusRequester != null -> {
                firstItemFocusRequester
            }

            restoreFocusIndex != null -> {
                pagingItems.peek(restoreFocusIndex)
                    ?.let(::focusTargetIdFor)
                    ?.let(itemFocusRequesters::get)
            }

            else -> pendingRestoreFocusTargetId?.let(itemFocusRequesters::get)
        },
        requestKey = Triple(
            restoreFocusToken,
            pendingRestoreFocusTargetId,
            loadedFocusTargetIds
        ),
        enabled = restoreFocusToken > 0 &&
            pendingRestoreFocusTargetId != null &&
            restoreFocusIndex != null,
        attempts = 40,
        retryDelayMs = 50,
        onFocused = {
            Log.d(
                HomeGridFocusDebugTag,
                "grid restore focused token=$restoreFocusToken target=$pendingRestoreFocusTargetId index=$restoreFocusIndex"
            )
            pendingRestoreFocusTargetId?.let { targetId ->
                onRestoreFocusConsumed?.invoke(targetId)
            }
        }
    )

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(MEDIA_GRID_COLUMNS),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 28.dp),
        horizontalArrangement =  Arrangement.spacedBy(space = FeedGridColumnSpacing),
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
                val focusRequester = focusRequesterFor(index, item)
                FeedPosterCard(
                    item = item,
                    onClick = {
                        onMediaClick(item)
                    },
                    onFocused = {
                        onItemFocused?.invoke(item, index)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (focusRequester != null) {
                                Modifier.focusRequester(focusRequester)
                            } else {
                                Modifier
                            }
                        )
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
    firstItemFocusRequester: FocusRequester? = null,
    focusKeyPrefix: String? = null,
    pendingRestoreFocusTargetId: String? = null,
    restoreFocusToken: Int = 0,
    onItemFocused: ((MediaItemCompat) -> Unit)? = null,
    onRestoreFocusConsumed: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val displayItems = remember(items, duplicatesMode) {
        buildStaticGridEntries(
            sourceItems = items,
            duplicatesMode = duplicatesMode
        )
    }

    fun focusTargetIdFor(item: MediaItemCompat): String? {
        return focusKeyPrefix?.let { prefix ->
            "$prefix:item:${item.apiName}|${item.id}|${item.url}"
        }
    }

    val displayFocusTargetIds = remember(displayItems, focusKeyPrefix) {
        displayItems.mapNotNull { entry -> focusTargetIdFor(entry.item) }
    }
    val itemFocusRequesters = rememberFocusRequesterMap(displayFocusTargetIds)
    val firstItemTargetId = remember(displayItems, focusKeyPrefix) {
        displayItems.firstOrNull()?.item?.let(::focusTargetIdFor)
    }

    fun focusRequesterFor(index: Int, item: MediaItemCompat): FocusRequester? {
        val targetId = focusTargetIdFor(item)
        return when {
            index == 0 && firstItemFocusRequester != null -> firstItemFocusRequester
            targetId != null -> itemFocusRequesters[targetId]
            else -> null
        }
    }

    val restoreFocusIndex = remember(
        displayItems,
        pendingRestoreFocusTargetId,
        restoreFocusToken,
        focusKeyPrefix
    ) {
        if (restoreFocusToken <= 0 || pendingRestoreFocusTargetId == null) {
            null
        } else {
            displayItems.indexOfFirst { entry ->
                focusTargetIdFor(entry.item) == pendingRestoreFocusTargetId
            }.takeIf { it >= 0 }
        }
    }

    LaunchedEffect(restoreFocusIndex, pendingRestoreFocusTargetId, restoreFocusToken) {
        val targetIndex = restoreFocusIndex ?: return@LaunchedEffect
        if (restoreFocusToken <= 0) return@LaunchedEffect
        gridState.scrollToItem(targetIndex)
    }

    FocusRequestEffect(
        requester = when {
            pendingRestoreFocusTargetId == firstItemTargetId && firstItemFocusRequester != null -> {
                firstItemFocusRequester
            }

            else -> pendingRestoreFocusTargetId?.let(itemFocusRequesters::get)
        },
        requestKey = restoreFocusToken to pendingRestoreFocusTargetId,
        enabled = restoreFocusToken > 0 &&
            pendingRestoreFocusTargetId != null &&
            restoreFocusIndex != null,
        attempts = 40,
        retryDelayMs = 50,
        onFocused = {
            pendingRestoreFocusTargetId?.let { targetId ->
                onRestoreFocusConsumed?.invoke(targetId)
            }
        }
    )

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(MEDIA_GRID_COLUMNS),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 28.dp),
        horizontalArrangement =  Arrangement.spacedBy(space = FeedGridColumnSpacing),
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
            val focusRequester = focusRequesterFor(index, item)
            FeedPosterCard(
                item = item,
                onClick = {
                    onMediaClick(item)
                },
                onFocused = {
                    onItemFocused?.invoke(item)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (focusRequester != null) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        }
                    )
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
