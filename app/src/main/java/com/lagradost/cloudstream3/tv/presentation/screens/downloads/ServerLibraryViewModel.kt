package com.lagradost.cloudstream3.tv.presentation.screens.downloads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.homeserver.HomeServerApi
import com.lagradost.cloudstream3.homeserver.HomeServerConfig
import com.lagradost.cloudstream3.homeserver.ServerLibraryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ServerLibraryUiState(
    val items: List<DownloadItemUiModel> = emptyList(),
)

/** Videos stored on the home server, shown in the downloads list next to the local ones. */
class ServerLibraryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ServerLibraryUiState())
    val uiState: StateFlow<ServerLibraryUiState> = _uiState.asStateFlow()

    private var appContext: Context? = null
    private var refreshJob: Job? = null

    fun bind(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        refreshJob = viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(RefreshIntervalMs)
            }
        }
    }

    fun delete(item: DownloadItemUiModel) {
        val context = appContext ?: return
        val id = item.serverItemId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val base = HomeServerConfig.baseUrl(context) ?: return@launch
            runCatching { HomeServerApi(base).delete(id) }
            refresh()
        }
    }

    private suspend fun refresh() {
        val context = appContext ?: return
        val base = HomeServerConfig.baseUrl(context)
        val items = if (base == null) {
            emptyList()
        } else {
            // Server unreachable: keep what is shown instead of flickering to empty.
            withContext(Dispatchers.IO) { runCatching { HomeServerApi(base).library() }.getOrNull() }
                ?.map(::toUiModel) ?: return
        }
        _uiState.value = ServerLibraryUiState(items)
    }

    private fun toUiModel(item: ServerLibraryItem): DownloadItemUiModel {
        val createdMs = item.createdAt * 1000
        return DownloadItemUiModel(
            id = "server_${item.id}",
            episodeId = item.episodeId ?: item.id.hashCode(),
            parentId = item.parentId ?: 0,
            title = item.title,
            episodeName = null,
            episodeNumber = item.episode,
            seasonNumber = item.season,
            description = null,
            posterUrl = item.poster,
            backdropUrl = item.poster,
            mediaType = if (item.season != null) DownloadMediaType.Series else DownloadMediaType.Movie,
            sourceUrl = item.url,
            apiName = item.apiName,
            state = DownloadState.Downloaded(fileSizeBytes = item.size),
            startedAtMillis = createdMs,
            updatedAtMillis = createdMs,
            downloadedAtMillis = createdMs,
            serverItemId = item.id,
        )
    }

    private companion object {
        const val RefreshIntervalMs = 20_000L
    }
}
