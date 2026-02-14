package com.lagradost.cloudstream3.tv.presentation.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.syncproviders.SyncRepo
import com.lagradost.cloudstream3.tv.compat.home.SearchResponseMapper.toMediaItemCompat
import com.lagradost.cloudstream3.ui.library.ListSorting
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.DataStoreHelper.currentAccount
import com.lagradost.cloudstream3.utils.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private const val LAST_SYNC_API_KEY = "last_sync_api"
private const val DEFAULT_LIBRARY_ERROR_MESSAGE = "Unable to fetch library"
private const val PRIORITY_FAVORITES = 0
private const val PRIORITY_WATCHING = 1
private const val PRIORITY_PLAN_TO_WATCH = 2
private const val PRIORITY_DEFAULT = 3

data class LibraryScreenUiState(
    val isLoading: Boolean = true,
    val currentApiName: String = "",
    val sections: List<LibrarySectionUiState> = emptyList(),
    val errorMessage: String? = null,
    val isAnySyncApiAvailable: Boolean = true,
)

class LibraryViewModel : ViewModel() {
    private val availableSyncApis: List<SyncRepo>
        get() = AccountManager.syncApis.filter { it.isAvailable }

    private var currentSyncApi: SyncRepo? = null
        set(value) {
            field = value
            setKey("$currentAccount/$LAST_SYNC_API_KEY", value?.name)
        }

    private val _uiState = MutableStateFlow(
        LibraryScreenUiState(
            currentApiName = currentSyncApi?.name.orEmpty(),
            isAnySyncApiAvailable = availableSyncApis.isNotEmpty()
        )
    )
    val uiState = _uiState.asStateFlow()

    private val reloadLibraryObserver: (Boolean) -> Unit = { forceReload ->
        reloadPages(forceReload)
    }

    init {
        currentSyncApi = resolveCurrentSyncApi()
        _uiState.update { state ->
            state.copy(
                currentApiName = currentSyncApi?.name.orEmpty(),
                isAnySyncApiAvailable = availableSyncApis.isNotEmpty()
            )
        }
        MainActivity.reloadLibraryEvent += reloadLibraryObserver
        reloadPages(forceReload = false)
    }

    fun reloadPages(forceReload: Boolean = true) {
        val syncApi = resolveCurrentSyncApi().also { resolvedApi ->
            currentSyncApi = resolvedApi
        }

        if (
            !forceReload &&
            _uiState.value.sections.isNotEmpty() &&
            syncApi?.requireLibraryRefresh != true
        ) {
            return
        }

        if (syncApi == null) {
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    currentApiName = "",
                    sections = emptyList(),
                    errorMessage = null,
                    isAnySyncApiAvailable = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = true,
                    currentApiName = syncApi.name,
                    errorMessage = null,
                    isAnySyncApiAvailable = true
                )
            }

            val libraryResult = withContext(Dispatchers.IO) {
                syncApi.library()
            }
            val error = libraryResult.exceptionOrNull()
            if (error != null) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        sections = emptyList(),
                        errorMessage = error.message ?: DEFAULT_LIBRARY_ERROR_MESSAGE,
                        isAnySyncApiAvailable = true
                    )
                }
                return@launch
            }

            val library = libraryResult.getOrNull()
            if (library == null) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        sections = emptyList(),
                        errorMessage = DEFAULT_LIBRARY_ERROR_MESSAGE,
                        isAnySyncApiAvailable = true
                    )
                }
                return@launch
            }

            syncApi.requireLibraryRefresh = false

            val pages = library.allLibraryLists.map { list ->
                SyncAPI.Page(list.name, list.items)
            }

            val desiredSortingMethod = ListSorting.entries.getOrNull(DataStoreHelper.librarySortingMode)
            val sortingMethod = if (
                desiredSortingMethod != null &&
                library.supportedListSorting.contains(desiredSortingMethod)
            ) {
                desiredSortingMethod
            } else {
                ListSorting.Query
            }

            pages.forEach { page ->
                page.sort(sortingMethod, null)
            }

            val orderedPages = pages
                .withIndex()
                .sortedWith(
                    compareBy<IndexedValue<SyncAPI.Page>>(
                        { pagePriority(it.value.title) },
                        { it.index }
                    )
                )
                .map { it.value }

            val context = CloudStreamApp.context
            val sections = orderedPages.mapIndexed { index, page ->
                val resolvedTitle = page.title.asStringNull(context) ?: page.title.toString()
                LibrarySectionUiState(
                    id = "${resolvedTitle}_${index}",
                    title = resolvedTitle,
                    items = page.items.map { item -> item.toMediaItemCompat() }
                )
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    currentApiName = syncApi.name,
                    sections = sections,
                    errorMessage = null,
                    isAnySyncApiAvailable = true
                )
            }
        }
    }

    private fun resolveCurrentSyncApi(): SyncRepo? {
        val apis = availableSyncApis
        if (apis.isEmpty()) return null

        val currentName = currentSyncApi?.name
        val currentApi = currentName?.let { name ->
            apis.firstOrNull { api -> api.name == name }
        }
        if (currentApi != null) return currentApi

        val lastSelection = getKey<String>("$currentAccount/$LAST_SYNC_API_KEY")
        return apis.firstOrNull { api -> api.name == lastSelection } ?: apis.firstOrNull()
    }

    private fun pagePriority(title: UiText): Int {
        val stringRes = (title as? UiText.StringResource)?.resId
        return when (stringRes) {
            R.string.favorites_list_name -> PRIORITY_FAVORITES
            R.string.type_watching -> PRIORITY_WATCHING
            R.string.type_plan_to_watch -> PRIORITY_PLAN_TO_WATCH
            else -> fallbackPriority(title)
        }
    }

    private fun fallbackPriority(title: UiText): Int {
        val normalized = title
            .asStringNull(CloudStreamApp.context)
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?: return PRIORITY_DEFAULT

        return when (normalized) {
            "favorites", "ulubione" -> PRIORITY_FAVORITES
            "watching", "w trakcie" -> PRIORITY_WATCHING
            "plan to watch", "planowane" -> PRIORITY_PLAN_TO_WATCH
            else -> PRIORITY_DEFAULT
        }
    }

    override fun onCleared() {
        MainActivity.reloadLibraryEvent -= reloadLibraryObserver
        super.onCleared()
    }
}
