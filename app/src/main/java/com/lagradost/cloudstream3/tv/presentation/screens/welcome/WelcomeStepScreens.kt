package com.lagradost.cloudstream3.tv.presentation.screens.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import com.lagradost.cloudstream3.tv.presentation.focus.TvFocusMemory
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.AllLanguagesName
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.compat.PluginItem
import com.lagradost.cloudstream3.tv.compat.PluginStatus
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.AddRepositoryForm
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.ExtensionsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsListRow
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsRowWidget
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.ui.settings.SettingsProvidersScreen
import com.lagradost.cloudstream3.ui.settings.appLanguages
import com.lagradost.cloudstream3.ui.settings.nameNextToFlagEmoji
import com.lagradost.cloudstream3.utils.SubtitleHelper.getNameNextToFlagEmoji
import qrcode.QRCode
import java.util.Locale

private const val PublicRepositoriesUrl = "https://recloudstream.github.io/repos/"

private fun stepChecklist(step: WelcomeStep) =
    WelcomeChecklist(done = (1 until step.number).toSet(), current = step.number)

/** Scrolling list with the Welcome insets; rows keep a stable key. */
@Composable
private fun WelcomeList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(start = WelcomeTokens.ListStart, top = TvDimens.SettingsDp12, bottom = TvDimens.SettingsDp12),
        verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsDp8),
        content = content,
    )
}

/**
 * Focus plumbing shared by every scrolling list: RIGHT on a row jumps to the primary footer button
 * ([primary]), UP from the footer (or LEFT from its first button) returns to the row that was focused last.
 */
@Stable
internal class WelcomeListFocus(val listState: LazyListState) {
    private val memory = TvFocusMemory()

    /** Primary footer button (Next / Finish / Download selected); set by the screen while composing. */
    var primary: FocusRequester? = null

    val scrollable by derivedStateOf { listState.canScrollForward || listState.canScrollBackward }

    /** Marks a row of the list ([key] is its stable key). */
    fun row(key: String, base: Modifier = Modifier): Modifier = with(memory) {
        base.track(key).focusProperties { primary?.let { right = it } }
    }

    /** Footer button; the first one also returns to the list with LEFT. */
    fun footer(first: Boolean = false): Modifier = Modifier.focusProperties {
        memory.lastKey?.let { key ->
            val target = memory.requesterFor(key)
            up = target
            if (first) left = target
        }
    }

    @Composable
    fun hint(@androidx.annotation.StringRes text: Int = R.string.tv_welcome_jump_hint): String? =
        if (scrollable) stringResource(text) else null
}

@Composable
internal fun rememberWelcomeListFocus(initialIndex: Int = 0): WelcomeListFocus {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    return remember(state) { WelcomeListFocus(state) }
}

// --- step 1: app language -----------------------------------------------------------------

internal data class LanguageRow(val tag: String, val label: String, val summary: String?)

internal fun appLanguageLabel(tag: String): String =
    appLanguages.firstOrNull { it.second.equals(tag, ignoreCase = true) }?.nameNextToFlagEmoji() ?: tag

