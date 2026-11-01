package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.AudioFile
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.utils.ExtractorLink

internal fun buildTrackPanelItems(
    currentLink: ExtractorLink,
    selectedAudioTrackIndex: Int,
): PlayerPanelItems {
    val trackItems = buildList {
        add(
            SidePanelMenuItem(
                id = "track_default",
                title = playerString(
                    resId = R.string.action_default,
                    fallback = "Default",
                ),
                selected = selectedAudioTrackIndex == -1,
                actionToken = TvPlayerPanelItemAction.SelectDefaultTrack,
            )
        )
        currentLink.audioTracks.forEachIndexed { index, audioTrack ->
            add(
                SidePanelMenuItem(
                    id = "track_$index",
                    title = formatAudioTrackLabel(index = index, track = audioTrack),
                    selected = selectedAudioTrackIndex == index,
                    actionToken = TvPlayerPanelItemAction.SelectTrack(index),
                )
            )
        }
    }
    val initialFocusedItemId = trackItems.firstOrNull { it.selected }?.id
        ?: trackItems.firstOrNull()?.id
    return PlayerPanelItems(
        items = trackItems,
        initialFocusedItemId = initialFocusedItemId,
    )
}

private fun formatAudioTrackLabel(
    index: Int,
    track: AudioFile,
): String {
    val host = runCatching {
        android.net.Uri.parse(track.url).host
    }.getOrNull()
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }

    return if (host == null) {
        "Track ${index + 1}"
    } else {
        "Track ${index + 1} ($host)"
    }
}
