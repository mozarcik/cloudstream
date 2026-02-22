package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.AutoDownloadMode
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.cloudstream3.network.initClient
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.services.BackupWorkManager
import com.lagradost.cloudstream3.ui.setup.HAS_DONE_SETUP_KEY
import com.lagradost.cloudstream3.utils.BackupUtils
import com.lagradost.cloudstream3.utils.BackupUtils.restorePrompt
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.InAppUpdater.installPreReleaseIfNeeded
import com.lagradost.cloudstream3.utils.InAppUpdater.runAutoUpdate
import com.lagradost.cloudstream3.utils.UIHelper.clipboardHelper
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import com.lagradost.cloudstream3.utils.txt
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.System.currentTimeMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object UpdatesSettingsScreenIds {
    const val UpdatesMain = "settings_updates"
    const val UpdatesApkInstaller = "settings_updates/apk_installer"
    const val UpdatesBackupFrequency = "settings_updates/backup_frequency"
    const val UpdatesBackupPath = "settings_updates/backup_path"
    const val UpdatesAutoPluginDownload = "settings_updates/auto_plugin_download"
    const val UpdatesLogcat = "settings_updates/logcat"
}

private data class UpdatesIntOption(
    val value: Int,
    val label: String
)

private object UpdatesSettingsTokens {
    const val LogcatClipboardLabel = "Logcat"
    const val LogcatFilePrefix = "logcat_"
}

@Composable
fun rememberUpdatesSettingsFeature(
    updatesTitle: String
): UpdatesSettingsFeature {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context, updatesTitle) {
        UpdatesSettingsFeature(
            context = context,
            updatesTitle = updatesTitle
        )
    }
}

