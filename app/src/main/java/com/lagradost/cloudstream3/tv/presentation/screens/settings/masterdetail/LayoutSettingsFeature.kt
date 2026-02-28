package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import android.app.Activity
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getActivity
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.SearchQuality
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.home.HomeChildItemAdapter
import com.lagradost.cloudstream3.ui.home.ParentItemAdapter
import com.lagradost.cloudstream3.ui.search.SearchAdapter
import com.lagradost.cloudstream3.ui.search.SearchResultBuilder
import com.lagradost.cloudstream3.ui.settings.Globals.updateTv
import com.lagradost.cloudstream3.tv.compat.theme.resolveLegacyPrimaryPreviewColor
import com.lagradost.cloudstream3.utils.UIHelper.toPx

private object LayoutSettingsScreenIds {
    const val LayoutMain = "settings_ui"
    const val LayoutPrimaryColor = "settings_ui/primary_color"
    const val LayoutTheme = "settings_ui/theme"
    const val LayoutAppLayout = "settings_ui/app_layout"
    const val LayoutPosterUi = "settings_ui/poster_ui"
    const val LayoutConfirmExit = "settings_ui/confirm_exit"
    const val LayoutSearchQuality = "settings_ui/search_quality"
}

private object LayoutSettingsDefaults {
    const val AdvancedSearchKey = "advanced_search"
    const val SearchSuggestionsKey = "search_suggestions_enabled"
}

private data class LayoutIntOption(
    val value: Int,
    val label: String
)

private data class LayoutStringOption(
    val value: String,
    val label: String
)

@Composable
fun rememberLayoutSettingsFeature(
    layoutTitle: String
): LayoutSettingsFeature {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context, layoutTitle) {
        LayoutSettingsFeature(
            context = context,
            layoutTitle = layoutTitle
        )
    }
}