@Composable
internal fun LanguageStepScreen(state: WelcomeUiState, actions: WelcomeActions) {
    val rows = remember {
        appLanguages.map {
            LanguageRow(
                tag = it.second,
                label = it.nameNextToFlagEmoji(),
                summary = Locale.forLanguageTag(it.second).getDisplayLanguage(Locale.ENGLISH).takeIf { name -> name.isNotBlank() },
            )
        }
    }
    val selectedTag = remember(state.languageTag) {
        rows.firstOrNull { it.tag.equals(state.languageTag, ignoreCase = true) }?.tag
            ?: rows.firstOrNull { it.tag.substringBefore('-') == state.languageTag.substringBefore('-') }?.tag
    }
    val selectedIndex = rows.indexOfFirst { it.tag == selectedTag }.coerceAtLeast(0)
    val list = rememberWelcomeListFocus((selectedIndex - 2).coerceAtLeast(0))
    val nextRequester = remember { FocusRequester() }
    list.primary = nextRequester
    // After a selection (which may recreate the activity) focus goes to Next instead of back to the row.
    val selectedRequester = remember { FocusRequester() }
    val skipRowFocus = remember { state.focusPrimary }
    FocusRequestEffect(selectedRequester, Unit, enabled = !skipRowFocus)
    LaunchedEffect(state.focusPrimary) {
        if (state.focusPrimary) {
            // Let the window settle first (after a recreate the system restores its own focus).
            delay(300)
            nextRequester.requestFocusWithRetry()
            // A changed language recreates the activity right after this: the effect is cancelled
            // before the flag is cleared, so the new activity focuses Next again.
            delay(600)
            actions.consumePrimaryFocus()
        }
    }
    WelcomeScaffold(
        card = {
            WelcomeCardPanel(
                label = stepLabel(WelcomeStep.Language),
                title = stringResource(R.string.tv_welcome_step_language),
                subtitle = stringResource(R.string.tv_welcome_step_language_summary),
                hint = list.hint(R.string.tv_welcome_language_hint),
                footer = {
                    WelcomeButton(stringResource(R.string.tv_welcome_back), actions.back, list.footer(first = true))
                    WelcomeButton(stringResource(R.string.skip_setup), actions.finish, list.footer())
                    WelcomeButton(stringResource(R.string.tv_welcome_next), actions.next, list.footer(), primary = true, focusRequester = nextRequester)
                },
            ) {
                WelcomeList(state = list.listState) {
                    items(rows, key = { it.tag }) { row ->
                        val selected = row.tag == selectedTag
                        SettingsListRow(
                            title = row.label,
                            summary = row.summary,
                            widget = SettingsRowWidget.Radio(selected),
                            onClick = { actions.selectLanguage(row.tag) },
                            modifier = list.row(row.tag, if (selected) Modifier.focusRequester(selectedRequester) else Modifier),
                        )
                    }
                }
            }
        },
        preview = { WelcomePreview(stepChecklist(WelcomeStep.Language)) },
    )
}

@Composable
private fun stepLabel(step: WelcomeStep) = stringResource(R.string.tv_welcome_step_format, step.number)

// --- step 2: repository -------------------------------------------------------------------

@Composable
internal fun RepositoryStepScreen(state: WelcomeUiState, extensions: ExtensionsUiState.Ready?, actions: WelcomeActions) {
    when (state.sub) {
        WelcomeSub.AddRepository -> {
            WelcomeScaffold(
                card = {
                    // The shared Settings form: header, two fields and the disabled-until-URL action.
                    AddRepositoryForm(addRepository = actions.addRepository, onBack = actions.back)
                },
                preview = { WelcomePreview(stepChecklist(WelcomeStep.Repository)) },
            )
        }
        WelcomeSub.CommunityRepositories -> CommunityRepositoriesScreen(actions)
        WelcomeSub.DeleteRepository -> DeleteRepositoryScreen(extensions, actions)
        else -> RepositoryListScreen(extensions, actions)
    }
}

