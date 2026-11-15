package com.lagradost.cloudstream3.tv.presentation.screens.welcome

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getActivity
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.tv.compat.PluginItem
import com.lagradost.cloudstream3.tv.compat.RepositoryItem
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.ExtensionsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.ExtensionsViewModel
import com.lagradost.cloudstream3.utils.BackupUtils.restorePrompt

/** Everything the screens can ask for; the screens themselves hold no logic. */
internal class WelcomeActions(
    val next: () -> Unit,
    val back: () -> Unit,
    val startManual: () -> Unit,
    val openSub: (WelcomeSub) -> Unit,
    val goTo: (WelcomeStep) -> Unit,
    val finish: () -> Unit,
    val pickBackup: () -> Unit,
    val selectLanguage: (String) -> Unit,
    val consumePrimaryFocus: () -> Unit,
    val toggleProviderLanguage: (String) -> Unit,
    val toggleMediaType: (String) -> Unit,
    val openRepository: (String) -> Unit,
    val togglePlugin: (String) -> Unit,
    val selectPlugins: (Set<String>) -> Unit,
    val downloadSelected: (List<PluginItem>) -> Unit,
    val addRepository: (String?, String, (Result<RepositoryItem>) -> Unit) -> Unit,
    val removeRepository: (String) -> Unit,
)

/**
 * First-run flow. Shown by `TvApp` while HAS_DONE_SETUP is not true; [onFinished] is called after
 * Finish / Skip setup / Start watching have stored the flag.
 */
@Composable
fun WelcomeRoute(
    onFinished: () -> Unit,
    viewModel: WelcomeViewModel = viewModel(),
    extensionsViewModel: ExtensionsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val extensions by extensionsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity: Activity? = remember(context) { context.getActivity() }

    LaunchedEffect(extensionsViewModel, context) {
        extensionsViewModel.setupPrefsListener(context.applicationContext)
    }

    // Keep the plugin lists of the open repository fresh while extensions are being downloaded.
    val finished = state.download?.finished
    LaunchedEffect(finished, state.download == null) {
        state.openRepositoryUrl?.let { extensionsViewModel.refreshData(it) }
    }

    val actions = remember(viewModel, extensionsViewModel, activity) {
        WelcomeActions(
            next = viewModel::next,
            back = { viewModel.back() },
            startManual = viewModel::startManual,
            openSub = viewModel::openSub,
            goTo = viewModel::goTo,
            finish = {
                viewModel.markSetupDone()
                onFinished()
            },
            pickBackup = {
                if (activity != null) {
                    activity.restorePrompt()
                }
            },
            selectLanguage = { tag ->
                viewModel.setLanguage(tag)
                CommonActivity.setLocale(activity, tag)
                activity?.recreate()
            },
            consumePrimaryFocus = viewModel::consumePrimaryFocus,
            toggleProviderLanguage = viewModel::toggleProviderLanguage,
            toggleMediaType = viewModel::toggleMediaType,
            openRepository = { url ->
                extensionsViewModel.loadPluginsForRepo(url)
                viewModel.openRepository(url)
            },
            togglePlugin = viewModel::togglePlugin,
            selectPlugins = viewModel::selectPlugins,
            downloadSelected = { plugins -> activity?.let { viewModel.downloadSelected(it, plugins) } },
            addRepository = extensionsViewModel::addRepository,
            removeRepository = { url -> extensionsViewModel.removeRepository(url) {} },
        )
    }

    BackHandler(enabled = state.step != WelcomeStep.Start || state.sub != WelcomeSub.None) {
        viewModel.back()
    }

    WelcomeContent(
        state = state,
        extensions = extensions as? ExtensionsUiState.Ready,
        actions = actions,
        onLoadPlugins = extensionsViewModel::loadPluginsForRepo,
    )
}

@Composable
private fun WelcomeContent(
    state: WelcomeUiState,
    extensions: ExtensionsUiState.Ready?,
    actions: WelcomeActions,
    onLoadPlugins: (String) -> Unit,
) {
    when (state.step) {
        WelcomeStep.Start -> when (state.sub) {
            WelcomeSub.RestoreInfo -> RestoreInfoScreen(actions)
            WelcomeSub.RestoreDone -> RestoreDoneScreen(state, extensions, actions)
            WelcomeSub.RestoreFailed -> RestoreFailedScreen(state, actions)
            else -> StartScreen(actions)
        }
        WelcomeStep.Language -> LanguageStepScreen(state, actions)
        WelcomeStep.Repository -> RepositoryStepScreen(state, extensions, actions)
        WelcomeStep.Extensions -> ExtensionsStepScreen(state, extensions, actions, onLoadPlugins)
        WelcomeStep.ExtensionLanguages -> ExtensionLanguagesStepScreen(state, actions)
        WelcomeStep.Media -> MediaStepScreen(state, actions)
        WelcomeStep.Done -> DoneScreen(state, extensions, actions)
    }
}
