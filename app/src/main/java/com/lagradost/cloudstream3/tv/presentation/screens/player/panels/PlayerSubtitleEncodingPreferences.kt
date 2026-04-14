package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.screens.player.normalizeSubtitleEncodingValue
import com.lagradost.cloudstream3.ui.player.CustomDecoder

internal data class TvPlayerSubtitleEncodingOption(
    val value: String?,
    val label: String,
)

internal data class TvPlayerSubtitleEncodingSelection(
    val value: String?,
    val label: String,
    val options: List<TvPlayerSubtitleEncodingOption>,
)

internal fun readSubtitleEncodingSelection(
    context: Context,
): TvPlayerSubtitleEncodingSelection {
    val settingsManager = PreferenceManager.getDefaultSharedPreferences(context)
    val optionLabels = context.resources.getStringArray(R.array.subtitles_encoding_list)
    val optionValues = context.resources.getStringArray(R.array.subtitles_encoding_values)
    val options = optionLabels.mapIndexed { index, label ->
        TvPlayerSubtitleEncodingOption(
            value = normalizeSubtitleEncodingValue(optionValues.getOrNull(index)),
            label = label,
        )
    }
    val selectedValue = normalizeSubtitleEncodingValue(
        settingsManager.getString(
            context.getString(R.string.subtitles_encoding_key),
            null,
        )
    )
    val selectedLabel = options.firstOrNull { option ->
        option.value == selectedValue
    }?.label ?: options.firstOrNull()?.label.orEmpty()

    return TvPlayerSubtitleEncodingSelection(
        value = selectedValue,
        label = selectedLabel,
        options = options,
    )
}

internal fun persistSubtitleEncodingSelection(
    context: Context,
    value: String?,
) {
    val normalizedValue = normalizeSubtitleEncodingValue(value)
    PreferenceManager.getDefaultSharedPreferences(context).edit {
        putString(
            context.getString(R.string.subtitles_encoding_key),
            normalizedValue.orEmpty(),
        )
    }
    CustomDecoder.updateForcedEncoding(context)
}