@Composable
private fun RepositoryListScreen(extensions: ExtensionsUiState.Ready?, actions: WelcomeActions) {
    val repositories = extensions?.repositories.orEmpty()
    val firstRow = rememberFocusOnEnter(repositories.isEmpty())
    val removable = repositories.filterNot { it.isPrebuilt }
    val list = rememberWelcomeListFocus()
    val nextRequester = remember { FocusRequester() }
    list.primary = nextRequester
    WelcomeScaffold(
        card = {
            WelcomeCardPanel(
                label = stepLabel(WelcomeStep.Repository),
                title = stringResource(R.string.tv_welcome_step_repository_title),
                subtitle = if (repositories.isEmpty()) {
                    stringResource(R.string.tv_welcome_repository_subtitle)
                } else {
                    pluralStringResource(R.plurals.tv_welcome_repositories_added, repositories.size, repositories.size)
                },
                hint = list.hint(),
                footer = {
                    WelcomeButton(stringResource(R.string.tv_welcome_back), actions.back, list.footer(first = true))
                    WelcomeButton(stringResource(R.string.tv_welcome_next), actions.next, list.footer(), primary = true, focusRequester = nextRequester)
                },
            ) {
                if (repositories.isEmpty()) {
                    WelcomeIntro(stringResource(R.string.tv_welcome_no_repositories))
                }
                WelcomeList(state = list.listState) {
                    items(repositories, key = { it.url }) { repository ->
                        // Information only (the first one takes the initial focus, like in the mockup).
                        SettingsListRow(
                            title = repository.name,
                            summary = repository.url.removePrefix("https://").removePrefix("http://"),
                            onClick = {},
                            leading = { _ -> androidx.tv.material3.Icon(Icons.Outlined.Extension, null) },
                            modifier = list.row(repository.url, if (repository == repositories.first()) Modifier.focusRequester(firstRow) else Modifier),
                        )
                    }
                    item(key = "add") {
                        GlyphRow(
                            title = stringResource(if (repositories.isEmpty()) R.string.add_repository else R.string.tv_welcome_add_another_repository),
                            summary = stringResource(R.string.tv_welcome_add_repository_summary),
                            icon = Icons.Outlined.Add,
                            onClick = { actions.openSub(WelcomeSub.AddRepository) },
                            modifier = list.row("add", if (repositories.isEmpty()) Modifier.focusRequester(firstRow) else Modifier),
                        )
                    }
                    if (repositories.isEmpty()) {
                        item(key = "community") {
                            GlyphRow(
                                title = stringResource(R.string.tv_welcome_community_repositories),
                                icon = Icons.Outlined.Public,
                                onClick = { actions.openSub(WelcomeSub.CommunityRepositories) },
                                modifier = list.row("community"),
                            )
                        }
                    }
                    if (removable.isNotEmpty()) {
                        item(key = "delete") {
                            GlyphRow(
                                title = stringResource(R.string.delete_repository),
                                summary = if (removable.size == 1) {
                                    stringResource(R.string.tv_welcome_delete_repository_summary, removable.first().name)
                                } else {
                                    stringResource(R.string.tv_welcome_delete_repository_choose)
                                },
                                icon = Icons.Outlined.DeleteOutline,
                                onClick = {
                                    if (removable.size == 1) actions.removeRepository(removable.first().url)
                                    else actions.openSub(WelcomeSub.DeleteRepository)
                                },
                                modifier = list.row("delete"),
                            )
                        }
                    }
                }
            }
        },
        preview = {
            WelcomePreview(stepChecklist(WelcomeStep.Repository), note = stringResource(R.string.tv_welcome_repository_note))
        },
    )
}

@Composable
private fun DeleteRepositoryScreen(extensions: ExtensionsUiState.Ready?, actions: WelcomeActions) {
    val removable = extensions?.repositories.orEmpty().filterNot { it.isPrebuilt }
    val firstRow = rememberFocusOnEnter()
    val list = rememberWelcomeListFocus()
    WelcomeScaffold(
        card = {
            WelcomeCardPanel(
                label = stepLabel(WelcomeStep.Repository),
                title = stringResource(R.string.delete_repository),
                subtitle = stringResource(R.string.tv_welcome_delete_repository_choose),
                hint = list.hint(),
                footer = { WelcomeButton(stringResource(R.string.tv_welcome_back), actions.back, list.footer(first = true)) },
            ) {
                WelcomeList(state = list.listState) {
                    items(removable, key = { it.url }) { repository ->
                        SettingsListRow(
                            title = repository.name,
                            summary = repository.url.removePrefix("https://").removePrefix("http://"),
                            destructive = true,
                            onClick = {
                                actions.removeRepository(repository.url)
                                if (removable.size <= 1) actions.back()
                            },
                            leading = { _ -> androidx.tv.material3.Icon(Icons.Outlined.DeleteOutline, null) },
                            modifier = list.row(repository.url, if (repository == removable.first()) Modifier.focusRequester(firstRow) else Modifier),
                        )
                    }
                }
            }
        },
        preview = { WelcomePreview(stepChecklist(WelcomeStep.Repository)) },
    )
}

