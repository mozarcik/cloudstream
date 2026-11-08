package com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.PluginItem
import com.lagradost.cloudstream3.tv.compat.PluginSettingsHostActivity
import com.lagradost.cloudstream3.tv.compat.PluginStatus
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsInfoParagraph
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsLevelHeader
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsRowWidget
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsSectionLabel
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsActionRow
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/**
 * Plugin level (mockups SettingsPlugin / SettingsPluginActions): icon + name + status, description,
 * authors and the action rows (Update / Settings / Delete / Download). Delete asks for confirmation
 * in the shared dialog; an action in progress turns its row into a spinner row.
 */
@Composable
fun PluginActionButtons(
    plugin: PluginItem,
    repositoryName: String,
    viewModel: ExtensionsViewModel,
    onPluginChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current as? Activity
    val context = LocalContext.current
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

    val firstRowRequester = remember { FocusRequester() }
    FocusRequestEffect(requester = firstRowRequester, requestKey = plugin.internalName)

    val idle = actionState == PluginActionState.Idle
    var firstRowAssigned = false
    fun firstRow(): Modifier {
        return if (!firstRowAssigned) {
            firstRowAssigned = true
            Modifier.focusRequester(firstRowRequester)
        } else {
            Modifier
        }
    }

    BackHandler(enabled = showDeleteConfirmation) { showDeleteConfirmation = false }

    if (showDeleteConfirmation) {
        DeleteConfirmationPanel(
            plugin = plugin,
            repositoryName = repositoryName,
            onDelete = {
                showDeleteConfirmation = false
                actionState = PluginActionState.Deleting
                error = null
                viewModel.deletePlugin(plugin) { result ->
                    actionState = PluginActionState.Idle
                    result.onSuccess {
                        Toast.makeText(context, context.getString(R.string.plugin_deleted), Toast.LENGTH_SHORT).show()
                        onPluginChanged()
                    }.onFailure { throwable ->
                        error = throwable.message ?: context.getString(R.string.plugin_delete_failed)
                    }
                }
            },
            onCancel = { showDeleteConfirmation = false },
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        PluginInfoHeader(plugin = plugin)
        Column(
            verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsItemGap),
            modifier = Modifier.padding(start = TvDimens.SettingsPaneStartInset, top = TvDimens.SettingsListTopPadding, bottom = TvDimens.SettingsListTopPadding),
        ) {
        error?.let { message ->
            SettingsInfoParagraph(text = message, color = TvColors.ErrorText)
        }

        if (plugin.status == PluginStatus.UPDATE_AVAILABLE) {
            val updating = actionState == PluginActionState.Updating
            SettingsActionRow(
                title = stringResource(if (updating) R.string.plugin_updating else R.string.update),
                icon = Icons.Outlined.SystemUpdateAlt,
                loading = updating,
                enabled = idle || updating,
                onClick = {
                    if (!idle) return@SettingsActionRow
                    if (activity == null) {
                        error = context.getString(R.string.error)
                        return@SettingsActionRow
                    }
                    actionState = PluginActionState.Updating
                    error = null
                    viewModel.updatePlugin(activity, plugin) { result ->
                        actionState = PluginActionState.Idle
                        result.onSuccess {
                            Toast.makeText(context, context.getString(R.string.plugin_updated), Toast.LENGTH_SHORT).show()
                            onPluginChanged()
                        }.onFailure { throwable ->
                            error = throwable.message ?: context.getString(R.string.plugin_update_failed)
                        }
                    }
                },
                modifier = firstRow(),
            )
        }

        if (plugin.status == PluginStatus.NOT_DOWNLOADED) {
            val downloading = actionState == PluginActionState.Downloading
            SettingsActionRow(
                title = stringResource(if (downloading) R.string.plugin_downloading else R.string.download),
                icon = Icons.Outlined.FileDownload,
                loading = downloading,
                enabled = (idle || downloading) && activity != null,
                onClick = {
                    if (!idle) return@SettingsActionRow
                    if (activity == null) {
                        error = context.getString(R.string.error)
                        return@SettingsActionRow
                    }
                    actionState = PluginActionState.Downloading
                    error = null
                    viewModel.downloadPlugin(activity, plugin) { result ->
                        actionState = PluginActionState.Idle
                        result.onSuccess {
                            Toast.makeText(context, context.getString(R.string.plugin_downloaded), Toast.LENGTH_SHORT).show()
                            onPluginChanged()
                        }.onFailure { throwable ->
                            error = throwable.message ?: context.getString(R.string.plugin_download_failed)
                        }
                    }
                },
                modifier = firstRow(),
            )
        }

        if (plugin.hasSettings) {
            SettingsActionRow(
                title = stringResource(R.string.title_settings),
                subtitle = stringResource(R.string.plugin_settings_legacy_summary),
                icon = Icons.Outlined.Settings,
                enabled = idle,
                widget = SettingsRowWidget.Custom { _ ->
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(TvDimens.SettingsItemGlyphSize),
                    )
                },
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
                modifier = firstRow(),
            )
        }

        if (plugin.status != PluginStatus.NOT_DOWNLOADED) {
            val removing = actionState == PluginActionState.Deleting
            SettingsActionRow(
                title = stringResource(if (removing) R.string.plugin_removing else R.string.delete),
                icon = Icons.Outlined.DeleteOutline,
                loading = removing,
                destructive = true,
                enabled = idle || removing,
                onClick = { if (idle) showDeleteConfirmation = true },
                modifier = firstRow(),
            )
        }
        }
    }

}

