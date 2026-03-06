package com.lagradost.cloudstream3.tv.presentation.screens.downloads

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.TvConfirmDialog
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.focus.rememberFocusRequesterMap
import com.lagradost.cloudstream3.tv.presentation.focus.resolveAdjacentFocusKey

private const val BackdropCrossfadeDurationMs = 300
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

    LaunchedEffect(Unit) {
        onScroll(true)
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DownloadsBackdrop(
            backdropUrl = focusedBackdrop,
            fallbackTitle = focusedItem?.title ?: stringResource(R.string.title_downloads),
            modifier = Modifier.fillMaxSize()
        )

        val horizontalMargin = 36.dp
        val contentMaxWidth = (maxWidth - horizontalMargin * 2f).coerceAtLeast(0.dp)
        val contentWidth = (maxWidth * 0.76f).coerceAtMost(contentMaxWidth)

        if (allItems.isEmpty()) {
            DownloadsEmptyState(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(contentWidth)
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 30.dp, bottom = 34.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(contentWidth)
            ) {
                item(key = "downloads_screen_title") {
                    Text(
                        text = stringResource(R.string.title_downloads),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                itemsIndexed(
                    items = allItems,
                    key = { _, item -> item.id }
                ) { _, item ->
                    DownloadItemCard(
                        item = item,
                        onCardClick = { onOpenDetails(item) },
                        onPlayClick = { playDownloadedItem(item, onPlayDownloaded) },
                        onDeleteClick = { pendingDeleteItem = item },
                        onCardFocused = { focusedItemId = item.id },
                        focusRequester = itemFocusRequesters.getValue(item.id),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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

@Composable
private fun DownloadsBackdrop(
    backdropUrl: String?,
    fallbackTitle: String,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val scrimColor = MaterialTheme.colorScheme.scrim
    val placeholderGradient = remember(surfaceVariantColor, surfaceColor) {
        listOf(
            surfaceVariantColor,
            surfaceColor.copy(alpha = 0.92f),
            surfaceColor,
        )
    }
    val horizontalOverlay = remember(scrimColor, surfaceColor) {
        listOf(
            scrimColor.copy(alpha = 0.78f),
            scrimColor.copy(alpha = 0.54f),
            surfaceColor.copy(alpha = 0.42f),
        )
    }
    val verticalOverlay = remember(scrimColor) {
        listOf(
            Color.Transparent,
            scrimColor.copy(alpha = 0.35f),
            scrimColor.copy(alpha = 0.55f),
        )
    }
    Box(
        modifier = modifier.background(surfaceColor)
    ) {
        Crossfade(
            targetState = backdropUrl,
            animationSpec = tween(durationMillis = BackdropCrossfadeDurationMs),
            label = "downloads_backdrop_fade"
        ) { currentBackdrop ->
            if (currentBackdrop.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = placeholderGradient
                            )
                        )
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentBackdrop)
                        .crossfade(true)
                        .build(),
                    contentDescription = fallbackTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(4.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = horizontalOverlay
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = verticalOverlay
                    )
                )
        )
    }
}

@Composable
private fun DownloadsEmptyState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(64.dp)
                .height(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.tv_downloads_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.tv_downloads_empty_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(460.dp)
        )
    }
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
        onDismiss = onDismiss
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