class UpdatesSettingsFeature(
    private val context: android.content.Context,
    private val updatesTitle: String
) {
    private val settingsManager by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val autoUpdateKey by lazy { context.getString(R.string.auto_update_key) }
    private val apkInstallerKey by lazy { context.getString(R.string.apk_installer_key) }
    private val automaticBackupKey by lazy { context.getString(R.string.automatic_backup_key) }
    private val backupPathKey by lazy { context.getString(R.string.backup_path_key) }
    private val backupDirKey by lazy { context.getString(R.string.backup_dir_key) }
    private val autoUpdatePluginsKey by lazy { context.getString(R.string.auto_update_plugins_key) }
    private val autoDownloadPluginsKey by lazy { context.getString(R.string.auto_download_plugins_key) }

    val staticScreens: List<SettingsScreen> = listOf(
        UpdatesMainScreen(),
        ApkInstallerScreen(),
        BackupFrequencyScreen(),
        BackupPathScreen(),
        AutoPluginDownloadScreen(),
        LogcatScreen()
    )

    private inner class UpdatesMainScreen : SettingsScreen {
        override val id: String = UpdatesSettingsScreenIds.UpdatesMain
        override val title: String = updatesTitle

        override suspend fun load(): List<SettingsEntry> {
            val apkInstallerOptions = apkInstallerOptions()
            val backupFrequencyOptions = periodicBackupOptions()
            val autoPluginDownloadOptions = autoPluginDownloadOptions()

            val currentApkInstaller = settingsManager.getInt(apkInstallerKey, 0)
            val currentBackupFrequency = settingsManager.getInt(automaticBackupKey, 0)
            val currentAutoPluginDownload = settingsManager.getInt(autoDownloadPluginsKey, 0)
            val currentBackupPath = settingsManager.getString(
                backupDirKey,
                null
            ) ?: BackupUtils.getDefaultBackupDir(context)?.filePath()

            return buildList {
                add(
                    headerEntry(
                        stableKey = "updates_header_app_updates",
                        title = context.getString(R.string.pref_category_app_updates)
                    )
                )
                add(
                    itemEntry(
                        stableKey = "updates_manual_check",
                        title = context.getString(R.string.check_for_update),
                        subtitle = BuildConfig.VERSION_NAME,
                        fallbackIconRes = R.drawable.ic_baseline_system_update_24,
                        action = {
                            val activity = context as? Activity ?: return@itemEntry
                            ioSafe {
                                if (!activity.runAutoUpdate(checkAutoUpdate = false)) {
                                    activity.runOnUiThread {
                                        showToast(R.string.no_update_found)
                                    }
                                }
                            }
                        }
                    )
                )
                if (BuildConfig.FLAVOR == "stable") {
                    add(
                        itemEntry(
                            stableKey = "updates_install_prerelease",
                            title = context.getString(R.string.install_prerelease),
                            subtitle = null,
                            fallbackIconRes = R.drawable.ic_baseline_developer_mode_24,
                            action = {
                                (context as? Activity)?.installPreReleaseIfNeeded()
                            }
                        )
                    )
                }
                add(
                    itemEntry(
                        stableKey = "updates_apk_installer",
                        title = context.getString(R.string.apk_installer_settings),
                        subtitle = selectedIntLabel(apkInstallerOptions, currentApkInstaller),
                        fallbackIconRes = R.drawable.netflix_download,
                        nextScreenId = UpdatesSettingsScreenIds.UpdatesApkInstaller
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "updates_auto_update",
                        title = context.getString(R.string.updates_settings),
                        subtitle = context.getString(R.string.updates_settings_des),
                        fallbackIconRes = R.drawable.ic_baseline_notifications_active_24,
                        value = settingsManager.getBoolean(autoUpdateKey, true),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(autoUpdateKey, enabled)
                            }
                        }
                    )
                )

                add(
                    headerEntry(
                        stableKey = "updates_header_backup",
                        title = context.getString(R.string.pref_category_backup)
                    )
                )
                add(
                    itemEntry(
                        stableKey = "updates_backup_now",
                        title = context.getString(R.string.backup_settings),
                        subtitle = null,
                        fallbackIconRes = R.drawable.baseline_save_as_24,
                        action = {
                            BackupUtils.backup(context)
                        }
                    )
                )
                add(
                    itemEntry(
                        stableKey = "updates_backup_frequency",
                        title = context.getString(R.string.backup_frequency),
                        subtitle = selectedIntLabel(backupFrequencyOptions, currentBackupFrequency),
                        fallbackIconRes = R.drawable.baseline_save_as_24,
                        nextScreenId = UpdatesSettingsScreenIds.UpdatesBackupFrequency
                    )
                )
                add(
                    itemEntry(
                        stableKey = "updates_restore",
                        title = context.getString(R.string.restore_settings),
                        subtitle = null,
                        fallbackIconRes = R.drawable.baseline_restore_page_24,
                        action = {
                            (context as? FragmentActivity)?.restorePrompt()
                        }
                    )
                )
                add(
                    itemEntry(
                        stableKey = "updates_backup_path",
                        title = context.getString(R.string.backup_path_title),
                        subtitle = currentBackupPath,
                        fallbackIconRes = R.drawable.ic_baseline_folder_open_24,
                        nextScreenId = UpdatesSettingsScreenIds.UpdatesBackupPath
                    )
                )

                add(
                    headerEntry(
                        stableKey = "updates_header_extensions",
                        title = context.getString(R.string.pref_category_extensions)
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "updates_auto_update_plugins",
                        title = context.getString(R.string.automatic_plugin_updates),
                        subtitle = null,
                        fallbackIconRes = R.drawable.ic_baseline_extension_24,
                        value = settingsManager.getBoolean(autoUpdatePluginsKey, true),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(autoUpdatePluginsKey, enabled)
                            }
                        }
                    )
                )
                add(
                    itemEntry(
                        stableKey = "updates_auto_download_plugins",
                        title = context.getString(R.string.automatic_plugin_download),
                        subtitle = selectedIntLabel(autoPluginDownloadOptions, currentAutoPluginDownload),
                        fallbackIconRes = R.drawable.ic_baseline_extension_24,
                        nextScreenId = UpdatesSettingsScreenIds.UpdatesAutoPluginDownload
                    )
                )
                add(
                    itemEntry(
                        stableKey = "updates_manual_plugins_update",
                        title = context.getString(R.string.update_plugins),
                        subtitle = context.getString(R.string.update_plugins_manually),
                        fallbackIconRes = R.drawable.ic_baseline_extension_24,
                        action = {
                            val activity = context as? Activity ?: return@itemEntry
                            reloadAndUpdatePlugins(activity)
                        }
                    )
                )

                add(
                    headerEntry(
                        stableKey = "updates_header_actions",
                        title = context.getString(R.string.pref_category_actions)
                    )
                )
                add(
                    itemEntry(
                        stableKey = "updates_logcat",
                        title = context.getString(R.string.show_log_cat),
                        subtitle = null,
                        fallbackIconRes = R.drawable.baseline_description_24,
                        nextScreenId = UpdatesSettingsScreenIds.UpdatesLogcat
                    )
                )
                add(
                    itemEntry(
                        stableKey = "updates_redo_setup",
                        title = context.getString(R.string.redo_setup_process),
                        subtitle = null,
                        fallbackIconRes = R.drawable.ic_baseline_construction_24,
                        action = {
                            setKey(HAS_DONE_SETUP_KEY, false)
                            (context as? Activity)?.recreate()
                        }
                    )
                )
            }
        }
    }

    private inner class ApkInstallerScreen : SettingsScreen {
        override val id: String = UpdatesSettingsScreenIds.UpdatesApkInstaller
        override val title: String = context.getString(R.string.apk_installer_settings)

        override suspend fun load(): List<SettingsEntry> {
            val options = apkInstallerOptions()
            val current = settingsManager.getInt(apkInstallerKey, 0)
            return buildList {
                options.forEachIndexed { index, option ->
                    add(
                        itemEntry(
                            stableKey = "updates_apk_installer_option_$index",
                            title = option.label,
                            showCheckmark = option.value == current,
                            action = {
                                settingsManager.edit {
                                    putInt(apkInstallerKey, option.value)
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class BackupFrequencyScreen : SettingsScreen {
        override val id: String = UpdatesSettingsScreenIds.UpdatesBackupFrequency
        override val title: String = context.getString(R.string.backup_frequency)

        override suspend fun load(): List<SettingsEntry> {
            val options = periodicBackupOptions()
            val current = settingsManager.getInt(automaticBackupKey, 0)
            return buildList {
                options.forEachIndexed { index, option ->
                    add(
                        itemEntry(
                            stableKey = "updates_backup_frequency_option_$index",
                            title = option.label,
                            showCheckmark = option.value == current,
                            action = {
                                settingsManager.edit {
                                    putInt(automaticBackupKey, option.value)
                                }
                                BackupWorkManager.enqueuePeriodicWork(
                                    context = context,
                                    intervalHours = option.value.toLong()
                                )
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class BackupPathScreen : SettingsScreen {
        override val id: String = UpdatesSettingsScreenIds.UpdatesBackupPath
        override val title: String = context.getString(R.string.backup_path_title)

        override suspend fun load(): List<SettingsEntry> {
            val dirs = getBackupDirsForDisplay()
            val current = settingsManager.getString(backupDirKey, null)
                ?: BackupUtils.getDefaultBackupDir(context)?.filePath()

            return buildList {
                dirs.forEachIndexed { index, dir ->
                    add(
                        itemEntry(
                            stableKey = "updates_backup_dir_option_$index",
                            title = dir,
                            showCheckmark = dir == current,
                            action = {
                                settingsManager.edit {
                                    putString(backupPathKey, dir)
                                    putString(backupDirKey, dir)
                                }
                            }
                        )
                    )
                }
                add(
                    itemEntry(
                        stableKey = "updates_backup_dir_custom",
                        title = context.getString(R.string.custom),
                        subtitle = context.getString(R.string.coming_soon),
                        fallbackIconRes = R.drawable.ic_baseline_folder_open_24,
                        action = {
                            showToast(R.string.coming_soon)
                        }
                    )
                )
            }
        }
    }

    private inner class AutoPluginDownloadScreen : SettingsScreen {
        override val id: String = UpdatesSettingsScreenIds.UpdatesAutoPluginDownload
        override val title: String = context.getString(R.string.automatic_plugin_download_mode_title)

        override suspend fun load(): List<SettingsEntry> {
            val options = autoPluginDownloadOptions()
            val current = settingsManager.getInt(autoDownloadPluginsKey, 0)

            return buildList {
                options.forEachIndexed { index, option ->
                    add(
                        itemEntry(
                            stableKey = "updates_auto_plugin_download_option_$index",
                            title = option.label,
                            showCheckmark = option.value == current,
                            action = {
                                settingsManager.edit {
                                    putInt(autoDownloadPluginsKey, option.value)
                                }
                                app.initClient(context)
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class LogcatScreen : SettingsScreen {
        override val id: String = UpdatesSettingsScreenIds.UpdatesLogcat
        override val title: String = context.getString(R.string.show_log_cat)

        override suspend fun load(): List<SettingsEntry> {
            return listOf(
                itemEntry(
                    stableKey = "updates_logcat_copy",
                    title = context.getString(R.string.sort_copy),
                    subtitle = context.getString(R.string.show_log_cat),
                    fallbackIconRes = R.drawable.baseline_description_24,
                    action = {
                        val dump = readLogcatDump()
                        clipboardHelper(
                            label = txt(UpdatesSettingsTokens.LogcatClipboardLabel),
                            text = dump
                        )
                    }
                ),
                itemEntry(
                    stableKey = "updates_logcat_clear",
                    title = context.getString(R.string.sort_clear),
                    subtitle = context.getString(R.string.show_log_cat),
                    fallbackIconRes = R.drawable.baseline_description_24,
                    action = {
                        runCatching {
                            Runtime.getRuntime().exec("logcat -c")
                        }.onFailure(::logError)
                    }
                ),
                itemEntry(
                    stableKey = "updates_logcat_save",
                    title = context.getString(R.string.sort_save),
                    subtitle = context.getString(R.string.show_log_cat),
                    fallbackIconRes = R.drawable.baseline_description_24,
                    action = {
                        saveLogcatToFile(readLogcatDump())
                    }
                )
            )
        }
    }

    private fun apkInstallerOptions(): List<UpdatesIntOption> {
        return intOptions(
            namesRes = R.array.apk_installer_pref,
            valuesRes = R.array.apk_installer_values
        )
    }

    private fun periodicBackupOptions(): List<UpdatesIntOption> {
        return intOptions(
            namesRes = R.array.periodic_work_names,
            valuesRes = R.array.periodic_work_values
        )
    }

    private fun autoPluginDownloadOptions(): List<UpdatesIntOption> {
        val labels = context.resources.getStringArray(R.array.auto_download_plugin)
        val values = AutoDownloadMode.entries
            .sortedBy { mode -> mode.value }
            .map { mode -> mode.value }
        val optionCount = minOf(labels.size, values.size)
        return buildList {
            for (index in 0 until optionCount) {
                add(UpdatesIntOption(value = values[index], label = labels[index]))
            }
        }
    }

    private fun intOptions(namesRes: Int, valuesRes: Int): List<UpdatesIntOption> {
        val names = context.resources.getStringArray(namesRes)
        val values = context.resources.getIntArray(valuesRes)
        val optionCount = minOf(names.size, values.size)
        return buildList {
            for (index in 0 until optionCount) {
                add(UpdatesIntOption(value = values[index], label = names[index]))
            }
        }
    }

    private fun selectedIntLabel(options: List<UpdatesIntOption>, current: Int): String {
        return options.firstOrNull { option -> option.value == current }?.label
            ?: current.toString()
    }

    private fun getBackupDirsForDisplay(): List<String> {
        return safe {
            val defaultDir = BackupUtils.getDefaultBackupDir(context)?.filePath()
            val first = listOf(defaultDir)
            (runCatching {
                first + BackupUtils.getCurrentBackupDir(context).let { pair ->
                    pair.first?.filePath() ?: pair.second
                }
            }.getOrNull() ?: first).filterNotNull().distinct()
        } ?: emptyList()
    }

    private fun readLogcatDump(): String {
        return runCatching {
            val process = Runtime.getRuntime().exec("logcat -d")
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.lineSequence().joinToString("\n")
            }
        }.getOrElse {
            logError(it)
            ""
        }
    }

    private fun saveLogcatToFile(logcatDump: String) {
        ioSafe {
            var fileStream: OutputStream? = null
            try {
                val date = SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.getDefault())
                    .format(Date(currentTimeMillis()))
                fileStream = VideoDownloadManager.setupStream(
                    context = context,
                    name = UpdatesSettingsTokens.LogcatFilePrefix + date,
                    folder = null,
                    extension = "txt",
                    tryResume = false
                ).openNew()
                fileStream.writer().use { writer ->
                    writer.write(logcatDump)
                }
            } catch (t: Throwable) {
                logError(t)
                showToast(t.message)
            } finally {
                runCatching {
                    fileStream?.close()
                }
            }
        }
    }
}

@Suppress("DEPRECATION_ERROR")
private fun reloadAndUpdatePlugins(activity: Activity) {
    activity.ioSafe {
        PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_manuallyReloadAndUpdatePlugins(
            activity = activity
        )
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
