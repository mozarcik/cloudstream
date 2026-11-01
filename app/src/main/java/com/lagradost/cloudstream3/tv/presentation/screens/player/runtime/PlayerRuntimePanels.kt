package com.lagradost.cloudstream3.tv.presentation.screens.player.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.MenuListSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelSelectionIndicatorStyle
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

@Composable
internal fun RuntimeAudioTracksSidePanel(
    tracks: List<PlayerAudioTrackOption>,
    selectedTrackId: String?,
    onCloseRequested: () -> Unit,
    onSelectDefault: () -> Unit,
    onSelectTrack: (PlayerAudioTrackOption) -> Unit,
) {
    val defaultItemId = "runtime_track_default"
    val defaultTitle = stringResource(R.string.action_default)
    val items = remember(defaultTitle, tracks, selectedTrackId) {
        buildList {
            add(
                SidePanelMenuItem(
                    id = defaultItemId,
                    title = defaultTitle,
                    selected = selectedTrackId == null,
                    onClick = onSelectDefault,
                )
            )
            tracks.forEach { track ->
                add(
                    SidePanelMenuItem(
                        id = track.selectionId,
                        title = track.title,
                        titleTextStyle = TvTextStyles.PlayerPanelRow,
                        supportingTexts = track.supportingTexts,
                        selected = selectedTrackId == track.selectionId,
                        onClick = {
                            onSelectTrack(track)
                        },
                    )
                )
            }
        }
    }
    val initialFocusedItemId = selectedTrackId?.takeIf { selectedId ->
        items.any { item -> item.id == selectedId }
    } ?: defaultItemId

    MenuListSidePanel(
        visible = true,
        onCloseRequested = onCloseRequested,
        title = stringResource(R.string.audio_tracks),
        items = items,
        titleStyle = TvTextStyles.PlayerPanelTitle,
        topPadding = TvDimens.PlayerPanelTopPadding,
        rowHeight = TvDimens.PlayerPanelRowHeight,
        showSelectionRadio = true,
        selectionIndicatorStyle = SidePanelSelectionIndicatorStyle.TrailingRadio,
        initialFocusedItemId = initialFocusedItemId,
        closeOnLeftPress = false,
        closeOnFocusExit = false,
        enableContentAnimation = false,
        enableItemAnimations = false,
    )
}

@Composable
internal fun RuntimeVideoTracksSidePanel(
    sourceName: String,
    tracks: List<PlayerVideoTrackOption>,
    selectedTrackId: String?,
    onCloseRequested: () -> Unit,
    onSelectDefault: () -> Unit,
    onSelectTrack: (PlayerVideoTrackOption) -> Unit,
) {
    val defaultItemId = "runtime_video_track_default"
    val defaultTitle = stringResource(R.string.action_default)
    val autoQualityText = stringResource(R.string.tv_player_video_auto_quality)
    val adaptiveSubtitle = if (sourceName.isBlank()) {
        stringResource(R.string.tv_player_video_adaptive)
    } else {
        stringResource(R.string.tv_player_video_adaptive_source, sourceName)
    }
    val items = remember(defaultTitle, autoQualityText, tracks, selectedTrackId) {
        buildList {
            add(
                SidePanelMenuItem(
                    id = defaultItemId,
                    title = defaultTitle,
                    supportingTexts = listOf(autoQualityText),
                    selected = selectedTrackId == null,
                    onClick = onSelectDefault,
                )
            )
            tracks.forEach { track ->
                add(
                    SidePanelMenuItem(
                        id = track.selectionId,
                        title = track.title,
                        titleTextStyle = TvTextStyles.PlayerPanelRow,
                        supportingTexts = track.supportingTexts,
                        selected = selectedTrackId == track.selectionId,
                        onClick = {
                            onSelectTrack(track)
                        },
                    )
                )
            }
        }
    }
    val initialFocusedItemId = selectedTrackId?.takeIf { selectedId ->
        items.any { item -> item.id == selectedId }
    } ?: defaultItemId

    MenuListSidePanel(
        visible = true,
        onCloseRequested = onCloseRequested,
        title = stringResource(R.string.video_tracks),
        subtitle = adaptiveSubtitle,
        items = items,
        titleStyle = TvTextStyles.PlayerPanelTitle,
        topPadding = TvDimens.PlayerPanelTopPadding,
        rowHeight = TvDimens.PlayerPanelRowHeight,
        showSelectionRadio = true,
        selectionIndicatorStyle = SidePanelSelectionIndicatorStyle.TrailingRadio,
        initialFocusedItemId = initialFocusedItemId,
        closeOnLeftPress = false,
        closeOnFocusExit = false,
        enableContentAnimation = false,
        enableItemAnimations = false,
    )
}
