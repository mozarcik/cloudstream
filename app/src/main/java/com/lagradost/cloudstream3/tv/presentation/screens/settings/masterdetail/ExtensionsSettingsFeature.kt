package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import android.content.Context
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.first
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.PluginItem
import com.lagradost.cloudstream3.tv.compat.PluginStatus
import com.lagradost.cloudstream3.tv.compat.RepositoryItem
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.AddRepositoryForm
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.CustomIcons
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.ExtensionsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.ExtensionsViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.PluginActionButtons
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.PluginPreviewCard
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val ExtensionsScreenPrefix = "settings_extensions"
private const val ExtensionsAddRepoScreenId = "$ExtensionsScreenPrefix/add_repository"
private const val ExtensionsRepoScreenPrefix = "$ExtensionsScreenPrefix/repository/"
private const val ExtensionsPluginScreenPrefix = "$ExtensionsScreenPrefix/plugin/"

@Composable
fun rememberExtensionsSettingsFeature(
    extensionsTitle: String
): ExtensionsSettingsFeature {
    val context = LocalContext.current
    val viewModel = remember { ExtensionsViewModel() }

    LaunchedEffect(viewModel, context) {
        viewModel.setupPrefsListener(context)
    }

    return remember(viewModel, extensionsTitle, context) {
        ExtensionsSettingsFeature(
            context = context,
            viewModel = viewModel,
            extensionsTitle = extensionsTitle
        )
    }
}

