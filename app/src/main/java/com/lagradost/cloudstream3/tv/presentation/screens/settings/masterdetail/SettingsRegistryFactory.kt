package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.lagradost.cloudstream3.R
import kotlinx.coroutines.delay

private object SettingsScreenIds {
    const val Root = "settings_root"
    const val General = "settings_general"
    const val GeneralLanguage = "settings_general_language"
    const val GeneralLanguageApp = "settings_general_language_app"
    const val GeneralLanguageSubtitles = "settings_general_language_subtitles"
    const val GeneralStartup = "settings_general_startup"
    const val Player = "settings_player"
    const val PlayerQuality = "settings_player_quality"
    const val PlayerQualityStreaming = "settings_player_quality_streaming"
    const val PlayerQualityDownloads = "settings_player_quality_downloads"
    const val Providers = "settings_providers"
    const val Ui = "settings_ui"
    const val Updates = "settings_updates"
    const val Account = "settings_account"
    const val Extensions = "settings_extensions"
}

private class StaticSettingsScreen(
    override val id: String,
    override val title: String,
    private val entriesProvider: suspend () -> List<SettingsEntry>
) : SettingsScreen {
    override suspend fun load(): List<SettingsEntry> = entriesProvider()
}

@Composable
fun rememberMasterDetailSettingsRegistry(): SettingsRegistry {
    val context = LocalContext.current
    val titleExtensions = context.getString(R.string.extensions)
    val titleGeneral = context.getString(R.string.category_general)
    val titleAccount = context.getString(R.string.category_account)
    val extensionsFeature = rememberExtensionsSettingsFeature(
        extensionsTitle = titleExtensions
    )
    val generalFeature = rememberGeneralSettingsFeature(
        generalTitle = titleGeneral
    )
    val titleProviders = context.getString(R.string.category_providers)
    val providersFeature = rememberProvidersSettingsFeature(
        providersTitle = titleProviders
    )
    val titleUi = context.getString(R.string.category_ui)
    val layoutFeature = rememberLayoutSettingsFeature(
        layoutTitle = titleUi
    )
    val titleUpdates = context.getString(R.string.category_updates)
    val updatesFeature = rememberUpdatesSettingsFeature(
        updatesTitle = titleUpdates
    )
    val titlePlayer = context.getString(R.string.category_player)
    val playerFeature = rememberPlayerSettingsFeature(
        playerTitle = titlePlayer
    )
    val accountFeature = rememberAccountSettingsFeature(
        accountTitle = titleAccount
    )
    return remember(
        context,
        titleExtensions,
        titleGeneral,
        titleProviders,
        titleUi,
        titleUpdates,
        titlePlayer,
        titleAccount,
        extensionsFeature,
        generalFeature,
        providersFeature,
        layoutFeature,
        updatesFeature,
        playerFeature,
        accountFeature
    ) {
        val titleSettings = context.getString(R.string.title_settings)

        val screens = listOf(
            StaticSettingsScreen(
                id = SettingsScreenIds.Root,
                title = titleSettings
            ) {
                listOf(
                    entry(
                        key = "root_general",
                        title = titleGeneral,
                        subtitle = "Language, startup, storage and network",
                        icon = Icons.Default.Settings,
                        nextScreenId = SettingsScreenIds.General
                    ),
                    entry(
                        key = "root_player",
                        title = titlePlayer,
                        subtitle = "Playback, subtitles and advanced options",
                        icon = Icons.Default.PlayArrow,
                        nextScreenId = SettingsScreenIds.Player
                    ),
                    entry(
                        key = "root_providers",
                        title = titleProviders,
                        subtitle = "Source priorities and provider timeout",
                        icon = Icons.Default.Build,
                        nextScreenId = SettingsScreenIds.Providers
                    ),
                    entry(
                        key = "root_ui",
                        title = titleUi,
                        subtitle = "Theme, layout and visual behavior",
                        icon = Icons.Default.Info,
                        nextScreenId = SettingsScreenIds.Ui
                    ),
                    entry(
                        key = "root_updates",
                        title = titleUpdates,
                        subtitle = "Application and extension updates",
                        icon = Icons.Default.Refresh,
                        nextScreenId = SettingsScreenIds.Updates
                    ),
                    entry(
                        key = "root_account",
                        title = titleAccount,
                        subtitle = "Profile, synchronization and backups",
                        icon = Icons.Default.AccountCircle,
                        nextScreenId = SettingsScreenIds.Account
                    ),
                    entry(
                        key = "root_extensions",
                        title = titleExtensions,
                        subtitle = "Repositories and plugins",
                        icon = Icons.Default.Extension,
                        nextScreenId = SettingsScreenIds.Extensions
                    )
                )
            },
            StaticSettingsScreen(
                id = SettingsScreenIds.General,
                title = titleGeneral
            ) {
                listOf(
                    entry(
                        key = "general_language",
                        title = "Language",
                        subtitle = "App and subtitles",
                        icon = Icons.Default.Language,
                        nextScreenId = SettingsScreenIds.GeneralLanguage
                    ),
                    entry(
                        key = "general_startup",
                        title = "Startup",
                        subtitle = "Default behavior after launch",
                        icon = Icons.Default.Build,
                        nextScreenId = SettingsScreenIds.GeneralStartup
                    ),
                    entry(
                        key = "general_account",
                        title = titleAccount,
                        subtitle = "Profiles and sync",
                        icon = Icons.Default.AccountCircle
                    )
                )
            },
            StaticSettingsScreen(
                id = SettingsScreenIds.GeneralLanguage,
                title = "Language"
            ) {
                listOf(
                    entry(
                        key = "general_language_app",
                        title = "App language",
                        subtitle = "UI language",
                        icon = Icons.Default.Language,
                        nextScreenId = SettingsScreenIds.GeneralLanguageApp
                    ),
                    entry(
                        key = "general_language_subtitles",
                        title = "Subtitle language",
                        subtitle = "Default subtitles",
                        icon = Icons.Default.Language,
                        nextScreenId = SettingsScreenIds.GeneralLanguageSubtitles
                    )
                )
            },
            StaticSettingsScreen(
                id = SettingsScreenIds.GeneralLanguageApp,
                title = "App language"
            ) {
                delay(60)
                listOf(
                    entry(key = "language_en", title = "English"),
                    entry(key = "language_pl", title = "Polski"),
                    entry(key = "language_de", title = "Deutsch")
                )
            },
            StaticSettingsScreen(
                id = SettingsScreenIds.GeneralLanguageSubtitles,
                title = "Subtitle language"
            ) {
                delay(60)
                listOf(
                    entry(key = "subtitle_en", title = "English"),
                    entry(key = "subtitle_pl", title = "Polski"),
                    entry(key = "subtitle_none", title = "Disabled")
                )
            },
            StaticSettingsScreen(
                id = SettingsScreenIds.GeneralStartup,
                title = "Startup"
            ) {
                listOf(
                    entry(key = "startup_autoplay", title = "Auto play next episode"),
                    entry(key = "startup_resume", title = "Resume playback"),
                    entry(key = "startup_recommendations", title = "Show recommendations")
                )
            }
        )

        SettingsRegistry(
            screens = screens +
                extensionsFeature.staticScreens +
                generalFeature.staticScreens +
                providersFeature.staticScreens +
                layoutFeature.staticScreens +
                updatesFeature.staticScreens +
                playerFeature.staticScreens +
                accountFeature.staticScreens,
            dynamicScreenResolver = extensionsFeature::resolve
        )
    }
}

private fun entry(
    key: String,
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    nextScreenId: String? = null
): SettingsEntry {
    return SettingsEntry(
        title = title,
        subtitle = subtitle,
        icon = icon,
        stableKey = key,
        nextScreenId = nextScreenId,
        action = null
    )
}

object MasterDetailSettingsDefaults {
    const val RootScreenId: String = SettingsScreenIds.Root
}
