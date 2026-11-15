package com.lagradost.cloudstream3.tv.presentation.screens.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.AllLanguagesName
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.ExtensionsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsGlyph
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsListRow
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens

/** Requests focus for the returned requester when the screen (or [key]) enters composition. */
@Composable
internal fun rememberFocusOnEnter(key: Any? = Unit): FocusRequester {
    val requester = remember(key) { FocusRequester() }
    FocusRequestEffect(requester = requester, requestKey = key)
    return requester
}

@Composable
internal fun GlyphRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    destructive: Boolean = false,
) {
    SettingsListRow(
        title = title,
        summary = summary,
        onClick = onClick,
        modifier = modifier,
        destructive = destructive,
        leading = { SettingsGlyph(imageVector = icon) },
    )
}

private val RowsModifier = Modifier.padding(start = WelcomeTokens.ListStart, top = TvDimens.SettingsDp16)

/** Mockup 1: choose how to start. */
@Composable
internal fun StartScreen(actions: WelcomeActions) {
    val firstRow = rememberFocusOnEnter()
    WelcomeScaffold(
        card = {
            WelcomeCardPanel(
                title = stringResource(R.string.tv_welcome_title),
                subtitle = stringResource(R.string.tv_welcome_subtitle),
                icon = { WelcomeAppIcon() },
            ) {
                Column(RowsModifier, verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsDp8)) {
                    GlyphRow(
                        title = stringResource(R.string.tv_welcome_manual),
                        summary = stringResource(R.string.tv_welcome_manual_summary),
                        icon = Icons.Outlined.Tune,
                        onClick = actions.startManual,
                        modifier = Modifier.focusRequester(firstRow),
                    )
                    GlyphRow(
                        title = stringResource(R.string.tv_welcome_restore),
                        summary = stringResource(R.string.tv_welcome_restore_summary),
                        icon = Icons.Outlined.Restore,
                        onClick = { actions.openSub(WelcomeSub.RestoreInfo) },
                    )
                    GlyphRow(
                        title = stringResource(R.string.skip_setup),
                        summary = stringResource(R.string.tv_welcome_skip_summary),
                        icon = Icons.Outlined.SkipNext,
                        onClick = actions.finish,
                    )
                }
            }
        },
        preview = {
            WelcomePreview(
                checklist = WelcomeChecklist(done = emptySet(), current = null),
                note = stringResource(R.string.tv_welcome_restore_note),
            )
        },
    )
}

/** Mockup 2a: what a backup restores; the system file picker opens from here. */
@Composable
internal fun RestoreInfoScreen(actions: WelcomeActions) {
    val firstRow = rememberFocusOnEnter()
    WelcomeScaffold(
        card = {
            WelcomeCardPanel(
                title = stringResource(R.string.tv_welcome_restore),
                subtitle = stringResource(R.string.tv_welcome_restore_pick_subtitle),
            ) {
                Column(RowsModifier, verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsDp8)) {
                    GlyphRow(
                        title = stringResource(R.string.tv_welcome_restore_choose_file),
                        summary = stringResource(R.string.tv_welcome_restore_choose_file_summary),
                        icon = Icons.Outlined.FileDownload,
                        onClick = actions.pickBackup,
                        modifier = Modifier.focusRequester(firstRow),
                    )
                    GlyphRow(
                        title = stringResource(R.string.tv_welcome_back),
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        onClick = actions.back,
                    )
                }
            }
        },
        preview = { RestoreWhatPreview() },
    )
}

@Composable
private fun RestoreWhatPreview() {
    Text(stringResource(R.string.tv_welcome_restore_what_title), style = WelcomeText.PreviewHeading, color = TvColors.Text)
    Column(Modifier.padding(top = TvDimens.SettingsDp24), verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsDp6)) {
        Text(stringResource(R.string.tv_welcome_restore_restored).uppercase(), style = WelcomeText.Label, color = TvColors.Success)
        listOf(R.string.tv_welcome_restore_settings, R.string.tv_welcome_restore_data, R.string.tv_welcome_restore_repositories)
            .forEach { Text(stringResource(it), style = WelcomeText.StepTitle, color = TvColors.Text) }
        Text(
            stringResource(R.string.tv_welcome_restore_not_restored).uppercase(),
            style = WelcomeText.Label,
            color = TvColors.Label,
            modifier = Modifier.padding(top = TvDimens.SettingsDp16),
        )
        listOf(R.string.tv_welcome_restore_extensions, R.string.tv_welcome_restore_accounts, R.string.tv_welcome_restore_download_folder)
            .forEach { Text(stringResource(it), style = WelcomeText.StepTitle, color = TvColors.Meta) }
    }
}

