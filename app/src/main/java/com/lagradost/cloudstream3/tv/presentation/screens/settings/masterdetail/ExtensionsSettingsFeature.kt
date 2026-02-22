package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.PluginItem
import com.lagradost.cloudstream3.tv.compat.PluginStatus
import com.lagradost.cloudstream3.tv.compat.RepositoryItem
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.AddRepositoryForm
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.CustomIcons
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.ExtensionsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.ExtensionsViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.PluginActionButtons
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

    return remember(viewModel, extensionsTitle) {
        ExtensionsSettingsFeature(
            viewModel = viewModel,
            extensionsTitle = extensionsTitle
        )
    }
}

class ExtensionsSettingsFeature(
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

        override suspend fun load(): List<SettingsEntry> {
            return when (val state = viewModel.uiState.value) {
                is ExtensionsUiState.Loading -> listOf(
                    simpleEntry(
                        stableKey = "extensions_loading",
                        title = "Ładowanie rozszerzeń..."
                    )
                )
                is ExtensionsUiState.Error -> listOf(
                    simpleEntry(
                        stableKey = "extensions_error",
                        title = "Błąd ładowania rozszerzeń",
                        subtitle = state.message
                    )
                )
                is ExtensionsUiState.Ready -> {
                    buildList {
                        add(
                            simpleEntry(
                                stableKey = "extensions_add_repository",
                                title = "Dodaj repozytorium",
                                subtitle = "Dodaj nowe źródło pluginów",
                                nextScreenId = ExtensionsAddRepoScreenId
                            )
                        )
                        add(
                            simpleEntry(
                                stableKey = "extensions_plugin_stats_inline",
                                title = "Statystyki pluginów",
                                subtitle = state.pluginStats?.let { stats ->
                                    "Pobrane: ${stats.downloaded}, niepobrane: ${stats.notDownloaded}, razem: ${stats.total}"
                                } ?: "Ładowanie statystyk..."
                            )
                        )

                        if (state.repositories.isEmpty()) {
                            add(
                                simpleEntry(
                                    stableKey = "extensions_no_repositories",
                                    title = "Brak repozytoriów",
                                    subtitle = "Dodaj repozytorium aby przeglądać pluginy"
                                )
                            )
                        } else {
                            state.repositories.forEach { repository ->
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
                    }
                }
            }
        }
    }

    private inner class AddRepositoryScreen : SettingsScreen {
        override val id: String = ExtensionsAddRepoScreenId
        override val title: String = "Dodaj repozytorium"
        override val hasCustomContent: Boolean = true

        @Composable
        override fun Content(
            modifier: Modifier,
            contentPadding: PaddingValues,
            isPreview: Boolean,
            onBack: () -> Unit,
            onDataChanged: (String) -> Unit
        ) {
            AddRepositoryForm(
                viewModel = viewModel,
                onBack = onBack,
                onRepositoryAdded = {
                    onDataChanged(ExtensionsScreenPrefix)
                },
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        }
    }

    private inner class RepositoryPluginsScreen(
        private val repoUrl: String
    ) : SettingsScreen {
        override val id: String = repositoryScreenId(repoUrl)
        override val title: String
            get() = currentReadyState()
                ?.repositories
                ?.firstOrNull { repository -> repository.url == repoUrl }
                ?.name
                ?: "Repozytorium"

        override suspend fun load(): List<SettingsEntry> {
            val state = viewModel.uiState.value
            if (state !is ExtensionsUiState.Ready) {
                return listOf(
                    simpleEntry(
                        stableKey = "repository_loading_$repoUrl",
                        title = "Ładowanie repozytorium..."
                    )
                )
            }

            val repository = state.repositories.firstOrNull { it.url == repoUrl }
            if (repository == null) {
                return listOf(
                    simpleEntry(
                        stableKey = "repository_missing_$repoUrl",
                        title = "Repozytorium nie istnieje",
                        subtitle = "Wróć do listy repozytoriów"
                    )
                )
            }

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
            get() = resolvePlugin(repositoryUrl, pluginInternalName)?.name ?: "Plugin"
        override val hasCustomContent: Boolean = true

        override suspend fun load(): List<SettingsEntry> {
            val plugin = resolvePlugin(repositoryUrl, pluginInternalName)
            return if (plugin == null) {
                listOf(
                    simpleEntry(
                        stableKey = "plugin_loading_$pluginInternalName",
                        title = "Ładowanie pluginu..."
                    )
                )
            } else {
                listOf(
                    simpleEntry(
                        stableKey = "plugin_status_$pluginInternalName",
                        title = plugin.name,
                        subtitle = pluginStatusSubtitle(plugin)
                    )
                )
            }
        }

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

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (plugin == null) {
                    Text(
                        text = "Ładowanie pluginu...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    PluginActionButtons(
                        plugin = plugin,
                        viewModel = viewModel,
                        onPluginChanged = {
                            onDataChanged(ExtensionsScreenPrefix)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    private fun buildRepositoryEntries(
        repository: RepositoryItem,
        plugins: List<PluginItem>?
    ): List<SettingsEntry> {
        return buildList {
            add(
                simpleEntry(
                    stableKey = "repository_header_${repository.url.hashCode()}",
                    title = repository.name,
                    subtitle = repository.url
                )
            )

            when {
                plugins == null -> {
                    add(
                        simpleEntry(
                            stableKey = "repository_plugins_loading_${repository.url.hashCode()}",
                            title = "Ładowanie pluginów..."
                        )
                    )
                }
                plugins.isEmpty() -> {
                    add(
                        simpleEntry(
                            stableKey = "repository_plugins_empty_${repository.url.hashCode()}",
                            title = "Brak pluginów",
                            subtitle = "To repozytorium nie udostępnia pluginów"
                        )
                    )
                }
                else -> {
                    plugins.forEach { plugin ->
                        add(
                            simpleEntry(
                                stableKey = "plugin_${plugin.internalName}",
                                title = plugin.name,
                                subtitle = pluginStatusSubtitle(plugin),
                                iconUrl = plugin.iconUrl,
                                fallbackIconRes = R.drawable.ic_baseline_extension_24,
                                nextScreenId = pluginScreenId(
                                    repositoryUrl = repository.url,
                                    pluginInternalName = plugin.internalName
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun pluginStatusSubtitle(plugin: PluginItem): String {
        return when (plugin.status) {
            PluginStatus.NOT_DOWNLOADED -> "Niepobrany • v${plugin.version}"
            PluginStatus.DOWNLOADED -> "Pobrany • v${plugin.version}"
            PluginStatus.UPDATE_AVAILABLE -> "Aktualizacja dostępna • v${plugin.version}"
        }
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
        nextScreenId: String? = null
    ): SettingsEntry {
        return SettingsEntry(
            title = title,
            subtitle = subtitle,
            icon = icon,
            iconUrl = iconUrl,
            fallbackIconRes = fallbackIconRes,
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
