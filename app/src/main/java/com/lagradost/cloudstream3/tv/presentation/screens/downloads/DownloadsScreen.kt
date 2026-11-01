package com.lagradost.cloudstream3.tv.presentation.screens.downloads

import android.util.Log
import androidx.compose.foundation.background
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import com.lagradost.cloudstream3.tv.presentation.screens.dashboard.DashboardTopBarHeight
import com.lagradost.cloudstream3.tv.presentation.screens.dashboard.LocalDashboardContentEntry
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.TvConfirmDialog
import com.lagradost.cloudstream3.tv.presentation.common.TvEmptyState
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.focus.rememberFocusRequesterMap
import com.lagradost.cloudstream3.tv.presentation.focus.resolveAdjacentFocusKey
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

private const val DebugTag = "TvDownloadsScreen"

@Composable
fun DownloadsScreen(
    onOpenDetails: (DownloadItemUiModel) -> Unit,
    onPlayDownloaded: (DownloadItemUiModel) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    topBarFocusRequester: FocusRequester? = null,
    onTopBarDownNavigationEnabledChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val allItems = remember(uiState.downloadingItems, uiState.downloadedItems) {
        uiState.downloadingItems + uiState.downloadedItems
    }
    val itemIds = remember(allItems) { allItems.map(DownloadItemUiModel::id) }
    val itemFocusRequesters = rememberFocusRequesterMap(itemIds)

    var focusedItemId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<DownloadItemUiModel?>(null) }
    var pendingRestoreFocusId by remember { mutableStateOf<String?>(null) }
    var topBarFocusFallbackToken by remember { mutableIntStateOf(0) }
    val restoreFocusTargetId = pendingRestoreFocusId

    val focusedItem = remember(allItems, focusedItemId) {
        allItems.firstOrNull { it.id == focusedItemId } ?: allItems.firstOrNull()
    }
    val focusedBackdrop = focusedItem?.backdropUrl
        ?.takeIf { it.isNotBlank() }
        ?: focusedItem?.posterUrl?.takeIf { it.isNotBlank() }

    LaunchedEffect(context) {
        viewModel.bind(context)
    }

    // The list sits under the (overlay) top bar, so the default Down search does not find it: enter it explicitly.
    val contentEntry = LocalDashboardContentEntry.current
    val coroutineScope = rememberCoroutineScope()
    val entryRequester = (focusedItemId?.let(itemFocusRequesters::get)
        ?: allItems.firstOrNull()?.let { itemFocusRequesters[it.id] })
    DisposableEffect(contentEntry, entryRequester) {
        val handler: () -> Boolean = {
            entryRequester?.let { requester ->
                coroutineScope.launch { requester.requestFocusWithRetry(attempts = 18) }
                true
            } ?: false
        }
        contentEntry.handler = handler
        onDispose { if (contentEntry.handler === handler) contentEntry.handler = null }
    }

    LaunchedEffect(allItems.isNotEmpty()) {
        onTopBarDownNavigationEnabledChanged(allItems.isNotEmpty())
    }

    LaunchedEffect(itemIds, restoreFocusTargetId) {
        if (restoreFocusTargetId != null && restoreFocusTargetId !in itemFocusRequesters) {
            pendingRestoreFocusId = null
        }
    }

    FocusRequestEffect(
        requester = restoreFocusTargetId?.let(itemFocusRequesters::get),
        requestKey = restoreFocusTargetId,
        enabled = pendingDeleteItem == null && restoreFocusTargetId != null,
        onFocused = {
            focusedItemId = restoreFocusTargetId
            pendingRestoreFocusId = null
        }
    )

    FocusRequestEffect(
        requester = topBarFocusRequester,
        requestKey = topBarFocusFallbackToken,
        enabled = topBarFocusRequester != null &&
            topBarFocusFallbackToken > 0 &&
            pendingDeleteItem == null &&
            allItems.isEmpty(),
        onFocused = {
            topBarFocusFallbackToken = 0
        }
    )

    val shouldShowTopBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 100
        }
    }
    LaunchedEffect(shouldShowTopBar) { onScroll(shouldShowTopBar) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Ground)
    ) {
        DownloadsBackdrop(
            backdropUrl = focusedBackdrop,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        if (allItems.isEmpty()) {
            DownloadsEmptyState(
                modifier = Modifier
                    .padding(top = DashboardTopBarHeight)
                    .padding(bottom = TvDimens.DownloadsEmptyStateLift)
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = TvDimens.ScreenHorizontalPadding,
                    end = TvDimens.ScreenHorizontalPadding,
                    // The top bar overlays the screen (see DashboardScaffold), so the list starts below it.
                    top = DashboardTopBarHeight + TvDimens.DownloadsContentTopGap,
                    bottom = TvDimens.SearchListBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(TvDimens.DownloadRowGap),
                modifier = Modifier.fillMaxSize()
            ) {
                downloadSection(
                    headerRes = R.string.downloading,
                    keyPrefix = "downloading",
                    items = uiState.downloadingItems,
                    isFirst = true,
                    focusRequesters = itemFocusRequesters,
                    onOpenDetails = onOpenDetails,
                    onPlay = { playDownloadedItem(it, onPlayDownloaded) },
                    onDelete = { pendingDeleteItem = it },
                    onFocused = { focusedItemId = it.id },
                )
                downloadSection(
                    headerRes = R.string.downloaded,
                    keyPrefix = "downloaded",
                    items = uiState.downloadedItems,
                    isFirst = uiState.downloadingItems.isEmpty(),
                    focusRequesters = itemFocusRequesters,
                    onOpenDetails = onOpenDetails,
                    onPlay = { playDownloadedItem(it, onPlayDownloaded) },
                    onDelete = { pendingDeleteItem = it },
                    onFocused = { focusedItemId = it.id },
                )
            }
        }

        pendingDeleteItem?.let { itemToDelete ->
            DeleteDownloadDialog(
                itemTitle = itemToDelete.title,
                onDismiss = { pendingDeleteItem = null },
                onConfirm = {
                    val nextFocusId = resolveNextDownloadsFocusId(
                        items = itemIds,
                        removedItemId = itemToDelete.id
                    )
                    pendingRestoreFocusId = nextFocusId
                    if (nextFocusId == null) {
                        topBarFocusFallbackToken += 1
                    }
                    viewModel.deleteItem(
                        context = context,
                        item = itemToDelete
                    )
                    pendingDeleteItem = null
                }
            )
        }
    }
}

