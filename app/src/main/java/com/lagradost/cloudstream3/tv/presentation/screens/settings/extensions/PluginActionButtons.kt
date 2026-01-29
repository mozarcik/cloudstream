package com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.compat.PluginItem
import com.lagradost.cloudstream3.tv.compat.PluginStatus
import android.app.Activity

/**
 * Action buttons for plugin (Download/Update/Delete)
 */
@Composable
fun PluginActionButtons(
    plugin: PluginItem,
    viewModel: ExtensionsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        plugin.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Autorzy: ${plugin.authors.joinToString(", ")}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Wersja: ${plugin.version}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        error?.let {
            Text(
                text = it,
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Action buttons based on status
        if (plugin.status == PluginStatus.NOT_DOWNLOADED) {
            Button(
                onClick = {
                    if (activity != null) {
                        isLoading = true
                        error = null
                        viewModel.downloadPlugin(activity, plugin) { result ->
                            isLoading = false
                            result.onFailure { error = it.message ?: "Błąd pobierania" }
                        }
                    } else {
                        error = "Activity not available"
                    }
                },
                enabled = !isLoading && activity != null
            ) {
                Text(if (isLoading) "Pobieranie..." else "Pobierz")
            }
        }
    }
}
            
//            PluginStatus.DOWNLOADED -> {
//                Button(
//                    onClick = {
//                        isLoading = true
//                        error = null
//                        viewModel.deletePlugin(plugin) { result ->
//                            isLoading = false
//                            result.onFailure { error = it.message ?: "Błąd usuwania" }
//                        }
//                    },
//                    enabled = !isLoading
//                ) {
//                    Text(if (isLoading) "Usuwanie..." else "Usuń")
//                }
//            }
//
//            PluginStatus.UPDATE_AVAILABLE -> {
//                Button(
//                    onClick = {
//                        if (activity != null) {
//                            isLoading = true
//                            error = null
//                            viewModel.updatePlugin(activity, plugin) { result ->
//                                isLoading = false
//                                result.onFailure { error = it.message ?: "Błąd aktualizacji" }
//                            }
//                        } else {
//                            error = "Activity not available"
//                        }
//                    },
//                    enabled = !isLoading && activity != null
//                ) {
//                    Text(if (isLoading) "Aktualizowanie..." else "Aktualizuj")
//                }
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                Button(
//                    onClick = {
//                        isLoading = true
//                        error = null
//                        viewModel.deletePlugin(plugin) { result ->
//                            isLoading = false
//                            result.onFailure { error = it.message ?: "Błąd usuwania" }
//                        }
//                    },
//                    enabled = !isLoading
//                ) {
//                    Text(if (isLoading) "Usuwanie..." else "Usuń")
//                }
//            }

