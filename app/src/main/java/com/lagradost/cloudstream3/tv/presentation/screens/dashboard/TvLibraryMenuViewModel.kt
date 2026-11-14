package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.syncproviders.SyncRepo
import com.lagradost.cloudstream3.tv.compat.home.PinnedLibraryLists
import com.lagradost.cloudstream3.tv.presentation.screens.library.LibraryListGroup
import com.lagradost.cloudstream3.tv.presentation.screens.library.LibraryListRow
import com.lagradost.cloudstream3.tv.presentation.screens.library.LibraryListsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.library.LibrarySectionUiState
import com.lagradost.cloudstream3.tv.presentation.screens.library.toSectionUiStates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

/** Stable across reloads (a section id carries its position), so a pinned list stays pinned. */
internal fun libraryMenuListKey(providerName: String, title: String) = "$providerName/$title"

private const val PreviewItemCount = 6

/**
 * Lists of every signed-in library account at once, for the Library screen and (the pinned ones)
 * the navigation menu. Loaded in the
 * background when the Dashboard starts (accounts in parallel) and again on library / account
 * reloads; moving around the menu never triggers a load.
 */
internal class TvLibraryMenuViewModel : ViewModel() {
    private val _providers = MutableStateFlow<List<ProviderLists>>(emptyList())
    val providers: StateFlow<List<ProviderLists>> = _providers.asStateFlow()

    private val pinnedKeys = MutableStateFlow(PinnedLibraryLists.read())

    /** The menu shows only pinned lists; a provider without one has no group. */
    val menuGroups: StateFlow<List<TvNavListGroup>> = combine(_providers, pinnedKeys) { providers, pinned ->
        providers.mapNotNull { provider ->
            val lists = provider.sections
                .map { libraryMenuListKey(provider.providerName, it.title) to it }
                .filter { (key, _) -> key in pinned }
                .map { (key, section) -> TvNavList(key, section.title, section.gridItems.size) }
            if (lists.isEmpty()) null
            else TvNavListGroup(provider.providerName, lists, provider.isLoading, provider.hasError)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val listsUiState: StateFlow<LibraryListsUiState> = combine(_providers, pinnedKeys) { providers, pinned ->
        val groups = providers.map { provider ->
            LibraryListGroup(
                providerName = provider.providerName,
                rows = provider.sections.map { section ->
                    val key = libraryMenuListKey(provider.providerName, section.title)
                    LibraryListRow(
                        key = key,
                        title = section.title,
                        count = section.gridItems.size,
                        pinned = key in pinned,
                        previewItems = section.previewItems.take(PreviewItemCount),
                    )
                },
            )
        }.filter { it.rows.isNotEmpty() }
        LibraryListsUiState(
            groups = groups,
            listCount = groups.sumOf { it.rows.size },
            pinnedCount = groups.sumOf { group -> group.rows.count { it.pinned } },
            isLoading = providers.any { it.isLoading },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LibraryListsUiState())

    /** Last focused row of the Library screen, so coming back from a list lands on it. */
    var lastFocusedListKey: String? = null

    /**
     * Row whose list was opened from the Library screen. Focus lands on the first row by itself when the
     * screen comes back, which would overwrite [lastFocusedListKey]; the opened row wins until it has focus again.
     */
    var openedListKey: String? = null

    private var loadJob: Job? = null

    private val reloadObserver: (Boolean) -> Unit = { force -> load(force) }
    private val accountObserver: (Boolean) -> Unit = {
        pinnedKeys.value = PinnedLibraryLists.read()
        load(force = true)
    }

    init {
        MainActivity.reloadLibraryEvent += reloadObserver
        MainActivity.reloadAccountEvent += accountObserver
        load(force = false)
    }

    fun section(listKey: String): LibrarySectionUiState? {
        return _providers.value.firstNotNullOfOrNull { provider ->
            provider.sections.firstOrNull { libraryMenuListKey(provider.providerName, it.title) == listKey }
        }
    }

    /** Pins / unpins a list; returns true when it is pinned afterwards. */
    fun togglePin(listKey: String): Boolean {
        val pinned = listKey !in pinnedKeys.value
        val updated = if (pinned) pinnedKeys.value + listKey else pinnedKeys.value - listKey
        pinnedKeys.value = updated
        PinnedLibraryLists.write(updated)
        return pinned
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
