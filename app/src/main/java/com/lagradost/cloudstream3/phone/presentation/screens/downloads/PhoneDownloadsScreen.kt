package com.lagradost.cloudstream3.phone.presentation.screens.downloads

import com.lagradost.cloudstream3.phone.presentation.common.navBarBottom
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.phone.presentation.common.CsIconButton
import com.lagradost.cloudstream3.phone.presentation.common.CsKicker
import com.lagradost.cloudstream3.phone.presentation.common.LocalPermanentMenu
import com.lagradost.cloudstream3.phone.presentation.common.CsProgress
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneDimens
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneShapes
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import com.lagradost.cloudstream3.tv.presentation.screens.downloads.DownloadItemUiModel
import com.lagradost.cloudstream3.tv.presentation.screens.downloads.DownloadState
import com.lagradost.cloudstream3.tv.presentation.screens.downloads.DownloadsViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.downloads.buildMetaLine
import com.lagradost.cloudstream3.tv.presentation.screens.downloads.buildProgressStatus

@Composable
fun PhoneDownloadsScreen(
    onOpenMenu: () -> Unit,
    onOpenDetails: (DownloadItemUiModel) -> Unit,
    onPlayDownloaded: (DownloadItemUiModel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<DownloadItemUiModel?>(null) }
    LaunchedEffect(context) { viewModel.bind(context) }

    val usedBytes = state.downloadedItems.sumOf { (it.state as? DownloadState.Downloaded)?.fileSizeBytes ?: 0L }

    Column(modifier.fillMaxSize().background(PhoneColors.Ground).statusBarsPadding()) {
        Row(Modifier.padding(start = 8.dp, end = 16.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!LocalPermanentMenu.current) CsIconButton(Icons.Default.Menu, stringResource(R.string.phone_open_menu), onOpenMenu, container = Color.Transparent) else androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.title_downloads), style = PhoneText.Level, color = PhoneColors.Text, modifier = Modifier.weight(1f))
            if (usedBytes > 0) Text(stringResource(R.string.phone_downloads_used, Formatter.formatShortFileSize(context, usedBytes)), style = PhoneText.Meta, color = PhoneColors.Text2)
        }

        if (state.downloadingItems.isEmpty() && state.downloadedItems.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.tv_downloads_empty_title), style = PhoneText.Section, color = PhoneColors.Text)
                Text(stringResource(R.string.tv_downloads_empty_subtitle), style = PhoneText.Body, color = PhoneColors.Text2, modifier = Modifier.padding(top = 8.dp))
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp + navBarBottom()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                section(R.string.downloading, "downloading", state.downloadingItems, onOpenDetails, onPlayDownloaded) { pendingDelete = it }
                section(R.string.downloaded, "downloaded", state.downloadedItems, onOpenDetails, onPlayDownloaded) { pendingDelete = it }
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = PhoneColors.Sheet,
            title = { Text(stringResource(R.string.phone_delete_download_title), style = PhoneText.Section, color = PhoneColors.Text) },
            text = {
                val label = buildString {
                    append(item.title)
                    if (item.seasonNumber != null && item.episodeNumber != null) append(" · S${item.seasonNumber}:E${item.episodeNumber}")
                    item.episodeName?.takeIf { it.isNotBlank() }?.let { append(" $it") }
                }
                val size = (item.state as? DownloadState.Downloaded)?.let { Formatter.formatShortFileSize(context, it.fileSizeBytes) }
                Text(
                    if (size != null) stringResource(R.string.phone_delete_download_message, label, size) else stringResource(R.string.delete_message, label),
                    style = PhoneText.Body, color = PhoneColors.Body,
                )
            },
            // Cancel is the default (first, filled) action, same as the TV dialog.
            confirmButton = { TextButton({ pendingDelete = null }) { Text(stringResource(R.string.cancel), style = PhoneText.Button, color = PhoneColors.Text) } },
            dismissButton = {
                TextButton({ viewModel.deleteItem(context, item); pendingDelete = null }) {
                    Text(stringResource(R.string.delete), style = PhoneText.Button, color = PhoneColors.Danger)
                }
            },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    headerRes: Int,
    keyPrefix: String,
    items: List<DownloadItemUiModel>,
    onOpenDetails: (DownloadItemUiModel) -> Unit,
    onPlay: (DownloadItemUiModel) -> Unit,
    onDelete: (DownloadItemUiModel) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = "$keyPrefix-header") {
        CsKicker("${stringResource(headerRes)} · ${items.size}", Modifier.padding(top = 10.dp, bottom = 6.dp))
    }
    itemsIndexed(items, key = { index, item -> "$keyPrefix-${item.id}#$index" }) { _, item ->
        DownloadRow(item, onOpenDetails, onPlay, onDelete)
    }
}

@Composable
private fun DownloadRow(
    item: DownloadItemUiModel,
    onOpenDetails: (DownloadItemUiModel) -> Unit,
    onPlay: (DownloadItemUiModel) -> Unit,
    onDelete: (DownloadItemUiModel) -> Unit,
) {
    val context = LocalContext.current
    val done = item.state is DownloadState.Downloaded
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(112.dp, 63.dp).clip(PhoneShapes.Poster).background(PhoneColors.Placeholder)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.phone_play_item, item.title)) { if (done) onPlay(item) else onOpenDetails(item) },
            contentAlignment = Alignment.Center,
        ) {
            val image = item.backdropUrl?.takeIf { it.isNotBlank() } ?: item.posterUrl
            if (!image.isNullOrBlank()) AsyncImage(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (done) Box(Modifier.size(32.dp).clip(PhoneShapes.Pill).background(Color(0x8C0E1015)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, null, tint = PhoneColors.Text, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f).clickable(role = Role.Button) { onOpenDetails(item) }) {
            Text(item.title, style = PhoneText.Row.copy(fontSize = 14.sp), color = PhoneColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(buildMetaLine(item, context), style = PhoneText.Meta.copy(fontSize = 12.sp), color = PhoneColors.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            val progress = when (val s = item.state) {
                is DownloadState.Downloading -> s.progress
                is DownloadState.Paused -> s.progress
                else -> null
            }
            if (progress != null) {
                CsProgress(progress, Modifier.padding(top = 7.dp))
                Text(buildProgressStatus(item, context), style = PhoneText.Meta.copy(fontSize = 12.sp), color = PhoneColors.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
        }
        CsIconButton(Icons.Default.Delete, stringResource(R.string.phone_delete_item, item.title), { onDelete(item) }, container = Color.Transparent, size = 44.dp)
    }
}
