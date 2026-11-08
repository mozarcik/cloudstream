package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.syncproviders.SyncRepo
import com.lagradost.cloudstream3.tv.presentation.screens.library.LibrarySectionUiState
import com.lagradost.cloudstream3.tv.presentation.screens.library.toSectionUiStates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
internal data class ProviderLists(
    val providerName: String,
    val sections: List<LibrarySectionUiState> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
)

internal fun libraryMenuListKey(providerName: String, sectionId: String) = "$providerName/$sectionId"

/**
 * Lists of every signed-in library account at once, for the navigation menu. Loaded in the
 * background when the Dashboard starts (accounts in parallel) and again on library / account
 * reloads; moving around the menu never triggers a load.
 */
internal class TvLibraryMenuViewModel : ViewModel() {
    private val _providers = MutableStateFlow<List<ProviderLists>>(emptyList())
    val providers: StateFlow<List<ProviderLists>> = _providers.asStateFlow()

    private var loadJob: Job? = null

    private val reloadObserver: (Boolean) -> Unit = { force -> load(force) }
    private val accountObserver: (Boolean) -> Unit = { load(force = true) }

    init {
        MainActivity.reloadLibraryEvent += reloadObserver
        MainActivity.reloadAccountEvent += accountObserver
        load(force = false)
    }

    fun section(listKey: String): LibrarySectionUiState? {
        return _providers.value.firstNotNullOfOrNull { provider ->
            provider.sections.firstOrNull { libraryMenuListKey(provider.providerName, it.id) == listKey }
        }
    }

    private fun load(force: Boolean) {
        // Local (always available) goes last, below the signed-in accounts.
        val localName = AccountManager.localListApi.name
        val apis = AccountManager.syncApis.filter { it.isAvailable }.sortedBy { it.name == localName }
        val previous = _providers.value.associateBy { it.providerName }
        if (!force && apis.isNotEmpty() && apis.all { previous[it.name]?.isLoading == false && !it.requireLibraryRefresh }) {
            return
        }

        _providers.value = apis.map { api ->
            // Keep what is already known while it reloads, so the menu does not flash empty.
            ProviderLists(api.name, previous[api.name]?.sections.orEmpty(), isLoading = true)
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            apis.forEach { api -> launch { loadProvider(api) } }
        }
    }

    private suspend fun loadProvider(api: SyncRepo) {
        val result = withContext(Dispatchers.IO) { api.library() }
        val library = result.getOrNull()
        val sections = library?.also { api.requireLibraryRefresh = false }?.toSectionUiStates().orEmpty()
        update(api.name) {
            ProviderLists(
                providerName = api.name,
                sections = sections,
                isLoading = false,
                hasError = library == null,
            )
        }
    }

    private fun update(providerName: String, transform: (ProviderLists) -> ProviderLists) {
        _providers.update { list ->
            list.map { if (it.providerName == providerName) transform(it) else it }
        }
    }

    override fun onCleared() {
        MainActivity.reloadLibraryEvent -= reloadObserver
        MainActivity.reloadAccountEvent -= accountObserver
        super.onCleared()
    }
}