class ExtensionsSettingsFeature(
    private val context: Context,
    private val viewModel: ExtensionsViewModel,
    private val extensionsTitle: String
) {
    val staticScreens: List<SettingsScreen> = listOf(
        ExtensionsMainScreen(),
        AddRepositoryScreen()
    )

    fun resolve(screenId: String): SettingsScreen? {
        parseRepositoryScreen(screenId)?.let { repoUrl ->
            return RepositoryPluginsScreen(repoUrl = repoUrl)
        }
        parsePluginScreen(screenId)?.let { (repoUrl, pluginInternalName) ->
            return PluginDetailsScreen(
                repositoryUrl = repoUrl,
                pluginInternalName = pluginInternalName
            )
        }
        return null
    }

    private fun currentReadyState(): ExtensionsUiState.Ready? {
        return viewModel.uiState.value as? ExtensionsUiState.Ready
    }

    private fun repositoryScreenId(repositoryUrl: String): String {
        return ExtensionsRepoScreenPrefix + encode(repositoryUrl)
    }

    private fun pluginScreenId(repositoryUrl: String, pluginInternalName: String): String {
        return ExtensionsPluginScreenPrefix + encode(repositoryUrl) + "|" + encode(pluginInternalName)
    }

    private fun parseRepositoryScreen(screenId: String): String? {
        if (!screenId.startsWith(ExtensionsRepoScreenPrefix)) return null
        val encoded = screenId.removePrefix(ExtensionsRepoScreenPrefix)
        return decode(encoded)
    }

    private fun parsePluginScreen(screenId: String): Pair<String, String>? {
        if (!screenId.startsWith(ExtensionsPluginScreenPrefix)) return null
        val payload = screenId.removePrefix(ExtensionsPluginScreenPrefix)
        val separatorIndex = payload.indexOf('|')
        if (separatorIndex <= 0 || separatorIndex >= payload.lastIndex) return null

        val encodedRepoUrl = payload.substring(0, separatorIndex)
        val encodedInternalName = payload.substring(separatorIndex + 1)
        return decode(encodedRepoUrl) to decode(encodedInternalName)
    }

    private inner class ExtensionsMainScreen : SettingsScreen {
        override val id: String = ExtensionsScreenPrefix
        override val title: String = extensionsTitle

        override suspend fun onRetry() {
            viewModel.refreshData()
        }

        override suspend fun load(): List<SettingsEntry> {
            val state = viewModel.uiState.first { it !is ExtensionsUiState.Loading }
            if (state is ExtensionsUiState.Error) {
                throw SettingsLoadException(
                    errorTitle = context.getString(
                        R.string.tv_settings_load_error_format,
                        extensionsTitle.lowercase()
                    ),
                    message = state.message
                )
            }
            val ready = state as? ExtensionsUiState.Ready ?: return emptyList()
            return buildList {
                add(
                    simpleEntry(
                        stableKey = "extensions_add_repository",
                        title = context.getString(R.string.add_repository),
                        subtitle = context.getString(R.string.tv_settings_add_repository_summary),
                        icon = Icons.Default.Add,
                        nextScreenId = ExtensionsAddRepoScreenId
                    )
                )

                if (ready.repositories.isEmpty()) {
                    add(
                        SettingsEntry(
                            title = context.getString(R.string.tv_settings_no_repositories),
                            subtitle = context.getString(R.string.tv_settings_no_repositories_hint),
                            stableKey = "extensions_no_repositories",
                            type = SettingsEntryType.Message
                        )
                    )
                } else {
                    ready.repositories.forEach { repository ->
                        add(
                            simpleEntry(
                                stableKey = "repo_${repository.url.hashCode()}",
                                title = repository.name,
                                subtitle = repository.url,
                                icon = CustomIcons.GitHub,
                                iconUrl = repository.iconUrl,
                                nextScreenId = repositoryScreenId(repository.url)
                            )
                        )
                    }
                }

                ready.pluginStats?.let { stats ->
                    add(
                        SettingsEntry(
                            title = context.getString(
                                R.string.tv_settings_plugin_stats_format,
                                stats.downloaded,
                                stats.notDownloaded,
                                stats.total
                            ),
                            stableKey = "extensions_plugin_stats_inline",
                            type = SettingsEntryType.Message
                        )
                    )
                }
            }
        }
    }

    private inner class AddRepositoryScreen : SettingsScreen {
        override val id: String = ExtensionsAddRepoScreenId
        override val title: String = context.getString(R.string.add_repository)
        override val hasCustomContent: Boolean = true

        @Composable
        override fun Content(
            modifier: Modifier,
            contentPadding: PaddingValues,
            isPreview: Boolean,
            onBack: () -> Unit,
            onDataChanged: (String) -> Unit
        ) {
            // The next level of "Add repository" has no preview.
            if (isPreview) return
            AddRepositoryForm(
                viewModel = viewModel,
                onBack = onBack,
                onRepositoryAdded = {
                    onDataChanged(ExtensionsScreenPrefix)
                },
                modifier = modifier.fillMaxSize()
            )
        }
    }

    private inner class RepositoryPluginsScreen(
        private val repoUrl: String
    ) : SettingsScreen {
        override val id: String = repositoryScreenId(repoUrl)
        override val title: String
            get() = repositoryName(repoUrl)

        override suspend fun load(): List<SettingsEntry> {
            val state = viewModel.uiState.first { it !is ExtensionsUiState.Loading }
            if (state !is ExtensionsUiState.Ready) {
                throw SettingsLoadException(
                    errorTitle = context.getString(R.string.tv_settings_load_error_format, title),
                    message = (state as? ExtensionsUiState.Error)?.message.orEmpty()
                )
            }

            val repository = state.repositories.firstOrNull { it.url == repoUrl }
                ?: throw SettingsLoadException(
                    errorTitle = context.getString(R.string.tv_settings_load_error_format, title),
                    message = context.getString(R.string.no_repository_found_error)
                )

            val cachedPlugins = state.pluginsByRepo[repoUrl]
            val plugins = if (cachedPlugins != null) {
                cachedPlugins
            } else {
                viewModel.loadPluginsForRepo(repoUrl)
                viewModel.awaitPluginsForRepo(repoUrl = repoUrl)
            }

            return buildRepositoryEntries(repository, plugins)
        }
    }

    private inner class PluginDetailsScreen(
        private val repositoryUrl: String,
        private val pluginInternalName: String
    ) : SettingsScreen {
        override val id: String = pluginScreenId(repositoryUrl, pluginInternalName)
        override val title: String
            get() = resolvePlugin(repositoryUrl, pluginInternalName)?.name ?: pluginInternalName
        override val hasCustomContent: Boolean = true

        @Composable
        override fun Content(
            modifier: Modifier,
            contentPadding: PaddingValues,
            isPreview: Boolean,
            onBack: () -> Unit,
            onDataChanged: (String) -> Unit
        ) {
            val uiState by viewModel.uiState.collectAsState()
            val readyState = uiState as? ExtensionsUiState.Ready
            val plugin = readyState
                ?.pluginsByRepo
                ?.get(repositoryUrl)
                ?.firstOrNull { item -> item.internalName == pluginInternalName }

            LaunchedEffect(repositoryUrl, readyState?.pluginsByRepo?.get(repositoryUrl)) {
                if (plugin == null) {
                    viewModel.loadPluginsForRepo(repositoryUrl)
                }
            }

            if (plugin == null) return
            if (isPreview) {
                PluginPreviewCard(plugin = plugin, modifier = modifier)
            } else {
                PluginActionButtons(
                    plugin = plugin,
                    repositoryName = repositoryName(repositoryUrl),
                    viewModel = viewModel,
                    onPluginChanged = {
                        onDataChanged(ExtensionsScreenPrefix)
                    },
                    modifier = modifier
                )
            }
        }
    }

    private fun repositoryName(repoUrl: String): String {
        return currentReadyState()
            ?.repositories
            ?.firstOrNull { repository -> repository.url == repoUrl }
            ?.name
            ?: repoUrl
    }

    private fun buildRepositoryEntries(
        repository: RepositoryItem,
        plugins: List<PluginItem>?
    ): List<SettingsEntry> {
        if (plugins.isNullOrEmpty()) {
            return listOf(
                SettingsEntry(
                    title = context.getString(R.string.tv_settings_no_plugins),
                    subtitle = context.getString(R.string.tv_settings_no_plugins_hint),
                    stableKey = "repository_plugins_empty_${repository.url.hashCode()}",
                    type = SettingsEntryType.Message
                )
            )
        }
        return plugins.map { plugin ->
            simpleEntry(
                stableKey = "plugin_${plugin.internalName}",
                title = plugin.name,
                subtitle = pluginStatusSubtitle(plugin),
                initials = settingsInitials(plugin.name),
                tileColorArgb = settingsInitialsColor(plugin.name).toArgb(),
                iconUrl = plugin.iconUrl,
                trailingIcon = if (plugin.status == PluginStatus.NOT_DOWNLOADED) null else Icons.Outlined.CheckCircle,
                nextScreenId = pluginScreenId(
                    repositoryUrl = repository.url,
                    pluginInternalName = plugin.internalName
                )
            )
        }
    }

    private fun pluginStatusSubtitle(plugin: PluginItem): String {
        val label = when (plugin.status) {
            PluginStatus.NOT_DOWNLOADED -> context.getString(R.string.tv_settings_not_downloaded)
            PluginStatus.DOWNLOADED -> context.getString(R.string.downloaded)
            PluginStatus.UPDATE_AVAILABLE -> context.getString(R.string.tv_settings_update_available)
        }
        return context.getString(R.string.tv_settings_plugin_row_format, label, plugin.version)
    }

    private fun resolvePlugin(repositoryUrl: String, pluginInternalName: String): PluginItem? {
        val readyState = currentReadyState() ?: return null
        return readyState.pluginsByRepo[repositoryUrl]
            ?.firstOrNull { plugin -> plugin.internalName == pluginInternalName }
    }

    private fun simpleEntry(
        stableKey: String,
        title: String,
        subtitle: String? = null,
        icon: ImageVector? = null,
        iconUrl: String? = null,
        fallbackIconRes: Int? = null,
        initials: String? = null,
        tileColorArgb: Int? = null,
        trailingIcon: ImageVector? = null,
        nextScreenId: String? = null
    ): SettingsEntry {
        return SettingsEntry(
            title = title,
            subtitle = subtitle,
            icon = icon,
            iconUrl = iconUrl,
            fallbackIconRes = fallbackIconRes,
            initials = initials,
            tileColorArgb = tileColorArgb,
            trailingIcon = trailingIcon,
            stableKey = stableKey,
            nextScreenId = nextScreenId,
            action = null
        )
    }

    private fun encode(input: String): String {
        return URLEncoder.encode(input, StandardCharsets.UTF_8.toString())
    }

    private fun decode(input: String): String {
        return runCatching {
            URLDecoder.decode(input, StandardCharsets.UTF_8.toString())
        }.getOrDefault(input)
    }
}
