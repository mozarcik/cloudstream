package com.lagradost.cloudstream3.tv.presentation.screens.downloads

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.download.DOWNLOAD_ACTION_PLAY_FILE
import com.lagradost.cloudstream3.ui.download.DownloadButtonSetup
import com.lagradost.cloudstream3.ui.download.DownloadClickEvent
import com.lagradost.cloudstream3.utils.VideoDownloadHelper
import com.lagradost.cloudstream3.utils.VideoDownloadManager

private const val BackdropCrossfadeDurationMs = 300

@Composable
fun DownloadsScreen(
    onOpenDetails: (DownloadItemUiModel) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val allItems = remember(uiState.downloadingItems, uiState.downloadedItems) {
        uiState.downloadingItems + uiState.downloadedItems
    }

    var focusedItemId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<DownloadItemUiModel?>(null) }

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
                ) { index, item ->
                    DownloadItemCard(
                        item = item,
                        onCardClick = { onOpenDetails(item) },
                        onPlayClick = { playDownloadedItem(item) },
                        onDeleteClick = { pendingDeleteItem = item },
                        onCardFocused = { focusedItemId = item.id },
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
                    VideoDownloadManager.deleteFilesAndUpdateSettings(
                        context = context,
                        ids = setOf(itemToDelete.episodeId),
                        scope = coroutineScope
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
    Box(
        modifier = modifier.background(Color(0xFF0D1118))
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
                                colors = listOf(
                                    Color(0xFF17202E),
                                    Color(0xFF111724),
                                    Color(0xFF0C1016)
                                )
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
                        colors = listOf(
                            Color.Black.copy(alpha = 0.78f),
                            Color.Black.copy(alpha = 0.54f),
                            Color.Black.copy(alpha = 0.42f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.55f)
                        )
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
    val dismissFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        dismissFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            colors = SurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
            ),
            modifier = Modifier.width(620.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.delete_file),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                Text(
                    text = itemTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stringResource(R.string.delete_message).format(itemTitle),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .focusRequester(dismissFocusRequester)
                            .weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.delete),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}

private fun playDownloadedItem(item: DownloadItemUiModel) {
    if (item.state !is DownloadState.Downloaded) return

    DownloadButtonSetup.handleDownloadClick(
        DownloadClickEvent(
            action = DOWNLOAD_ACTION_PLAY_FILE,
            data = VideoDownloadHelper.DownloadEpisodeCached(
                name = item.episodeName,
                poster = item.posterUrl,
                episode = item.episodeNumber ?: 0,
                season = item.seasonNumber,
                parentId = item.parentId,
                score = null,
                description = item.description,
                cacheTime = item.startedAtMillis,
                id = item.episodeId,
            )
        )
    )
}
