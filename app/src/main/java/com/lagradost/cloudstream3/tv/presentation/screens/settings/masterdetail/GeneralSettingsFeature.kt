package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.cloudstream3.network.initClient
import com.lagradost.cloudstream3.ui.settings.Globals
import com.lagradost.cloudstream3.ui.settings.appLanguages
import com.lagradost.cloudstream3.ui.settings.getCurrentLocale
import com.lagradost.cloudstream3.ui.settings.nameNextToFlagEmoji
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import com.lagradost.cloudstream3.utils.VideoDownloadManager.getBasePath

private object GeneralSettingsTokens {
    val DisclaimerMaxContentWidth = 460.dp
    val DisclaimerSectionSpacing = 12.dp
}

private object GeneralSettingsScreenIds {
    const val General = "settings_general"
    const val Language = "settings_general_language"
    const val Disclaimer = "settings_general_disclaimer"
    const val Dns = "settings_general_dns"
    const val DownloadPath = "settings_general_download_path"
}

@Composable
fun rememberGeneralSettingsFeature(
    generalTitle: String
): GeneralSettingsFeature {
    val context = LocalContext.current
    return remember(context, generalTitle) {
        GeneralSettingsFeature(
            context = context,
            generalTitle = generalTitle
        )
    }
}

class GeneralSettingsFeature(
    private val context: Context,
    private val generalTitle: String
) {
    val staticScreens: List<SettingsScreen> = listOf(
        GeneralMainScreen(),
        AppLanguageScreen(),
        DisclaimerScreen(),
        DnsScreen(),
        DownloadPathScreen()
    )

    private val settingsManager by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val localeKey by lazy { context.getString(R.string.locale_key) }
    private val downloadPathKey by lazy { context.getString(R.string.download_path_key) }
    private val downloadPathVisualKey by lazy { context.getString(R.string.download_path_key_visual) }
    private val downloadParallelKey by lazy { context.getString(R.string.download_parallel_key) }
    private val downloadConcurrentKey by lazy { context.getString(R.string.download_concurrent_key) }
    private val dnsStorageKey by lazy { context.getString(R.string.dns_pref) }
    private val jsDelivrProxyKey by lazy { context.getString(R.string.jsdelivr_proxy_key) }
    private val beneneCountKey by lazy { context.getString(R.string.benene_count) }

    private inner class GeneralMainScreen : SettingsScreen {
        override val id: String = GeneralSettingsScreenIds.General
        override val title: String = generalTitle

        override suspend fun load(): List<SettingsEntry> {
            val dnsNames = context.resources.getStringArray(R.array.dns_pref)
            val dnsValues = context.resources.getIntArray(R.array.dns_pref_values)
            val currentDnsValue = settingsManager.getInt(dnsStorageKey, 0)
            val currentDnsName = dnsNames.getOrNull(dnsValues.indexOf(currentDnsValue))
                ?: dnsNames.firstOrNull()
                ?: context.getString(R.string.none)

            val currentDownloadPath = settingsManager.getString(
                downloadPathVisualKey,
                null
            ) ?: VideoDownloadManager.getDefaultDir(context)?.filePath()

            val currentParallel = settingsManager.getInt(downloadParallelKey, 3).coerceIn(1, 10)
            val currentConcurrent = settingsManager.getInt(downloadConcurrentKey, 3).coerceIn(1, 10)
            val currentJsDelivr = getKey<Boolean>(jsDelivrProxyKey)
                ?: settingsManager.getBoolean(jsDelivrProxyKey, false)
            val beneneCount = settingsManager.getInt(beneneCountKey, 0)

            return buildList {
                add(
                    itemEntry(
                        stableKey = "general_app_language",
                        title = context.getString(R.string.app_language),
                        subtitle = currentLanguageLabel(),
                        fallbackIconRes = R.drawable.ic_baseline_language_24,
                        nextScreenId = GeneralSettingsScreenIds.Language
                    )
                )
                add(
                    itemEntry(
                        stableKey = "general_disclaimer",
                        title = context.getString(R.string.legal_notice),
                        subtitle = context.getString(R.string.legal_notice),
                        fallbackIconRes = R.drawable.ic_baseline_warning_24,
                        nextScreenId = GeneralSettingsScreenIds.Disclaimer
                    )
                )
                add(
                    itemEntry(
                        stableKey = "general_benene",
                        title = context.getString(R.string.benene),
                        fallbackIconRes = R.drawable.benene,
                        subtitle = if (beneneCount <= 0) {
                            context.getString(R.string.benene_count_text_none)
                        } else {
                            context.getString(R.string.benene_count_text).format(beneneCount)
                        },
                        action = {
                            val nextCount = settingsManager.getInt(beneneCountKey, 0) + 1
                            Globals.beneneCount = nextCount
                            settingsManager.edit {
                                putInt(beneneCountKey, nextCount)
                            }
                        }
                    )
                )

                add(
                    headerEntry(
                        stableKey = "general_header_downloads",
                        title = context.getString(R.string.title_downloads)
                    )
                )
                add(
                    itemEntry(
                        stableKey = "general_download_path",
                        title = context.getString(R.string.download_path_pref),
                        subtitle = currentDownloadPath,
                        fallbackIconRes = R.drawable.netflix_download,
                        nextScreenId = GeneralSettingsScreenIds.DownloadPath
                    )
                )
                add(
                    sliderEntry(
                        stableKey = "general_download_parallel",
                        title = context.getString(R.string.parallel_downloads),
                        subtitle = context.getString(R.string.download_parallel_settings_des),
                        fallbackIconRes = R.drawable.arrow_or_edge_24px,
                        value = currentParallel,
                        range = 1..10,
                        onValueChanged = { newValue ->
                            settingsManager.edit {
                                putInt(downloadParallelKey, newValue)
                            }
                        }
                    )
                )
                add(
                    sliderEntry(
                        stableKey = "general_download_concurrent",
                        title = context.getString(R.string.concurrent_connections),
                        subtitle = context.getString(R.string.concurrent_connections_settings_des),
                        fallbackIconRes = R.drawable.arrow_and_edge_24px,
                        value = currentConcurrent,
                        range = 1..10,
                        onValueChanged = { newValue ->
                            settingsManager.edit {
                                putInt(downloadConcurrentKey, newValue)
                            }
                        }
                    )
                )

                add(
                    headerEntry(
                        stableKey = "general_header_bypass",
                        title = context.getString(R.string.pref_category_bypass)
                    )
                )
                add(
                    itemEntry(
                        stableKey = "general_dns",
                        title = context.getString(R.string.dns_pref),
                        subtitle = currentDnsName,
                        fallbackIconRes = R.drawable.ic_baseline_dns_24,
                        nextScreenId = GeneralSettingsScreenIds.Dns
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "general_jsdelivr_proxy",
                        title = context.getString(R.string.jsdelivr_proxy),
                        subtitle = context.getString(R.string.jsdelivr_proxy_summary),
                        fallbackIconRes = R.drawable.ic_github_logo,
                        value = currentJsDelivr,
                        onValueChanged = { enabled ->
                            setKey(jsDelivrProxyKey, enabled)
                            settingsManager.edit {
                                putBoolean(jsDelivrProxyKey, enabled)
                            }
                        }
                    )
                )
                add(
                    headerEntry(
                        stableKey = "general_header_links",
                        title = context.getString(R.string.pref_category_links)
                    )
                )
                add(
                    itemEntry(
                        stableKey = "general_link_github",
                        title = context.getString(R.string.github),
                        subtitle = "https://github.com/recloudstream/cloudstream",
                        fallbackIconRes = R.drawable.ic_github_logo,
                        action = {
                            openLink("https://github.com/recloudstream/cloudstream")
                        }
                    )
                )
                add(
                    itemEntry(
                        stableKey = "general_link_lightnovel",
                        title = context.getString(R.string.lightnovel),
                        subtitle = "https://github.com/LagradOst/QuickNovel",
                        fallbackIconRes = R.drawable.quick_novel_icon,
                        action = {
                            openLink("https://github.com/LagradOst/QuickNovel")
                        }
                    )
                )
                add(
                    itemEntry(
                        stableKey = "general_link_discord",
                        title = context.getString(R.string.discord),
                        subtitle = "https://discord.gg/5Hus6fM",
                        fallbackIconRes = R.drawable.ic_baseline_discord_24,
                        action = {
                            openLink("https://discord.gg/5Hus6fM")
                        }
                    )
                )
                add(
                    itemEntry(
                        stableKey = "general_link_wiki",
                        title = context.getString(R.string.cs3wiki),
                        subtitle = "https://cloudstream.miraheze.org/",
                        fallbackIconRes = R.drawable.baseline_description_24,
                        action = {
                            openLink("https://cloudstream.miraheze.org/")
                        }
                    )
                )
            }
        }
    }

    private inner class AppLanguageScreen : SettingsScreen {
        override val id: String = GeneralSettingsScreenIds.Language
        override val title: String = context.getString(R.string.app_language)

        override suspend fun load(): List<SettingsEntry> {
            val currentTag = settingsManager.getString(localeKey, null)
                ?: getCurrentLocale(context)

            return buildList {
                add(
                    headerEntry(
                        stableKey = "general_language_header",
                        title = context.getString(R.string.app_language)
                    )
                )
                appLanguages.forEach { language ->
                    val selected = language.second == currentTag
                    add(
                        itemEntry(
                            stableKey = "general_language_${language.second}",
                            title = language.nameNextToFlagEmoji(),
                            showCheckmark = selected,
                            action = {
                                CommonActivity.setLocale(context, language.second)
                                settingsManager.edit {
                                    putString(localeKey, language.second)
                                }
                                (context as? Activity)?.recreate()
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class DisclaimerScreen : SettingsScreen {
        override val id: String = GeneralSettingsScreenIds.Disclaimer
        override val title: String = context.getString(R.string.legal_notice)
        override val hasCustomContent: Boolean = true

        override suspend fun load(): List<SettingsEntry> {
            return listOf(
                itemEntry(
                    stableKey = "general_disclaimer_preview",
                    title = context.getString(R.string.legal_notice),
                    subtitle = disclaimerPreviewSummary(),
                    fallbackIconRes = R.drawable.ic_baseline_warning_24
                )
            )
        }

        @Composable
        override fun Content(
            modifier: Modifier,
            contentPadding: PaddingValues,
            isPreview: Boolean,
            onBack: () -> Unit,
            onDataChanged: (String) -> Unit
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.TopStart
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = GeneralSettingsTokens.DisclaimerMaxContentWidth),
                    verticalArrangement = Arrangement.spacedBy(GeneralSettingsTokens.DisclaimerSectionSpacing)
                ) {
                    Text(
                        text = context.getString(R.string.legal_notice),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = context.getString(R.string.legal_notice_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }

    private inner class DnsScreen : SettingsScreen {
        override val id: String = GeneralSettingsScreenIds.Dns
        override val title: String = context.getString(R.string.dns_pref)

        override suspend fun load(): List<SettingsEntry> {
            val dnsNames = context.resources.getStringArray(R.array.dns_pref)
            val dnsValues = context.resources.getIntArray(R.array.dns_pref_values)
            val currentDns = settingsManager.getInt(dnsStorageKey, 0)

            return buildList {
                add(
                    headerEntry(
                        stableKey = "general_dns_header",
                        title = context.getString(R.string.dns_pref)
                    )
                )
                dnsNames.forEachIndexed { index, name ->
                    val value = dnsValues.getOrNull(index) ?: return@forEachIndexed
                    add(
                        itemEntry(
                            stableKey = "general_dns_$value",
                            title = name,
                            subtitle = if (value == currentDns) context.getString(R.string.tv_selected) else null,
                            action = {
                                settingsManager.edit {
                                    putInt(dnsStorageKey, value)
                                }
                                app.initClient(context)
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class DownloadPathScreen : SettingsScreen {
        override val id: String = GeneralSettingsScreenIds.DownloadPath
        override val title: String = context.getString(R.string.download_path_pref)

        override suspend fun load(): List<SettingsEntry> {
            val dirs = getDownloadDirs()
            val currentPath = settingsManager.getString(
                downloadPathVisualKey,
                null
            ) ?: VideoDownloadManager.getDefaultDir(context)?.filePath()

            return buildList {
                add(
                    headerEntry(
                        stableKey = "general_download_path_header",
                        title = context.getString(R.string.download_path_pref)
                    )
                )
                dirs.forEach { dir ->
                    add(
                        itemEntry(
                            stableKey = "general_download_path_$dir",
                            title = dir,
                            subtitle = if (dir == currentPath) context.getString(R.string.tv_selected) else null,
                            action = {
                                settingsManager.edit {
                                    putString(downloadPathKey, dir)
                                    putString(downloadPathVisualKey, dir)
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    private fun getDownloadDirs(): List<String> {
        return safe {
            val defaultDir = VideoDownloadManager.getDefaultDir(context)?.filePath()
            val currentDir = context.getBasePath().let { path -> path.first?.filePath() ?: path.second }
            (listOf(defaultDir) +
                    context.getExternalFilesDirs("").mapNotNull { file -> file?.path } +
                    currentDir)
                .filterNotNull()
                .distinct()
        } ?: emptyList()
    }

    private fun currentLanguageLabel(): String {
        val currentTag = settingsManager.getString(localeKey, null)
            ?: getCurrentLocale(context)
        return appLanguages.firstOrNull { language -> language.second == currentTag }
            ?.nameNextToFlagEmoji()
            ?: currentTag
    }

    private fun disclaimerPreviewSummary(): String {
        val compact = context.getString(R.string.legal_notice_text)
            .replace('\n', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (compact.length > 180) compact.take(180) + "..." else compact
    }

    private fun openLink(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            CommonActivity.showToast(R.string.app_not_found_error)
        }
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
        valueText = value.toString(),
        onSliderChanged = onValueChanged
    )
}
