package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import android.app.Activity
import android.content.Context
import android.text.format.Formatter.formatShortFileSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.actions.VideoClickActionHolder
import com.lagradost.cloudstream3.mvvm.logError
import java.io.File

private object PlayerSettingsScreenIds {
    const val PlayerMain = "settings_player"
    const val PlayerDefault = "settings_player/default"
    const val PlayerTitleLimit = "settings_player/title_limit"
    const val PlayerInfo = "settings_player/info"
    const val PlayerSoftwareDecoding = "settings_player/software_decoding"
    const val PlayerBufferDisk = "settings_player/buffer_disk"
    const val PlayerBufferSize = "settings_player/buffer_size"
    const val PlayerBufferLength = "settings_player/buffer_length"
}

private data class IntOption(
    val value: Int,
    val label: String
)

private data class StringOption(
    val value: String,
    val label: String
)

@Composable
fun rememberPlayerSettingsFeature(
    playerTitle: String
): PlayerSettingsFeature {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context, playerTitle) {
        PlayerSettingsFeature(
            context = context,
            playerTitle = playerTitle
        )
    }
}

class PlayerSettingsFeature(
    private val context: Context,
    private val playerTitle: String
) {
    private val settingsManager by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val episodeSyncEnabledKey by lazy { context.getString(R.string.episode_sync_enabled_key) }
    private val playerDefaultKey by lazy { context.getString(R.string.player_default_key) }
    private val preferLimitTitleKey by lazy { context.getString(R.string.prefer_limit_title_key) }
    private val showNameKey by lazy { context.getString(R.string.show_name_key) }
    private val showResolutionKey by lazy { context.getString(R.string.show_resolution_key) }
    private val showMediaInfoKey by lazy { context.getString(R.string.show_media_info_key) }
    private val pipEnabledKey by lazy { context.getString(R.string.pip_enabled_key) }
    private val playerResizeEnabledKey by lazy { context.getString(R.string.player_resize_enabled_key) }
    private val playbackSpeedEnabledKey by lazy { context.getString(R.string.playback_speed_enabled_key) }
    private val autoplayNextKey by lazy { context.getString(R.string.autoplay_next_key) }
    private val skipOpEnabledKey by lazy { context.getString(R.string.enable_skip_op_from_database) }
    private val softwareDecodingKey by lazy { context.getString(R.string.software_decoding_key) }
    private val extraBrightnessKey by lazy { context.getString(R.string.extra_brightness_key) }
    private val bufferDiskKey by lazy { context.getString(R.string.video_buffer_disk_key) }
    private val bufferSizeKey by lazy { context.getString(R.string.video_buffer_size_key) }
    private val bufferLengthKey by lazy { context.getString(R.string.video_buffer_length_key) }
    private val androidTvSeekOnKey by lazy { context.getString(R.string.android_tv_interface_on_seek_key) }
    private val androidTvSeekOffKey by lazy { context.getString(R.string.android_tv_interface_off_seek_key) }

    val staticScreens: List<SettingsScreen> = listOf(
        PlayerMainScreen(),
        PlayerDefaultScreen(),
        PlayerTitleLimitScreen(),
        PlayerInfoScreen(),
        PlayerSoftwareDecodingScreen(),
        PlayerBufferDiskScreen(),
        PlayerBufferSizeScreen(),
        PlayerBufferLengthScreen()
    )

    private inner class PlayerMainScreen : SettingsScreen {
        override val id: String = PlayerSettingsScreenIds.PlayerMain
        override val title: String = playerTitle

        override suspend fun load(): List<SettingsEntry> {
            val titleLimitOptions = intOptions(
                namesRes = R.array.limit_title_pref_names,
                valuesRes = R.array.limit_title_pref_values
            )
            val softwareDecodingOptions = intOptions(
                namesRes = R.array.software_decoding_switch,
                valuesRes = R.array.software_decoding_switch_values
            )
            val bufferDiskOptions = intOptions(
                namesRes = R.array.video_buffer_size_names,
                valuesRes = R.array.video_buffer_size_values
            )
            val bufferSizeOptions = intOptions(
                namesRes = R.array.video_buffer_size_names,
                valuesRes = R.array.video_buffer_size_values
            )
            val bufferLengthOptions = intOptions(
                namesRes = R.array.video_buffer_length_names,
                valuesRes = R.array.video_buffer_length_values
            )

            val currentDefaultPlayer = settingsManager.getString(playerDefaultKey, "").orEmpty()
            val currentTitleLimit = settingsManager.getInt(preferLimitTitleKey, 0)
            val currentSoftwareDecoding = settingsManager.getInt(softwareDecodingKey, -1)
            val currentBufferDisk = settingsManager.getInt(bufferDiskKey, 0)
            val currentBufferSize = settingsManager.getInt(bufferSizeKey, 0)
            val currentBufferLength = settingsManager.getInt(bufferLengthKey, 0)
            val currentSeekWhenVisible = settingsManager.getInt(androidTvSeekOnKey, 30).coerceIn(5, 60)
            val currentSeekWhenHidden = settingsManager.getInt(androidTvSeekOffKey, 10).coerceIn(5, 60)

            return buildList {
                add(
                    toggleEntry(
                        stableKey = "player_episode_sync",
                        title = context.getString(R.string.episode_sync_settings),
                        subtitle = context.getString(R.string.episode_sync_settings_des),
                        fallbackIconRes = R.drawable.baseline_sync_24,
                        value = settingsManager.getBoolean(episodeSyncEnabledKey, true),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(episodeSyncEnabledKey, enabled)
                            }
                        }
                    )
                )

                add(
                    headerEntry(
                        stableKey = "player_header_defaults",
                        title = context.getString(R.string.pref_category_defaults)
                    )
                )
                add(
                    itemEntry(
                        stableKey = "player_default",
                        title = context.getString(R.string.player_pref),
                        subtitle = resolvePlayerLabel(currentDefaultPlayer),
                        fallbackIconRes = R.drawable.netflix_play,
                        nextScreenId = PlayerSettingsScreenIds.PlayerDefault
                    )
                )

                add(
                    headerEntry(
                        stableKey = "player_header_layout",
                        title = context.getString(R.string.pref_category_player_layout)
                    )
                )
                add(
                    itemEntry(
                        stableKey = "player_title_limit",
                        title = context.getString(R.string.limit_title),
                        subtitle = selectedIntLabel(titleLimitOptions, currentTitleLimit),
                        fallbackIconRes = R.drawable.ic_baseline_text_format_24,
                        nextScreenId = PlayerSettingsScreenIds.PlayerTitleLimit
                    )
                )
                add(
                    itemEntry(
                        stableKey = "player_info_flags",
                        title = context.getString(R.string.limit_title_rez),
                        subtitle = resolvePlayerInfoSummary(),
                        fallbackIconRes = R.drawable.ic_baseline_text_format_24,
                        nextScreenId = PlayerSettingsScreenIds.PlayerInfo
                    )
                )

                add(
                    headerEntry(
                        stableKey = "player_header_features",
                        title = context.getString(R.string.pref_category_player_features)
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "player_feature_pip",
                        title = context.getString(R.string.picture_in_picture),
                        subtitle = context.getString(R.string.picture_in_picture_des),
                        fallbackIconRes = R.drawable.ic_baseline_picture_in_picture_alt_24,
                        value = settingsManager.getBoolean(pipEnabledKey, true),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(pipEnabledKey, enabled)
                            }
                        }
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "player_feature_resize",
                        title = context.getString(R.string.player_size_settings),
                        subtitle = context.getString(R.string.player_size_settings_des),
                        fallbackIconRes = R.drawable.ic_baseline_aspect_ratio_24,
                        value = settingsManager.getBoolean(playerResizeEnabledKey, true),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(playerResizeEnabledKey, enabled)
                            }
                        }
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "player_feature_speed",
                        title = context.getString(R.string.eigengraumode_settings),
                        subtitle = context.getString(R.string.speed_setting_summary),
                        fallbackIconRes = R.drawable.ic_baseline_speed_24,
                        value = settingsManager.getBoolean(playbackSpeedEnabledKey, false),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(playbackSpeedEnabledKey, enabled)
                            }
                        }
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "player_feature_autoplay_next",
                        title = context.getString(R.string.autoplay_next_settings),
                        subtitle = context.getString(R.string.autoplay_next_settings_des),
                        fallbackIconRes = R.drawable.ic_baseline_skip_next_24,
                        value = settingsManager.getBoolean(autoplayNextKey, true),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(autoplayNextKey, enabled)
                            }
                        }
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "player_feature_skip_op",
                        title = context.getString(R.string.video_skip_op),
                        subtitle = context.getString(R.string.enable_skip_op_from_database_des),
                        fallbackIconRes = R.drawable.ic_baseline_skip_next_24,
                        value = settingsManager.getBoolean(skipOpEnabledKey, true),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(skipOpEnabledKey, enabled)
                            }
                        }
                    )
                )
                add(
                    itemEntry(
                        stableKey = "player_feature_software_decoding",
                        title = context.getString(R.string.software_decoding),
                        subtitle = selectedIntLabel(softwareDecodingOptions, currentSoftwareDecoding),
                        fallbackIconRes = R.drawable.ic_baseline_extension_24,
                        nextScreenId = PlayerSettingsScreenIds.PlayerSoftwareDecoding
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "player_feature_extra_brightness",
                        title = context.getString(R.string.extra_brightness_settings),
                        subtitle = context.getString(R.string.extra_brightness_settings_des),
                        fallbackIconRes = R.drawable.sun_7_24,
                        value = settingsManager.getBoolean(extraBrightnessKey, false),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(extraBrightnessKey, enabled)
                            }
                        }
                    )
                )

                add(
                    headerEntry(
                        stableKey = "player_header_cache",
                        title = context.getString(R.string.pref_category_cache)
                    )
                )
                add(
                    itemEntry(
                        stableKey = "player_cache_disk",
                        title = context.getString(R.string.video_buffer_disk_settings),
                        subtitle = selectedIntLabel(bufferDiskOptions, currentBufferDisk),
                        fallbackIconRes = R.drawable.ic_baseline_storage_24,
                        nextScreenId = PlayerSettingsScreenIds.PlayerBufferDisk
                    )
                )
                add(
                    itemEntry(
                        stableKey = "player_cache_size",
                        title = context.getString(R.string.video_buffer_size_settings),
                        subtitle = selectedIntLabel(bufferSizeOptions, currentBufferSize),
                        fallbackIconRes = R.drawable.ic_baseline_storage_24,
                        nextScreenId = PlayerSettingsScreenIds.PlayerBufferSize
                    )
                )
                add(
                    itemEntry(
                        stableKey = "player_cache_length",
                        title = context.getString(R.string.video_buffer_length_settings),
                        subtitle = selectedIntLabel(bufferLengthOptions, currentBufferLength),
                        fallbackIconRes = R.drawable.ic_baseline_storage_24,
                        nextScreenId = PlayerSettingsScreenIds.PlayerBufferLength
                    )
                )
                add(
                    itemEntry(
                        stableKey = "player_cache_clear",
                        title = context.getString(R.string.video_buffer_clear_settings),
                        subtitle = resolveCacheSizeSummary(),
                        fallbackIconRes = R.drawable.ic_baseline_delete_outline_24,
                        action = {
                            runCatching {
                                context.cacheDir.deleteRecursively()
                            }.onFailure(::logError)
                        }
                    )
                )

                add(
                    headerEntry(
                        stableKey = "player_header_android_tv",
                        title = context.getString(R.string.pref_category_android_tv)
                    )
                )
                add(
                    sliderEntry(
                        stableKey = "player_android_tv_seek_visible",
                        title = context.getString(R.string.android_tv_interface_on_seek_settings),
                        subtitle = context.getString(R.string.android_tv_interface_on_seek_settings_summary),
                        fallbackIconRes = R.drawable.go_forward_30,
                        value = currentSeekWhenVisible,
                        range = 5..60,
                        step = 5,
                        onValueChanged = { value ->
                            settingsManager.edit {
                                putInt(androidTvSeekOnKey, value)
                            }
                        }
                    )
                )
                add(
                    sliderEntry(
                        stableKey = "player_android_tv_seek_hidden",
                        title = context.getString(R.string.android_tv_interface_off_seek_settings),
                        subtitle = context.getString(R.string.android_tv_interface_off_seek_settings_summary),
                        fallbackIconRes = R.drawable.go_forward_30,
                        value = currentSeekWhenHidden,
                        range = 5..60,
                        step = 5,
                        onValueChanged = { value ->
                            settingsManager.edit {
                                putInt(androidTvSeekOffKey, value)
                            }
                        }
                    )
                )
            }
        }
    }

    private inner class PlayerDefaultScreen : SettingsScreen {
        override val id: String = PlayerSettingsScreenIds.PlayerDefault
        override val title: String = context.getString(R.string.player_pref)

        override suspend fun load(): List<SettingsEntry> {
            val current = settingsManager.getString(playerDefaultKey, "").orEmpty()
            val options = resolvePlayerOptions()
            return buildList {
                options.forEachIndexed { index, option ->
                    add(
                        itemEntry(
                            stableKey = "player_default_option_$index",
                            title = option.label,
                            showCheckmark = option.value == current,
                            action = {
                                settingsManager.edit {
                                    putString(playerDefaultKey, option.value)
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class PlayerTitleLimitScreen : SettingsScreen {
        override val id: String = PlayerSettingsScreenIds.PlayerTitleLimit
        override val title: String = context.getString(R.string.limit_title)

        override suspend fun load(): List<SettingsEntry> {
            val options = intOptions(
                namesRes = R.array.limit_title_pref_names,
                valuesRes = R.array.limit_title_pref_values
            )
            val current = settingsManager.getInt(preferLimitTitleKey, 0)
            return buildList {
                options.forEachIndexed { index, option ->
                    add(
                        itemEntry(
                            stableKey = "player_title_limit_option_$index",
                            title = option.label,
                            showCheckmark = option.value == current,
                            action = {
                                settingsManager.edit {
                                    putInt(preferLimitTitleKey, option.value)
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class PlayerInfoScreen : SettingsScreen {
        override val id: String = PlayerSettingsScreenIds.PlayerInfo
        override val title: String = context.getString(R.string.limit_title_rez)

        override suspend fun load(): List<SettingsEntry> {
            val optionNames = context.resources.getStringArray(R.array.title_info_pref_names)
            val optionKeys = context.resources.getStringArray(R.array.title_info_pref_values)
            val optionDefaults = resolvePlayerInfoDefaults()
            val optionCount = minOf(optionNames.size, optionKeys.size)

            return buildList {
                for (index in 0 until optionCount) {
                    val key = optionKeys[index]
                    add(
                        toggleEntry(
                            stableKey = "player_info_option_$index",
                            title = optionNames[index],
                            subtitle = null,
                            fallbackIconRes = R.drawable.ic_baseline_text_format_24,
                            value = settingsManager.getBoolean(key, optionDefaults[key] ?: false),
                            onValueChanged = { enabled ->
                                settingsManager.edit {
                                    putBoolean(key, enabled)
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class PlayerSoftwareDecodingScreen : SettingsScreen {
        override val id: String = PlayerSettingsScreenIds.PlayerSoftwareDecoding
        override val title: String = context.getString(R.string.software_decoding)

        override suspend fun load(): List<SettingsEntry> {
            val options = intOptions(
                namesRes = R.array.software_decoding_switch,
                valuesRes = R.array.software_decoding_switch_values
            )
            val current = settingsManager.getInt(softwareDecodingKey, -1)
            return buildList {
                options.forEachIndexed { index, option ->
                    add(
                        itemEntry(
                            stableKey = "player_software_decoding_option_$index",
                            title = option.label,
                            showCheckmark = option.value == current,
                            action = {
                                settingsManager.edit {
                                    putInt(softwareDecodingKey, option.value)
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class PlayerBufferDiskScreen : SettingsScreen {
        override val id: String = PlayerSettingsScreenIds.PlayerBufferDisk
        override val title: String = context.getString(R.string.video_buffer_disk_settings)

        override suspend fun load(): List<SettingsEntry> {
            return loadBufferSelectionScreen(
                key = bufferDiskKey,
                currentValue = settingsManager.getInt(bufferDiskKey, 0),
                namesRes = R.array.video_buffer_size_names,
                valuesRes = R.array.video_buffer_size_values,
                stablePrefix = "player_buffer_disk"
            )
        }
    }

    private inner class PlayerBufferSizeScreen : SettingsScreen {
        override val id: String = PlayerSettingsScreenIds.PlayerBufferSize
        override val title: String = context.getString(R.string.video_buffer_size_settings)

        override suspend fun load(): List<SettingsEntry> {
            return loadBufferSelectionScreen(
                key = bufferSizeKey,
                currentValue = settingsManager.getInt(bufferSizeKey, 0),
                namesRes = R.array.video_buffer_size_names,
                valuesRes = R.array.video_buffer_size_values,
                stablePrefix = "player_buffer_size"
            )
        }
    }

    private inner class PlayerBufferLengthScreen : SettingsScreen {
        override val id: String = PlayerSettingsScreenIds.PlayerBufferLength
        override val title: String = context.getString(R.string.video_buffer_length_settings)

        override suspend fun load(): List<SettingsEntry> {
            return loadBufferSelectionScreen(
                key = bufferLengthKey,
                currentValue = settingsManager.getInt(bufferLengthKey, 0),
                namesRes = R.array.video_buffer_length_names,
                valuesRes = R.array.video_buffer_length_values,
                stablePrefix = "player_buffer_length"
            )
        }
    }

    private fun loadBufferSelectionScreen(
        key: String,
        currentValue: Int,
        namesRes: Int,
        valuesRes: Int,
        stablePrefix: String
    ): List<SettingsEntry> {
        val options = intOptions(namesRes = namesRes, valuesRes = valuesRes)
        return buildList {
            options.forEachIndexed { index, option ->
                add(
                    itemEntry(
                        stableKey = "$stablePrefix-$index",
                        title = option.label,
                        showCheckmark = option.value == currentValue,
                        action = {
                            settingsManager.edit {
                                putInt(key, option.value)
                            }
                        }
                    )
                )
            }
        }
    }

    private fun resolvePlayerOptions(): List<StringOption> {
        val options = mutableListOf(
            StringOption(
                value = "",
                label = context.getString(R.string.player_settings_play_in_app)
            )
        )
        val players = VideoClickActionHolder.getPlayers(context as? Activity)
        players.forEach { action ->
            val label = action.name.asStringNull(context) ?: action.javaClass.simpleName
            options.add(
                StringOption(
                    value = action.uniqueId(),
                    label = label
                )
            )
        }
        return options.distinctBy { option -> option.value }
    }

    private fun resolvePlayerLabel(currentValue: String): String {
        return resolvePlayerOptions()
            .firstOrNull { option -> option.value == currentValue }
            ?.label
            ?: context.getString(R.string.player_settings_play_in_app)
    }

    private fun resolvePlayerInfoDefaults(): Map<String, Boolean> {
        return mapOf(
            showNameKey to true,
            showResolutionKey to true,
            showMediaInfoKey to false
        )
    }

    private fun resolvePlayerInfoSummary(): String {
        val optionNames = context.resources.getStringArray(R.array.title_info_pref_names)
        val optionKeys = context.resources.getStringArray(R.array.title_info_pref_values)
        val optionDefaults = resolvePlayerInfoDefaults()
        val optionCount = minOf(optionNames.size, optionKeys.size)

        val selectedLabels = buildList {
            for (index in 0 until optionCount) {
                val key = optionKeys[index]
                val selected = settingsManager.getBoolean(key, optionDefaults[key] ?: false)
                if (selected) {
                    add(optionNames[index])
                }
            }
        }
        return if (selectedLabels.isEmpty()) {
            context.getString(R.string.none)
        } else {
            selectedLabels.joinToString(", ")
        }
    }

    private fun resolveCacheSizeSummary(): String {
        return runCatching {
            formatShortFileSize(context, getFolderSize(context.cacheDir))
        }.getOrElse {
            logError(it)
            context.getString(R.string.none)
        }
    }

    private fun intOptions(namesRes: Int, valuesRes: Int): List<IntOption> {
        val names = context.resources.getStringArray(namesRes)
        val values = context.resources.getIntArray(valuesRes)
        val optionCount = minOf(names.size, values.size)
        return buildList {
            for (index in 0 until optionCount) {
                add(IntOption(value = values[index], label = names[index]))
            }
        }
    }

    private fun selectedIntLabel(options: List<IntOption>, current: Int): String {
        return options.firstOrNull { option -> option.value == current }?.label
            ?: current.toString()
    }

    private fun getFolderSize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isFile) {
                file.length()
            } else {
                getFolderSize(file)
            }
        }
        return size
    }
}

private fun itemEntry(
    stableKey: String,
    title: String,
    subtitle: String? = null,
    fallbackIconRes: Int? = null,
    showCheckmark: Boolean = false,
    nextScreenId: String? = null,
    action: (() -> Unit)? = null
): SettingsEntry {
    return SettingsEntry(
        title = title,
        subtitle = subtitle,
        fallbackIconRes = fallbackIconRes,
        showCheckmark = showCheckmark,
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