/** Mockup 2b: backup restored; extensions are never part of a backup. */
@Composable
internal fun RestoreDoneScreen(state: WelcomeUiState, extensions: ExtensionsUiState.Ready?, actions: WelcomeActions) {
    val firstRow = rememberFocusOnEnter()
    val hasRepositories = extensions?.repositories?.isNotEmpty() == true
    WelcomeScaffold(
        card = {
            WelcomeCardPanel(
                title = stringResource(R.string.tv_welcome_restored_title),
                subtitle = stringResource(R.string.tv_welcome_restored_subtitle),
            ) {
                state.restoreFileName?.let { WelcomeIntro(it) }
                Column(RowsModifier, verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsDp8)) {
                    GlyphRow(
                        title = stringResource(R.string.tv_welcome_download_extensions),
                        summary = stringResource(R.string.tv_welcome_download_extensions_summary),
                        icon = Icons.Outlined.FileDownload,
                        onClick = { actions.goTo(WelcomeStep.Extensions) },
                        modifier = Modifier.focusRequester(firstRow),
                    )
                    GlyphRow(
                        title = stringResource(R.string.tv_welcome_finish_setup),
                        icon = Icons.Outlined.Check,
                        onClick = actions.finish,
                    )
                }
            }
        },
        preview = {
            WelcomePreview(
                checklist = WelcomeChecklist(
                    done = if (hasRepositories) setOf(1, 2, 5) else setOf(1, 5),
                    current = 3,
                ),
                note = stringResource(R.string.tv_welcome_restored_note),
            )
        },
    )
}

/** Mockup 8: all set, with what was chosen in the steps. */
@Composable
internal fun DoneScreen(state: WelcomeUiState, extensions: ExtensionsUiState.Ready?, actions: WelcomeActions) {
    val firstRow = rememberFocusOnEnter()
    val context = LocalContext.current
    val languageSummary = remember(state.languageTag) {
        appLanguageLabel(state.languageTag)
    }
    val downloaded = remember { PluginManager.getPluginsOnline().size + PluginManager.getPluginsLocal().size }
    val allLanguages = stringResource(R.string.all_languages_preference)
    val languages = remember(state.providerLanguages, state.hasExtensionLanguagesStep, allLanguages) {
        if (!state.hasExtensionLanguagesStep || AllLanguagesName in state.providerLanguages) allLanguages
        else providerLanguageOptions(allLanguages).filter { it.key in state.providerLanguages }
            .joinToString { it.label }.ifBlank { context.getString(R.string.none) }
    }
    val media = remember(state.mediaTypes) {
        TvType.entries.sorted().filter { it.ordinal.toString() in state.mediaTypes }
            .joinToString { context.getString(mediaTypeLabelRes(it)) }.ifBlank { context.getString(R.string.none) }
    }
    val repositories = extensions?.repositories?.size ?: 0
    val summaries = listOf(
        languageSummary,
        pluralStringResource(R.plurals.tv_welcome_repositories_count, repositories, repositories),
        pluralStringResource(R.plurals.tv_welcome_extensions_downloaded, downloaded, downloaded),
        languages,
        media,
    )
    WelcomeScaffold(
        card = {
            WelcomeCardPanel(
                title = stringResource(R.string.tv_welcome_done_title),
                subtitle = stringResource(R.string.tv_welcome_done_subtitle),
            ) {
                Column(RowsModifier, verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsDp8)) {
                    GlyphRow(
                        title = stringResource(R.string.tv_welcome_start_watching),
                        icon = Icons.Outlined.Check,
                        onClick = actions.finish,
                        modifier = Modifier.focusRequester(firstRow),
                    )
                    GlyphRow(
                        title = stringResource(R.string.tv_welcome_back),
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        onClick = actions.back,
                    )
                }
            }
        },
        preview = {
            WelcomePreview(WelcomeChecklist(done = setOf(1, 2, 3, 4, 5), current = null, summaries = summaries))
        },
    )
}

/** Mockup 2c: the backup could not be restored. */
@Composable
internal fun RestoreFailedScreen(state: WelcomeUiState, actions: WelcomeActions) {
    val firstRow = rememberFocusOnEnter()
    WelcomeScaffold(
        card = {
            WelcomeCardPanel(
                title = stringResource(R.string.tv_welcome_restore_failed_title),
                subtitle = stringResource(R.string.restore_failed_format, state.restoreFileName ?: "?"),
            ) {
                Column(RowsModifier, verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsDp8)) {
                    GlyphRow(
                        title = stringResource(R.string.tv_welcome_restore_choose_another),
                        icon = Icons.Outlined.FileDownload,
                        onClick = actions.pickBackup,
                        modifier = Modifier.focusRequester(firstRow),
                    )
                    GlyphRow(
                        title = stringResource(R.string.tv_welcome_back),
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        onClick = actions.back,
                    )
                }
            }
        },
        preview = {
            Text(stringResource(R.string.tv_welcome_supported_files), style = WelcomeText.PreviewHeading, color = TvColors.Text)
            Column(Modifier.padding(top = TvDimens.SettingsDp24), verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsDp6)) {
                Text(stringResource(R.string.tv_welcome_supported_files_summary), style = WelcomeText.StepTitle, color = TvColors.Meta)
            }
        },
    )
}