/** "DOWNLOADING • 2" header and its rows; an empty section is left out. */
private fun LazyListScope.downloadSection(
    headerRes: Int,
    keyPrefix: String,
    items: List<DownloadItemUiModel>,
    isFirst: Boolean,
    focusRequesters: Map<String, FocusRequester>,
    onOpenDetails: (DownloadItemUiModel) -> Unit,
    onPlay: (DownloadItemUiModel) -> Unit,
    onDelete: (DownloadItemUiModel) -> Unit,
    onFocused: (DownloadItemUiModel) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = "${keyPrefix}_header") {
        Text(
            text = stringResource(headerRes).uppercase() + " \u2022 " + items.size,
            style = TvTextStyles.Eyebrow,
            color = TvColors.Label,
            maxLines = 1,
            modifier = Modifier
                .padding(top = if (isFirst) 0.dp else TvDimens.DownloadsSectionGap)
                .height(TvDimens.DownloadsSectionHeaderHeight),
        )
    }
    items(items = items, key = { it.id }) { download ->
        DownloadItemRow(
            item = download,
            onClick = { onOpenDetails(download) },
            onPlayClick = { onPlay(download) },
            onDeleteClick = { onDelete(download) },
            onRowFocused = { onFocused(download) },
            focusRequester = focusRequesters.getValue(download.id),
        )
    }
}

/** Focused item's backdrop at the top right, 35% opaque, faded into the background; no animation. */
@Composable
private fun DownloadsBackdrop(
    backdropUrl: String?,
    modifier: Modifier = Modifier,
) {
    if (backdropUrl.isNullOrBlank()) return
    Box(
        modifier = modifier
            .width(TvDimens.DownloadsBackdropWidth)
            .height(TvDimens.DownloadsBackdropHeight)
    ) {
        AsyncImage(
            model = backdropUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = TvDimens.DownloadsBackdropAlpha,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(0f to TvColors.Ground, 0.5f to Color.Transparent, 1f to Color.Transparent))
        )
        // Bottom fade as in the mockup, plus a short top fade: the image starts under the top bar.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to TvColors.Ground,
                        0.2f to Color.Transparent,
                        0.5f to Color.Transparent,
                        1f to TvColors.Ground,
                    )
                )
        )
    }
}

@Composable
private fun DownloadsEmptyState(modifier: Modifier = Modifier) {
    TvEmptyState(
        icon = Icons.Default.Download,
        title = stringResource(R.string.tv_downloads_empty_title),
        message = stringResource(R.string.tv_downloads_empty_subtitle),
        modifier = modifier,
    )
}

@Composable
private fun DeleteDownloadDialog(
    itemTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    TvConfirmDialog(
        title = stringResource(R.string.delete_file),
        description = stringResource(R.string.delete_message, itemTitle),
        primaryText = stringResource(R.string.delete),
        secondaryText = stringResource(R.string.cancel),
        onPrimary = onConfirm,
        onSecondary = onDismiss,
        onDismiss = onDismiss,
        destructive = true,
        focusSecondaryFirst = true,
    )
}

private fun playDownloadedItem(
    item: DownloadItemUiModel,
    onPlayDownloaded: (DownloadItemUiModel) -> Unit,
) {
    if (item.state !is DownloadState.Downloaded) {
        Log.d(
            DebugTag,
            "playDownloadedItem ignored id=${item.episodeId} state=${item.state::class.simpleName}"
        )
        return
    }

    onPlayDownloaded(item)
    Log.d(
        DebugTag,
        "playDownloadedItem routed to compose player id=${item.episodeId} parentId=${item.parentId}"
    )
}

private fun resolveNextDownloadsFocusId(
    items: List<String>,
    removedItemId: String,
): String? {
    return resolveAdjacentFocusKey(keys = items, removedKey = removedItemId)
}