@Composable
private fun CommunityRepositoriesScreen(actions: WelcomeActions) {
    val qr = remember {
        runCatching {
            QRCode.ofSquares()
                .withColor(TvColors.OnFocus.toArgb())
                .withBackgroundColor(TvColors.Focus.toArgb())
                .build(PublicRepositoriesUrl)
                .render()
                .nativeImage() as android.graphics.Bitmap
        }.getOrNull()
    }
    val backRequester = rememberFocusOnEnter()
    WelcomeScaffold(
        card = {
            WelcomeCardPanel(
                label = stepLabel(WelcomeStep.Repository),
                title = stringResource(R.string.tv_welcome_community_repositories),
                subtitle = stringResource(R.string.tv_welcome_community_subtitle),
                footer = {
                    WelcomeButton(
                        stringResource(R.string.tv_welcome_back),
                        actions.back,
                        modifier = Modifier.focusRequester(backRequester),
                    )
                },
            ) {
                Column(Modifier.padding(start = WelcomeTokens.HeaderStart, top = TvDimens.SettingsDp24)) {
                    qr?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(WelcomeTokens.QrSize)
                                .background(TvColors.Focus, RoundedCornerShape(TvDimens.SettingsQrRadius))
                                .padding(TvDimens.SettingsDp8),
                        )
                    }
                    Text(
                        PublicRepositoriesUrl,
                        style = com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles.SettingsInfo,
                        color = TvColors.Text,
                        modifier = Modifier.padding(top = TvDimens.SettingsDp16),
                    )
                }
            }
        },
        preview = { WelcomePreview(stepChecklist(WelcomeStep.Repository)) },
    )
}

// --- step 3: extensions -------------------------------------------------------------------

private fun languageName(tag: String?): String? =
    tag?.takeIf { it.isNotBlank() }?.let { Locale.forLanguageTag(it).displayLanguage.takeIf { name -> name.isNotBlank() } ?: it }

@Composable
internal fun ExtensionsStepScreen(
    state: WelcomeUiState,
    extensions: ExtensionsUiState.Ready?,
    actions: WelcomeActions,
    onLoadPlugins: (String) -> Unit,
) {
    val repositories = extensions?.repositories.orEmpty()
    androidx.compose.runtime.LaunchedEffect(repositories) { repositories.forEach { onLoadPlugins(it.url) } }
    val openUrl = state.openRepositoryUrl
    if (state.sub == WelcomeSub.RepositoryPlugins && openUrl != null) {
        RepositoryPluginsScreen(state, openUrl, extensions, actions)
        return
    }
    val lastRepo = state.lastRepositoryUrl
    val focusTarget = rememberFocusOnEnter(state.sub)
    val list = rememberWelcomeListFocus()
    val nextRequester = remember { FocusRequester() }
    list.primary = nextRequester
    WelcomeScaffold(
        card = {
            WelcomeCardPanel(
                label = stepLabel(WelcomeStep.Extensions),
                title = stringResource(R.string.tv_welcome_step_extensions),
                subtitle = stringResource(R.string.tv_welcome_step_extensions_summary),
                hint = list.hint(),
                footer = {
                    WelcomeButton(stringResource(R.string.tv_welcome_back), actions.back, list.footer(first = true))
                    WelcomeButton(stringResource(R.string.tv_welcome_skip_step), actions.next, list.footer())
                    WelcomeButton(stringResource(R.string.tv_welcome_next), actions.next, list.footer(), primary = true, focusRequester = nextRequester)
                },
            ) {
                WelcomeIntro(stringResource(R.string.tv_welcome_extensions_intro))
                WelcomeList(state = list.listState) {
                    items(repositories, key = { it.url }) { repository ->
                        val plugins = extensions?.pluginsByRepo?.get(repository.url)
                        val focusHere = if (lastRepo != null) repository.url == lastRepo else repository == repositories.first()
                        SettingsListRow(
                            title = repository.name,
                            summary = if (plugins == null) {
                                stringResource(R.string.tv_welcome_loading)
                            } else {
                                stringResource(
                                    R.string.tv_welcome_repository_summary,
                                    pluralStringResource(R.plurals.tv_welcome_extensions_count, plugins.size, plugins.size),
                                    plugins.count { it.status != PluginStatus.NOT_DOWNLOADED },
                                )
                            },
                            widget = SettingsRowWidget.Chevron,
                            onClick = { actions.openRepository(repository.url) },
                            modifier = list.row(repository.url, if (focusHere) Modifier.focusRequester(focusTarget) else Modifier),
                        )
                    }
                }
            }
        },
        preview = { WelcomePreview(stepChecklist(WelcomeStep.Extensions), note = stringResource(R.string.tv_welcome_extensions_note)) },
    )
}

