package com.lagradost.cloudstream3.phone.presentation.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.designsystem.CsColors
import com.lagradost.cloudstream3.phone.presentation.common.CsButton
import com.lagradost.cloudstream3.phone.presentation.common.CsIconButton
import com.lagradost.cloudstream3.phone.presentation.theme.LocalPhoneAccent
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerSubtitleSyncController
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerOverlayStateHolder
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.formatSubtitleDelayValue
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelItemAction
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSubtitlePanelScreen
import com.lagradost.cloudstream3.tv.presentation.screens.player.runtime.PlayerRuntimeTracksStateHolder
import com.lagradost.cloudstream3.tv.presentation.screens.player.subtitlesync.rememberPlayerSubtitleSyncStateHolder
import kotlinx.coroutines.delay

private val PanelShape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
private val PanelColor = Color(0xFF1B1F27)

/** Right-side panels over the dimmed video (mockups "PlayerSubtitles" / "PlayerSync"). One panel at a time. */
@Composable
internal fun PhonePlayerPanels(
    sourceName: String,
    panelsState: TvPlayerPanelsUiState,
    tracks: PlayerRuntimeTracksStateHolder,
    overlayState: PlayerOverlayStateHolder,
    exoPlayer: ExoPlayer,
    subtitleSync: TvPlayerSubtitleSyncController,
    selectedSubtitle: SubtitleData?,
    onClosePanel: () -> Unit,
    onSubtitlesBackPressed: () -> Boolean,
    onItemAction: (TvPlayerPanelItemAction) -> Unit,
    onRetrySource: (Int) -> Unit,
    onRuntimeSubtitleDelayChanged: (Long) -> Unit,
) {
    when {
        tracks.subtitleSyncPanelVisible -> SyncPanel(exoPlayer, subtitleSync, selectedSubtitle != null, { tracks.subtitleSyncPanelVisible = false }, onRuntimeSubtitleDelayChanged)
        tracks.audioPanelVisible -> PanelFrame(stringResource(R.string.tv_player_control_audio), onClose = { tracks.audioPanelVisible = false }) {
            val rows = listOf(SidePanelMenuItem("default", stringResource(R.string.action_default), selected = tracks.selectedAudioTrackId == null)) +
                tracks.audioTracks.map { SidePanelMenuItem(it.selectionId, it.title, selected = it.selectionId == tracks.selectedAudioTrackId, supportingTexts = it.supportingTexts) }
            ItemList(rows) { item ->
                if (item.id == "default") tracks.selectAudioTrack(null) else tracks.audioTracks.firstOrNull { it.selectionId == item.id }?.let(tracks::selectAudioTrack)
                tracks.audioPanelVisible = false
            }
        }
        tracks.videoPanelVisible -> PanelFrame(stringResource(R.string.tv_player_control_video), onClose = { tracks.videoPanelVisible = false }) {
            val rows = listOf(SidePanelMenuItem("default", stringResource(R.string.action_default), selected = tracks.selectedVideoTrackId == null)) +
                tracks.videoTracks.map { SidePanelMenuItem(it.selectionId, it.title, selected = it.selectionId == tracks.selectedVideoTrackId, supportingTexts = it.supportingTexts) }
            ItemList(rows) { item ->
                if (item.id == "default") tracks.selectVideoTrack(null) else tracks.videoTracks.firstOrNull { it.selectionId == item.id }?.let(tracks::selectVideoTrack)
                tracks.videoPanelVisible = false
            }
        }
        panelsState.activePanel == TvPlayerSidePanel.Sources -> PanelFrame(stringResource(R.string.sources), onClose = onClosePanel) {
            ItemList(panelsState.sourceItems) { item -> (item.actionToken as? TvPlayerPanelItemAction)?.let(onItemAction) }
        }
        panelsState.activePanel == TvPlayerSidePanel.Tracks -> PanelFrame(stringResource(R.string.audio_tracks), onClose = onClosePanel) {
            ItemList(panelsState.trackItems) { item -> (item.actionToken as? TvPlayerPanelItemAction)?.let(onItemAction) }
        }
        panelsState.activePanel == TvPlayerSidePanel.Subtitles -> {
            val screen = panelsState.subtitlePanelScreen
            val title = stringResource(
                when (screen) {
                    TvPlayerSubtitlePanelScreen.Main -> R.string.player_subtitles_settings
                    TvPlayerSubtitlePanelScreen.EncodingSelection -> R.string.subtitles_encoding
                    TvPlayerSubtitlePanelScreen.OnlineSearch -> R.string.player_load_subtitles_online
                    TvPlayerSubtitlePanelScreen.OnlineLanguageSelection -> R.string.subs_subtitle_languages
                },
            )
            PanelFrame(
                title = title,
                onClose = onClosePanel,
                onBack = if (screen == TvPlayerSubtitlePanelScreen.Main) null else ({ if (!onSubtitlesBackPressed()) onClosePanel() }),
            ) {
                val items = when (screen) {
                    TvPlayerSubtitlePanelScreen.Main -> panelsState.subtitleItems
                    TvPlayerSubtitlePanelScreen.EncodingSelection -> panelsState.subtitleEncodingItems
                    else -> panelsState.subtitleOnlineItems
                }
                ItemList(items, onInlineChange = { token, value ->
                    if (token == TvPlayerPanelItemAction.EditOnlineSubtitlesQuery) onItemAction(TvPlayerPanelItemAction.UpdateOnlineSubtitlesQuery(value))
                }) { item -> (item.actionToken as? TvPlayerPanelItemAction)?.let(onItemAction) }
            }
        }
    }

    overlayState.sourceErrorDialogEffect?.let { effect ->
        AlertDialog(
            onDismissRequest = { overlayState.sourceErrorDialogEffect = null },
            containerColor = PanelColor,
            title = { Text(effect.dialog.sourceLabel, style = PhoneText.Section, color = PhoneColors.Text) },
            text = { Text(effect.dialog.message, style = PhoneText.Body, color = PhoneColors.Body) },
            confirmButton = {
                TextButton({ overlayState.sourceErrorDialogEffect = null; onRetrySource(effect.dialog.sourceIndex) }) {
                    Text(stringResource(R.string.phone_player_source_error_retry), style = PhoneText.Button, color = PhoneColors.Text)
                }
            },
            dismissButton = { TextButton({ overlayState.sourceErrorDialogEffect = null }) { Text(stringResource(R.string.cancel), style = PhoneText.Button, color = PhoneColors.Text2) } },
        )
    }
}

