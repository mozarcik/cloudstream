package com.lagradost.cloudstream3.tv.presentation.screens.welcome

import android.app.Activity
import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.compat.ExtensionsCompat
import com.lagradost.cloudstream3.tv.compat.PluginItem
import com.lagradost.cloudstream3.ui.settings.getCurrentLocale
import com.lagradost.cloudstream3.ui.setup.HAS_DONE_SETUP_KEY
import com.lagradost.cloudstream3.utils.AppContextUtils.getApiProviderLangSettings
import com.lagradost.cloudstream3.utils.BackupUtils
import com.lagradost.cloudstream3.utils.DataStoreHelper
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State and persistence of the first-run flow. Repositories and plugin lists come from
 * `ExtensionsViewModel` (combined in the route, the two never call each other); the choices of the
 * steps are written straight to the same preferences the Settings screens use.
 */
class WelcomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val localeKey = application.getString(R.string.locale_key)
    private val providerLangKey = application.getString(R.string.provider_lang_key)
    private val mediaKey = application.getString(R.string.prefer_media_type_key)

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    init {
        reloadChoices()
        // A result left over from a restore started elsewhere (Settings) is not for this flow.
        BackupUtils.consumeRestoreResult()
        observeRestore()
    }

    private fun reloadChoices() {
        val app = getApplication<Application>()
        // All media types except NSFW are selected by default (legacy setup); stored right away.
        val defaultMedia = TvType.entries.filter { it != TvType.NSFW }.map { it.ordinal.toString() }.toSet()
        val media = prefs.getStringSet(mediaKey, null)?.toSet() ?: defaultMedia.also { saveMedia(it) }
        _uiState.update {
            it.copy(
                languageTag = prefs.getString(localeKey, null) ?: getCurrentLocale(app),
                providerLanguages = app.getApiProviderLangSettings().toSet(),
                mediaTypes = media,
            )
        }
    }

    // --- navigation -------------------------------------------------------------------------

    fun startManual() = _uiState.update { it.copy(step = WelcomeStep.Language, sub = WelcomeSub.None) }

    fun openRestoreInfo() = _uiState.update { it.copy(sub = WelcomeSub.RestoreInfo) }

    fun openSub(sub: WelcomeSub) = _uiState.update { it.copy(sub = sub) }

    fun goTo(step: WelcomeStep) = _uiState.update { it.copy(step = step, sub = WelcomeSub.None) }

    fun next() {
        _uiState.update { state ->
            val next = when (state.step) {
                WelcomeStep.Start -> WelcomeStep.Language
                WelcomeStep.Language -> WelcomeStep.Repository
                WelcomeStep.Repository -> WelcomeStep.Extensions
                WelcomeStep.Extensions -> if (hasMultipleExtensionLanguages()) {
                    WelcomeStep.ExtensionLanguages
                } else {
                    WelcomeStep.Media
                }
                WelcomeStep.ExtensionLanguages -> WelcomeStep.Media
                WelcomeStep.Media, WelcomeStep.Done -> WelcomeStep.Done
            }
            state.copy(
                step = next,
                sub = WelcomeSub.None,
                hasExtensionLanguagesStep = hasMultipleExtensionLanguages(),
            )
        }
    }

    /** Back: closes a secondary screen first, then goes one step back. False on the start screen. */
    fun back(): Boolean {
        val state = _uiState.value
        if (state.sub != WelcomeSub.None) {
            _uiState.update {
                if (it.sub == WelcomeSub.RepositoryPlugins) closeRepository(it)
                else it.copy(sub = WelcomeSub.None)
            }
            return true
        }
        val previous = when (state.step) {
            WelcomeStep.Start -> return false
            WelcomeStep.Language -> WelcomeStep.Start
            WelcomeStep.Repository -> WelcomeStep.Language
            WelcomeStep.Extensions -> WelcomeStep.Repository
            WelcomeStep.ExtensionLanguages -> WelcomeStep.Extensions
            WelcomeStep.Media -> if (state.hasExtensionLanguagesStep) WelcomeStep.ExtensionLanguages else WelcomeStep.Extensions
            WelcomeStep.Done -> WelcomeStep.Media
        }
        _uiState.update { it.copy(step = previous) }
        return true
    }

    /** Legacy rule: the language step exists only when installed extensions offer several languages. */
    private fun hasMultipleExtensionLanguages(): Boolean =
        synchronized(APIHolder.apis) { APIHolder.apis.distinctBy { it.lang }.size > 1 }

    // --- choices ----------------------------------------------------------------------------

    fun setLanguage(tag: String) {
        prefs.edit { putString(localeKey, tag) }
        _uiState.update { it.copy(languageTag = tag, focusPrimary = true) }
    }

    fun consumePrimaryFocus() = _uiState.update { it.copy(focusPrimary = false) }

    fun toggleProviderLanguage(key: String) {
        val updated = _uiState.value.providerLanguages.toggled(key)
        prefs.edit { putStringSet(providerLangKey, updated) }
        _uiState.update { it.copy(providerLanguages = updated) }
    }

    fun toggleMediaType(key: String) {
        val updated = _uiState.value.mediaTypes.toggled(key)
        saveMedia(updated)
        _uiState.update { it.copy(mediaTypes = updated) }
    }

    private fun saveMedia(values: Set<String>) {
        prefs.edit { putStringSet(mediaKey, values) }
        // Regenerate the chosen home page, like the legacy setup.
        DataStoreHelper.currentHomePage = null
    }

    /** Called by Finish / Skip / Start watching; the route then leaves the flow. */
    fun markSetupDone() = setKey(HAS_DONE_SETUP_KEY, true)

    // --- repositories and extensions --------------------------------------------------------

    fun openRepository(url: String) = _uiState.update {
        it.copy(sub = WelcomeSub.RepositoryPlugins, openRepositoryUrl = url, lastRepositoryUrl = url, selectedPlugins = emptySet())
    }

    private fun closeRepository(state: WelcomeUiState) =
        state.copy(sub = WelcomeSub.None, openRepositoryUrl = null, selectedPlugins = emptySet())

    fun togglePlugin(internalName: String) =
        _uiState.update { it.copy(selectedPlugins = it.selectedPlugins.toggled(internalName)) }

    fun selectPlugins(internalNames: Set<String>) = _uiState.update { it.copy(selectedPlugins = internalNames) }

    /** Downloads one after another; the user picked these, nothing is ever downloaded automatically. */
    fun downloadSelected(activity: Activity, plugins: List<PluginItem>) {
        if (plugins.isEmpty() || _uiState.value.download != null) return
        viewModelScope.launch {
            plugins.forEachIndexed { index, plugin ->
                _uiState.update { it.copy(download = WelcomeDownload(plugins.size, index, plugin.name)) }
                ExtensionsCompat.downloadPlugin(activity, plugin).onFailure {
                    CommonActivity.showToast(R.string.plugin_download_failed)
                }
            }
            _uiState.update { it.copy(download = null, selectedPlugins = emptySet()) }
        }
    }

    // --- restore ----------------------------------------------------------------------------

    /** Result of the system picker restore; the holder outlives the recreate() after a success. */
    private fun observeRestore() {
        viewModelScope.launch {
            BackupUtils.restoreResult.filterNotNull().collect { result ->
                BackupUtils.consumeRestoreResult()
                when (result) {
                    is BackupUtils.RestoreResult.Success -> {
                        reloadChoices()
                        _uiState.update {
                            it.copy(step = WelcomeStep.Start, sub = WelcomeSub.RestoreDone, restoreFileName = result.fileName)
                        }
                    }
                    is BackupUtils.RestoreResult.Failure -> _uiState.update {
                        it.copy(step = WelcomeStep.Start, sub = WelcomeSub.RestoreFailed, restoreFileName = result.fileName)
                    }
                }
            }
        }
    }

    private fun Set<String>.toggled(key: String): Set<String> =
        if (key in this) this - key else this + key
}
