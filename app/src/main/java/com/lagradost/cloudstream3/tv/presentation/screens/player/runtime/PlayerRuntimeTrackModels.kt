package com.lagradost.cloudstream3.tv.presentation.screens.player.runtime

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import java.util.Locale

internal data class PlayerAudioTrackOption(
    val selectionId: String,
    val title: String,
    val supportingTexts: List<String>,
    val language: String?,
    val trackGroup: androidx.media3.common.TrackGroup,
    val trackIndex: Int,
    val isSelected: Boolean,
)

internal data class PlayerVideoTrackOption(
    val selectionId: String,
    val title: String,
    val supportingTexts: List<String>,
    val width: Int,
    val height: Int,
    val trackGroup: androidx.media3.common.TrackGroup,
    val trackIndex: Int,
    val isSelected: Boolean,
)

private data class RuntimeAudioTrackLabel(
    val title: String,
    val supportingTexts: List<String>,
)

private data class RuntimeVideoTrackLabel(
    val title: String,
    val supportingTexts: List<String>,
)

private data class RuntimeVideoTrackCandidate(
    val selectionId: String,
    val format: Format,
    val trackGroup: androidx.media3.common.TrackGroup,
    val trackIndex: Int,
    val isSelected: Boolean,
)

internal fun extractRuntimeAudioTracks(tracks: Tracks): List<PlayerAudioTrackOption> {
    val runtimeTracks = mutableListOf<PlayerAudioTrackOption>()
    var ordinal = 0
    tracks.groups.forEachIndexed { groupIndex, group ->
        if (group.type != C.TRACK_TYPE_AUDIO) {
            return@forEachIndexed
        }
        val trackGroup = group.mediaTrackGroup
        for (trackIndex in 0 until trackGroup.length) {
            if (!group.isTrackSupported(trackIndex)) {
                continue
            }
            val format = trackGroup.getFormat(trackIndex)
            val selectionId = "runtime_track_${groupIndex}_${trackIndex}_${format.id.orEmpty()}"
            val formattedLabel = formatRuntimeAudioTrackLabel(index = ordinal, format = format)
            runtimeTracks += PlayerAudioTrackOption(
                selectionId = selectionId,
                title = formattedLabel.title,
                supportingTexts = formattedLabel.supportingTexts,
                language = format.language,
                trackGroup = trackGroup,
                trackIndex = trackIndex,
                isSelected = group.isTrackSelected(trackIndex),
            )
            ordinal += 1
        }
    }
    return runtimeTracks
}

internal fun extractRuntimeVideoTracks(tracks: Tracks): List<PlayerVideoTrackOption> {
    val runtimeTracks = mutableListOf<RuntimeVideoTrackCandidate>()
    tracks.groups.forEachIndexed { groupIndex, group ->
        if (group.type != C.TRACK_TYPE_VIDEO) {
            return@forEachIndexed
        }
        val trackGroup = group.mediaTrackGroup
        for (trackIndex in 0 until trackGroup.length) {
            if (!group.isTrackSupported(trackIndex)) {
                continue
            }
            val format = trackGroup.getFormat(trackIndex)
            runtimeTracks += RuntimeVideoTrackCandidate(
                selectionId = "runtime_video_track_${groupIndex}_${trackIndex}_${format.id.orEmpty()}",
                format = format,
                trackGroup = trackGroup,
                trackIndex = trackIndex,
                isSelected = group.isTrackSelected(trackIndex),
            )
        }
    }
    val sortedTracks = runtimeTracks.sortedWith(
        compareByDescending<RuntimeVideoTrackCandidate> { candidate ->
            candidate.format.height.takeIf { it > 0 } ?: Int.MIN_VALUE
        }.thenByDescending { candidate ->
            candidate.format.width.takeIf { it > 0 } ?: Int.MIN_VALUE
        }
    )
    return sortedTracks.mapIndexed { index, candidate ->
        val formattedLabel = formatRuntimeVideoTrackLabel(index = index, format = candidate.format)
        PlayerVideoTrackOption(
            selectionId = candidate.selectionId,
            title = formattedLabel.title,
            supportingTexts = formattedLabel.supportingTexts,
            width = candidate.format.width,
            height = candidate.format.height,
            trackGroup = candidate.trackGroup,
            trackIndex = candidate.trackIndex,
            isSelected = candidate.isSelected,
        )
    }
}

internal fun applyRuntimeAudioTrackSelection(
    player: ExoPlayer,
    track: PlayerAudioTrackOption?,
) {
    val newParameters = player.trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        .apply {
            if (track != null) {
                setOverrideForType(TrackSelectionOverride(track.trackGroup, track.trackIndex))
                track.language?.takeIf { it.isNotBlank() }?.let(::setPreferredAudioLanguage)
            } else {
                setPreferredAudioLanguage(null)
            }
        }
        .build()
    player.trackSelectionParameters = newParameters
}

internal fun applyRuntimeVideoTrackSelection(
    player: ExoPlayer,
    track: PlayerVideoTrackOption?,
) {
    val maxWidth = track?.width?.takeIf { it > 0 } ?: Int.MAX_VALUE
    val maxHeight = track?.height?.takeIf { it > 0 } ?: Int.MAX_VALUE
    val newParameters = player.trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
        .setMaxVideoSize(maxWidth, maxHeight)
        .apply {
            if (track != null) {
                setOverrideForType(TrackSelectionOverride(track.trackGroup, track.trackIndex))
            }
        }
        .build()
    player.trackSelectionParameters = newParameters
}

private fun formatRuntimeAudioTrackLabel(
    index: Int,
    format: Format,
): RuntimeAudioTrackLabel {
    val languageLabel = format.language
        ?.takeIf { it.isNotBlank() && !it.equals("und", ignoreCase = true) }
        ?.let(::languageDisplayName)
        ?: format.label?.takeIf { it.isNotBlank() }
        ?: "Audio ${index + 1}"
    val codecLabel = format.sampleMimeType
        ?.substringAfter('/')
        ?.uppercase(Locale.ROOT)
    val channelsLabel = when (format.channelCount) {
        1 -> "MONO"
        2 -> "STEREO"
        6 -> "5.1"
        8 -> "7.1"
        else -> format.channelCount.takeIf { it > 0 }?.let { "$it CH" }
    }
    return RuntimeAudioTrackLabel(
        title = languageLabel,
        supportingTexts = listOfNotNull(codecLabel, channelsLabel),
    )
}

private fun formatRuntimeVideoTrackLabel(
    index: Int,
    format: Format,
): RuntimeVideoTrackLabel {
    val resolutionLabel = if (format.width > 0 && format.height > 0) {
        "${format.width}x${format.height}"
    } else {
        null
    }
    val codecLabel = format.sampleMimeType
        ?.substringAfter('/')
        ?.uppercase(Locale.ROOT)
    val title = format.label?.takeIf { it.isNotBlank() }
        ?: resolutionLabel
        ?: "Video ${index + 1}"
    return RuntimeVideoTrackLabel(
        title = title,
        supportingTexts = listOfNotNull(
            resolutionLabel?.takeUnless { it == title },
            codecLabel,
        ),
    )
}

private fun languageDisplayName(languageTag: String): String {
    val locale = Locale.forLanguageTag(languageTag)
    val languageCode = locale.language
    if (languageCode.isNullOrBlank()) {
        return languageTag
    }
    val localized = locale.getDisplayLanguage(Locale.getDefault())
    return localized.takeIf { it.isNotBlank() } ?: languageTag
}