/** Confirmation level: "Delete plugin?" with the question as subtitle, focus on Cancel. */
@Composable
private fun DeleteConfirmationPanel(
    plugin: PluginItem,
    repositoryName: String,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cancelRequester = remember { FocusRequester() }
    FocusRequestEffect(requester = cancelRequester, requestKey = plugin.internalName)
    Column(modifier = modifier.fillMaxSize()) {
        SettingsLevelHeader(
            title = stringResource(R.string.delete_plugin),
            description = stringResource(R.string.tv_settings_plugin_delete_message, plugin.name, repositoryName),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsItemGap),
            modifier = Modifier.padding(start = TvDimens.SettingsPaneStartInset, top = TvDimens.SettingsListTopPadding),
        ) {
            SettingsActionRow(
                title = stringResource(R.string.delete),
                icon = Icons.Outlined.DeleteOutline,
                destructive = true,
                onClick = onDelete,
            )
            SettingsActionRow(
                title = stringResource(R.string.cancel),
                icon = Icons.Default.Close,
                onClick = onCancel,
                modifier = Modifier.focusRequester(cancelRequester),
            )
        }
    }
}

/** Plugin name as the panel title, status as its subtitle, then description and authors. */
@Composable
private fun PluginInfoHeader(plugin: PluginItem, maxDescriptionLines: Int = Int.MAX_VALUE) {
    SettingsLevelHeader(title = plugin.name, description = pluginStatusText(plugin))
    Column(modifier = Modifier.padding(start = TvDimens.SettingsPaneStartInset)) {
        plugin.description?.takeIf { it.isNotBlank() }?.let { description ->
            SettingsInfoParagraph(text = description, maxLines = maxDescriptionLines)
        }
        if (plugin.authors.isNotEmpty()) {
            SettingsSectionLabel(text = stringResource(R.string.tv_settings_authors))
            SettingsInfoParagraph(text = plugin.authors.joinToString(", "), color = TvColors.Text)
        }
    }
}

/** Plugin card for the preview panel: same header, description and authors, without actions. */
@Composable
fun PluginPreviewCard(plugin: PluginItem, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        PluginInfoHeader(plugin = plugin, maxDescriptionLines = 6)
    }
}

@Composable
private fun pluginStatusText(plugin: PluginItem): String {
    val label = when (plugin.status) {
        PluginStatus.NOT_DOWNLOADED -> stringResource(R.string.tv_settings_not_downloaded)
        PluginStatus.DOWNLOADED -> stringResource(R.string.downloaded)
        PluginStatus.UPDATE_AVAILABLE -> stringResource(R.string.tv_settings_update_available)
    }
    return stringResource(R.string.tv_settings_plugin_status_format, label, plugin.version)
}

private enum class PluginActionState {
    Idle,
    OpeningSettings,
    Downloading,
    Updating,
    Deleting
}