@Composable
private fun RepositoryPluginsScreen(
    state: WelcomeUiState,
    url: String,
    extensions: ExtensionsUiState.Ready?,
    actions: WelcomeActions,
) {
    val context = LocalContext.current
    val repository = extensions?.repositories?.firstOrNull { it.url == url }
    val plugins = extensions?.pluginsByRepo?.get(url)
    val download = state.download
    val downloading = download != null
    val firstRow = rememberFocusOnEnter(plugins != null)
    val selectAllRequester = remember { FocusRequester() }
    val downloadRequester = remember { FocusRequester() }
    val list = rememberWelcomeListFocus()
    // Mapping is done once per list change, not per row on every recomposition.
    val rows = remember(plugins, download?.currentName) {
        plugins.orEmpty().map { plugin ->
            val label = when {
                plugin.name == download?.currentName -> context.getString(R.string.plugin_downloading)
                plugin.status == PluginStatus.NOT_DOWNLOADED -> context.getString(R.string.tv_settings_not_downloaded)
                else -> context.getString(R.string.downloaded)
            }
            PluginRow(
                plugin = plugin,
                summary = listOfNotNull(label, "v${plugin.version}", languageName(plugin.language)).joinToString(" • "),
            )
        }
    }
    val selectable = rows.filter { it.plugin.status == PluginStatus.NOT_DOWNLOADED }.map { it.plugin }
    val selected = rows.filter { it.plugin.internalName in state.selectedPlugins }.map { it.plugin }
    // RIGHT goes to "Download selected"; with nothing selected yet, to "Select all".
    list.primary = when {
        downloading -> null
        selected.isNotEmpty() -> downloadRequester
        selectable.isNotEmpty() -> selectAllRequester
        else -> null
    }
    WelcomeScaffold(
        card = {
            WelcomeCardPanel(
                label = stepLabel(WelcomeStep.Extensions),
                title = repository?.name ?: url,
                subtitle = if (download != null) {
                    pluralStringResource(R.plurals.tv_welcome_downloading_count, download.total, download.total)
                } else {
                    stringResource(R.string.tv_welcome_select_extensions)
                },
                progress = download?.let { it.finished.toFloat() / it.total.coerceAtLeast(1) },
                hint = list.hint(),
                footer = {
                    WelcomeButton(stringResource(R.string.tv_welcome_back), actions.back, list.footer(first = true))
                    if (!downloading) {
                        WelcomeButton(
                            stringResource(R.string.tv_welcome_select_all),
                            { actions.selectPlugins(selectable.map { it.internalName }.toSet()) },
                            list.footer(),
                            enabled = selectable.isNotEmpty(),
                            focusRequester = selectAllRequester,
                        )
                        WelcomeButton(
                            stringResource(R.string.tv_welcome_download_selected, selected.size),
                            { actions.downloadSelected(selected) },
                            list.footer(),
                            primary = true,
                            enabled = selected.isNotEmpty(),
                            focusRequester = downloadRequester,
                        )
                    }
                },
            ) {
                if (plugins != null && rows.isEmpty()) {
                    WelcomeIntro(stringResource(R.string.tv_settings_no_plugins))
                }
                WelcomeList(state = list.listState) {
                    items(rows, key = { it.plugin.internalName }) { row ->
                        val downloaded = row.plugin.status != PluginStatus.NOT_DOWNLOADED
                        SettingsListRow(
                            title = row.plugin.name,
                            summary = row.summary,
                            widget = SettingsRowWidget.Checkbox(downloaded || row.plugin.internalName in state.selectedPlugins),
                            onClick = { if (!downloaded && !downloading) actions.togglePlugin(row.plugin.internalName) },
                            modifier = list.row(
                                row.plugin.internalName,
                                if (row == rows.first()) Modifier.focusRequester(firstRow) else Modifier,
                            ),
                        )
                    }
                }
            }
        },
        preview = { WelcomePreview(stepChecklist(WelcomeStep.Extensions), note = stringResource(R.string.tv_welcome_extensions_note)) },
    )
}

private data class PluginRow(val plugin: PluginItem, val summary: String)

// --- step 4: extension languages ----------------------------------------------------------

internal data class ProviderLanguageOption(val key: String, val label: String)

