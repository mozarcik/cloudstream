package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import android.content.Context

internal data class SettingsIntOption(
    val value: Int,
    val label: String
)

internal fun Context.settingsIntOptions(namesRes: Int, valuesRes: Int): List<SettingsIntOption> {
    val names = resources.getStringArray(namesRes)
    val values = resources.getIntArray(valuesRes)
    val optionCount = minOf(names.size, values.size)
    return buildList {
        for (index in 0 until optionCount) {
            add(SettingsIntOption(value = values[index], label = names[index]))
        }
    }
}

internal fun selectedIntLabel(options: List<SettingsIntOption>, value: Int): String {
    return options.firstOrNull { option -> option.value == value }?.label
        ?: value.toString()
}

internal fun itemEntry(
    stableKey: String,
    title: String,
    subtitle: String? = null,
    fallbackIconRes: Int? = null,
    initials: String? = null,
    compactRow: Boolean = false,
    showCheckmark: Boolean? = null,
    trailingColorArgb: Int? = null,
    nextScreenId: String? = null,
    action: (() -> Unit)? = null
): SettingsEntry {
    return SettingsEntry(
        title = title,
        subtitle = subtitle,
        fallbackIconRes = fallbackIconRes,
        initials = initials,
        compactRow = compactRow,
        showCheckmark = showCheckmark == true,
        type = if (showCheckmark != null) SettingsEntryType.Radio else SettingsEntryType.Item,
        trailingColorArgb = trailingColorArgb,
        stableKey = stableKey,
        nextScreenId = nextScreenId,
        action = action
    )
}

internal fun headerEntry(
    stableKey: String,
    title: String
): SettingsEntry {
    return SettingsEntry(
        title = title,
        stableKey = stableKey,
        type = SettingsEntryType.Header
    )
}

internal fun toggleEntry(
    stableKey: String,
    title: String,
    subtitle: String?,
    fallbackIconRes: Int? = null,
    value: Boolean,
    onValueChanged: (Boolean) -> Unit
): SettingsEntry {
    return SettingsEntry(
        title = title,
        subtitle = subtitle,
        fallbackIconRes = fallbackIconRes,
        stableKey = stableKey,
        type = SettingsEntryType.Toggle,
        toggleValue = value,
        onToggleChanged = onValueChanged
    )
}

internal fun sliderEntry(
    stableKey: String,
    title: String,
    subtitle: String?,
    fallbackIconRes: Int? = null,
    value: Int,
    range: IntRange,
    step: Int = 1,
    onValueChanged: (Int) -> Unit
): SettingsEntry {
    return SettingsEntry(
        title = title,
        subtitle = subtitle,
        fallbackIconRes = fallbackIconRes,
        stableKey = stableKey,
        type = SettingsEntryType.Slider,
        sliderValue = value,
        sliderRange = range,
        sliderStep = step,
        valueText = value.toString(),
        onSliderChanged = onValueChanged
    )
}