@Composable
private fun PanelFrame(
    title: String,
    onClose: () -> Unit,
    widthDp: Int = 360,
    onBack: (() -> Unit)? = null,
    trailing: String? = null,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0x73000000)).clickable(indication = null, interactionSource = null, onClick = onClose)) {
        Column(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.End)).width(widthDp.dp).clip24().background(PanelColor)
                .clickable(indication = null, interactionSource = null) {}.padding(top = 8.dp),
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = if (onBack == null) 20.dp else 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) CsIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.phone_back), onBack, container = Color.Transparent, size = 44.dp)
                Text(title, style = PhoneText.Section.copy(fontSize = 18.sp), color = PhoneColors.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (trailing != null) Text(trailing, style = PhoneText.Button.copy(fontSize = 16.sp), color = LocalPhoneAccent.current, modifier = Modifier.padding(end = 8.dp))
                CsIconButton(Icons.Default.Close, stringResource(R.string.phone_player_close_panel), onClose, container = Color.Transparent, size = 44.dp)
            }
            content()
        }
    }
}

private fun Modifier.clip24(): Modifier = this.then(Modifier.clip(PanelShape))

@Composable
private fun ItemList(
    items: List<SidePanelMenuItem>,
    onInlineChange: (Any?, String) -> Unit = { _, _ -> },
    onClick: (SidePanelMenuItem) -> Unit,
) {
    // Language groups (subtitles) start collapsed except the one with the selected track; the header toggles it.
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(items) {
        val keys = items.mapNotNull { it.expandableGroupKey }.toSet()
        items.forEach { item -> item.expandableGroupKey?.let { if (!expanded.containsKey(it)) expanded[it] = item.chevronExpanded } }
        expanded.keys.toList().filterNot(keys::contains).forEach(expanded::remove)
    }
    val visible = items.filter { item ->
        item.isVisible && (item.parentGroupKey?.let { expanded[it] ?: true } ?: true)
    }
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(visible, key = { index, item -> "${item.id}#$index" }) { _, item ->
            when {
                item.isSectionHeader -> Text(
                    item.title.uppercase(), style = PhoneText.Kicker, color = PhoneColors.Label,
                    modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                )
                item.inlineTextField != null -> BasicTextField(
                    value = item.inlineTextField.value,
                    onValueChange = { onInlineChange(item.inlineTextField.valueChangeToken, it) },
                    singleLine = true,
                    textStyle = PhoneText.Row.copy(color = PhoneColors.Text),
                    cursorBrush = SolidColor(PhoneColors.Text),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).background(Color(0x14FFFFFF), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                            if (item.inlineTextField.value.isEmpty()) Text(item.inlineTextField.placeholder, style = PhoneText.Row, color = PhoneColors.Label)
                            inner()
                        }
                    },
                )
                else -> {
                    val groupKey = item.expandableGroupKey
                    val isOpen = groupKey?.let { expanded[it] ?: item.chevronExpanded } ?: item.chevronExpanded
                    // Metadata stays on one line ("720p • FrameX • M3U8"), not one line per field.
                    val meta = item.supportingTexts.filter { it.isNotBlank() }.joinToString(" • ")
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp)
                            .background(if (item.selected) Color(0x14FFFFFF) else Color.Transparent)
                            .clickable(enabled = item.enabled, role = Role.Button) {
                                if (groupKey != null) expanded[groupKey] = !isOpen else { onClick(item); item.onClick() }
                            }
                            .padding(start = if (item.parentGroupKey != null) 32.dp else 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.title, style = PhoneText.Row.copy(fontSize = 14.sp),
                                color = if (!item.enabled) PhoneColors.Label else item.titleTint ?: PhoneColors.Text,
                                maxLines = item.titleMaxLines.coerceAtLeast(1), overflow = TextOverflow.Ellipsis,
                            )
                            if (meta.isNotEmpty()) Text(meta, style = PhoneText.Meta.copy(fontSize = 12.sp), color = PhoneColors.Text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        item.trailingText?.let { Text(it, style = PhoneText.Meta, color = PhoneColors.Text2) }
                        if (item.selected && groupKey == null) Icon(Icons.Default.Check, null, tint = LocalPhoneAccent.current, modifier = Modifier.size(20.dp))
                        if (item.showChevron) Icon(if (isOpen) Icons.Default.ExpandMore else Icons.Default.ChevronRight, null, tint = PhoneColors.Label)
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncPanel(
    player: ExoPlayer,
    controller: TvPlayerSubtitleSyncController,
    hasSubtitle: Boolean,
    onClose: () -> Unit,
    onDelayChanged: (Long) -> Unit,
) {
    val holder = rememberPlayerSubtitleSyncStateHolder(player, controller, hasSubtitle, onDelayChanged)
    LaunchedEffect(controller, hasSubtitle) {
        holder.subtitleDelayMs = controller.subtitleDelayMs()
        while (hasSubtitle) {
            holder.debugPollCues()
            holder.playerPositionMs = player.currentPosition.coerceAtLeast(0L)
            delay(500)
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(holder.activeCueIndex) { if (holder.activeCueIndex >= 0) listState.animateScrollToItem(holder.activeCueIndex) }
    fun nudge(deltaMs: Long) = holder.updateSubtitleDelay(holder.subtitleDelayMs + deltaMs)

    PanelFrame(stringResource(R.string.tv_player_control_sync), onClose, widthDp = 560, trailing = stringResource(R.string.phone_player_sync_delay, formatSubtitleDelayValue(holder.subtitleDelayMs))) {
        Row(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                itemsIndexed(holder.cueUiModels, key = { index, _ -> index }) { index, cue ->
                    val now = index == holder.activeCueIndex
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp).background(if (now) Color(0x1A9CC1FF) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable(enabled = holder.controlsEnabled, role = Role.Button) { holder.syncToCue(cue.cue) }.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(cue.timestampRange.substringBefore(' '), style = PhoneText.Meta.copy(fontSize = 12.sp), color = PhoneColors.Text2)
                        Text(cue.joinedText, style = PhoneText.Row.copy(fontSize = 14.sp), color = PhoneColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
            }
            Column(Modifier.width(216.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CsButton(stringResource(R.string.phone_player_sync_minus_1s), { nudge(-1000) }, enabled = holder.controlsEnabled, modifier = Modifier.weight(1f))
                    CsButton(stringResource(R.string.phone_player_sync_plus_1s), { nudge(1000) }, enabled = holder.controlsEnabled, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CsButton(stringResource(R.string.phone_player_sync_minus_100ms), { nudge(-100) }, enabled = holder.controlsEnabled, modifier = Modifier.weight(1f))
                    CsButton(stringResource(R.string.phone_player_sync_plus_100ms), { nudge(100) }, enabled = holder.controlsEnabled, modifier = Modifier.weight(1f))
                }
                CsButton(stringResource(R.string.phone_player_sync_reset), { holder.updateSubtitleDelay(0L) }, enabled = holder.controlsEnabled, modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.phone_player_sync_hint), style = PhoneText.Meta.copy(fontSize = 12.sp), color = PhoneColors.Text2, modifier = Modifier.padding(4.dp))
            }
        }
    }
}
