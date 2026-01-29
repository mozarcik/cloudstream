package com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.compat.PluginStatus
import com.lagradost.cloudstream3.tv.presentation.screens.settings.SettingsNode

/**
 * Builds the settings tree for Extensions section
 */
@Composable
fun buildExtensionsSettingsTree(
    viewModel: ExtensionsViewModel
): SettingsNode {
    val uiState by viewModel.uiState.collectAsState()
    

    // Early return for non-ready states
    val readyState = when (val state = uiState) {
        is ExtensionsUiState.Ready -> {
            state
        }
        is ExtensionsUiState.Loading -> {
            return SettingsNode(
                id = "extensions",
                title = "Rozszerzenia",
                content = { _ ->
                    Text("Ładowanie...", modifier = Modifier.padding(16.dp))
                }
            )
        }
        is ExtensionsUiState.Error -> {
            return SettingsNode(
                id = "extensions",
                title = "Rozszerzenia",
                content = { _ ->
                    Text("Błąd: ${state.message}", modifier = Modifier.padding(16.dp), color = Color.Red)
                }
            )
        }
    }
    
    val children = buildList {

        // 1. "Dodaj repozytorium" button
        add(
            SettingsNode(
                id = "extensions_add_repo",
                title = "Dodaj repozytorium",
                icon = Icons.Default.Add,
                content = { onBack ->
                    AddRepositoryForm(
                        viewModel = viewModel,
                        onBack = onBack
                    )
                }
            )
        )
        
        // 2. Plugin stats widget (non-clickable display)
        add(
            SettingsNode(
                id = "extensions_stats",
                title = "Statystyki",
                content = { _ ->
                    PluginStatsWidget(stats = readyState.pluginStats)
                }
            )
        )
        
        // 3. Repository list
        readyState.repositories.forEach { repo ->
            add(buildRepositoryNode(repo, viewModel, readyState))
        }
    }
    
    return SettingsNode(
        id = "extensions",
        title = "Rozszerzenia",
        icon = null, // Will be set from main tree
        children = children
    )
}

/**
 * Builds a single repository node with plugins as children
 */
private fun buildRepositoryNode(
    repo: com.lagradost.cloudstream3.tv.compat.RepositoryItem,
    viewModel: ExtensionsViewModel,
    readyState: ExtensionsUiState.Ready
): SettingsNode {
    // Use URL hash for stable ID
    val repoIdHash = repo.url.hashCode().toString().replace("-", "n")
    
    // Trigger lazy loading of plugins when repository is expanded
    viewModel.loadPluginsForRepo(repo.url)
    
    // Get plugins list (null = loading, empty = no plugins, list = has plugins)
    val pluginsList = readyState.pluginsByRepo[repo.url]

    val children = buildList {
        // First item: Remove repository
        add(
            SettingsNode(
                id = "repo_${repoIdHash}_delete",
                title = "Usuń repozytorium",
                content = { onBack ->
                    DeleteRepositoryConfirmation(
                        repository = repo,
                        viewModel = viewModel,
                        onBack = onBack
                    )
                }
            )
        )
        
        // Plugin list based on loading state
        when {
            pluginsList == null -> {
                // Still loading
                add(
                    SettingsNode(
                        id = "repo_${repoIdHash}_loading",
                        title = "Ładowanie...",
                        content = { _ ->
                            Text(
                                text = "Pobieranie listy pluginów...",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                )
            }
            pluginsList.isEmpty() -> {
                // Loaded but empty
                add(
                    SettingsNode(
                        id = "repo_${repoIdHash}_empty",
                        title = "Brak pluginów",
                        content = { _ ->
                            Text(
                                text = "To repozytorium nie zawiera żadnych pluginów",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                )
            }
            else -> {
                // Has plugins
                pluginsList.forEach { plugin ->
                    add(buildPluginNode(plugin, repoIdHash, viewModel))
                }
            }
        }
    }
    
    return SettingsNode(
        id = "repo_$repoIdHash",
        title = repo.name,
        description = repo.url,
        iconUrl = repo.iconUrl,
        icon = if (repo.iconUrl.isNullOrBlank()) CustomIcons.GitHub else null,
        children = children,
    )
}

/**
 * Builds a single plugin node with actions
 */
private fun buildPluginNode(
    plugin: com.lagradost.cloudstream3.tv.compat.PluginItem,
    repoIdHash: String,
    viewModel: ExtensionsViewModel
): SettingsNode {
    // Use plugin internal name for stable ID
    val pluginIdHash = plugin.internalName.hashCode().toString().replace("-", "n")
    
    return SettingsNode(
        id = "plugin_${repoIdHash}_$pluginIdHash",
        iconUrl = plugin.iconUrl,
        fallbackIconRes = com.lagradost.cloudstream3.R.drawable.ic_baseline_extension_24,
        title = plugin.name,
        description = plugin.description,
        content = { _ ->
            PluginActionButtons(
                plugin = plugin,
                viewModel = viewModel
            )
        },
    )
}

/**
 * Simple confirmation widget for repository deletion
 */
@Composable
private fun DeleteRepositoryConfirmation(
    repository: com.lagradost.cloudstream3.tv.compat.RepositoryItem,
    viewModel: ExtensionsViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Czy na pewno chcesz usunąć repozytorium \"${repository.name}\"?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Button(
            onClick = {
                viewModel.removeRepository(repository.url) { result ->
                    result.onSuccess {
                        onBack() // Go back after successful deletion
                    }
                    result.onFailure {
                        // TODO: Show error toast
                    }
                }
            }
        ) {
            Text("Usuń repozytorium")
        }
    }
}