class LayoutSettingsFeature(
    private val context: android.content.Context,
    private val layoutTitle: String
) {
    private val settingsManager by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val primaryColorKey by lazy { context.getString(R.string.primary_color_key) }
    private val appThemeKey by lazy { context.getString(R.string.app_theme_key) }
    private val appLayoutKey by lazy { context.getString(R.string.app_layout_key) }
    private val bottomTitleKey by lazy { context.getString(R.string.bottom_title_key) }
    private val overscanKey by lazy { context.getString(R.string.overscan_key) }
    private val posterSizeKey by lazy { context.getString(R.string.poster_size_key) }
    private val showTrailersKey by lazy { context.getString(R.string.show_trailers_key) }
    private val showKitsuPostersKey by lazy { context.getString(R.string.show_kitsu_posters_key) }
    private val showCastInDetailsKey by lazy { context.getString(R.string.show_cast_in_details_key) }
    private val showFillersKey by lazy { context.getString(R.string.show_fillers_key) }
    private val randomButtonKey by lazy { context.getString(R.string.random_button_key) }
    private val confirmExitKey by lazy { context.getString(R.string.confirm_exit_key) }
    private val filterSearchQualityKey by lazy { context.getString(R.string.pref_filter_search_quality_key) }

    val staticScreens: List<SettingsScreen> = listOf(
        LayoutMainScreen(),
        PrimaryColorScreen(),
        AppThemeScreen(),
        AppLayoutScreen(),
        PosterUiScreen(),
        ConfirmExitScreen(),
        SearchQualityScreen()
    )

    private inner class LayoutMainScreen : SettingsScreen {
        override val id: String = LayoutSettingsScreenIds.LayoutMain
        override val title: String = layoutTitle

        override suspend fun load(): List<SettingsEntry> {
            val appLayoutOptions = appLayoutOptions()
            val currentLayout = settingsManager.getInt(appLayoutKey, -1)

            val appThemeOptions = appThemeOptions()
            val currentTheme = settingsManager.getString(
                appThemeKey,
                appThemeOptions.firstOrNull()?.value
            )

            val primaryColorOptions = primaryColorOptions()
            val currentPrimaryColor = settingsManager.getString(
                primaryColorKey,
                primaryColorOptions.firstOrNull()?.value
            )

            val confirmExitOptions = confirmExitOptions()
            val confirmExit = settingsManager.getInt(confirmExitKey, -1)

            val overscan = settingsManager.getInt(overscanKey, 0).coerceIn(0, 100)
            val posterSize = settingsManager.getInt(posterSizeKey, 0).coerceIn(0, 15)

            return buildList {
                add(
                    headerEntry(
                        stableKey = "layout_header_looks",
                        title = context.getString(R.string.pref_category_looks)
                    )
                )
                add(
                    itemEntry(
                        stableKey = "layout_primary_color",
                        title = context.getString(R.string.primary_color_settings),
                        subtitle = selectedStringLabel(
                            options = primaryColorOptions,
                            value = currentPrimaryColor
                        ),
                        fallbackIconRes = R.drawable.ic_baseline_color_lens_24,
                        nextScreenId = LayoutSettingsScreenIds.LayoutPrimaryColor
                    )
                )
                add(
                    itemEntry(
                        stableKey = "layout_theme",
                        title = context.getString(R.string.app_theme_settings),
                        subtitle = selectedStringLabel(
                            options = appThemeOptions,
                            value = currentTheme
                        ),
                        fallbackIconRes = R.drawable.ic_baseline_color_lens_24,
                        nextScreenId = LayoutSettingsScreenIds.LayoutTheme
                    )
                )
                add(
                    itemEntry(
                        stableKey = "layout_app_layout",
                        title = context.getString(R.string.app_layout),
                        subtitle = selectedIntLabel(
                            options = appLayoutOptions,
                            value = currentLayout
                        ),
                        fallbackIconRes = R.drawable.ic_baseline_tv_24,
                        nextScreenId = LayoutSettingsScreenIds.LayoutAppLayout
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "layout_bottom_title",
                        title = context.getString(R.string.bottom_title_settings),
                        subtitle = context.getString(R.string.bottom_title_settings_des),
                        fallbackIconRes = R.drawable.title_24px,
                        value = settingsManager.getBoolean(bottomTitleKey, true),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(bottomTitleKey, enabled)
                            }
                            clearSharedPools()
                        }
                    )
                )
                add(
                    sliderEntry(
                        stableKey = "layout_overscan",
                        title = context.getString(R.string.overscan_settings),
                        subtitle = context.getString(R.string.overscan_settings_des),
                        fallbackIconRes = R.drawable.arrows_input_24px,
                        value = overscan,
                        range = 0..100,
                        step = 1,
                        onValueChanged = { value ->
                            settingsManager.edit {
                                putInt(overscanKey, value)
                            }
                            applyOverscan(value)
                        }
                    )
                )
                add(
                    sliderEntry(
                        stableKey = "layout_poster_size",
                        title = context.getString(R.string.poster_size_settings),
                        subtitle = context.getString(R.string.poster_size_settings_des),
                        fallbackIconRes = R.drawable.baseline_grid_view_24,
                        value = posterSize,
                        range = 0..15,
                        step = 1,
                        onValueChanged = { value ->
                            settingsManager.edit {
                                putInt(posterSizeKey, value)
                            }
                            clearSharedPools()
                            HomeChildItemAdapter.updatePosterSize(context, value)
                        }
                    )
                )

                add(
                    headerEntry(
                        stableKey = "layout_header_features",
                        title = context.getString(R.string.pref_category_ui_features)
                    )
                )
                add(
                    itemEntry(
                        stableKey = "layout_poster_ui",
                        title = context.getString(R.string.poster_ui_settings),
                        subtitle = posterUiSummary(),
                        fallbackIconRes = R.drawable.ic_baseline_tv_24,
                        nextScreenId = LayoutSettingsScreenIds.LayoutPosterUi
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "layout_advanced_search",
                        title = context.getString(R.string.advanced_search),
                        subtitle = context.getString(R.string.advanced_search_des),
                        fallbackIconRes = R.drawable.search_icon,
                        value = settingsManager.getBoolean(LayoutSettingsDefaults.AdvancedSearchKey, true),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(LayoutSettingsDefaults.AdvancedSearchKey, enabled)
                            }
                        }
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "layout_search_suggestions",
                        title = context.getString(R.string.search_suggestions),
                        subtitle = context.getString(R.string.search_suggestions_des),
                        fallbackIconRes = R.drawable.search_icon,
                        value = settingsManager.getBoolean(LayoutSettingsDefaults.SearchSuggestionsKey, true),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(LayoutSettingsDefaults.SearchSuggestionsKey, enabled)
                            }
                        }
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "layout_show_trailers",
                        title = context.getString(R.string.show_trailers_settings),
                        subtitle = null,
                        fallbackIconRes = R.drawable.baseline_theaters_24,
                        value = settingsManager.getBoolean(showTrailersKey, true),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(showTrailersKey, enabled)
                            }
                        }
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "layout_show_kitsu_posters",
                        title = context.getString(R.string.kitsu_settings),
                        subtitle = null,
                        fallbackIconRes = R.drawable.kitsu_icon,
                        value = settingsManager.getBoolean(showKitsuPostersKey, true),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(showKitsuPostersKey, enabled)
                            }
                        }
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "layout_show_cast_panel",
                        title = context.getString(R.string.show_cast_in_details),
                        subtitle = null,
                        fallbackIconRes = R.drawable.ic_baseline_people_24,
                        value = settingsManager.getBoolean(showCastInDetailsKey, true),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(showCastInDetailsKey, enabled)
                            }
                        }
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "layout_show_fillers",
                        title = context.getString(R.string.show_fillers_settings),
                        subtitle = null,
                        fallbackIconRes = R.drawable.ic_baseline_skip_next_24,
                        value = settingsManager.getBoolean(showFillersKey, false),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(showFillersKey, enabled)
                            }
                        }
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "layout_random_button",
                        title = context.getString(R.string.random_button_settings),
                        subtitle = context.getString(R.string.random_button_settings_desc),
                        fallbackIconRes = R.drawable.ic_baseline_play_arrow_24,
                        value = settingsManager.getBoolean(randomButtonKey, false),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(randomButtonKey, enabled)
                            }
                        }
                    )
                )
                add(
                    itemEntry(
                        stableKey = "layout_confirm_exit",
                        title = context.getString(R.string.confirm_before_exiting_title),
                        subtitle = selectedIntLabel(
                            options = confirmExitOptions,
                            value = confirmExit
                        ),
                        fallbackIconRes = R.drawable.ic_baseline_exit_24,
                        nextScreenId = LayoutSettingsScreenIds.LayoutConfirmExit
                    )
                )
                add(
                    itemEntry(
                        stableKey = "layout_filter_search_quality",
                        title = context.getString(R.string.pref_filter_search_quality),
                        subtitle = filterSearchQualitySummary(),
                        fallbackIconRes = R.drawable.ic_baseline_filter_list_24,
                        nextScreenId = LayoutSettingsScreenIds.LayoutSearchQuality
                    )
                )
            }
        }
    }

    private inner class PrimaryColorScreen : SettingsScreen {
        override val id: String = LayoutSettingsScreenIds.LayoutPrimaryColor
        override val title: String = context.getString(R.string.primary_color_settings)

        override suspend fun load(): List<SettingsEntry> {
            val options = primaryColorOptions()
            val current = settingsManager.getString(
                primaryColorKey,
                options.firstOrNull()?.value
            )
            return buildList {
                options.forEachIndexed { index, option ->
                    add(
                        itemEntry(
                            stableKey = "layout_primary_color_option_$index",
                            title = option.label,
                            showCheckmark = option.value == current,
                            trailingColorArgb = context.resolveLegacyPrimaryPreviewColor(option.value),
                            action = {
                                runCatching {
                                    settingsManager.edit {
                                        putString(primaryColorKey, option.value)
                                    }
                                    (context as? Activity)?.recreate()
                                }.onFailure(::logError)
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class AppThemeScreen : SettingsScreen {
        override val id: String = LayoutSettingsScreenIds.LayoutTheme
        override val title: String = context.getString(R.string.app_theme_settings)

        override suspend fun load(): List<SettingsEntry> {
            val options = appThemeOptions()
            val current = settingsManager.getString(
                appThemeKey,
                options.firstOrNull()?.value
            )
            return buildList {
                options.forEachIndexed { index, option ->
                    add(
                        itemEntry(
                            stableKey = "layout_theme_option_$index",
                            title = option.label,
                            showCheckmark = option.value == current,
                            action = {
                                runCatching {
                                    settingsManager.edit {
                                        putString(appThemeKey, option.value)
                                    }
                                    (context as? Activity)?.recreate()
                                }.onFailure(::logError)
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class AppLayoutScreen : SettingsScreen {
        override val id: String = LayoutSettingsScreenIds.LayoutAppLayout
        override val title: String = context.getString(R.string.app_layout)

        override suspend fun load(): List<SettingsEntry> {
            val options = appLayoutOptions()
            val current = settingsManager.getInt(appLayoutKey, -1)
            return buildList {
                options.forEachIndexed { index, option ->
                    add(
                        itemEntry(
                            stableKey = "layout_app_layout_option_$index",
                            title = option.label,
                            showCheckmark = option.value == current,
                            action = {
                                runCatching {
                                    settingsManager.edit {
                                        putInt(appLayoutKey, option.value)
                                    }
                                    context.updateTv()
                                    (context as? Activity)?.recreate()
                                }.onFailure(::logError)
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class PosterUiScreen : SettingsScreen {
        override val id: String = LayoutSettingsScreenIds.LayoutPosterUi
        override val title: String = context.getString(R.string.poster_ui_settings)

        override suspend fun load(): List<SettingsEntry> {
            val optionLabels = context.resources.getStringArray(R.array.poster_ui_options)
            val optionKeys = context.resources.getStringArray(R.array.poster_ui_options_values)
            val optionCount = minOf(optionLabels.size, optionKeys.size)

            return buildList {
                for (index in 0 until optionCount) {
                    val key = optionKeys[index]
                    add(
                        toggleEntry(
                            stableKey = "layout_poster_ui_option_$key",
                            title = optionLabels[index],
                            subtitle = null,
                            fallbackIconRes = R.drawable.ic_baseline_tv_24,
                            value = settingsManager.getBoolean(key, true),
                            onValueChanged = { enabled ->
                                settingsManager.edit {
                                    putBoolean(key, enabled)
                                }
                                SearchResultBuilder.updateCache(context)
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class ConfirmExitScreen : SettingsScreen {
        override val id: String = LayoutSettingsScreenIds.LayoutConfirmExit
        override val title: String = context.getString(R.string.confirm_before_exiting_title)

        override suspend fun load(): List<SettingsEntry> {
            val options = confirmExitOptions()
            val current = settingsManager.getInt(confirmExitKey, -1)
            return buildList {
                options.forEachIndexed { index, option ->
                    add(
                        itemEntry(
                            stableKey = "layout_confirm_exit_option_$index",
                            title = option.label,
                            showCheckmark = option.value == current,
                            action = {
                                settingsManager.edit {
                                    putInt(confirmExitKey, option.value)
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class SearchQualityScreen : SettingsScreen {
        override val id: String = LayoutSettingsScreenIds.LayoutSearchQuality
        override val title: String = context.getString(R.string.pref_filter_search_quality)

        override suspend fun load(): List<SettingsEntry> {
            val selected = settingsManager.getStringSet(filterSearchQualityKey, setOf())
                ?.toMutableSet()
                ?: mutableSetOf()

            return buildList {
                SearchQuality.entries.sorted().forEach { quality ->
                    val value = quality.ordinal.toString()
                    add(
                        toggleEntry(
                            stableKey = "layout_search_quality_${quality.name}",
                            title = quality.name,
                            subtitle = null,
                            fallbackIconRes = R.drawable.ic_baseline_filter_list_24,
                            value = selected.contains(value),
                            onValueChanged = { enabled ->
                                val updated = settingsManager.getStringSet(filterSearchQualityKey, setOf())
                                    ?.toMutableSet()
                                    ?: mutableSetOf()
                                if (enabled) {
                                    updated.add(value)
                                } else {
                                    updated.remove(value)
                                }
                                settingsManager.edit {
                                    putStringSet(filterSearchQualityKey, updated)
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    private fun appLayoutOptions(): List<LayoutIntOption> {
        return intOptions(
            namesRes = R.array.app_layout,
            valuesRes = R.array.app_layout_values
        )
    }

    private fun confirmExitOptions(): List<LayoutIntOption> {
        return intOptions(
            namesRes = R.array.confirm_exit,
            valuesRes = R.array.confirm_exit_values
        )
    }

    private fun appThemeOptions(): List<LayoutStringOption> {
        val names = context.resources.getStringArray(R.array.themes_names).toMutableList()
        val values = context.resources.getStringArray(R.array.themes_names_values).toMutableList()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            removeIncompatibleOptions(names, values) { item -> item.startsWith("Monet") }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            removeIncompatibleOptions(names, values) { item -> item.startsWith("System") }
        }
        val optionCount = minOf(names.size, values.size)
        return buildList {
            for (index in 0 until optionCount) {
                add(LayoutStringOption(value = values[index], label = names[index]))
            }
        }
    }

    private fun primaryColorOptions(): List<LayoutStringOption> {
        val names = context.resources.getStringArray(R.array.themes_overlay_names).toMutableList()
        val values = context.resources.getStringArray(R.array.themes_overlay_names_values).toMutableList()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            removeIncompatibleOptions(names, values) { item -> item.startsWith("Monet") }
        }
        val optionCount = minOf(names.size, values.size)
        return buildList {
            for (index in 0 until optionCount) {
                add(LayoutStringOption(value = values[index], label = names[index]))
            }
        }
    }

    private fun intOptions(namesRes: Int, valuesRes: Int): List<LayoutIntOption> {
        val names = context.resources.getStringArray(namesRes)
        val values = context.resources.getIntArray(valuesRes)
        val optionCount = minOf(names.size, values.size)
        return buildList {
            for (index in 0 until optionCount) {
                add(LayoutIntOption(value = values[index], label = names[index]))
            }
        }
    }

    private fun selectedStringLabel(options: List<LayoutStringOption>, value: String?): String {
        return options.firstOrNull { option -> option.value == value }?.label
            ?: value
            ?: context.getString(R.string.none)
    }

    private fun selectedIntLabel(options: List<LayoutIntOption>, value: Int): String {
        return options.firstOrNull { option -> option.value == value }?.label
            ?: value.toString()
    }

    private fun posterUiSummary(): String {
        val optionLabels = context.resources.getStringArray(R.array.poster_ui_options)
        val optionKeys = context.resources.getStringArray(R.array.poster_ui_options_values)
        val optionCount = minOf(optionLabels.size, optionKeys.size)

        val enabled = buildList {
            for (index in 0 until optionCount) {
                if (settingsManager.getBoolean(optionKeys[index], true)) {
                    add(optionLabels[index])
                }
            }
        }
        return if (enabled.isEmpty()) {
            context.getString(R.string.none)
        } else {
            enabled.joinToString(", ")
        }
    }

    private fun filterSearchQualitySummary(): String {
        val selected = settingsManager.getStringSet(filterSearchQualityKey, setOf()) ?: setOf()
        val labels = SearchQuality.entries.sorted()
            .filter { quality -> selected.contains(quality.ordinal.toString()) }
            .map { quality -> quality.name }
        return if (labels.isEmpty()) {
            context.getString(R.string.none)
        } else {
            labels.joinToString(", ")
        }
    }

    private fun removeIncompatibleOptions(
        names: MutableList<String>,
        values: MutableList<String>,
        shouldRemove: (String) -> Boolean
    ) {
        val indexes = values
            .mapIndexedNotNull { index, value -> if (shouldRemove(value)) index else null }
        var removed = 0
        indexes.forEach { index ->
            names.removeAt(index - removed)
            values.removeAt(index - removed)
            removed += 1
        }
    }

    private fun clearSharedPools() {
        HomeChildItemAdapter.sharedPool.clear()
        ParentItemAdapter.sharedPool.clear()
        SearchAdapter.sharedPool.clear()
    }

    private fun applyOverscan(value: Int) {
        val padding = value.toPx
        (context.getActivity() as? MainActivity)?.binding?.homeRoot?.setPadding(
            padding,
            padding,
            padding,
            padding
        )
    }
}

private fun itemEntry(
    stableKey: String,
    title: String,
    subtitle: String? = null,
    fallbackIconRes: Int? = null,
    showCheckmark: Boolean = false,
    trailingColorArgb: Int? = null,
    nextScreenId: String? = null,
    action: (() -> Unit)? = null
): SettingsEntry {
    return SettingsEntry(
        title = title,
        subtitle = subtitle,
        fallbackIconRes = fallbackIconRes,
        showCheckmark = showCheckmark,
        trailingColorArgb = trailingColorArgb,
        stableKey = stableKey,
        nextScreenId = nextScreenId,
        action = action
    )
}

private fun headerEntry(
    stableKey: String,
    title: String
): SettingsEntry {
    return SettingsEntry(
        title = title,
        stableKey = stableKey,
        type = SettingsEntryType.Header
    )
}

private fun toggleEntry(
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

private fun sliderEntry(
    stableKey: String,
    title: String,
    subtitle: String?,
    fallbackIconRes: Int? = null,
    value: Int,
    range: IntRange,
    step: Int,
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
