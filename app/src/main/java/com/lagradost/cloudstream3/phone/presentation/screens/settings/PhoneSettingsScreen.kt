package com.lagradost.cloudstream3.phone.presentation.screens.settings

import com.lagradost.cloudstream3.phone.presentation.common.navBarBottom
import androidx.activity.compose.BackHandler
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.compat.PluginSettingsHostActivity
import com.lagradost.cloudstream3.phone.presentation.theme.LocalPhoneAccent
import com.lagradost.cloudstream3.designsystem.CsColors
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.layout.heightIn
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.phone.presentation.common.CsButton
import com.lagradost.cloudstream3.phone.presentation.common.CsIconButton
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneDimens
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneShapes
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import com.lagradost.cloudstream3.tv.compat.PluginItem
import com.lagradost.cloudstream3.tv.compat.PluginStatus
import com.lagradost.cloudstream3.tv.compat.RepositoryItem
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.ExtensionsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.ExtensionsViewModel

@Composable
internal fun LevelHeader(title: String, onBack: (() -> Unit)?, icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.AutoMirrored.Filled.ArrowBack, backLabel: String = stringResource(R.string.phone_back)) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (onBack != null) CsIconButton(icon, backLabel, onBack, container = Color.Transparent) else androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        Text(title, style = PhoneText.Level, color = PhoneColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun NavRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, tint = PhoneColors.Text, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = PhoneText.Row, color = PhoneColors.Text)
            if (subtitle != null) Text(subtitle, style = PhoneText.Meta, color = PhoneColors.Text2)
        }
        Icon(Icons.Default.ChevronRight, null, tint = PhoneColors.Label)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun PhoneExtensionsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val vm: ExtensionsViewModel = viewModel()
    LaunchedEffect(vm, context) { vm.setupPrefsListener(context) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var openRepo by remember { mutableStateOf<RepositoryItem?>(null) }
    var openPluginName by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    BackHandler(enabled = openRepo != null || showAdd) {
        when {
            showAdd -> showAdd = false
            openPluginName != null -> openPluginName = null
            else -> openRepo = null
        }
    }
    val failed = stringResource(R.string.phone_action_failed, "%s")
    fun report(result: Result<*>) {
        result.exceptionOrNull()?.let { Toast.makeText(context, failed.format(it.localizedMessage ?: it.javaClass.simpleName), Toast.LENGTH_LONG).show() }
    }

    Column(modifier.fillMaxSize().background(PhoneColors.Ground).statusBarsPadding().imePadding()) {
        val repo = openRepo
        when {
            showAdd -> {
                LevelHeader(stringResource(R.string.phone_add_repository), { showAdd = false })
                AddRepositoryForm { name, url ->
                    vm.addRepository(name, url) { r -> report(r); if (r.isSuccess) showAdd = false }
                }
            }
            repo != null -> {
                LaunchedEffect(repo.url) { vm.loadPluginsForRepo(repo.url) }
                val plugins = (state as? ExtensionsUiState.Ready)?.pluginsByRepo?.get(repo.url)
                val plugin = openPluginName?.let { name -> plugins?.firstOrNull { it.internalName == name } }
                if (plugin != null) {
                    LevelHeader(plugin.name, { openPluginName = null })
                    PluginDetails(
                        plugin = plugin, repositoryName = repo.name,
                        onInstall = { context.findActivity()?.let { vm.downloadPlugin(it, plugin, ::report) } },
                        onUpdate = { context.findActivity()?.let { vm.updatePlugin(it, plugin, ::report) } },
                        onUninstall = { vm.deletePlugin(plugin, ::report) },
                        onSettingsClosed = { vm.refreshData(repo.url) },
                    )
                } else {
                    LevelHeader(repo.name, { openRepo = null })
                    RepoPlugins(
                        plugins = plugins,
                        onOpenPlugin = { openPluginName = it.internalName },
                        onRemoveRepo = { vm.removeRepository(repo.url) { r -> report(r); if (r.isSuccess) openRepo = null } },
                    )
                }
            }
            else -> {
                LevelHeader(stringResource(R.string.phone_settings_extensions), onBack)
                val repos = (state as? ExtensionsUiState.Ready)?.repositories.orEmpty()
                LazyColumn(contentPadding = PaddingValues(start = PhoneDimens.ScreenPadding, top = PhoneDimens.ScreenPadding, end = PhoneDimens.ScreenPadding, bottom = PhoneDimens.ScreenPadding + navBarBottom()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "add") { CsButton(stringResource(R.string.phone_add_repository), { showAdd = true }, primary = true, modifier = Modifier.fillMaxWidth()) }
                    when {
                        state is ExtensionsUiState.Loading -> item(key = "loading") { LoadingRow(stringResource(R.string.phone_loading_repositories)) }
                        state is ExtensionsUiState.Ready && repos.isEmpty() -> item(key = "empty") { Text(stringResource(R.string.phone_no_repositories), style = PhoneText.Body, color = PhoneColors.Text2, modifier = Modifier.padding(vertical = 12.dp)) }
                    }
                    itemsIndexed(repos, key = { index, r -> "${r.url}#$index" }) { _, r ->
                        Box(Modifier.clip(PhoneShapes.Card).background(PhoneColors.Sheet)) { IconRow(r.iconUrl, r.name, r.url, { openRepo = r }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingRow(text: String) {
    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalPhoneAccent.current)
        Text(text, style = PhoneText.Body, color = PhoneColors.Text2)
    }
}

/** Icon from the repository / plugin manifest, or a tile with the initials when there is none. */
@Composable
private fun IconTile(iconUrl: String?, name: String, size: androidx.compose.ui.unit.Dp) {
    val colors = TvColors.SettingsTilePalette
    Box(
        Modifier.size(size).clip(PhoneShapes.Poster).background(colors[(name.hashCode() and Int.MAX_VALUE) % colors.size]),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.take(2).uppercase(), style = PhoneText.Meta.copy(fontSize = (size.value * 0.34f).sp), color = Color.White)
        if (!iconUrl.isNullOrBlank()) AsyncImage(iconUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun IconRow(iconUrl: String?, title: String, subtitle: String?, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(iconUrl, title, 40.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = PhoneText.Row, color = PhoneColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = PhoneText.Meta, color = PhoneColors.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
        Icon(Icons.Default.ChevronRight, null, tint = PhoneColors.Label)
    }
}

@Composable
private fun pluginStatusLabel(plugin: PluginItem): String = when (plugin.status) {
    PluginStatus.NOT_DOWNLOADED -> stringResource(R.string.phone_plugin_status_not_installed)
    PluginStatus.DOWNLOADED -> stringResource(R.string.phone_plugin_status_installed)
    PluginStatus.UPDATE_AVAILABLE -> stringResource(R.string.phone_plugin_status_update)
}

/** One plugin: icon, version, authors, description and its actions (install / update / settings / uninstall). */
@Composable
private fun PluginDetails(
    plugin: PluginItem,
    repositoryName: String,
    onInstall: () -> Unit,
    onUpdate: () -> Unit,
    onUninstall: () -> Unit,
    onSettingsClosed: () -> Unit,
) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { onSettingsClosed() }
    LazyColumn(contentPadding = PaddingValues(start = PhoneDimens.ScreenPadding, top = PhoneDimens.ScreenPadding, end = PhoneDimens.ScreenPadding, bottom = PhoneDimens.ScreenPadding + navBarBottom()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconTile(plugin.iconUrl, plugin.name, 64.dp)
                Column {
                    Text(plugin.name, style = PhoneText.Level, color = PhoneColors.Text)
                    Text(listOfNotNull(stringResource(R.string.phone_plugin_version, plugin.version), pluginStatusLabel(plugin)).joinToString(" • "), style = PhoneText.Meta, color = PhoneColors.Text2)
                    Text(repositoryName, style = PhoneText.Meta, color = PhoneColors.Label)
                }
            }
        }
        if (plugin.authors.isNotEmpty()) item(key = "authors") {
            Text(stringResource(R.string.phone_plugin_authors, plugin.authors.joinToString(", ")), style = PhoneText.Meta, color = PhoneColors.Text2)
        }
        plugin.description?.takeIf { it.isNotBlank() }?.let { description ->
            item(key = "description") { Text(description, style = PhoneText.Body, color = PhoneColors.Body) }
        }
        item(key = "actions") {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (plugin.status) {
                    PluginStatus.NOT_DOWNLOADED -> CsButton(stringResource(R.string.phone_install), onInstall, primary = true, modifier = Modifier.fillMaxWidth())
                    PluginStatus.UPDATE_AVAILABLE -> CsButton(stringResource(R.string.phone_update), onUpdate, primary = true, modifier = Modifier.fillMaxWidth())
                    PluginStatus.DOWNLOADED -> Unit
                }
                if (plugin.hasSettings && plugin.status != PluginStatus.NOT_DOWNLOADED) {
                    CsButton(
                        stringResource(R.string.phone_plugin_settings),
                        { settingsLauncher.launch(PluginSettingsHostActivity.createIntent(context, plugin.repositoryUrl, plugin.internalName)) },
                        icon = Icons.Default.Settings, modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (plugin.status != PluginStatus.NOT_DOWNLOADED) {
                    CsButton(stringResource(R.string.phone_uninstall), { confirmDelete = true }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = PhoneColors.Sheet,
            title = { Text(stringResource(R.string.phone_plugin_delete_title, plugin.name), style = PhoneText.Section, color = PhoneColors.Text) },
            text = { Text(stringResource(R.string.phone_plugin_delete_message), style = PhoneText.Body, color = PhoneColors.Body) },
            confirmButton = { TextButton({ confirmDelete = false }) { Text(stringResource(R.string.cancel), style = PhoneText.Button, color = PhoneColors.Text) } },
            dismissButton = { TextButton({ confirmDelete = false; onUninstall() }) { Text(stringResource(R.string.phone_uninstall), style = PhoneText.Button, color = PhoneColors.Danger) } },
        )
    }
}

@Composable
private fun AddRepositoryForm(onSubmit: (String?, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    Column(Modifier.padding(PhoneDimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Field(name, { name = it }, stringResource(R.string.phone_repository_name_optional), KeyboardType.Text)
        Field(url, { url = it }, stringResource(R.string.phone_repository_url), KeyboardType.Uri)
        CsButton(stringResource(R.string.phone_add), { onSubmit(name.trim().ifBlank { null }, url.trim()) }, primary = true, enabled = url.isNotBlank(), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, hint: String, type: KeyboardType) {
    Box(Modifier.fillMaxWidth().height(56.dp).clip(PhoneShapes.Card).background(PhoneColors.Card).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(hint, style = PhoneText.Row, color = PhoneColors.Label)
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = PhoneText.Row.copy(color = PhoneColors.Text),
            cursorBrush = SolidColor(PhoneColors.Text),
            keyboardOptions = KeyboardOptions(keyboardType = type),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RepoPlugins(
    plugins: List<PluginItem>?,
    onOpenPlugin: (PluginItem) -> Unit,
    onRemoveRepo: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(start = PhoneDimens.ScreenPadding, top = PhoneDimens.ScreenPadding, end = PhoneDimens.ScreenPadding, bottom = PhoneDimens.ScreenPadding + navBarBottom()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            plugins == null -> item(key = "loading") { LoadingRow(stringResource(R.string.phone_loading_plugins)) }
            plugins.isEmpty() -> item(key = "none") { Text(stringResource(R.string.phone_no_plugins), style = PhoneText.Body, color = PhoneColors.Text2) }
            else -> itemsIndexed(plugins, key = { index, p -> "${p.url}#$index" }) { _, p ->
                Box(Modifier.clip(PhoneShapes.Card).background(PhoneColors.Sheet)) {
                    IconRow(
                        p.iconUrl, p.name,
                        listOfNotNull(stringResource(R.string.phone_plugin_version, p.version), pluginStatusLabel(p)).joinToString(" • "),
                        { onOpenPlugin(p) },
                        trailing = if (p.status == PluginStatus.NOT_DOWNLOADED) null else ({
                            Icon(if (p.status == PluginStatus.UPDATE_AVAILABLE) Icons.Default.Refresh else Icons.Default.CheckCircle, null, tint = if (p.status == PluginStatus.UPDATE_AVAILABLE) LocalPhoneAccent.current else CsColors.Info, modifier = Modifier.size(20.dp))
                        }),
                    )
                }
            }
        }
        item(key = "remove") { CsButton(stringResource(R.string.phone_remove), onRemoveRepo, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) }
    }
}
