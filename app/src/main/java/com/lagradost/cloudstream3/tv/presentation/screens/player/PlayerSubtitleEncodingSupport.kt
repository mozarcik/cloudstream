package com.lagradost.cloudstream3.tv.presentation.screens.player

import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.player.SubtitleOrigin

internal fun normalizeSubtitleEncodingValue(value: String?): String? {
    return value?.trim()?.takeIf { encoding ->
        encoding.isNotEmpty()
    }
}

internal fun shouldRestorePlaybackForSubtitleEncodingChange(
    selectedSubtitle: SubtitleData?,
    currentEncodingValue: String?,
    newEncodingValue: String?,
): Boolean {
    val normalizedCurrentValue = normalizeSubtitleEncodingValue(currentEncodingValue)
    val normalizedNewValue = normalizeSubtitleEncodingValue(newEncodingValue)
    if (normalizedCurrentValue == normalizedNewValue) {
        return false
    }

    val subtitle = selectedSubtitle ?: return false
    return subtitle.origin != SubtitleOrigin.EMBEDDED_IN_VIDEO
}