internal fun providerLanguageOptions(allLabel: String): List<ProviderLanguageOption> =
    synchronized(APIHolder.apis) {
        val languages = APIHolder.apis
            .map { it.lang to (getNameNextToFlagEmoji(it.lang) ?: it.lang) }
            .distinctBy { it.first }
            .sortedBy { it.second.substringAfter(' ').lowercase(Locale.ROOT) }
        listOf(ProviderLanguageOption(AllLanguagesName, allLabel)) + languages.map { ProviderLanguageOption(it.first, it.second) }
    }

@Composable
internal fun ExtensionLanguagesStepScreen(state: WelcomeUiState, actions: WelcomeActions) {
    val allLabel = stringResource(R.string.all_languages_preference)
    val options = remember(allLabel) { providerLanguageOptions(allLabel) }
    val firstRow = rememberFocusOnEnter()
    val nextRequester = remember { FocusRequester() }
    val list = rememberWelcomeListFocus()
    list.primary = nextRequester
    WelcomeScaffold(
        card = {
            WelcomeCardPanel(
                label = stepLabel(WelcomeStep.ExtensionLanguages),
                title = stringResource(R.string.tv_welcome_step_extension_languages),
                subtitle = stringResource(R.string.provider_languages_tip),
                hint = list.hint(),
                footer = {
                    WelcomeButton(stringResource(R.string.tv_welcome_back), actions.back, list.footer(first = true))
                    WelcomeButton(stringResource(R.string.tv_welcome_next), actions.next, list.footer(), primary = true, focusRequester = nextRequester)
                },
            ) {
                WelcomeList(state = list.listState) {
                    items(options, key = { it.key }) { option ->
                        SettingsListRow(
                            title = option.label,
                            widget = SettingsRowWidget.Checkbox(option.key in state.providerLanguages),
                            onClick = { actions.toggleProviderLanguage(option.key) },
                            modifier = list.row(option.key, if (option == options.first()) Modifier.focusRequester(firstRow) else Modifier),
                        )
                    }
                }
            }
        },
        preview = { WelcomePreview(stepChecklist(WelcomeStep.ExtensionLanguages)) },
    )
}

// --- step 5: preferred media --------------------------------------------------------------

internal fun mediaTypeLabelRes(type: TvType): Int = when (type) {
    TvType.Movie -> R.string.movies
    TvType.AnimeMovie -> R.string.tv_welcome_media_anime_movies
    TvType.TvSeries -> R.string.tv_series
    TvType.Cartoon -> R.string.cartoons
    TvType.Anime -> R.string.anime
    TvType.OVA -> R.string.ova
    TvType.Torrent -> R.string.torrent
    TvType.Documentary -> R.string.documentaries
    TvType.AsianDrama -> R.string.asian_drama
    TvType.Live -> R.string.livestreams
    TvType.NSFW -> R.string.nsfw
    TvType.Others -> R.string.others
    else -> with(SettingsProvidersScreen) { type.toStringRes() }
}

@Composable
internal fun MediaStepScreen(state: WelcomeUiState, actions: WelcomeActions) {
    val types = remember { TvType.entries.sorted() }
    val firstRow = rememberFocusOnEnter()
    val finishRequester = remember { FocusRequester() }
    val list = rememberWelcomeListFocus()
    list.primary = finishRequester
    WelcomeScaffold(
        card = {
            WelcomeCardPanel(
                label = stepLabel(WelcomeStep.Media),
                title = stringResource(R.string.tv_welcome_step_media),
                subtitle = stringResource(R.string.preferred_media_subtext),
                hint = list.hint(),
                footer = {
                    WelcomeButton(stringResource(R.string.tv_welcome_back), actions.back, list.footer(first = true))
                    WelcomeButton(stringResource(R.string.tv_welcome_finish), actions.next, list.footer(), primary = true, focusRequester = finishRequester)
                },
            ) {
                WelcomeList(state = list.listState) {
                    items(types, key = { it.ordinal }) { type ->
                        SettingsListRow(
                            title = stringResource(mediaTypeLabelRes(type)),
                            widget = SettingsRowWidget.Checkbox(type.ordinal.toString() in state.mediaTypes),
                            onClick = { actions.toggleMediaType(type.ordinal.toString()) },
                            modifier = list.row(type.ordinal.toString(), if (type == types.first()) Modifier.focusRequester(firstRow) else Modifier),
                        )
                    }
                }
            }
        },
        preview = { WelcomePreview(stepChecklist(WelcomeStep.Media)) },
    )
}
