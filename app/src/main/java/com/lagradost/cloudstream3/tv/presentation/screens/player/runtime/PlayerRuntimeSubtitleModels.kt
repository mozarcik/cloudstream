package com.lagradost.cloudstream3.tv.presentation.screens.player.runtime

import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.Tracks
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.player.SubtitleOrigin
import com.lagradost.cloudstream3.utils.SubtitleHelper.fromTagToLanguageName

internal data class PlayerEmbeddedSubtitleSnapshot(
    val subtitle: SubtitleData,
    val isSelected: Boolean,
    val selectionFlags: Int,
    val rawLabel: String?,
) {
    fun toDebugLogString(): String {
        return buildString {
            append("id=")
            append(subtitle.getId())
            append(", label=")
            append(rawLabel ?: "null")
            append(", language=")
            append(subtitle.languageCode ?: "null")
            append(", mime=")
            append(subtitle.mimeType)
            append(", selected=")
            append(isSelected)
            append(", selectionFlags=")
            append(selectionFlags)
            append(selectionFlags.toSelectionFlagsLabel())
        }
    }
}

internal fun extractEmbeddedSubtitleSnapshots(tracks: Tracks): List<PlayerEmbeddedSubtitleSnapshot> {
    val embeddedSubtitles = mutableListOf<PlayerEmbeddedSubtitleSnapshot>()
    tracks.groups.forEach { group ->
        if (group.type != C.TRACK_TYPE_TEXT) {
            return@forEach
        }
        val trackGroup = group.mediaTrackGroup
        for (trackIndex in 0 until trackGroup.length) {
            if (!group.isTrackSupported(trackIndex)) {
                continue
            }
            val format = trackGroup.getFormat(trackIndex)
            val formatId = format.id?.stripRuntimeTrackId() ?: continue
            val languageCode = format.language
                ?.takeIf { language -> language.isNotBlank() && !language.startsWith("-") }
                ?: continue
            val originalName = fromTagToLanguageName(languageCode)
                ?.takeIf { name -> name.isNotBlank() }
                ?: languageCode
            embeddedSubtitles += PlayerEmbeddedSubtitleSnapshot(
                subtitle = SubtitleData(
                    originalName = originalName,
                    nameSuffix = format.label ?: "",
                    url = formatId,
                    origin = SubtitleOrigin.EMBEDDED_IN_VIDEO,
                    mimeType = format.sampleMimeType ?: MimeTypes.APPLICATION_SUBRIP,
                    headers = emptyMap(),
                    languageCode = languageCode,
                ),
                isSelected = group.isTrackSelected(trackIndex),
                selectionFlags = format.selectionFlags,
                rawLabel = format.label,
            )
        }
    }
    return embeddedSubtitles
}

private fun Int.toSelectionFlagsLabel(): String {
    val flags = buildList {
        if (this@toSelectionFlagsLabel and C.SELECTION_FLAG_DEFAULT != 0) {
            add("default")
        }
        if (this@toSelectionFlagsLabel and C.SELECTION_FLAG_FORCED != 0) {
            add("forced")
        }
        if (this@toSelectionFlagsLabel and C.SELECTION_FLAG_AUTOSELECT != 0) {
            add("autoselect")
        }
    }
    return if (flags.isEmpty()) {
        ""
    } else {
        " (${flags.joinToString(separator = ",")})"
    }
}
