package com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.PluginItem
import com.lagradost.cloudstream3.tv.compat.PluginSettingsHostActivity
import com.lagradost.cloudstream3.tv.compat.PluginStatus

/**
 * Action buttons for plugin (Settings/Download/Update/Delete).
 */
@Composable
fun PluginActionButtons(
    plugin: PluginItem,
    viewModel: ExtensionsViewModel,
    onPluginChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activity = androidx.compose.ui.platform.LocalContext.current as? Activity
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentPlugin by rememberUpdatedState(plugin)
    val currentOnPluginChanged by rememberUpdatedState(onPluginChanged)

    var actionState by remember(plugin.internalName, plugin.status) {
        mutableStateOf(PluginActionState.Idle)
    }
    var error by remember(plugin.internalName, plugin.status) { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember(plugin.internalName, plugin.status) {
        mutableStateOf(false)
    }

    LaunchedEffect(plugin.internalName, plugin.status) {
        if (plugin.status == PluginStatus.NOT_DOWNLOADED) {
            showDeleteConfirmation = false
        }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        actionState = PluginActionState.Idle
        error = null
        viewModel.refreshData(currentPlugin.repositoryUrl)
        currentOnPluginChanged()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        plugin.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (plugin.hasSettings) {
            Text(
                text = stringResource(R.string.plugin_settings_legacy_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Autorzy: ${plugin.authors.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "-"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Wersja: ${plugin.version}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        error?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (plugin.hasSettings) {
            OutlinedButton(
                onClick = {
                    error = null
                    actionState = PluginActionState.OpeningSettings
                    settingsLauncher.launch(
                        PluginSettingsHostActivity.createIntent(
                            context = context,
                            repositoryUrl = plugin.repositoryUrl,
                            internalName = plugin.internalName
                        )
                    )
                },
                enabled = actionState == PluginActionState.Idle
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.title_settings))
            }
        }

        when (plugin.status) {
            PluginStatus.NOT_DOWNLOADED -> {
                Button(
                    onClick = {
                        if (activity == null) {
                            error = context.getString(R.string.error)
                            return@Button
                        }

                        actionState = PluginActionState.Downloading
                        error = null
                        viewModel.downloadPlugin(activity, plugin) { result ->
                            actionState = PluginActionState.Idle
                            result.onSuccess {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.plugin_downloaded),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onPluginChanged()
                            }.onFailure { throwable ->
                                error = throwable.message ?: context.getString(R.string.plugin_download_failed)
                            }
                        }
                    },
                    enabled = actionState == PluginActionState.Idle && activity != null
                ) {
                    Text(
                        text = if (actionState == PluginActionState.Downloading) {
                            stringResource(R.string.plugin_downloading)
                        } else {
                            stringResource(R.string.download)
                        }
                    )
                }
            }

            PluginStatus.DOWNLOADED -> {
                DeletePluginButton(
                    enabled = actionState == PluginActionState.Idle,
                    isRemoving = actionState == PluginActionState.Deleting,
                    onClick = {
                        showDeleteConfirmation = true
                    }
                )
            }

            PluginStatus.UPDATE_AVAILABLE -> {
                Button(
                    onClick = {
                        if (activity == null) {
                            error = context.getString(R.string.error)
                            return@Button
                        }

                        actionState = PluginActionState.Updating
                        error = null
                        viewModel.updatePlugin(activity, plugin) { result ->
                            actionState = PluginActionState.Idle
                            result.onSuccess {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.plugin_updated),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onPluginChanged()
                            }.onFailure { throwable ->
                                error = throwable.message ?: context.getString(R.string.plugin_update_failed)
                            }
                        }
                    },
                    enabled = actionState == PluginActionState.Idle && activity != null
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdateAlt,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (actionState == PluginActionState.Updating) {
                            stringResource(R.string.plugin_updating)
                        } else {
                            stringResource(R.string.update)
                        }
                    )
                }

                DeletePluginButton(
                    enabled = actionState == PluginActionState.Idle,
                    isRemoving = actionState == PluginActionState.Deleting,
                    onClick = {
                        showDeleteConfirmation = true
                    }
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        DeletePluginDialog(
            pluginName = plugin.name,
            onDismiss = {
                showDeleteConfirmation = false
            },
            onConfirm = {
                showDeleteConfirmation = false
                actionState = PluginActionState.Deleting
                error = null
                viewModel.deletePlugin(plugin) { result ->
                    actionState = PluginActionState.Idle
                    result.onSuccess {
                        Toast.makeText(
                            context,
                            context.getString(R.string.plugin_deleted),
                            Toast.LENGTH_SHORT
                        ).show()
                        onPluginChanged()
                    }.onFailure { throwable ->
                        error = throwable.message ?: context.getString(R.string.plugin_delete_failed)
                    }
                }
            }
        )
    }
}

@Composable
private fun DeletePluginButton(
    enabled: Boolean,
    isRemoving: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled
    ) {
        Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isRemoving) {
                stringResource(R.string.plugin_removing)
            } else {
                stringResource(R.string.delete)
            }
        )
    }
}

@Composable
private fun DeletePluginDialog(
    pluginName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            colors = SurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
            ),
            modifier = Modifier.widthIn(max = 720.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.delete_plugin),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = pluginName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.delete_message, pluginName),
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}

private enum class PluginActionState {
    Idle,
    OpeningSettings,
    Downloading,
    Updating,
    Deleting
}
