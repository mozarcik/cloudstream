package com.lagradost.cloudstream3.tv.presentation.screens.player.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.MenuListSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelSelectionIndicatorStyle

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
        showSelectionRadio = true,
        selectionIndicatorStyle = SidePanelSelectionIndicatorStyle.Checkmark,
        initialFocusedItemId = initialFocusedItemId,
        closeOnLeftPress = false,
        closeOnFocusExit = false,
        enableContentAnimation = false,
        enableItemAnimations = false,
    )
}

@Composable
internal fun RuntimeVideoTracksSidePanel(
    tracks: List<PlayerVideoTrackOption>,
    selectedTrackId: String?,
    onCloseRequested: () -> Unit,
    onSelectDefault: () -> Unit,
    onSelectTrack: (PlayerVideoTrackOption) -> Unit,
) {
    val defaultItemId = "runtime_video_track_default"
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
        items = items,
        showSelectionRadio = true,
        selectionIndicatorStyle = SidePanelSelectionIndicatorStyle.Checkmark,
        initialFocusedItemId = initialFocusedItemId,
        closeOnLeftPress = false,
        closeOnFocusExit = false,
        enableContentAnimation = false,
        enableItemAnimations = false,
    )
}
