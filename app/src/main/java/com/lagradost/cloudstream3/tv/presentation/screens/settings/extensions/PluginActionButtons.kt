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
import androidx.compose.material.icons.Icons
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
import com.lagradost.cloudstream3.tv.presentation.common.TvConfirmDialog
import com.lagradost.cloudstream3.tv.presentation.common.TvRowTrailing
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.LocalSettingsBreadcrumb
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsActionRow
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsDivider
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsInitialsTile
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsPreviewRow
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsPreviewSection
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsStateBox
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.settingsInitials
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.settingsInitialsColor
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        val crumb = LocalSettingsBreadcrumb.current
        if (crumb.isNotEmpty()) {
            Text(
                text = crumb,
                style = TvTextStyles.SettingsCrumb,
                color = TvColors.LabelPanel,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = TvDimens.SettingsHeaderHorizontalPadding),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TvDimens.SettingsDp14),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TvDimens.SettingsHeaderHorizontalPadding)
                .padding(top = TvDimens.SettingsDp10),
        ) {
            PluginTile(plugin = plugin, size = TvDimens.SettingsDp52, radius = TvDimens.SettingsDp13)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plugin.name,
                    style = TvTextStyles.Level,
                    color = TvColors.Text,
                    maxLines = 1,
                )
                Text(
                    text = pluginStatusText(plugin),
                    style = TvTextStyles.Chip,
                    color = if (plugin.status == PluginStatus.NOT_DOWNLOADED) TvColors.RowSubtitle else TvColors.Success,
                    maxLines = 1,
                    modifier = Modifier.padding(top = TvDimens.SettingsDp3),
                )
            }
        }
        plugin.description?.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                text = description,
                style = TvTextStyles.FeaturedBody,
                color = TvColors.Body,
                modifier = Modifier
                    .padding(horizontal = TvDimens.SettingsHeaderHorizontalPadding)
                    .padding(top = TvDimens.SettingsDp12),
            )
        }
        if (plugin.authors.isNotEmpty()) {
            Text(
                text = stringResource(R.string.tv_settings_authors).uppercase(),
                style = TvTextStyles.PanelGroupHeader,
                color = TvColors.LabelPanel,
                modifier = Modifier
                    .padding(horizontal = TvDimens.SettingsHeaderHorizontalPadding)
                    .padding(top = TvDimens.SettingsDp8),
            )
            Text(
                text = plugin.authors.joinToString(", "),
                style = TvTextStyles.Meta,
                color = TvColors.Text,
                modifier = Modifier
                    .padding(horizontal = TvDimens.SettingsHeaderHorizontalPadding)
                    .padding(top = TvDimens.SettingsDp3),
            )
        }
        SettingsDivider(top = TvDimens.SettingsDp14, bottom = TvDimens.SettingsDp8)

        error?.let { message ->
            SettingsStateBox(message = message, modifier = Modifier.padding(bottom = TvDimens.SettingsDp6))
        }

        if (plugin.status == PluginStatus.UPDATE_AVAILABLE) {
            val updating = actionState == PluginActionState.Updating
            SettingsActionRow(
                title = stringResource(if (updating) R.string.plugin_updating else R.string.update),
                icon = Icons.Outlined.SystemUpdateAlt,
                loading = updating,
                enabled = idle || updating,
                height = TvDimens.SettingsDp48,
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
                height = TvDimens.SettingsDp48,
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
                height = TvDimens.SettingsDp48,
                trailing = TvRowTrailing.Custom { _ ->
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(TvDimens.SettingsRowChevronSize),
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
                height = TvDimens.SettingsDp48,
                onClick = { if (idle) showDeleteConfirmation = true },
                modifier = firstRow(),
            )
        }
    }

    if (showDeleteConfirmation) {
        TvConfirmDialog(
            title = stringResource(R.string.delete_plugin),
            description = stringResource(R.string.tv_settings_plugin_delete_message, plugin.name, repositoryName),
            primaryText = stringResource(R.string.delete),
            secondaryText = stringResource(R.string.cancel),
            onPrimary = {
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
            onSecondary = { showDeleteConfirmation = false },
            onDismiss = { showDeleteConfirmation = false },
            destructive = true,
            focusSecondaryFirst = true,
            topOffset = TvDimens.PluginDeleteDialogTop,
        )
    }
}

/** Dimmed, non-interactive plugin card shown on the right while the row is focused. */
@Composable
fun PluginPreviewCard(plugin: PluginItem, modifier: Modifier = Modifier) {
    val inset = TvDimens.SettingsPreviewRowHorizontalPadding
    Column(modifier = modifier.fillMaxSize()) {
        PluginTile(plugin = plugin, size = TvDimens.SettingsDp48, radius = TvDimens.SettingsDp12, dimmed = true, modifier = Modifier.padding(horizontal = inset))
        Text(
            text = plugin.name,
            style = TvTextStyles.PanelTitle,
            color = TvColors.RowSubtitle,
            maxLines = 1,
            modifier = Modifier.padding(start = inset, end = inset, top = TvDimens.SettingsDp12),
        )
        Text(
            text = pluginStatusText(plugin),
            style = TvTextStyles.Chip,
            color = TvColors.SettingsPreviewSubtext,
            maxLines = 1,
            modifier = Modifier.padding(start = inset, end = inset, top = TvDimens.SettingsDp4),
        )
        plugin.description?.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                text = description,
                style = TvTextStyles.FeaturedBody,
                color = TvColors.SettingsPreviewText,
                maxLines = 4,
                modifier = Modifier.padding(start = inset, end = inset, top = TvDimens.SettingsDp12),
            )
        }
        if (plugin.authors.isNotEmpty()) {
            Column {
                SettingsPreviewSection(text = stringResource(R.string.tv_settings_authors))
                Text(
                    text = plugin.authors.joinToString(", "),
                    style = TvTextStyles.Meta,
                    color = TvColors.SettingsPreviewText,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = inset),
                )
            }
        }
        if (plugin.status != PluginStatus.NOT_DOWNLOADED) {
            SettingsPreviewSection(text = stringResource(R.string.pref_category_actions))
            SettingsPreviewRow(
                title = stringResource(R.string.delete_plugin),
                height = TvDimens.SettingsDp36,
                leading = {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = TvColors.SettingsPreviewText,
                        modifier = Modifier.size(TvDimens.SettingsDp14),
                    )
                },
            )
        }
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

@Composable
private fun PluginTile(
    plugin: PluginItem,
    size: Dp,
    radius: Dp,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    val shape = RoundedCornerShape(radius)
    val iconUrl = plugin.iconUrl
    if (!iconUrl.isNullOrBlank()) {
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current).data(iconUrl).build()
        )
        val state by painter.state.collectAsState()
        if (state !is AsyncImagePainter.State.Error) {
            Box(modifier = modifier.size(size).clip(shape).background(TvColors.Tile)) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alpha = if (dimmed) 0.6f else 1f,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            return
        }
    }
    val tone = settingsInitialsColor(plugin.name)
    SettingsInitialsTile(
        initials = settingsInitials(plugin.name),
        size = size,
        radius = radius,
        modifier = modifier,
        background = if (dimmed) tone.copy(alpha = 0.55f) else tone,
        contentColor = Color.White,
    )
}

private enum class PluginActionState {
    Idle,
    OpeningSettings,
    Downloading,
    Updating,
    Deleting
}
